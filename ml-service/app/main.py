"""
ML service for stack trace analysis with RAG
"""

from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(
    title="DebugAssistant ML Service",
    version="0.1.0",
)


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str


@app.get("/health", response_model=HealthResponse)
async def health_check():
    return HealthResponse(
        status="healthy",
        service="ml-service",
        version="0.1.0"
    )


@app.get("/")
async def root():
    return {"service": "DebugAssistant ML Service", "version": "0.1.0", "docs": "/docs"}