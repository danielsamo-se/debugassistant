"""
ML service for stack trace analysis with RAG
"""

from fastapi import FastAPI
from pydantic import BaseModel
from contextlib import asynccontextmanager

from app.schemas import (
    EmbedRequest, EmbedResponse,
    EmbedBatchRequest, EmbedBatchResponse,
    SimilarityRequest, SimilarityResponse
)
from app.services.embedding_service import get_embedding_service


@asynccontextmanager
async def lifespan(app: FastAPI):
    get_embedding_service()
    yield


app = FastAPI(
    title="DebugAssistant ML Service",
    version="0.1.0",
    lifespan=lifespan
)


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str


@app.get("/health", response_model=HealthResponse)
async def health_check():
    return HealthResponse(
        status="healthy",
        service="ml-service",
        version="0.1.0"
    )


@app.get("/")
async def root():
    return {"service": "DebugAssistant ML Service", "version": "0.1.0", "docs": "/docs"}


@app.post("/embed", response_model=EmbedResponse)
async def embed_text(request: EmbedRequest):
    service = get_embedding_service()
    embedding = service.embed(request.text)
    return EmbedResponse(
        embedding=embedding,
        dimension=service.dimension
    )


@app.post("/embed/batch", response_model=EmbedBatchResponse)
async def embed_batch(request: EmbedBatchRequest):
    service = get_embedding_service()
    embeddings = service.embed_batch(request.texts)
    return EmbedBatchResponse(
        embeddings=embeddings,
        dimension=service.dimension,
        count=len(embeddings)
    )


@app.post("/similarity", response_model=SimilarityResponse)
async def compute_similarity(request: SimilarityRequest):
    service = get_embedding_service()
    similarity = service.similarity(request.text1, request.text2)
    return SimilarityResponse(
        similarity=similarity,
        text1=request.text1,
        text2=request.text2
    )