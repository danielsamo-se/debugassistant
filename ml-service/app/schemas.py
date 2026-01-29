"""
Pydantic models for API requests and responses
"""

from pydantic import BaseModel
from typing import List, Optional, Dict, Any


class EmbedRequest(BaseModel):
    text: str


class EmbedBatchRequest(BaseModel):
    texts: List[str]


class EmbedResponse(BaseModel):
    embedding: List[float]
    dimension: int


class EmbedBatchResponse(BaseModel):
    embeddings: List[List[float]]
    dimension: int
    count: int


class SimilarityRequest(BaseModel):
    text1: str
    text2: str


class SimilarityResponse(BaseModel):
    similarity: float
    text1: str
    text2: str


class IndexRequest(BaseModel):
    text: str
    metadata: Dict[str, Any] = {}


class IndexBatchRequest(BaseModel):
    items: List[IndexRequest]


class IndexResponse(BaseModel):
    id: int
    message: str


class IndexBatchResponse(BaseModel):
    ids: List[int]
    count: int


class SearchRequest(BaseModel):
    text: str
    k: int = 5


class SearchResult(BaseModel):
    score: float
    metadata: Dict[str, Any]


class SearchResponse(BaseModel):
    results: List[SearchResult]
    query: str
    count: int


class StoreInfoResponse(BaseModel):
    size: int
    dimension: int