"""
Run retrieval evaluation for TF IDF and Sentence BERT
"""

import json
from pathlib import Path

import faiss
import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity


def load_json(path: Path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def load_jsonl(path: Path):
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                rows.append(json.loads(line))
    return rows


def get_documents(metadata: list[dict]) -> list[str]:
    documents = []
    for item in metadata:
        document = item.get("document")
        if document:
            documents.append(document)
        else:
            documents.append(item["text"])
    return documents


def get_document_ids(metadata: list[dict]) -> list[int]:
    return [item["id"] for item in metadata]


def reciprocal_rank(ranked_ids: list[int], expected_id: int) -> float:
    for rank, doc_id in enumerate(ranked_ids, start=1):
        if doc_id == expected_id:
            return 1.0 / rank
    return 0.0


def evaluate(run_name: str, predictions: list[list[int]], expected_ids: list[int]) -> dict:
    hit_1 = 0
    hit_3 = 0
    mrr = 0.0

    for ranked_ids, expected_id in zip(predictions, expected_ids):
        if ranked_ids and ranked_ids[0] == expected_id:
            hit_1 += 1
        if expected_id in ranked_ids[:3]:
            hit_3 += 1
        mrr += reciprocal_rank(ranked_ids, expected_id)

    total = len(expected_ids)

    return {
        "method": run_name,
        "num_queries": total,
        "hit_at_1": hit_1 / total,
        "hit_at_3": hit_3 / total,
        "mrr": mrr / total,
        "hit_at_1_count": hit_1,
        "hit_at_3_count": hit_3,
    }


def map_positions_to_ids(position_rows: list[list[int]], document_ids: list[int]) -> list[list[int]]:
    mapped = []
    for row in position_rows:
        mapped.append([document_ids[pos] for pos in row])
    return mapped


def run_tfidf(documents: list[str], queries: list[str], top_k: int) -> list[list[int]]:
    vectorizer = TfidfVectorizer()
    doc_matrix = vectorizer.fit_transform(documents)
    query_matrix = vectorizer.transform(queries)

    similarities = cosine_similarity(query_matrix, doc_matrix)

    predictions = []
    for row in similarities:
        ranked = np.argsort(row)[::-1][:top_k]
        predictions.append(ranked.tolist())

    return predictions


def run_sbert_faiss(
        documents: list[str],
        queries: list[str],
        top_k: int,
) -> list[list[int]]:
    model = SentenceTransformer("all-MiniLM-L6-v2")

    doc_embeddings = model.encode(
        documents,
        show_progress_bar=True,
        normalize_embeddings=True,
    )
    doc_embeddings = np.asarray(doc_embeddings, dtype=np.float32)

    index = faiss.IndexFlatIP(doc_embeddings.shape[1])
    index.add(doc_embeddings)

    query_embeddings = model.encode(
        queries,
        show_progress_bar=True,
        normalize_embeddings=True,
    )
    query_embeddings = np.asarray(query_embeddings, dtype=np.float32)

    _, indices = index.search(query_embeddings, top_k)

    predictions = []
    for row in indices:
        predictions.append(row.tolist())

    return predictions


def get_title_map(metadata: list[dict]) -> dict[int, str]:
    title_map = {}
    for item in metadata:
        title_map[item["id"]] = item["exception"]
    return title_map


def build_detailed_results(
        queries_data: list[dict],
        tfidf_predictions: list[list[int]],
        sbert_predictions: list[list[int]],
        title_map: dict[int, str],
) -> list[dict]:
    rows = []

    for query, tfidf_ids, sbert_ids in zip(queries_data, tfidf_predictions, sbert_predictions):
        expected_id = query["expected_id"]

        row = {
            "query_id": query["query_id"],
            "query_text": query["query_text"],
            "query_type": query["query_type"],
            "difficulty": query["difficulty"],
            "expected_id": expected_id,
            "expected_exception": title_map.get(expected_id, "unknown"),
            "tfidf_top_3_ids": tfidf_ids,
            "tfidf_top_3_exceptions": [title_map.get(doc_id, "unknown") for doc_id in tfidf_ids],
            "tfidf_hit_at_1": len(tfidf_ids) > 0 and tfidf_ids[0] == expected_id,
            "tfidf_hit_at_3": expected_id in tfidf_ids[:3],
            "sbert_top_3_ids": sbert_ids,
            "sbert_top_3_exceptions": [title_map.get(doc_id, "unknown") for doc_id in sbert_ids],
            "sbert_hit_at_1": len(sbert_ids) > 0 and sbert_ids[0] == expected_id,
            "sbert_hit_at_3": expected_id in sbert_ids[:3],
        }

        rows.append(row)

    return rows


def main():
    base_dir = Path(__file__).resolve().parent.parent
    data_dir = base_dir / "data"
    eval_dir = base_dir / "eval"

    metadata = load_json(data_dir / "error_metadata.json")
    queries_data = load_jsonl(eval_dir / "queries_v1.jsonl")

    documents = get_documents(metadata)
    document_ids = get_document_ids(metadata)
    title_map = get_title_map(metadata)

    queries = [item["query_text"] for item in queries_data]
    expected_ids = [item["expected_id"] for item in queries_data]

    top_k = 3

    tfidf_positions = run_tfidf(documents, queries, top_k)
    sbert_positions = run_sbert_faiss(documents, queries, top_k)

    tfidf_predictions = map_positions_to_ids(tfidf_positions, document_ids)
    sbert_predictions = map_positions_to_ids(sbert_positions, document_ids)

    tfidf_result = evaluate("tfidf", tfidf_predictions, expected_ids)
    sbert_result = evaluate("sentence_bert_faiss", sbert_predictions, expected_ids)

    results = {
        "tfidf": tfidf_result,
        "sentence_bert_faiss": sbert_result,
    }

    detailed_results = build_detailed_results(
        queries_data,
        tfidf_predictions,
        sbert_predictions,
        title_map,
    )

    output_path = eval_dir / "results_v1.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    detailed_output_path = eval_dir / "detailed_results_v1.json"
    with open(detailed_output_path, "w", encoding="utf-8") as f:
        json.dump(detailed_results, f, indent=2, ensure_ascii=False)

    print(json.dumps(results, indent=2, ensure_ascii=False))
    print("Saved detailed results to eval/detailed_results_v1.json")


if __name__ == "__main__":
    main()