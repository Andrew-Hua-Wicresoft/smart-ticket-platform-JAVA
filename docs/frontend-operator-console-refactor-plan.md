# Frontend Operator Console Refactor Plan

Generated: 2026-05-06
Branch: `design/frontend-operator-console-plan`
Source design: `DESIGN.md` v2, Operator's Console

## Goal

Apply Claude's v2 design direction as an engineering plan before touching production UI code.

The target is not "make it prettier." The target is to move the frontend away from default Ant Design Pro visuals into a distinct operator console:

- Light sidebar instead of AntD dark sidebar.
- Tokenized color system with CSS variables.
- Brand indigo for user actions.
- AI violet only for AI markers.
- AI content marked by `sigil + 2px left border + structured typography`, not purple panel flooding.
- First-class dark mode.
- 40px tabular KPI numbers.
- Command palette as a power-user navigation path.

## Current Evidence

The current implementation still reflects the old design:

- `frontend/src/layouts/AppLayout.tsx` uses `Sider theme="dark"` and inline hardcoded colors.
- `frontend/src/main.tsx` still sets Ant Design `colorPrimary: '#1677ff'`.
- `frontend/src/pages/tickets/TicketDetailPage.tsx` uses a purple AI header and hardcoded `#722ed1`.
- `frontend/src/pages/login/LoginPage.tsx` uses a card shadow and purple demo account block.
- `frontend/src/pages/admin/AdminStatsPage.tsx` still sizes KPI values around 30px.
- `frontend/src/components/MarkdownContent.tsx` owns page-level markdown presentation that should move toward reusable themed classes.

## What Already Exists

| Existing piece | Reuse decision |
| --- | --- |
| `AppLayout.tsx` role-aware nav, breadcrumbs, unread notification badge | Reuse behavior, replace visual shell and token usage. |
| `main.tsx` Ant Design `ConfigProvider` entrypoint | Reuse as theme injection point, expand into project theme provider. |
| `MarkdownContent.tsx` | Reuse markdown rendering, move style responsibility into theme CSS classes. |
| `StatusDot.tsx` / `PriorityBadge.tsx` | Reuse component boundaries, convert to tokenized styling. |
| `TicketDetailPage.tsx` AI suggestion logic | Reuse API/state behavior, replace panel rendering with `AiPanel` and `AiBlock`. |
| `AdminStatsPage.tsx` KPI data normalization | Reuse data logic, replace card rendering with `KpiCard`. |
| Existing API clients | Reuse for command palette search candidates where possible. Do not add backend API in this refactor. |

## Scope Challenge

This refactor will touch more than 8 frontend files if implemented fully. That is a smell if shipped as one undifferentiated diff.

Recommendation: make it a staged UI migration with a shared foundation first, then page-level migrations. This keeps the blast radius controlled and makes visual QA meaningful.

Rejected shortcut: directly patch inline styles in each page. It would be faster for one screen, but it keeps duplication and makes dark mode brittle.

Rejected overbuild: full design-system package, Storybook, or token compiler. That is a separate design platform project, not needed yet.

## Architecture Review

### Target Dependency Shape

```text
+-------------------------------+
| frontend/src/main.tsx         |
| AntD ConfigProvider + App     |
+---------------+---------------+
                |
                v
+-------------------------------+
| frontend/src/theme/           |
| tokens.ts                     |
| ThemeProvider.tsx             |
| index.css                     |
+---------------+---------------+
                |
                v
+-------------------------------+
| shared UI components          |
| Logo / PageHeader / KpiCard   |
| AiPanel / AiBlock             |
| CommandPalette                |
+---------------+---------------+
                |
                v
+-------------------------------+
| page migrations               |
| AppLayout / Login / Tickets   |
| Admin / KB / Notifications    |
+-------------------------------+
```

### Theme Strategy

Use Ant Design's existing `ConfigProvider` theme API instead of building a parallel styling runtime. The plan should map project tokens to Ant Design seed tokens and component tokens, then expose project-specific CSS variables for app shell and custom blocks.

Recommended structure:

```text
ThemeProvider
  |
  +-- reads persisted theme: "light" | "dark" | "system"
  +-- sets document.documentElement.dataset.theme
  +-- passes AntD algorithm: defaultAlgorithm | darkAlgorithm
  +-- enables AntD cssVar for faster dynamic switching
  +-- wraps <App />
```

Framework notes:

