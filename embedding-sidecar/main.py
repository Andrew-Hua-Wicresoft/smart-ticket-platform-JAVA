"""
Embedding sidecar for 智能工单系统.
FastAPI service that generates text embeddings using text2vec-large-chinese (1024d).
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import numpy as np
import os
import time
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="智能工单 Embedding Sidecar")

# Load model at startup
MODEL_NAME = os.getenv("EMBEDDING_MODEL", "shibing624/text2vec-large-chinese")
CACHE_DIR = os.getenv("MODEL_CACHE_DIR", "./model-cache")

logger.info(f"Loading embedding model: {MODEL_NAME}")
model = SentenceTransformer(MODEL_NAME, cache_folder=CACHE_DIR)
logger.info(f"Model loaded. Embedding dimension: {model.get_sentence_embedding_dimension()}")


class EmbedRequest(BaseModel):
    text: str


class EmbedResponse(BaseModel):
    embedding: list[float]
    dimension: int
    latency_ms: float


@app.post("/embed", response_model=EmbedResponse)
def embed(request: EmbedRequest):
    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")

    start = time.time()
    embedding = model.encode(request.text, normalize_embeddings=True)
    latency_ms = (time.time() - start) * 1000

    return EmbedResponse(
        embedding=embedding.tolist(),
        dimension=len(embedding),
        latency_ms=round(latency_ms, 1)
    )


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "model": MODEL_NAME,
        "dimension": model.get_sentence_embedding_dimension()
    }
