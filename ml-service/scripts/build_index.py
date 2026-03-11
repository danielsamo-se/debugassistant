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


def build_document(sample: dict) -> str:
    parts = [
        f"Exception: {sample.get('exception', '')}",
        f"Framework: {sample.get('framework', '')}",
        f"Language: {sample.get('language', '')}",
        f"Trace: {sample.get('trace', '')}",
        f"Fix: {sample.get('solution', '')}",
    ]
    return "\n".join(parts).strip()


def main():
    data_dir = Path(__file__).parent.parent / "data"
    sample_file = data_dir / "sample_traces.json"
    index_file = data_dir / "error_index.faiss"
    metadata_file = data_dir / "error_metadata.json"

    with open(sample_file, "r", encoding="utf-8") as f:
        samples = json.load(f)

    documents = [build_document(sample) for sample in samples]

    model = SentenceTransformer("all-MiniLM-L6-v2")
    embeddings = model.encode(
        documents,
        show_progress_bar=True,
        normalize_embeddings=True,
    )

    embeddings = np.asarray(embeddings, dtype=np.float32)

    index = faiss.IndexFlatIP(embeddings.shape[1])
    index.add(embeddings)
    faiss.write_index(index, str(index_file))

    metadata = []
    for sample, document in zip(samples, documents):
        metadata.append(
            {
                "id": sample["id"],
                "document": document,
                "text": sample["trace"],
                "language": sample["language"],
                "framework": sample["framework"],
                "exception": sample["exception"],
                "solution": sample["solution"],
                "url": sample["url"],
            }
        )

    with open(metadata_file, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)

    print(f"Indexed {index.ntotal} vectors with dimension {embeddings.shape[1]}")
    print("Saved metadata with document field")


if __name__ == "__main__":
    main()