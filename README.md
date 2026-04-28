# Smart Ticket Platform

AI-assisted internal IT service desk platform built with a hybrid Java + Python architecture.

The current codebase is at Phase 3 of the migration plan: React calls a Java Gateway, business domains are owned by the Java Platform Service, and Python is kept as an internal AI service for LLM and embedding workflows. Phase 4 governance work, including Nacos, Sentinel, and Kubernetes manifests, is intentionally not started yet.

Chinese documentation: [README_ZH.md](README_ZH.md)

## Current Progress

- Migration is complete through Phase 3: Java Gateway is the only public API entrypoint, Java Platform Service owns the business domains, and Python is limited to internal AI and embedding workflows.
- AI workflows are operational with DeepSeek-compatible provider settings, persisted ticket diagnostics, KB-first repair suggestions, local `all-MiniLM-L6-v2` embeddings, pgvector/HNSW search, and lightweight vector + keyword hybrid recall.
- RabbitMQ drives non-blocking AI flows: ticket creation triggers priority analysis, and ticket resolution triggers AI-generated knowledge base drafts.
- Customer self-service is kept low-cost by searching the local knowledge base before ticket submission without automatic LLM calls.
- Admins can submit tickets, review audit/statistics pages, and manage knowledge base drafts.
- Knowledge base articles are stored as Markdown and rendered as Markdown in the UI; AI-generated drafts use CommonMark and can be previewed before publishing.
- Phase 4 governance is still pending: Nacos, Sentinel, Kubernetes/Helm, CI/CD hardening, and production deployment templates are not in place yet.

## Capabilities

### Customers

- Submit IT support tickets through a guided form.
- Search the knowledge base with low-cost local hybrid search before creating a ticket.
- Track personal tickets and ticket status.
- Receive notifications when ticket status changes.

### Engineers

- Work from an open and in-progress ticket queue.
- Assign tickets atomically to avoid double-claiming.
- Add comments and resolution notes.
- Request AI repair suggestions from DeepSeek V4 Pro or another configured provider.
- Use KB-first similar article recommendations while working a ticket.
- Resolve tickets and trigger asynchronous knowledge draft generation.

### Administrators

- Submit tickets from the same guided workflow as customers.
- View ticket and knowledge base statistics.
- Review audit logs.
- Manage Markdown knowledge base drafts and published articles.
- Use the same unified Java API entrypoint as the frontend.

## Architecture

```text
+--------------------+
| React + TypeScript |
| Vite + Ant Design  |
| localhost:5173     |
+---------+----------+
          |
          | Public API: /api/v1/**
          v
+------------------------+
| Java Gateway           |
| Spring Cloud Gateway   |
| localhost:8080         |
+---------+--------------+
          |
          | Internal platform API: /api/**
          v
+------------------------+        +------------------------+
| Java Platform Service  +------->| Python AI Service      |
| Spring Boot 3.5 + JPA  |        | FastAPI + LangChain    |
| localhost:8081         |        | localhost:8100         |
+---------+--------------+        +---------+--------------+
          |                                 |
          v                                 v
+------------------------+        +------------------------+
| PostgreSQL 16          |        | RabbitMQ 4.1           |
| pgvector, HNSW, 384d   |        | async AI workflows     |
+------------------------+        +------------------------+
```

## Service Boundaries

| Service | Responsibility |
| --- | --- |
| `frontend` | Browser UI, route guards, API calls through the Java Gateway |
| `platform-java/gateway` | Single public entrypoint, request forwarding, future governance hooks |
| `platform-java/platform-service` | Auth, users, tickets, comments, notifications, knowledge metadata, audit logs, RabbitMQ events |
| `ai-service` | LLM provider calls, field extraction, follow-up questions, description enhancement, repair suggestions, embedding search, embedding rebuild, knowledge draft generation |
| `postgres` | Shared PostgreSQL 16 instance with pgvector |
| `rabbitmq` | Asynchronous ticket and AI workflow events |

## Event Model

Phase 3 uses RabbitMQ as the asynchronous backbone for AI and knowledge workflows.

