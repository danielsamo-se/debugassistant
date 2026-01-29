"""
Tests for RAG service endpoints
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


def test_analyze_without_context():
    response = client.post("/analyze", json={
        "stack_trace": "java.lang.NullPointerException\n\tat com.example.Service.process(Service.java:42)"
    })

    assert response.status_code == 200
    data = response.json()
    assert "analysis" in data
    assert "similar_errors" in data
    assert "context_used" in data
    assert data["context_used"] == False


def test_analyze_with_retrieval():
    client.post("/index/batch", json={
        "items": [
            {
                "text": "NullPointerException fix: check if object is null before calling methods",
                "metadata": {"source": "stackoverflow", "solution": "Add null check"}
            },
            {
                "text": "ArrayIndexOutOfBoundsException when accessing array",
                "metadata": {"source": "github"}
            }
        ]
    })

    response = client.post("/analyze", json={
        "stack_trace": "java.lang.NullPointerException\n\tat com.example.UserService.getUser(UserService.java:25)"
    })

    assert response.status_code == 200
    data = response.json()
    assert data["context_used"] == True
    assert len(data["similar_errors"]) > 0


def test_analyze_without_retrieval_flag():
    client.post("/index", json={
        "text": "Some indexed error",
        "metadata": {"source": "test"}
    })

    response = client.post("/analyze", json={
        "stack_trace": "java.lang.NullPointerException",
        "use_retrieval": False
    })

    assert response.status_code == 200
    data = response.json()
    assert data["context_used"] == False
    assert len(data["similar_errors"]) == 0


def test_analyze_returns_similar_errors_ranked():
    client.post("/index/batch", json={
        "items": [
            {"text": "Connection timeout to database server", "metadata": {"id": 1}},
            {"text": "NullPointerException in service layer", "metadata": {"id": 2}},
            {"text": "Null reference error when calling method", "metadata": {"id": 3}}
        ]
    })

    response = client.post("/analyze", json={
        "stack_trace": "java.lang.NullPointerException"
    })

    assert response.status_code == 200
    data = response.json()

    if len(data["similar_errors"]) >= 2:
        assert data["similar_errors"][0]["metadata"]["id"] in [2, 3]