- Ant Design supports global theming through `ConfigProvider` token configuration and preset algorithms.
- Ant Design CSS variable mode is suitable for runtime theme switching because it reduces style re-serialization during theme changes.
- React transitions should only be used for non-blocking UI updates such as command palette result updates or theme changes, not for ordinary form state.

References:

- Ant Design ConfigProvider: https://ant.design/components/config-provider/
- Ant Design theme customization and algorithms: https://ant-design.antgroup.com/docs/react/customize-theme
- Ant Design CSS variables: https://5x.ant.design/docs/react/css-variables/
- React `startTransition`: https://react.dev/reference/react/startTransition

### Font Strategy

Claude's design proposes Inter Tight, Inter, PingFang SC/Microsoft YaHei, and Geist Mono.

Engineering recommendation: do not load external font CDNs in the first implementation PR. Enterprise environments often block external CDNs, and this app should work offline in internal networks.

Implementation:

- Define the target font stack in CSS variables now.
- Use system fallbacks immediately.
- Add self-hosted fonts later only if brand review shows the system fallback is not good enough.

This preserves the design direction without making the app dependent on third-party font availability.

### AI Marker Strategy

The AI marker system must be implemented as reusable components/classes, not repeated inline styles.

```text
AI content source
  |
  +-- priorityReason
  +-- AI suggestion markdown
  +-- AI KB draft metadata
  +-- AI-assisted table row
        |
        v
  AiBlock / AiPanel / .ai-row
        |
        v
  sigil + left border + AI text style + tokenized color
```

Important implementation detail: "Latin/numbers/code italic, Chinese upright" is not reliably expressible by a single `font-style: italic` rule. The first implementation should use `font-style: italic` only on AI markdown body containers if visual QA accepts the Chinese rendering. If Chinese readability is poor, change the rule to "AI prose uses regular CJK with sigil + border; Latin/code may be italic where explicitly wrapped."

## Code Quality Review

### Main Risks

1. Hardcoded inline styles will fight the token system.

Recommendation: after creating `theme/index.css`, migrate high-traffic components first and leave low-risk pages for later PRs. Do not attempt a one-shot hardcoded-color purge unless the PR is explicitly scoped as visual-only.

2. Command palette can become a mixed concern.

Recommendation: keep v1 command palette local and boring:

- Search route labels locally.
- Search current loaded ticket/KB data only when already available.
- Add API-backed global search later with a dedicated backend endpoint.

3. Dark mode can regress AntD overlays.

Recommendation: use AntD `App` and `ConfigProvider` consistently so `message`, `Modal`, and dropdown-like components inherit the right theme context.

4. Documentation can drift from implementation.

Recommendation: each UI implementation PR must update the "Implementation status" section in this plan or replace it with a completed checklist.

## Test Review

No dedicated frontend unit/E2E framework is currently configured beyond TypeScript build and ESLint. For this refactor, build and lint are not enough because the highest risk is visual and interaction regression.

### Code Path Coverage Plan

```text
CODE PATH COVERAGE
==================
[+] ThemeProvider
    |
    +-- [GAP] light initial theme from localStorage
    +-- [GAP] dark initial theme from localStorage
    +-- [GAP] system theme fallback when no user preference exists
    +-- [GAP] invalid localStorage value falls back safely

[+] AppLayout shell
    |
    +-- [GAP] role-based menu still filters correctly
    +-- [GAP] notification badge still updates
    +-- [GAP] selected menu key still matches nested routes
    +-- [GAP] light sidebar meets design tokens
    +-- [GAP] dark mode still preserves contrast

[+] CommandPalette
    |
    +-- [GAP] opens with Cmd+K / Ctrl+K
    +-- [GAP] closes with Escape
    +-- [GAP] keyboard navigation through results
    +-- [GAP] clicking a route result navigates
    +-- [GAP] no-results state is clear

[+] AiPanel / AiBlock
    |
    +-- [GAP] renders sigil with accessible label
    +-- [GAP] renders 2px AI left border
    +-- [GAP] markdown headings/code/lists remain legible
    +-- [GAP] Chinese AI prose remains readable

[+] KpiCard
    |
    +-- [GAP] value uses 40px tabular numerals
    +-- [GAP] zero/undefined values render as 0, not NaN
    +-- [GAP] color semantics match metric type
```

### User Flow Coverage Plan