| Event | Purpose |
| --- | --- |
| `ticket.created` | Trigger non-blocking AI analysis after ticket creation |
| `ticket.updated` | React to business changes without blocking the main request |
| `ticket.resolved` | Generate knowledge draft candidates from resolution notes |
| `knowledge.published` | Notify downstream embedding or indexing workflows |
| `ai.analysis.completed` | Persist AI results back through the Java-owned boundary |
| `knowledge.draft.generated` | Surface generated drafts for engineer or admin review |

## LLM and Embedding

- LLM configuration is provider-compatible and currently supports DeepSeek-style, OpenAI-style, and Anthropic-style settings through environment variables.
- The default AI provider is DeepSeek through the OpenAI-compatible API: `AI_PROVIDER=deepseek`, `AI_MODEL=deepseek-v4-pro`, `AI_BASE_URL=https://api.deepseek.com`.
- For heavier models such as V4 Pro, keep `AI_REQUEST_TIMEOUT_SECONDS` aligned with the Gateway/client timeout.
- The Python service keeps the AI core logic isolated from the business platform.
- AI suggestion prompts are KB-first: reliable knowledge base matches must be cited by article id/title, while unreliable matches must be called out explicitly.
- Embeddings use `sentence-transformers/all-MiniLM-L6-v2`.
- Vectors are stored as 384-dimensional pgvector values.
- HNSW indexing is used for knowledge base similarity search.
- Internal KB search uses lightweight hybrid recall: pgvector candidates plus keyword candidates, domain keyword boosts, de-duplication, and score filtering.

### LLM Call Sites

| Call Site | Purpose | Trigger |
| --- | --- | --- |
| Priority analysis | Auto-assign ticket priority with reason | Ticket creation |
| Refine | Improve a user's ticket description | User clicks AI optimization |
| Suggest | Generate KB-first resolution suggestions | User requests AI suggestion |
| Knowledge draft | Generate KB draft article from resolution notes | Ticket resolved |
| Vision | Analyze uploaded screenshots | Planned image attachment flow |

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | React 19, TypeScript 5.9, Ant Design 6, Zustand 5, Vite 8 |
| Gateway | Spring Cloud Gateway, Spring Boot 3.5 |
| Java platform | Spring Boot 3.5, Java 17, Spring Data JPA, Spring Security, JWT, RabbitMQ |
| AI service | Python 3.11, FastAPI, LangChain, SQLAlchemy, sentence-transformers |
| Database | PostgreSQL 16, pgvector, HNSW index, `vector(384)` |
| Messaging | RabbitMQ 4.1 management image |
| Infrastructure | Docker Compose, Maven, future Kubernetes manifests |
| Future governance | Nacos, Sentinel, Kubernetes, Helm skeletons in Phase 4 |

## Quick Start

### Prerequisites

- Java 17+
- Node.js 18+ and npm
- Docker Desktop or Docker Engine
- Python 3.11+ for local AI service work
- A local `.env` based on [.env.example](.env.example)
- Optional: AI API key (`AI_API_KEY`, `LLM_API_KEY`, or legacy `CLAUDE_API_KEY`)

### 1. Configure Environment

Create `.env` locally and set at least:

```bash
POSTGRES_PASSWORD=zhigong123
JWT_SECRET=<base64-or-long-random-secret>
```

Optional LLM variables:

```bash
AI_PROVIDER=deepseek
AI_MODEL=deepseek-v4-pro
AI_API_KEY=<your-key>
AI_BASE_URL=https://api.deepseek.com
AI_REQUEST_TIMEOUT_SECONDS=45
```

Do not commit `.env`.

### 2. Start Infrastructure and AI Service

```bash
docker compose up -d postgres rabbitmq ai-service
```

Health checks:

```bash
curl -fsS http://localhost:8100/health
docker compose ps
```

### 3. Start the Java Platform Service

```bash
mvn -s .mvn/settings.xml -pl platform-java/platform-service spring-boot:run
```

Platform service health:

```bash
curl -fsS http://localhost:8081/actuator/health
```

### 4. Start the Java Gateway

