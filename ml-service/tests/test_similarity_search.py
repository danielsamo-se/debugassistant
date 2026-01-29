"""
Tests for similarity search endpoints
"""

import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.services.similarity_search import get_similarity_search

client = TestClient(app)


@pytest.fixture(autouse=True)
def clear_store():
    yield
    get_similarity_search().clear()


def test_index_single_document():
    response = client.post("/index", json={
        "text": "NullPointerException in UserService.getUser()",
        "metadata": {"source": "github", "language": "java"}
    })

    assert response.status_code == 200
    data = response.json()
    assert data["id"] == 0
    assert data["message"] == "Document indexed"


def test_index_batch():
    response = client.post("/index/batch", json={
        "items": [
            {"text": "NullPointerException", "metadata": {"type": "error"}},
            {"text": "ArrayIndexOutOfBounds", "metadata": {"type": "error"}},
            {"text": "Connection refused", "metadata": {"type": "network"}}
        ]
    })

    assert response.status_code == 200
    data = response.json()
    assert data["count"] == 3
    assert len(data["ids"]) == 3


def test_search_returns_similar():
    client.post("/index/batch", json={
        "items": [
            {"text": "NullPointerException when calling method on null object", "metadata": {"id": 1}},
            {"text": "ArrayIndexOutOfBoundsException array access", "metadata": {"id": 2}},
            {"text": "How to make pasta carbonara", "metadata": {"id": 3}}
        ]
    })

    response = client.post("/search", json={
        "text": "null pointer error",
        "k": 2
    })

    assert response.status_code == 200
    data = response.json()
    assert data["count"] == 2
    assert data["results"][0]["metadata"]["id"] == 1


def test_search_empty_store():
    response = client.post("/search", json={"text": "test", "k": 5})

    assert response.status_code == 200
    data = response.json()
    assert data["count"] == 0
    assert data["results"] == []


def test_store_info():
    client.post("/index", json={"text": "test document"})
    client.post("/index", json={"text": "another document"})

    response = client.get("/store/info")

    assert response.status_code == 200
    data = response.json()
    assert data["size"] == 2
    assert data["dimension"] == 384


def test_clear_store():
    client.post("/index", json={"text": "test"})
    response = client.post("/store/clear")

    assert response.status_code == 200

    info = client.get("/store/info").json()
    assert info["size"] == 0