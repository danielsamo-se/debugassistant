# DebugAssistant

[![Backend CI](https://github.com/USERNAME/debugassistant/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/USERNAME/debugassistant/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/USERNAME/debugassistant/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/USERNAME/debugassistant/actions/workflows/frontend-ci.yml)
[![ML Service CI](https://github.com/USERNAME/debugassistant/actions/workflows/ml-service-ci.yml/badge.svg)](https://github.com/USERNAME/debugassistant/actions/workflows/ml-service-ci.yml)

DebugAssistant analyzes stack traces using semantic search and an LLM debug agent. It matches errors against 50 curated error patterns using Sentence-BERT embeddings and FAISS, then an autonomous Gemini agent selects tools to generate structured explanations.

---

## Demo

![DebugAssistant Analysis](assets/debugassistant-demo.PNG)

---

## Highlights

* Sentence-BERT embeddings (all-MiniLM-L6-v2) with FAISS vector search
* LLM debug agent with autonomous tool calling (Google Gemini)
* 3 agent tools: stack trace analysis, semantic error search, framework best practices
* Retrieval benchmark with 50 manually designed queries and a TF IDF baseline comparison
* Parallel heuristic search against GitHub Issues and Stack Overflow
* Focused on Java and Spring debugging workflows
* Redis cache with MD5 key and 24h TTL
* JWT-protected per-user history in PostgreSQL
* OpenAPI/Swagger documentation

---

## Tech Stack

* **ML Service:** Python, FastAPI, Sentence-Transformers, FAISS, Google Gemini
* **Backend:** Java, Spring Boot, Redis, PostgreSQL
* **Frontend:** React 19, TypeScript, Vite
* **Infra:** Docker Compose (5 containers)

---

## Architecture

Stack trace → Spring Boot parses and extracts anchors → ML Service encodes with Sentence-BERT and searches FAISS index → Gemini agent autonomously selects tools and generates explanation → results cached in Redis → optionally saved to PostgreSQL per user

The backend also runs a parallel heuristic search against GitHub Issues and Stack Overflow for additional results.

---

## ML Service

FastAPI microservice for semantic search and LLM-based analysis.

**Semantic Search:**
50 curated error patterns embedded with all-MiniLM-L6-v2 (384 dimensions) and stored in a FAISS index (IndexFlatIP with L2 normalization for cosine similarity). At query time the stack trace is encoded with the same model and the top-k most similar patterns are retrieved.

**Debug Agent:**
The agent receives a stack trace and runs a tool-calling loop with Google Gemini (gemini-2.5-flash-lite). It autonomously selects from 3 tools:
* `analyze_stack_trace` — extracts exception type, framework and root cause
* `search_similar_errors` — searches the FAISS index for similar error patterns
* `get_framework_best_practices` — returns common fixes for Spring, Hibernate or Jackson

The agent iterates until it has enough context, then generates a structured explanation with root cause and fix suggestions.

**Endpoints:**
* `POST /search` — semantic search against FAISS index
* `POST /analyze` — full agent analysis (search + tool calling + LLM explanation)
* `GET /health` — service health check

---

## Evaluation

Retrieval benchmark in `ml-service/evaluation.ipynb` with 50 manually designed Java and Spring debugging queries.

* Sentence BERT + FAISS: Hit@1 0.88, Hit@3 0.98, MRR 0.927
* TF IDF baseline: Hit@1 0.68, Hit@3 0.86, MRR 0.763
* Top 1 retrieval improved from 34/50 to 44/50
* Complete misses in top 3 reduced from 7 cases to 1 case

Includes breakdown by query type and difficulty plus a short failure analysis.

---

## Frontend

React 19 + TypeScript with Vite.

**Structure:**
* `pages/` — Route-level views (Home, Login, Register, History)
* `components/` — Reusable UI (ResultCard, StackTraceInput, CopyButton, ErrorBoundary)
* `services/` — API client layer (analyzeService, authService, historyService)
* `context/` + `hooks/` — Auth state management (AuthContext, useAuth)

**Features:**
* Protected routes with JWT authentication
* Loading states and skeleton components
* Copy-to-clipboard for results
* Error boundaries for runtime error handling
* Agent analysis with rendered Markdown and visible tool selection

**Testing:**
* Unit/Component tests: Vitest + Testing Library
* E2E tests: Playwright

---

## Quickstart (Docker)

```bash
cp .env.example .env
docker compose up --build
```

* Frontend: http://localhost:8081
* Backend: http://localhost:8080
* Swagger UI: http://localhost:8080/swagger-ui/index.html

```bash
docker compose down
```

---

## API

### Analyze

`POST /api/analyze` — works without auth. If logged in via JWT, the run is also written to history.

```json
{
  "stackTrace": "..."
}
```

Response includes language, exceptionType, keywords, rootCause, heuristic results from GitHub/Stack Overflow, mlAnalysis with the agent's explanation, and toolsUsed listing which tools the agent autonomously selected.

### History (JWT-protected)

* `GET /api/history` — list past analyses
* `POST /api/history` — auto-saved on analyze when authenticated

---

## Configuration (.env)

```
GEMINI_API_KEY
GITHUB_API_TOKEN
JWT_SECRET_KEY
JWT_EXPIRATION
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
VITE_API_BASE
```

---

## Tests

```bash
mvn test                    # Backend tests
cd ml-service && pytest     # ML service tests
```

* Controller/integration tests via @SpringBootTest + MockMvc
* External clients (GitHub / Stack Overflow) are mocked
* ML service: unit tests for embedding, similarity search and RAG pipeline

---

## Security & Privacy

* JWT protects history endpoints
* Stored: stackTraceSnippet (max 500 chars) + derived fields + top URL + timestamps/user linkage
* Not stored: full raw stack traces, environment secrets, access tokens
