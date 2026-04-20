"""
Phase 1 AI service foundation for the smart ticket platform.

The service keeps the current local embedding capability and exposes
internal AI endpoints that the Java platform can call later without
changing the public API boundary again.
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
AI_PROVIDER = os.getenv("AI_PROVIDER", "anthropic")
AI_MODEL = os.getenv("AI_MODEL", "claude-sonnet-4-20250514")
AI_API_KEY = os.getenv("AI_API_KEY") or os.getenv("CLAUDE_API_KEY", "")
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite+pysqlite:///:memory:")

app = FastAPI(title="智能工单 AI Service", version="0.1.0")

logger.info("Loading embedding model: %s", MODEL_NAME)
model = SentenceTransformer(MODEL_NAME, cache_folder=CACHE_DIR)
logger.info("Model loaded with dimension %s", model.get_sentence_embedding_dimension())

# Phase 1 only verifies SQLAlchemy wiring. We do not persist AI tasks yet.
engine = create_engine(DATABASE_URL, future=True)


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
    follow_up_questions: list[str]


class ReindexResponse(BaseModel):
    provider: str
    model: str
    degraded: bool
    accepted: bool
    entity_type: str
    limit: int


def _embed_text(value: str) -> EmbedResponse:
    if not value or not value.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")

    start = time.time()
    embedding = model.encode(value, normalize_embeddings=True)
    latency_ms = (time.time() - start) * 1000

    return EmbedResponse(
        embedding=embedding.tolist(),
        dimension=len(embedding),
        latency_ms=round(latency_ms, 1),
    )


def _provider_payload(mode: str, content: str) -> AiResponse:
    degraded = not bool(AI_API_KEY.strip())
    return AiResponse(
        provider=AI_PROVIDER,
        model=AI_MODEL,
        mode=mode,
        degraded=degraded,
        content=content,
    )


def _default_questions(title: str | None, description: str) -> list[str]:
    subject = title or "该问题"
    return [
        f"{subject} 从什么时候开始出现，是否最近有变更？",
        "请补充报错信息、截图或影响范围。",
        "这个问题是持续存在还是间歇性出现？",
    ]


def _touch_database() -> str:
    with engine.connect() as connection:
        return str(connection.execute(text("select 1")).scalar_one())


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
    return []


@app.post("/internal/ai/follow-up-questions", response_model=AiResponse)
def follow_up_questions(request: TextGenerationRequest) -> AiResponse:
    questions = "\n".join(f"{index}. {question}" for index, question in enumerate(
        _default_questions(request.title, request.description), start=1
    ))
    return _provider_payload("follow-up-questions", questions)


@app.post("/internal/ai/refine", response_model=AiResponse)
def refine(request: TextGenerationRequest) -> AiResponse:
    summary = (
        f"标题：{request.title or '未提供'}\n"
        f"问题描述：{request.description.strip()}\n"
        "建议补充影响范围、复现步骤、报错截图和最近变更。"
    )
    return _provider_payload("refine", summary)


@app.post("/internal/ai/enhance-description", response_model=AiResponse)
def enhance_description(request: TextGenerationRequest) -> AiResponse:
    content = (
        f"{request.description.strip()}\n\n"
        "补充模板：影响用户/系统、发生时间、复现步骤、错误信息、已尝试处理。"
    )
    return _provider_payload("enhance-description", content)


@app.post("/internal/ai/suggest", response_model=AiResponse)
def suggest(request: TextGenerationRequest) -> AiResponse:
    content = (
        f"基于标题《{request.title or '未命名工单'}》的兜底建议："
        "先核对最近变更、日志、依赖服务状态，再结合知识库和历史工单确认处置步骤。"
    )
    return _provider_payload("suggest", content)


@app.post("/internal/ai/analyze-ticket", response_model=AnalyzeTicketResponse)
def analyze_ticket(request: AnalyzeTicketRequest) -> AnalyzeTicketResponse:
    priority = "HIGH" if any(keyword in request.description for keyword in ("中断", "无法登录", "全员", "阻塞")) else "MEDIUM"
    return AnalyzeTicketResponse(
        provider=AI_PROVIDER,
        model=AI_MODEL,
        degraded=not bool(AI_API_KEY.strip()),
        summary=f"{request.title}: {request.description[:120]}",
        suggested_priority=priority,
        follow_up_questions=_default_questions(request.title, request.description),
    )


@app.post("/internal/ai/kb-draft-generate", response_model=AiResponse)
def kb_draft_generate(request: TextGenerationRequest) -> AiResponse:
    content = (
        "## 问题描述\n"
        f"{request.description.strip()}\n\n"
        "## 处理步骤\n"
        "1. 复核日志与监控\n"
        "2. 确认依赖服务和变更记录\n"
        "3. 补充最终解决方案与回滚信息"
    )
    return _provider_payload("kb-draft-generate", content)


@app.post("/internal/ai/reindex", response_model=ReindexResponse)
def reindex(request: ReindexRequest) -> ReindexResponse:
    return ReindexResponse(
        provider=AI_PROVIDER,
        model=AI_MODEL,
        degraded=False,
        accepted=True,
        entity_type=request.entity_type,
        limit=request.limit,
    )


@app.get("/health")
async def health() -> dict[str, Any]:
    database_ping = _touch_database()
    return {
        "status": "ok",
        "provider": AI_PROVIDER,
        "model": AI_MODEL,
        "embedding_model": MODEL_NAME,
        "dimension": model.get_sentence_embedding_dimension(),
        "database_ping": database_ping,
    }
