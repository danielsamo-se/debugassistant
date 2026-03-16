"""
Similarity search using FAISS for efficient semantic retrieval
"""

import json
import logging
from pathlib import Path
from typing import List, Dict, Any, Optional

import faiss
import numpy as np


logger = logging.getLogger(__name__)


class SimilaritySearch:
    def __init__(self, dimension: int = 384):
        self.dimension = dimension
        self.index = faiss.IndexFlatIP(dimension)
        self.documents: List[Dict[str, Any]] = []
        self._load_prebuilt_index()

    def _load_prebuilt_index(self):
        data_dir = Path(__file__).parent.parent.parent / "data"
        index_path = data_dir / "error_index.faiss"
        metadata_path = data_dir / "error_metadata.json"

        if not index_path.exists():
            logger.info("No pre-built index found, starting empty")
            return

        if not metadata_path.exists():
            logger.warning("Index found but no metadata, starting empty")
            return

        try:
            self.index = faiss.read_index(str(index_path))
            with open(metadata_path, "r", encoding="utf-8") as f:
                self.documents = json.load(f)

            if self.index.ntotal != len(self.documents):
                logger.error(f"Index/metadata mismatch: {self.index.ntotal} vs {len(self.documents)}")
                self.index = faiss.IndexFlatIP(self.dimension)
                self.documents = []
                return

            logger.info(f"Loaded pre-built index with {self.index.ntotal} vectors")

        except Exception as e:
            logger.error(f"Failed to load pre-built index: {e}")
            self.index = faiss.IndexFlatIP(self.dimension)
            self.documents = []

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
