"""
RAG service combining retrieval and LLM generation for stack trace analysis
"""

import os
from typing import List, Dict, Any, Optional
from groq import Groq

from app.services.embedding_service import get_embedding_service
from app.services.similarity_search import get_similarity_search


SYSTEM_PROMPT = """You are a helpful debugging assistant. You analyze stack traces and error messages to help developers understand and fix issues.

When given a stack trace and relevant context from similar errors, you should:
1. Identify the root cause of the error
2. Explain why this error occurs
3. Suggest concrete solutions

Be concise and practical. Focus on actionable advice."""


class RAGService:
    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or os.getenv("GROQ_API_KEY")
        self.client = None
        if self.api_key:
            self.client = Groq(api_key=self.api_key)
        self.model = "llama-3.1-8b-instant"

    def _build_context(self, similar_docs: List[Dict[str, Any]]) -> str:
        if not similar_docs:
            return ""

        context_parts = ["Here are similar errors and their solutions:"]
        for i, doc in enumerate(similar_docs[:3], 1):
            metadata = doc["metadata"]
            text = metadata.get("text", "")
            source = metadata.get("source", "unknown")
            context_parts.append(f"\n{i}. [{source}] {text}")

        return "\n".join(context_parts)

    def _build_prompt(self, stack_trace: str, context: str) -> str:
        prompt = f"Stack trace:\n```\n{stack_trace}\n```"
        if context:
            prompt += f"\n\n{context}"
        prompt += "\n\nAnalyze this error and provide a solution."
        return prompt

    def analyze(self, stack_trace: str, use_retrieval: bool = True) -> Dict[str, Any]:
        embedding_service = get_embedding_service()
        similarity_search = get_similarity_search()

        similar_docs = []
        if use_retrieval and similarity_search.size() > 0:
            query_embedding = embedding_service.embed(stack_trace)
            similar_docs = similarity_search.search(query_embedding, k=3)

        context = self._build_context(similar_docs)
        prompt = self._build_prompt(stack_trace, context)

        response_text = self._generate(prompt)

        return {
            "analysis": response_text,
            "similar_errors": similar_docs,
            "context_used": bool(similar_docs)
        }

    def _generate(self, prompt: str) -> str:
        if not self.client:
            return self._fallback_response(prompt)

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.3,
                max_tokens=1024
            )
            return response.choices[0].message.content
        except Exception as e:
            return f"LLM error: {str(e)}"

    def _fallback_response(self, prompt: str) -> str:
        return "LLM not configured. Set GROQ_API_KEY environment variable for AI-powered analysis."


_rag_service: Optional[RAGService] = None


def get_rag_service() -> RAGService:
    global _rag_service
    if _rag_service is None:
        _rag_service = RAGService()
    return _rag_service