```bash
PLATFORM_SERVICE_URL=http://localhost:8081 \
mvn -s .mvn/settings.xml -pl platform-java/gateway spring-boot:run
```

Gateway health:

```bash
curl -fsS http://localhost:8080/actuator/health
```

### 5. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

### Full Docker Compose

```bash
docker compose up --build
```

This starts PostgreSQL, RabbitMQ, AI service, platform service, and gateway. The frontend is usually run separately with `npm run dev` for local development.

## Demo Accounts

All demo accounts use password `demo123`.

| Role | Username |
| --- | --- |
| Customer | `customer1` |
| Customer | `customer2` |
| Customer | `customer3` |
| Engineer | `engineer1` |
| Engineer | `engineer2` |
| Admin | `admin1` |

## API Overview

The browser should call only the Gateway public API under `/api/v1/**`. The Gateway rewrites requests to the Platform Service internal `/api/**` routes.

### Auth

- `POST /api/v1/auth/login`

### Tickets

- `POST /api/v1/tickets`
- `GET /api/v1/tickets`
- `GET /api/v1/tickets/{id}`
- `PUT /api/v1/tickets/{id}/assign`
- `PUT /api/v1/tickets/{id}/resolve`
- `GET /api/v1/tickets/{ticketId}/comments`
- `POST /api/v1/tickets/{ticketId}/comments`

### AI

- `POST /api/v1/ai/search` - vector and keyword hybrid search against the knowledge base.
- `POST /api/v1/ai/refine` - improve a ticket description with the configured LLM provider.
- `POST /api/v1/ai/suggest` - generate KB-first repair suggestions with the configured LLM provider.
- `GET /api/v1/ai/suggest/latest` - load the latest persisted ticket AI diagnostic without another model call.
- `POST /api/v1/ai/similar` - find similar knowledge or ticket references.

### Knowledge Base

- `GET /api/v1/kb/published`
- `GET /api/v1/kb/drafts`
- `PUT /api/v1/kb/{id}/publish`
- `PUT /api/v1/kb/{id}`
- `DELETE /api/v1/kb/{id}`

### Notifications and Admin

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/unread-count`
- `PUT /api/v1/notifications/{id}/read`
- `GET /api/v1/admin/stats`
- `GET /api/v1/admin/audit-logs`

## Schema Bootstrap

`platform-service` defaults `spring.sql.init.mode` to `always` for local development. The schema is idempotent with `CREATE TABLE IF NOT EXISTS`, and seed data uses guarded inserts. For production-style runs, override:

```bash
SQL_INIT_MODE=never
```

## Quality Checks

```bash
mvn -s .mvn/settings.xml test
cd frontend && npm run build
python3 -c "import py_compile; py_compile.compile('ai-service/main.py', cfile='/tmp/ai-service-main.pyc', doraise=True)"
docker compose config --quiet
docker compose build ai-service
```

## Project Structure

```text
.
|-- ai-service/                       # FastAPI AI and embedding service
|-- frontend/                         # React application
|-- infra/                            # Infrastructure notes and future deployment assets
|-- platform-java/
|   |-- gateway/                      # Spring Cloud Gateway
|   `-- platform-service/             # Spring Boot business platform
|-- docker-compose.yml
|-- DESIGN.md
|-- README.md
`-- README_ZH.md
```

## Security Notes

- JWT uses HS512 signing and requires `JWT_SECRET`.
- Passwords are BCrypt-hashed.
- Role-based access control covers customer, engineer, and admin workflows.
- Ticket assignment is atomic.
- LLM output is sanitized before persistence.
- AI interactions and business changes are logged for auditability.
- Secrets must stay in `.env` or deployment secret stores, never in Git.

## Roadmap

- Phase 0: MVP baseline stabilization.
- Phase 1: Java foundation and unified Gateway.
- Phase 2: Business domain migration to Java.
- Phase 3: AI service boundary and RabbitMQ-driven asynchronous workflows. Current implementation is in this phase.
- Phase 4: Nacos, Sentinel, Kubernetes, CI/CD, and deployment hardening. Not started.

## License

MIT