```text
USER FLOW COVERAGE
==================
[+] Login flow
    |
    +-- [GAP] user can log in and sees new shell
    +-- [GAP] demo account block is neutral, not AI violet

[+] Engineer ticket flow
    |
    +-- [GAP] engineer opens /tickets and can scan 15-20 rows on 1080p
    +-- [GAP] engineer sorts/filters without layout regression
    +-- [GAP] engineer opens a ticket and requests AI diagnosis
    +-- [GAP] persisted AI diagnosis renders with new AI marker

[+] Customer self-service flow
    |
    +-- [GAP] customer opens ticket create page
    +-- [GAP] KB match cards remain clickable
    +-- [GAP] helpful article modal still lets user confirm/close correctly

[+] Admin analytics flow
    |
    +-- [GAP] admin opens /admin/stats
    +-- [GAP] KPI cards are equal height and readable in light/dark

[+] Knowledge base flow
    |
    +-- [GAP] published article markdown still renders headings/tables/code
    +-- [GAP] draft preview still renders markdown and raw edit remains editable
```

### Required Verification

Minimum implementation verification:

- `npm run build`
- Targeted ESLint for changed frontend files.
- Browser QA in light and dark themes for `/login`, `/tickets`, `/tickets/:id`, `/tickets/create`, `/kb`, `/kb/drafts`, `/admin/stats`, `/notifications`.

Recommended but not required in the first UI PR:

- Add Playwright smoke tests for login, shell navigation, dark toggle, command palette, and ticket detail AI panel.

## Performance Review

### Risks

- Theme switching can cause unnecessary style recalculation if app styles remain mostly inline.
- Command palette can spam API calls if it searches remote data on every keypress.
- External font loading can delay first paint or fail in internal networks.

### Mitigations

- Prefer CSS classes and variables over repeated inline style objects.
- Enable AntD `cssVar` mode for theme switching.
- Keep command palette v1 mostly local; if remote search is added, use debouncing and `useDeferredValue`.
- Avoid external font CDNs in the first implementation; use system fallback or self-hosted fonts only.

## Failure Modes

| Flow | Production failure | Covered by plan |
| --- | --- | --- |
| Theme startup | Invalid saved theme breaks initial render | Add fallback branch in `ThemeProvider`. |
| Dark mode | AntD dropdown/modal remains light | Use AntD `App` + `ConfigProvider`; QA overlays. |
| Command palette | Hotkey steals focus while typing in input | Ignore shortcut when target is input/textarea/contenteditable. |
| Command palette | Stale async results after rapid typing | Keep v1 local or use deferred/debounced remote calls. |
| AI markdown | Chinese italic readability is poor | Visual QA; adjust rule if needed. |
| Ticket queue | Density target causes text truncation | Add column width/truncation rules and tooltip for long titles. |
| KB markdown | Table overflow breaks card width | Keep existing markdown table overflow handling. |
| Theme persistence | localStorage unavailable | Try/catch and fall back to system/light. |

No critical silent failure is acceptable. If implementation finds a silent failure path, add a visible fallback before merging.

## Implementation Phases

### PR 1: Design Baseline

Status: this branch.

- Update `DESIGN.md` to Operator's Console v2.
- Update `CLAUDE.md` hard UI rules and current project state.
- Add this engineering plan.
- Do not change runtime frontend behavior.

Acceptance:

- Docs accurately describe design direction and known constraints.
- No old Claude worktree code or backend/infra deletion is included.

### PR 2: Theme Foundation

Files likely touched:

- `frontend/src/main.tsx`
- `frontend/src/theme/tokens.ts`
- `frontend/src/theme/ThemeProvider.tsx`
- `frontend/src/theme/index.css`

Work:

- Create light/dark CSS variables.
- Configure AntD theme tokens and algorithms.
- Add theme persistence and system fallback.
- Move global base styles out of component inline styles.

Acceptance:

- App builds.
- Light/dark theme toggles without page reload.
- Existing pages still function with old layout.

### PR 3: App Shell and Shared Components

Files likely touched:

- `frontend/src/layouts/AppLayout.tsx`
- `frontend/src/components/Logo.tsx`
- `frontend/src/components/PageHeader.tsx`
- `frontend/src/components/CommandPalette.tsx`
- `frontend/src/components/KpiCard.tsx`
- `frontend/src/components/AiPanel.tsx`
- `frontend/src/components/AiBlock.tsx`

Work:

- Replace dark sidebar with light sidebar.
- Add top bar command palette trigger.
- Add theme toggle.
- Add reusable KPI and AI components.
- Preserve role-based navigation and notification badge behavior.

Acceptance:

- No role loses access to expected routes.
- `Cmd+K` / `Ctrl+K` opens command palette.
- AI components have sigil and 2px border.

