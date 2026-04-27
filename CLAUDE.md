# 智能工单系统 — Project Rules

## Design System
Always read DESIGN.md before making any visual or UI decisions.
All font choices, colors, spacing, and aesthetic direction are defined there.
Do not deviate without explicit user approval.
In QA mode, flag any code that doesn't match DESIGN.md.

Key rules:
- Blue (#1677ff) = user actions only
- Purple (#722ed1) = AI-generated content only
- Never mix blue and purple semantics
- System fonts only, no external font loading
- 8px spacing grid, comfortable density

## Current state (2026-04-27)

Phase 0–3 complete on `refactor/phase-3-ai-service-boundary`. Phase 4
(Nacos / Sentinel / Kubernetes) not started.

Stack: React + Vite frontend, Java Gateway (Spring Cloud Gateway), Java
Platform Service (Spring Boot 3.5 + JPA), Python AI Service (FastAPI +
DeepSeek + sentence-transformers), PostgreSQL 16 + pgvector, RabbitMQ.

Local dev runs hybrid: Docker for `postgres` / `rabbitmq` / `ai-service`
(see `docker-compose.yml`); native Maven for `gateway` (`:8080`) and
`platform-service` (`:8081`); native `npm run dev` for `frontend`
(`:5173`). Build context for Docker is trimmed by the root
`.dockerignore` and `ai-service/.dockerignore`.

Service health:
- frontend → http://localhost:5173/
- gateway → http://localhost:8080/actuator/health
- platform → http://localhost:8081/actuator/health
- ai-service → http://localhost:8100/health

Demo credentials (all `demo123`): `admin1` (ADMIN), `engineer1` /
`engineer2` (ENGINEER), `customer1` / `customer2` / `customer3` (CUSTOMER).
Seeded via `platform-java/platform-service/src/main/resources/data.sql`.

Schema bootstrap: `spring.sql.init.mode` defaults to `always` so a stale
local DB is self-healing on cold start. `schema.sql` is idempotent
(`CREATE TABLE IF NOT EXISTS`); `data.sql` uses `INSERT … WHERE NOT
EXISTS`. Override with `SQL_INIT_MODE=never` for production-style runs.

`.env` is gitignored and contains a real DeepSeek key. Never echo,
commit, or paste it. Use `.env.example` for sharing variable shape.

QA reports live in `.gstack/qa-reports/`.

## Skill routing

When the user's request matches an available skill, ALWAYS invoke it using the Skill
tool as your FIRST action. Do NOT answer directly, do NOT use other tools first.
The skill has specialized workflows that produce better results than ad-hoc answers.

Key routing rules:
- Product ideas, "is this worth building", brainstorming → invoke office-hours
- Bugs, errors, "why is this broken", 500 errors → invoke investigate
- Ship, deploy, push, create PR → invoke ship
- QA, test the site, find bugs → invoke qa
- Code review, check my diff → invoke review
- Update docs after shipping → invoke document-release
- Weekly retro → invoke retro
- Design system, brand → invoke design-consultation
- Visual audit, design polish → invoke design-review
- Architecture review → invoke plan-eng-review
