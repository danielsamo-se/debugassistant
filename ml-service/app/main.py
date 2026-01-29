"""
ML service for stack trace analysis with RAG
"""

from fastapi import FastAPI
from pydantic import BaseModel
from contextlib import asynccontextmanager

from app.schemas import (
    EmbedRequest, EmbedResponse,
    EmbedBatchRequest, EmbedBatchResponse,
    SimilarityRequest, SimilarityResponse,
    IndexRequest, IndexResponse,
    IndexBatchRequest, IndexBatchResponse,
    SearchRequest, SearchResponse, SearchResult,
    StoreInfoResponse,
    AnalyzeRequest, AnalyzeResponse
)
from app.services.embedding_service import get_embedding_service
from app.services.similarity_search import get_similarity_search
from app.services.rag_service import get_rag_service


@asynccontextmanager
async def lifespan(app: FastAPI):
    get_embedding_service()
    get_similarity_search()
    get_rag_service()
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


@app.post("/index", response_model=IndexResponse)
async def index_document(request: IndexRequest):
    embedding_service = get_embedding_service()
    store = get_similarity_search()

    embedding = embedding_service.embed(request.text)
    metadata = {**request.metadata, "text": request.text}
    doc_id = store.add(embedding, metadata)

    return IndexResponse(id=doc_id, message="Document indexed")


@app.post("/index/batch", response_model=IndexBatchResponse)
async def index_batch(request: IndexBatchRequest):
    embedding_service = get_embedding_service()
    store = get_similarity_search()

    texts = [item.text for item in request.items]
    embeddings = embedding_service.embed_batch(texts)
    metadatas = [{**item.metadata, "text": item.text} for item in request.items]
    ids = store.add_batch(embeddings, metadatas)

    return IndexBatchResponse(ids=ids, count=len(ids))


@app.post("/search", response_model=SearchResponse)
async def search(request: SearchRequest):
    embedding_service = get_embedding_service()
    store = get_similarity_search()

    query_embedding = embedding_service.embed(request.text)
    results = store.search(query_embedding, k=request.k)

    return SearchResponse(
        results=[SearchResult(**r) for r in results],
        query=request.text,
        count=len(results)
    )


@app.get("/store/info", response_model=StoreInfoResponse)
async def store_info():
    store = get_similarity_search()
    return StoreInfoResponse(
        size=store.size(),
        dimension=store.dimension
    )


@app.post("/store/clear")
async def clear_store():
    store = get_similarity_search()
    store.clear()
    return {"message": "Store cleared"}


@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze_stack_trace(request: AnalyzeRequest):
    rag_service = get_rag_service()
    result = rag_service.analyze(request.stack_trace, request.use_retrieval)

    return AnalyzeResponse(
        analysis=result["analysis"],
        similar_errors=[SearchResult(**r) for r in result["similar_errors"]],
        context_used=result["context_used"]
    )