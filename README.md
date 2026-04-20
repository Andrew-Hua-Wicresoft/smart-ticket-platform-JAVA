# 智能工单系统 / Smart Ticket Platform

AI-powered internal IT support ticket system. Customers submit tickets, AI suggests solutions from the knowledge base before they even hit "submit", engineers get AI-assisted resolution suggestions, and resolved tickets automatically generate knowledge base articles for next time.

Built with a Phase 1 hybrid foundation: Spring Boot 3.5 + Spring Cloud Gateway + PostgreSQL/pgvector + FastAPI AI service + React 19.

## What It Does

**For customers (提交工单):**
- Submit IT support tickets with title and description
- As you type, AI searches the knowledge base and suggests existing solutions (ticket deflection)
- If a KB article solves the problem, the ticket is never created, saving engineer time
- Track your tickets in "我的工单" with real-time status updates

**For engineers (工单队列):**
- See all open and in-progress tickets in a queue with priority badges
- Assign tickets to yourself with atomic locking (two engineers can't grab the same ticket)
- Get AI-powered resolution suggestions from Claude when working a ticket
- Resolve tickets with notes, which auto-generate draft KB articles

**For admins (数据分析):**
- Dashboard with KPI cards: total tickets, AI deflection rate, avg resolution time, KB article count
- Ticket status distribution breakdown
- Manage knowledge base: review AI-generated draft articles, edit, publish, or delete

## Architecture

```
┌──────────────┐     ┌────────────────────────┐     ┌────────────────────────┐
│ React 19     │────▶│ Gateway Service        │────▶│ Platform Service       │
│ TypeScript   │     │ Spring Cloud Gateway   │     │ Spring Boot 3.5 + JPA  │
│ Ant Design 6 │     │ port 8080              │     │ port 8081              │
│ Vite         │     └──────────┬─────────────┘     └──────────┬─────────────┘
│ port 5173    │                │                                │
└──────────────┘                │                                │
                                │                                │
                       ┌────────▼────────┐              ┌────────▼─────────┐
                       │ AI Service      │              │ PostgreSQL 16    │
                       │ FastAPI         │              │ + pgvector       │
                       │ all-MiniLM-L6-v2│              │ knowledge_base   │
                       │ port 8100       │              │ ai_interactions  │
                       └────────┬────────┘              └──────────────────┘
                                │
                       ┌────────▼────────┐
                       │ RabbitMQ 4.1    │
                       │ async backbone  │
                       └─────────────────┘
```

### Claude API Integration (5 call sites)

| Call Site | Purpose | Trigger |
|-----------|---------|---------|
| PRIORITY | Auto-assign ticket priority (HIGH/MEDIUM/LOW) with reason | Ticket creation |
| REFINE | Improve user's ticket description | User clicks "AI优化" |
| SUGGEST | Generate resolution suggestions for engineers | Engineer clicks "获取AI建议" |
| KB_GENERATE | Auto-generate KB article from resolution notes | Ticket resolved |
| VISION | Analyze uploaded screenshots (planned) | Image attachment |

### Vector Search

Knowledge base articles are embedded using `all-MiniLM-L6-v2` (384 dimensions) via a Python FastAPI sidecar. Similarity search uses pgvector's HNSW index with cosine distance. Minimum similarity threshold: 0.3.

## Quick Start

### Prerequisites

- Java 17
- Node.js 18+ and npm
- Docker
- Optional: LLM API key (`LLM_API_KEY` or legacy `CLAUDE_API_KEY`)
- Optional: Python 3.11+ for local AI service work

### 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

This starts PostgreSQL 16 with pgvector, creates the `zhigong` database, and runs `schema.sql` + `data.sql` (seed data with 6 users, 20 KB articles, 5 sample tickets).

### 2. Start the Platform Service

```bash
# Generate a JWT secret (required) if you are not using .env
export JWT_SECRET=$(openssl rand -base64 48)

# Load seed data on first run
SQL_INIT_MODE=always mvn -pl platform-java/platform-service spring-boot:run
```

Platform service starts on http://localhost:8081. After first run, drop `SQL_INIT_MODE=always` so seed data doesn't re-insert.

To enable vendor-compatible AI calls, set provider variables:
```bash
LLM_PROVIDER=anthropic LLM_API_KEY=... JWT_SECRET=$JWT_SECRET mvn -pl platform-java/platform-service spring-boot:run
```

### 3. Start the Gateway

```bash
mvn -pl platform-java/gateway spring-boot:run
```

Gateway starts on http://localhost:8080 and becomes the only public entrypoint for the browser.

### 4. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts on http://localhost:5173 with proxy to the gateway.

### 5. (Optional) Start the AI Service

```bash
cd ai-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8100
```

First run downloads the `all-MiniLM-L6-v2` model. The Phase 1 AI service exposes `/internal/ai/*` compatibility endpoints and preserves local embedding support.

### Docker Compose (all services)

```bash
docker compose up --build
```

Starts PostgreSQL + RabbitMQ + AI service + platform service + gateway. Add frontend separately with `npm run dev`.

## Demo Accounts

| Role | Username | Password | What they see |
|------|----------|----------|---------------|
| Customer | customer1 | demo123 | 提交工单, 我的工单, 知识库 |
| Customer | customer2 | demo123 | Same as above |
| Engineer | engineer1 | demo123 | 工单队列, 我的工单, 知识库, 待审核文章 |
| Engineer | engineer2 | demo123 | Same as above |
| Admin | admin1 | demo123 | All of the above + 数据分析 |

## API Endpoints

### Auth
- `POST /api/auth/login` — returns JWT token (24h expiry)

### Tickets
- `POST /api/tickets` — create ticket (CUSTOMER)
- `GET /api/tickets` — list tickets (role-filtered)
- `GET /api/tickets/{id}` — get ticket detail
- `POST /api/tickets/{id}/assign` — assign to self (ENGINEER)
- `POST /api/tickets/{id}/resolve` — resolve with notes (ENGINEER)

### AI
- `POST /api/ai/search` — vector similarity search against KB
- `POST /api/ai/refine` — improve ticket description with Claude
- `POST /api/ai/suggest` — get resolution suggestion from Claude

### Knowledge Base
- `GET /api/kb/published` — list published articles (all roles)
- `GET /api/kb/drafts` — list draft articles (ENGINEER/ADMIN)
- `POST /api/kb/{id}/publish` — publish draft (ENGINEER/ADMIN)
- `PUT /api/kb/{id}` — update article (ENGINEER/ADMIN)
- `DELETE /api/kb/{id}` — delete article (ENGINEER/ADMIN)

### Admin
- `GET /api/admin/stats` — dashboard statistics (ADMIN)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TypeScript, Ant Design 6, Vite, Zustand, Axios, dayjs |
| Gateway | Spring Cloud Gateway, Spring Boot 3.5 |
| Backend | Spring Boot 3.5, Java 17, Spring Security, JWT, JPA/Hibernate, jsoup, RabbitMQ starter |
| Database | PostgreSQL 16 + pgvector (HNSW index, vector(384)) |
| AI | Vendor-compatible LLM configuration (`anthropic`, `openai`, `deepseek`) + all-MiniLM-L6-v2 |
| Embedding | Python FastAPI AI service with sentence-transformers, LangChain, SQLAlchemy skeleton |
| Infrastructure | Docker Compose, Maven |

## Design System

See [DESIGN.md](DESIGN.md) for the full design system specification.

Key rules:
- Blue (#1677ff) = user actions (buttons, links, navigation)
- Purple (#722ed1) = AI-generated content (suggestions, confidence scores, generated articles)
- System fonts only (PingFang SC on macOS, Microsoft YaHei on Windows)
- 8px spacing grid
- Status indicators: 6px colored dot + text label

## Security

- JWT authentication with HS512 signing (24h expiry, startup validation requires `JWT_SECRET`)
- BCrypt password hashing
- Role-based access control (CUSTOMER, ENGINEER, ADMIN) with frontend route guards
- Atomic ticket assignment preventing race conditions
- LLM output sanitization via jsoup (strips all HTML before database storage)
- Input validation via typed DTOs with `@NotBlank`/`@Size` constraints on all endpoints
- Rate limiting: AI calls (10/user/minute), login attempts (5/IP/minute) via Guava RateLimiter
- Database credentials externalized via environment variables (no hardcoded defaults)
- Structured logging across all critical paths: auth, exceptions, ticket operations, rate limiting
- AI interaction logging for audit trail

## Project Structure

```
├── platform-java/
│   ├── gateway/                    # Spring Cloud Gateway public entrypoint
│   └── platform-service/           # Spring Boot business platform
│       └── src/main/java/com/ticket/zhigong/
│           ├── config/             # Security, request ID, LLM config
│           ├── controller/         # REST controllers
│           ├── dto/                # Request/response DTOs
│           ├── entity/             # JPA entities
│           ├── llm/                # Provider-compatible LLM client adapters
│           ├── repository/         # Spring Data JPA repositories
│           ├── security/           # JWT filter and helpers
│           └── service/            # Business logic services
├── ai-service/                     # FastAPI AI and embedding service
├── frontend/src/
│   ├── api/                         # Axios client + API functions
│   ├── components/                  # Shared components
│   ├── layouts/                     # App shell
│   ├── pages/                       # Login, Tickets, KB, Admin
│   └── stores/                      # Zustand auth store
├── infra/                          # Infrastructure notes and future manifests
├── .env.example                    # Template for required environment variables
├── docker-compose.yml              # Postgres + RabbitMQ + gateway + platform + AI
└── DESIGN.md            # Design system specification
```

## License

MIT
