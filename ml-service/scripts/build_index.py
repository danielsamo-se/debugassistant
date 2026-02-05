"""
Build FAISS index from sample error traces
"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import faiss
import numpy as np
from sentence_transformers import SentenceTransformer


def main():
    data_dir = Path(__file__).parent.parent / "data"
    sample_file = data_dir / "sample_traces.json"
    index_file = data_dir / "error_index.faiss"
    metadata_file = data_dir / "error_metadata.json"

    with open(sample_file, "r", encoding="utf-8") as f:
        samples = json.load(f)

    model = SentenceTransformer("all-MiniLM-L6-v2")
    texts = [sample["trace"] for sample in samples]
    embeddings = model.encode(texts, show_progress_bar=True, normalize_embeddings=True)

    index = faiss.IndexFlatIP(embeddings.shape[1])
    index.add(embeddings.astype(np.float32))
    faiss.write_index(index, str(index_file))

    metadata = [
        {
            "id": s["id"],
            "text": s["trace"],
            "language": s["language"],
            "framework": s["framework"],
            "exception": s["exception"],
            "solution": s["solution"],
            "url": s["url"]
        }
        for s in samples
    ]
    with open(metadata_file, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print(f"Indexed {index.ntotal} vectors ({embeddings.shape[1]} dim)")


if __name__ == "__main__":
    main()