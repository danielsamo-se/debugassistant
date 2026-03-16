import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from dotenv import load_dotenv
import os
import pytest
from app.services.similarity_search import get_similarity_search

load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

@pytest.fixture(autouse=True)
def clear_store():
    get_similarity_search().clear()
    yield