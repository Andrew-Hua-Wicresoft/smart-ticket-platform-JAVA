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

import json
import logging
import os
import time
import urllib.error
import urllib.request
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer
from sqlalchemy import create_engine, text

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MODEL_NAME = os.getenv("EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
CACHE_DIR = os.getenv("MODEL_CACHE_DIR", "./model-cache")
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite+pysqlite:///:memory:")
MAX_EMBED_CHARS = 1000

LEGACY_LLM_PROVIDER = os.getenv("LLM_PROVIDER", "").strip().lower()
LEGACY_LLM_MODEL = os.getenv("LLM_MODEL", "").strip()
LEGACY_LLM_API_KEY = os.getenv("LLM_API_KEY", "").strip()
LEGACY_LLM_BASE_URL = os.getenv("LLM_BASE_URL", "").strip()


def _first_non_empty(*values: str) -> str:
    for value in values:
        if value and value.strip():
            return value.strip()
    return ""


def _float_env(name: str, default: float) -> float:
    raw_value = os.getenv(name, "").strip()
    if not raw_value:
        return default
    try:
        return float(raw_value)
    except ValueError:
        logger.warning("Invalid %s=%s; using default %s", name, raw_value, default)
        return default


def _default_model_for_provider(provider: str) -> str:
    return {
        "deepseek": "deepseek-v4-pro",
        "openai": "gpt-4.1-mini",
        "openai-compatible": "gpt-4.1-mini",
        "anthropic": "claude-sonnet-4-20250514",
    }.get(provider, "deepseek-v4-pro")


def _default_base_url_for_provider(provider: str) -> str:
    return {
        "deepseek": "https://api.deepseek.com",
        "openai": "https://api.openai.com/v1",
        "openai-compatible": "",
        "anthropic": "",
    }.get(provider, "")


AI_PROVIDER = _first_non_empty(os.getenv("AI_PROVIDER", ""), LEGACY_LLM_PROVIDER, "anthropic").lower()
AI_MODEL = _first_non_empty(os.getenv("AI_MODEL", ""), LEGACY_LLM_MODEL, _default_model_for_provider(AI_PROVIDER))
AI_API_KEY = _first_non_empty(os.getenv("AI_API_KEY", ""), LEGACY_LLM_API_KEY, os.getenv("CLAUDE_API_KEY", ""))
AI_BASE_URL = _first_non_empty(os.getenv("AI_BASE_URL", ""), LEGACY_LLM_BASE_URL, _default_base_url_for_provider(AI_PROVIDER))
AI_REQUEST_TIMEOUT_SECONDS = _float_env("AI_REQUEST_TIMEOUT_SECONDS", 45.0)

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


def _supports_chat_completion(provider: str) -> bool:
    return provider in {"deepseek", "openai", "openai-compatible"}


def _resolve_chat_completions_url() -> str:
    base = AI_BASE_URL.rstrip("/")
    if base.endswith("/chat/completions"):
        return base
    if base.endswith("/v1"):
        return base + "/chat/completions"
    return base + "/chat/completions"


def _extract_openai_compatible_text(payload: dict[str, Any]) -> str | None:
    choices = payload.get("choices")
    if not isinstance(choices, list) or not choices:
        return None
    first = choices[0]
    if not isinstance(first, dict):
        return None
    message = first.get("message")
    if not isinstance(message, dict):
        return None
    content = message.get("content")
    return content if isinstance(content, str) else None


def _call_openai_compatible_chat(system_prompt: str,
                                 user_prompt: str,
                                 *,
                                 json_output: bool = False) -> str | None:
    if not _supports_chat_completion(AI_PROVIDER) or not AI_API_KEY or not AI_BASE_URL:
        return None

    body: dict[str, Any] = {
        "model": AI_MODEL,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
    }
    if json_output:
        body["response_format"] = {"type": "json_object"}

    request = urllib.request.Request(
        _resolve_chat_completions_url(),
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {AI_API_KEY}",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=AI_REQUEST_TIMEOUT_SECONDS) as response:
            payload = json.loads(response.read().decode("utf-8"))
            return _extract_openai_compatible_text(payload)
    except urllib.error.HTTPError as exc:
        logger.warning("AI provider HTTP error [%s]: %s", AI_PROVIDER, exc.read().decode("utf-8", errors="ignore"))
    except Exception as exc:
        logger.warning("AI provider call degraded [%s]: %s", AI_PROVIDER, exc)
    return None


def _generate_text(mode: str,
                   system_prompt: str,
                   user_prompt: str,
                   fallback: str) -> AiResponse:
    content = _call_openai_compatible_chat(system_prompt, user_prompt)
    if content and content.strip():
        return _provider_payload(mode, content.strip(), degraded=False)
    return _provider_payload(mode, fallback, degraded=True)


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
    fallback_questions = "\n".join(
        f"{index}. {question}"
        for index, question in enumerate(_default_questions(request.title, request.description), start=1)
    )
    return _generate_text(
        "follow-up-questions",
        "你是 IT 服务台助手。请基于工单标题和描述生成 2-3 个中文追问，每个问题一行，编号输出。",
        f"标题：{request.title or '未提供'}\n描述：{request.description.strip()}",
        fallback_questions,
    )


@app.post("/internal/ai/refine", response_model=AiResponse)
def refine(request: TextGenerationRequest) -> AiResponse:
    fallback_summary = (
        f"标题：{request.title or '未提供'}\n"
        f"问题描述：{request.description.strip()}\n"
        "建议补充影响范围、复现步骤、错误截图、日志片段和最近变更。"
    )
    return _generate_text(
        "refine",
        "你是 IT 工单辅助助手。请把用户描述整理成更适合工程师处理的中文问题摘要，并指出还缺哪些关键信息。",
        f"标题：{request.title or '未提供'}\n描述：{request.description.strip()}",
        fallback_summary,
    )


@app.post("/internal/ai/enhance-description", response_model=AiResponse)
def enhance_description(request: TextGenerationRequest) -> AiResponse:
    fallback_content = (
        f"{request.description.strip()}\n\n"
        "补充模板：影响用户/系统、发生时间、复现步骤、错误信息、已尝试处理、最近变更。"
    )
    return _generate_text(
        "enhance-description",
        "你是 IT 工单描述增强助手。请用中文把原始描述整理成更完整、结构化的工单描述。",
        f"标题：{request.title or '未提供'}\n描述：{request.description.strip()}",
        fallback_content,
    )


@app.post("/internal/ai/suggest", response_model=AiResponse)
def suggest(request: TextGenerationRequest) -> AiResponse:
    history = request.context.strip() if request.context else "暂无相似历史案例。"
    fallback_content = (
        f"建议先围绕《{request.title or '当前工单'}》执行以下排查：\n"
        "1. 复核最近变更、监控与错误日志。\n"
        "2. 检查依赖服务、网络与权限状态。\n"
        "3. 对照历史知识库或相似案例验证处理步骤。\n\n"
        f"历史参考：\n{history}"
    )
    return _generate_text(
        "suggest",
        "你是资深 IT 支持工程师。请根据工单信息和历史参考，给出清晰、可执行的中文处理建议。",
        f"标题：{request.title or '未提供'}\n描述：{request.description.strip()}\n\n历史参考：\n{history}",
        fallback_content,
    )


@app.post("/internal/ai/analyze-ticket", response_model=AnalyzeTicketResponse)
def analyze_ticket(request: AnalyzeTicketRequest) -> AnalyzeTicketResponse:
    fallback_priority = _priority_from_text(request.description)
    fallback_response = AnalyzeTicketResponse(
        provider=AI_PROVIDER,
        model=AI_MODEL,
        degraded=not bool(AI_API_KEY),
        summary=f"{request.title}: {request.description[:120]}",
        suggested_priority=fallback_priority,
        priority_reason=_priority_reason(fallback_priority, request.description),
        follow_up_questions=_default_questions(request.title, request.description),
    )

    content = _call_openai_compatible_chat(
        "你是 IT 工单分析器。请严格输出 JSON，对字段 summary、suggested_priority、priority_reason、follow_up_questions 赋值。"
        "suggested_priority 只能是 HIGH、MEDIUM、LOW。",
        (
            f"标题：{request.title}\n"
            f"描述：{request.description}\n"
            "返回 JSON 结构："
            "{\"summary\":\"...\",\"suggested_priority\":\"HIGH|MEDIUM|LOW\","
            "\"priority_reason\":\"...\",\"follow_up_questions\":[\"...\",\"...\"]}"
        ),
        json_output=True,
    )
    if not content:
        return fallback_response

    try:
        payload = json.loads(content)
        priority = str(payload.get("suggested_priority", fallback_priority)).upper()
        if priority not in {"HIGH", "MEDIUM", "LOW"}:
            priority = fallback_priority
        questions = payload.get("follow_up_questions")
        if not isinstance(questions, list) or not questions:
            questions = fallback_response.follow_up_questions
        else:
            questions = [str(item).strip() for item in questions if str(item).strip()][:3] or fallback_response.follow_up_questions

        return AnalyzeTicketResponse(
            provider=AI_PROVIDER,
            model=AI_MODEL,
            degraded=False,
            summary=str(payload.get("summary") or fallback_response.summary),
            suggested_priority=priority,
            priority_reason=str(payload.get("priority_reason") or _priority_reason(priority, request.description)),
            follow_up_questions=questions,
        )
    except Exception as exc:
        logger.warning("Analyze-ticket JSON parse degraded: %s", exc)
        return fallback_response


@app.post("/internal/ai/kb-draft-generate", response_model=AiResponse)
def kb_draft_generate(request: TextGenerationRequest) -> AiResponse:
    resolution_notes = request.context.strip() if request.context else "请补充最终解决步骤。"
    fallback_content = (
        "## 问题描述\n"
        f"{request.description.strip()}\n\n"
        "## 解决方案\n"
        f"{resolution_notes}\n\n"
        "## 注意事项\n"
        "请补充根因、影响范围、验证步骤和是否需要回滚。"
    )
    return _generate_text(
        "kb-draft-generate",
        "你是 IT 知识库作者。请基于工单标题、问题描述和解决方案，生成结构化中文知识库草稿，包含问题描述、解决方案、注意事项。",
        f"标题：{request.title or '未提供'}\n问题描述：{request.description.strip()}\n解决方案：{resolution_notes}",
        fallback_content,
    )


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
        "request_timeout_seconds": AI_REQUEST_TIMEOUT_SECONDS,
        "embedding_model": MODEL_NAME,
        "dimension": model.get_sentence_embedding_dimension(),
        "database_ping": _touch_database(),
    }