### PR 4: Page Migration

Files likely touched:

- `frontend/src/pages/login/LoginPage.tsx`
- `frontend/src/pages/tickets/TicketListPage.tsx`
- `frontend/src/pages/tickets/TicketDetailPage.tsx`
- `frontend/src/pages/tickets/TicketCreatePage.tsx`
- `frontend/src/pages/admin/AdminStatsPage.tsx`
- `frontend/src/pages/kb/KbListPage.tsx`
- `frontend/src/pages/kb/KbDraftsPage.tsx`
- `frontend/src/pages/notifications/NotificationListPage.tsx`
- `frontend/src/components/MarkdownContent.tsx`
- `frontend/src/components/StatusDot.tsx`
- `frontend/src/components/PriorityBadge.tsx`

Work:

- Replace hardcoded colors with tokens.
- Replace AI purple panels with `AiPanel` / `AiBlock`.
- Normalize table, KPI, markdown, status, and priority visuals.
- Keep business behavior unchanged.

Acceptance:

- All high-traffic pages match the v2 design rules.
- Markdown tables, code blocks, and headings remain usable.
- Ticket sorting/filtering behavior remains unchanged.

### PR 5: Visual QA and Hardening

Work:

- Run full browser QA in light and dark.
- Fix contrast and density issues.
- Optional: add Playwright smoke tests if the team wants this UI baseline to stay stable.

Acceptance:

- No known P1/P2 visual or interaction regressions.
- `npm run build` passes.
- Targeted lint passes or existing unrelated lint debt is documented.

## NOT In Scope

- Backend API changes. The refactor should not require Java or Python changes.
- Full global search backend for command palette. Use local route/actions first.
- Mobile-first redesign. Current product remains desktop-first.
- Storybook or a separate component package. Useful later, too much ceremony now.
- Replacing Ant Design. AntD 6 stays; we theme it rather than rebuild controls.
- LLM prompt changes. AI behavior stays unchanged; only presentation changes.
- Service catalog, SLA, approval, CMDB, or commercial ITSM features. Those belong to the next product maturity phase.

## Worktree Parallelization Strategy

| Step | Modules touched | Depends on |
| --- | --- | --- |
| Theme foundation | `frontend/src/theme`, `frontend/src/main.tsx` | PR 1 |
| App shell | `frontend/src/layouts`, shared components | Theme foundation |
| Shared visual components | `frontend/src/components` | Theme foundation |
| Ticket pages | `frontend/src/pages/tickets` | App shell, shared visual components |
| Admin and KB pages | `frontend/src/pages/admin`, `frontend/src/pages/kb` | Shared visual components |
| Login and notifications | `frontend/src/pages/login`, `frontend/src/pages/notifications` | App shell |
| QA hardening | all changed frontend modules | all migrations |

Parallel lanes after theme foundation:

- Lane A: App shell + command palette.
- Lane B: Shared visual components.
- Lane C: Login + notifications after shell stabilizes.
- Lane D: Ticket pages after AI components stabilize.
- Lane E: Admin + KB pages after KPI/Markdown components stabilize.

Execution order:

```text
PR 1 docs
  |
  v
PR 2 theme foundation
  |
  +--> Lane A shell
  +--> Lane B components
          |
          +--> Lane C login/notifications
          +--> Lane D tickets
          +--> Lane E admin/kb
                    |
                    v
              QA hardening
```

Conflict flags:

- `AppLayout.tsx` should be owned by one lane only.
- `MarkdownContent.tsx` should be owned by the KB/admin lane or shared component lane, not both.
- `TicketDetailPage.tsx` should wait for `AiPanel` / `AiBlock` to avoid duplicated AI styling.

## Review Summary

- Step 0 Scope Challenge: scope reduced from one-shot redesign to staged migration.
- Architecture Review: 4 risks identified, all handled in plan.
- Code Quality Review: hardcoded styles and mixed concerns identified, component extraction planned.
- Test Review: coverage diagram produced, gaps identified for theme, shell, command palette, AI components, KPI, and key user flows.
- Performance Review: theme switching, command palette, and fonts reviewed.
- NOT in scope: written.
- What already exists: written.
- TODOs.md updates: skipped, this plan captures the work directly and there is no existing `TODOS.md`.
- Failure modes: 8 failure modes listed, 0 accepted as silent.
- Outside voice: skipped.
- Parallelization: 5 post-foundation lanes, foundation remains sequential.
- Lake Score: 4/4 recommendations choose the complete staged path over shortcuts.
