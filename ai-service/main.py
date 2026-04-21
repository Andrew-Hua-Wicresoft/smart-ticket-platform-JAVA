"""
Phase 3 AI service boundary for the smart ticket platform.

The Java platform now treats this service as the single internal AI boundary:
- local embeddings and pgvector search
- ticket analysis and follow-up generation
- KB draft generation
- embedding reindex for knowledge-base content

Provider/model compatibility is kept here so the Java platform does not need
to know which upstream AI vendor is configured.
"""
from __future__ import annotations

import logging
import os
import time
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer
from sqlalchemy import create_engine, text

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MODEL_NAME = os.getenv("EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
CACHE_DIR = os.getenv("MODEL_CACHE_DIR", "./model-cache")
AI_PROVIDER = (os.getenv("AI_PROVIDER", "anthropic") or "anthropic").strip().lower()
AI_MODEL = os.getenv("AI_MODEL", "claude-sonnet-4-20250514")
AI_API_KEY = (os.getenv("AI_API_KEY") or os.getenv("CLAUDE_API_KEY", "")).strip()
AI_BASE_URL = os.getenv("AI_BASE_URL", "").strip()
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite+pysqlite:///:memory:")
MAX_EMBED_CHARS = 1000

app = FastAPI(title="智能工单 AI Service", version="0.2.0")

logger.info("Loading embedding model: %s", MODEL_NAME)
model = SentenceTransformer(MODEL_NAME, cache_folder=CACHE_DIR)
logger.info("Model loaded with dimension %s", model.get_sentence_embedding_dimension())

engine = create_engine(
    DATABASE_URL,
    future=True,
    connect_args={"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {},
)


class EmbedRequest(BaseModel):
    text: str


class EmbedResponse(BaseModel):
    embedding: list[float]
    dimension: int
    latency_ms: float


class SearchRequest(BaseModel):
    query: str
    top_k: int = Field(default=5, ge=1, le=20)


class SearchResult(BaseModel):
    source_id: str
    title: str
    content: str
    similarity: float


class TextGenerationRequest(BaseModel):
    title: str | None = None
    description: str
    context: str | None = None


class AnalyzeTicketRequest(BaseModel):
    title: str
    description: str


class ReindexRequest(BaseModel):
    entity_type: str = "knowledge-base"
    limit: int = Field(default=100, ge=1, le=5000)


class AiResponse(BaseModel):
    provider: str
    model: str
    mode: str
    degraded: bool
    content: str


class AnalyzeTicketResponse(BaseModel):
    provider: str
    model: str
    degraded: bool
    summary: str
    suggested_priority: str
    priority_reason: str
    follow_up_questions: list[str]


class ReindexResponse(BaseModel):
    provider: str
    model: str
    degraded: bool
    accepted: bool
    entity_type: str
    limit: int
    processed_count: int


def _embed_text(value: str) -> EmbedResponse:
    if not value or not value.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")

    start = time.time()
    embedding = model.encode(
        value.strip()[:MAX_EMBED_CHARS],
        normalize_embeddings=True,
    )
    latency_ms = (time.time() - start) * 1000

    return EmbedResponse(
        embedding=embedding.tolist(),
        dimension=len(embedding),
        latency_ms=round(latency_ms, 1),
    )


def _provider_payload(mode: str, content: str, degraded: bool | None = None) -> AiResponse:
    return AiResponse(
        provider=AI_PROVIDER,
        model=AI_MODEL,
        mode=mode,
        degraded=(not bool(AI_API_KEY)) if degraded is None else degraded,
        content=content,
    )


def _default_questions(title: str | None, description: str) -> list[str]:
    subject = title.strip() if title and title.strip() else "该问题"
    has_access_issue = any(keyword in description for keyword in ("无法登录", "登录失败", "权限", "访问被拒绝"))
    return [
        f"{subject} 从什么时候开始出现，最近是否有发布、配置或权限变更？",
        "请补充报错信息、日志片段、截图以及受影响范围。",
        "这个问题是持续存在还是间歇性出现？",
    ] if not has_access_issue else [
        f"{subject} 涉及哪些账号或用户组，是否全部无法访问？",
        "请补充报错信息、账号权限变更时间和系统截图。",
        "最近是否做过密码重置、角色调整或 SSO 配置变更？",
    ]


def _priority_from_text(description: str) -> str:
    high_keywords = ("全员", "生产", "中断", "阻塞", "无法登录", "无法访问", "核心系统", "停机")
    low_keywords = ("咨询", "建议", "优化", "申请", "开通", "新增")
    if any(keyword in description for keyword in high_keywords):
        return "HIGH"
    if any(keyword in description for keyword in low_keywords):
        return "LOW"
    return "MEDIUM"


def _priority_reason(priority: str, description: str) -> str:
    if priority == "HIGH":
        return "问题疑似影响业务连续性或较大范围用户，需要优先处理。"
    if priority == "LOW":
        return "更接近服务请求或非阻断性需求，可按低优先级排队。"
    if any(keyword in description for keyword in ("失败", "异常", "报错", "无法")):
        return "问题会影响单人或局部业务处理，建议按中优先级跟进。"
    return "当前信息未显示大范围影响，先按中优先级处理。"


def _vector_literal(embedding: list[float]) -> str:
    return "[" + ",".join(str(value) for value in embedding) + "]"


def _postgres_ready() -> bool:
    return engine.dialect.name.startswith("postgresql")


def _search_knowledge_base(query: str, top_k: int) -> list[SearchResult]:
    if not _postgres_ready():
        return []

    try:
        vector = _vector_literal(_embed_text(query).embedding)
        sql = text(
            """
            SELECT id, title, content,
                   1 - (content_embedding <=> cast(:vec as vector)) AS similarity
            FROM knowledge_base
            WHERE status = 'PUBLISHED'
              AND content_embedding IS NOT NULL
            ORDER BY content_embedding <=> cast(:vec as vector)
            LIMIT :top_k
            """
        )
        with engine.connect() as connection:
            rows = connection.execute(sql, {"vec": vector, "top_k": top_k}).mappings().all()
    except Exception as exc:
        logger.warning("Knowledge-base search degraded: %s", exc)
        return []

    results: list[SearchResult] = []
    for row in rows:
        similarity = float(row["similarity"] or 0.0)
        if similarity <= 0.3:
            continue
        results.append(
            SearchResult(
                source_id=str(row["id"]),
                title=row["title"],
                content=row["content"],
                similarity=similarity,
            )
        )
    return results


def _reindex_knowledge_base(limit: int) -> int:
    if not _postgres_ready():
        return 0

    select_sql = text(
        """
        SELECT id, content
        FROM knowledge_base
        WHERE content IS NOT NULL
        ORDER BY id
        LIMIT :limit
        """
    )
    update_sql = text(
        """
        UPDATE knowledge_base
        SET content_embedding = cast(:vec as vector)
        WHERE id = :id
        """
    )

    processed = 0
    try:
        with engine.begin() as connection:
            rows = connection.execute(select_sql, {"limit": limit}).mappings().all()
            for row in rows:
                vector = _vector_literal(_embed_text(row["content"]).embedding)
                connection.execute(update_sql, {"vec": vector, "id": row["id"]})
                processed += 1
    except Exception as exc:
        logger.warning("Knowledge-base reindex degraded: %s", exc)
        return 0

    return processed


def _touch_database() -> str:
    try:
        with engine.connect() as connection:
            return str(connection.execute(text("select 1")).scalar_one())
    except Exception as exc:
        return f"unavailable:{exc.__class__.__name__}"


@app.post("/embed", response_model=EmbedResponse)
def embed(request: EmbedRequest) -> EmbedResponse:
    return _embed_text(request.text)


@app.post("/internal/ai/embed", response_model=EmbedResponse)
def internal_embed(request: EmbedRequest) -> EmbedResponse:
    return _embed_text(request.text)


@app.post("/internal/ai/search", response_model=list[SearchResult])
def internal_search(request: SearchRequest) -> list[SearchResult]:
    query = request.query.strip()
    if not query:
        raise HTTPException(status_code=400, detail="Query cannot be empty")
    return _search_knowledge_base(query, request.top_k)


@app.post("/internal/ai/follow-up-questions", response_model=AiResponse)
def follow_up_questions(request: TextGenerationRequest) -> AiResponse:
    questions = "\n".join(
        f"{index}. {question}"
        for index, question in enumerate(_default_questions(request.title, request.description), start=1)
    )
    return _provider_payload("follow-up-questions", questions)


@app.post("/internal/ai/refine", response_model=AiResponse)
def refine(request: TextGenerationRequest) -> AiResponse:
    summary = (
        f"标题：{request.title or '未提供'}\n"
        f"问题描述：{request.description.strip()}\n"
        "建议补充影响范围、复现步骤、错误截图、日志片段和最近变更。"
    )
    return _provider_payload("refine", summary)


@app.post("/internal/ai/enhance-description", response_model=AiResponse)
def enhance_description(request: TextGenerationRequest) -> AiResponse:
    content = (
        f"{request.description.strip()}\n\n"
        "补充模板：影响用户/系统、发生时间、复现步骤、错误信息、已尝试处理、最近变更。"
    )
    return _provider_payload("enhance-description", content)


@app.post("/internal/ai/suggest", response_model=AiResponse)
def suggest(request: TextGenerationRequest) -> AiResponse:
    history = request.context.strip() if request.context else "暂无相似历史案例。"
    content = (
        f"建议先围绕《{request.title or '当前工单'}》执行以下排查：\n"
        "1. 复核最近变更、监控与错误日志。\n"
        "2. 检查依赖服务、网络与权限状态。\n"
        "3. 对照历史知识库或相似案例验证处理步骤。\n\n"
        f"历史参考：\n{history}"
    )
    return _provider_payload("suggest", content)


@app.post("/internal/ai/analyze-ticket", response_model=AnalyzeTicketResponse)
def analyze_ticket(request: AnalyzeTicketRequest) -> AnalyzeTicketResponse:
    priority = _priority_from_text(request.description)
    return AnalyzeTicketResponse(
        provider=AI_PROVIDER,
        model=AI_MODEL,
        degraded=not bool(AI_API_KEY),
        summary=f"{request.title}: {request.description[:120]}",
        suggested_priority=priority,
        priority_reason=_priority_reason(priority, request.description),
        follow_up_questions=_default_questions(request.title, request.description),
    )


@app.post("/internal/ai/kb-draft-generate", response_model=AiResponse)
def kb_draft_generate(request: TextGenerationRequest) -> AiResponse:
    resolution_notes = request.context.strip() if request.context else "请补充最终解决步骤。"
    content = (
        "## 问题描述\n"
        f"{request.description.strip()}\n\n"
        "## 解决方案\n"
        f"{resolution_notes}\n\n"
        "## 注意事项\n"
        "请补充根因、影响范围、验证步骤和是否需要回滚。"
    )
    return _provider_payload("kb-draft-generate", content)


@app.post("/internal/ai/reindex", response_model=ReindexResponse)
def reindex(request: ReindexRequest) -> ReindexResponse:
    if request.entity_type != "knowledge-base":
        raise HTTPException(status_code=400, detail="Only knowledge-base reindex is supported")

    processed_count = _reindex_knowledge_base(request.limit)
    return ReindexResponse(
        provider=AI_PROVIDER,
        model=AI_MODEL,
        degraded=not _postgres_ready(),
        accepted=True,
        entity_type=request.entity_type,
        limit=request.limit,
        processed_count=processed_count,
    )


@app.get("/health")
async def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "provider": AI_PROVIDER,
        "model": AI_MODEL,
        "base_url": AI_BASE_URL or None,
        "embedding_model": MODEL_NAME,
        "dimension": model.get_sentence_embedding_dimension(),
        "database_ping": _touch_database(),
    }
