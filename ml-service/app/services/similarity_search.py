"""
Similarity search using FAISS for efficient semantic retrieval
"""

import faiss
import numpy as np
from typing import List, Dict, Any, Optional


class SimilaritySearch:
    def __init__(self, dimension: int = 384):
        self.dimension = dimension
        self.index = faiss.IndexFlatIP(dimension)
        self.documents: List[Dict[str, Any]] = []

    def add(self, embedding: List[float], metadata: Dict[str, Any]) -> int:
        vector = np.array([embedding], dtype=np.float32)
        faiss.normalize_L2(vector)
        self.index.add(vector)
        self.documents.append(metadata)
        return len(self.documents) - 1

    def add_batch(self, embeddings: List[List[float]], metadatas: List[Dict[str, Any]]) -> List[int]:
        vectors = np.array(embeddings, dtype=np.float32)
        faiss.normalize_L2(vectors)
        self.index.add(vectors)

        start_id = len(self.documents)
        self.documents.extend(metadatas)
        return list(range(start_id, len(self.documents)))

    def search(self, query_embedding: List[float], k: int = 5) -> List[Dict[str, Any]]:
        if self.index.ntotal == 0:
            return []

        k = min(k, self.index.ntotal)
        query = np.array([query_embedding], dtype=np.float32)
        faiss.normalize_L2(query)

        scores, indices = self.index.search(query, k)

        results = []
        for score, idx in zip(scores[0], indices[0]):
            if idx >= 0:
                results.append({
                    "score": float(score),
                    "metadata": self.documents[idx]
                })
        return results

    def size(self) -> int:
        return self.index.ntotal

    def clear(self):
        self.index = faiss.IndexFlatIP(self.dimension)
        self.documents = []


_similarity_search: Optional[SimilaritySearch] = None


def get_similarity_search() -> SimilaritySearch:
    global _similarity_search
    if _similarity_search is None:
        _similarity_search = SimilaritySearch()
    return _similarity_search
