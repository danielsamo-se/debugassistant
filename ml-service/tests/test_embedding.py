"""
Tests for embedding service endpoints
"""

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_embed_single_text():
    response = client.post("/embed", json={"text": "NullPointerException in Java"})

    assert response.status_code == 200
    data = response.json()
    assert "embedding" in data
    assert "dimension" in data
    assert data["dimension"] == 384
    assert len(data["embedding"]) == 384


def test_embed_batch():
    texts = [
        "NullPointerException",
        "ArrayIndexOutOfBoundsException",
        "Connection refused"
    ]
    response = client.post("/embed/batch", json={"texts": texts})

    assert response.status_code == 200
    data = response.json()
    assert data["count"] == 3
    assert len(data["embeddings"]) == 3
    assert all(len(emb) == 384 for emb in data["embeddings"])


def test_similarity_similar_texts():
    response = client.post("/similarity", json={
        "text1": "NullPointerException when calling method",
        "text2": "NullPointer error on method invocation"
    })

    assert response.status_code == 200
    data = response.json()
    assert data["similarity"] > 0.7


def test_similarity_different_texts():
    response = client.post("/similarity", json={
        "text1": "NullPointerException in Java",
        "text2": "How to cook pasta"
    })

    assert response.status_code == 200
    data = response.json()
    assert data["similarity"] < 0.5