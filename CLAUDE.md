# 智能工单系统 — Project Rules

## Design System (v2 · Operator's Console)
Always read DESIGN.md before making any visual or UI decisions.
All tokens, fonts, colors, spacing, and the AI marker system are defined there.
Do not deviate without explicit user approval.
In QA mode, flag any code that doesn't match DESIGN.md.

Hard rules (any violation is a regression):
- All colors via CSS variables (`var(--brand)`, `var(--ai)`, etc.). NEVER hardcode hex.
- Brand indigo `--brand` (#5b6cff light / #818cf8 dark) = user actions only.
- AI violet `--ai` (#7c3aed light / #a78bfa dark) = AI marker only — used as sigil color, 2px left border, match scores. NEVER as full panel background.
- `✦` sigil (Unicode U+2726) prefixes all AI-touched labels, headers, and table titles.
- AI generated body text is `font-style: italic` for Latin/numbers/code. Chinese characters stay upright.
- 2px left border on all AI content blocks/panels.
- Sidebar is LIGHT (`--surface` background, 1px border) — never dark theme.
- Cards use 1px hairline borders, NEVER `box-shadow`.
- Typography: Inter Tight (display) + Inter (body Latin) + PingFang SC/Microsoft YaHei (CJK) + Geist Mono (data/code).
- KPI numbers are 40px Inter Tight 600 with `font-variant-numeric: tabular-nums`.
- Dark mode is first-class (`[data-theme="dark"]` on `<html>`). Never test only in light.
- 8px spacing grid, 24px page padding, target 15-20 ticket rows visible above the fold.
- No decorative gradients. Skeleton loading shimmer is the only allowed gradient exception.

## Current state (2026-05-06)

Phase 0-4 have been delivered into `main`: Java Gateway is the only public API entrypoint, Java Platform Service owns business domains, Python is the internal AI service, RabbitMQ drives async AI workflows, and Phase 4 adds optional Nacos/Sentinel governance plus Docker/Kubernetes/Helm/CI scaffolding.

Stack: React 19 + Vite frontend, Java Gateway (Spring Cloud Gateway), Java Platform Service (Spring Boot 3.5 + JPA), Python AI Service (FastAPI + DeepSeek-compatible provider + sentence-transformers), PostgreSQL 16 + pgvector, RabbitMQ.

Local dev runs hybrid: Docker for `postgres` / `rabbitmq` / `ai-service`; native Maven for `gateway` (`:8080`) and `platform-service` (`:8081`); native `npm run dev` for `frontend` (`:5173`). Governance services are opt-in through the Compose `governance` profile.

Demo credentials (all `demo123`): `admin1` (ADMIN), `engineer1` / `engineer2` (ENGINEER), `customer1` / `customer2` / `customer3` (CUSTOMER). Seeded via `platform-java/platform-service/src/main/resources/data.sql`.

`.env` is gitignored and may contain real local secrets. Never echo, commit, or paste it. Use `.env.example` for sharing variable shape.

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
