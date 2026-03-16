from dotenv import load_dotenv
import os
import pytest
from app.services.similarity_search import get_similarity_search

load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

@pytest.fixture(autouse=True)
def clear_store():
    get_similarity_search().clear()
    yield