# Design System — 智能工单系统 (v2 · Operator's Console)

## Product Context
- **What this is:** AI-powered IT support ticket system that auto-suggests solutions, learns from resolutions, and builds a self-improving knowledge base
- **Who it's for:** Internal users at a Chinese company. Three roles: customers (submit tickets), engineers (resolve with AI assistance), admins (analytics)
- **Space/industry:** Chinese enterprise IT helpdesk (peers: Feishu Helpdesk, Udesk, Meiqia, DingTalk workorder)
- **Project type:** Internal workspace tool (React 19 + Ant Design 6 + TypeScript)

## Aesthetic Direction
- **Direction:** Editorial-Industrial Hybrid
- **Decoration level:** Minimal (typography, spacing, and hairline borders do all the work — no shadows, gradients, illustrations, or background blobs)
- **Mood:** A workspace engineers want to live in. Linear's typographic discipline meets Bloomberg Terminal's data density. The product reads as an indie engineering tool, not an Ant Design Pro template. AI is a structural marker, not a colored panel.
- **Reference sites:** Linear (linear.app), Vercel (vercel.com), Geist Design (vercel.com/geist), Raycast (raycast.com)

## The Five Deliberate Risks (codified)

These are the choices that give this product its own face. They are NOT suggestions — they are constraints. Implementations that violate any of these are non-compliant.

1. **Light sidebar, no AntD Pro dark sidebar.** `#fafafa` surface with 1px right border. Brand recognition signal #1.
2. **AI generated text uses italic (Latin/numbers/code only).** Plus a `✦` sigil prefix. Plus a 2px left border on AI content blocks. Chinese characters never go italic — sigil + border do the work for CJK.
3. **KPI numbers are 40px Inter Display weight 600 with tabular-nums.** Bloomberg-grade. Color-coded by metric type (brand for action items, success for positive, warning for in-progress, AI for AI metrics).
4. **`⌘K` command palette is a first-class navigation primitive.** Top-bar entry, global hotkey, searches tickets/KB/people/actions.
5. **Dark mode is a v1 first-class citizen.** Not deferred. All tokens are CSS variables. Theme switch via `[data-theme="dark"]` on `<html>`.

## Typography
- **Display/Hero:** **Inter Tight** (weight 600, letter-spacing -0.025em to -0.035em) — used for page titles, hero headings, KPI numbers
- **Body (Latin + numbers):** **Inter** (weight 400 base, 500 medium, 600 semibold) with feature-settings `'cv02', 'cv03', 'cv04', 'cv11'` enabled
- **Body (Chinese):** **PingFang SC, Microsoft YaHei** (system fonts — 0ms load, native CJK rendering)
- **Data/Tables/Code/IDs:** **Geist Mono** with `font-variant-numeric: tabular-nums`
- **AI-generated body text:** Inter italic for Latin/numbers/code; Chinese remains regular weight (sigil + left-border carries the marker semantics)
- **Loading strategy:**
  - Inter / Inter Tight via Bunny Fonts CDN (privacy-respecting, GDPR-clean, no Google) OR self-host
  - Geist Mono via jsDelivr CDN OR self-host: `https://cdn.jsdelivr.net/npm/geist@1.4.0/dist/fonts/geist-mono/style.css`
  - PingFang SC / Microsoft YaHei = system fallback, never loaded over network
- **Scale:**
  - 11px — uppercase labels, group titles, metadata caps
  - 12px — captions, timestamps, helper text, similarity scores
  - 13px — table cells, secondary body, button text
  - 14px — body text, form inputs (BASE size)
  - 16px — section subheaders, ticket detail body
  - 20px — section headers
  - 28px — page titles (Inter Tight 600)
  - 36px — section heroes inside pages
  - 40px — KPI dashboard numbers (Inter Tight 600, tabular-nums)
  - 72px — landing/login hero (Inter Tight 600, letter-spacing -0.035em)
- **Weights:** 400 (body), 500 (medium emphasis, table titles), 600 (semibold, all display sizes)
- **Italic rule:** ONLY for AI-generated content (Latin/numbers/code). Never use italic for emphasis, quotes, or anything else. This protects the AI semantic.

## Color
- **Approach:** Aggressive monochrome — true neutral zinc/slate ramp (NOT Ant Design's blue-grey). Only TWO chromatic colors: brand indigo and AI violet. Status colors used semantically only.
- **All tokens are CSS custom properties** under `:root` (light) and `[data-theme="dark"]` (dark).

### Light theme tokens

| Token | Value | Usage |
|---|---|---|
| `--bg` | `#ffffff` | Body background, top bar |
| `--surface` | `#fafafa` | Sidebar, table headers, subtle fills |
| `--surface-raised` | `#ffffff` | Cards, panels, inputs |
| `--border` | `#e5e5e5` | Hairlines, default borders |
| `--border-strong` | `#d4d4d4` | Hover borders, button borders |
| `--text-primary` | `#0a0a0a` | Headings, primary text |
| `--text-secondary` | `#525252` | Body text, descriptions |
| `--text-tertiary` | `#a3a3a3` | Captions, placeholders, metadata |
| `--brand` | `#5b6cff` | All user actions (buttons, links, active nav, focus rings) |
| `--brand-hover` | `#4f5fe8` | Hover state of brand actions |
| `--brand-bg` | `#eef0ff` | Selected nav background, focus ring background |
| `--ai` | `#7c3aed` | ✦ sigil, AI left-borders, match scores, AI buttons |
| `--ai-soft` | `#a78bfa` | Hover borders on AI suggestion cards |
| `--ai-bg` | `#f5f0ff` | Reserved for AI badges only — NEVER as full panel background |
| `--success` | `#16a34a` | Resolved tickets, published KB, positive deltas |
| `--success-bg` | `#ecfdf5` | Success badges/banners only |
| `--warning` | `#d97706` | In-progress, pending action |
| `--warning-bg` | `#fffbeb` | Warning badges only |
| `--danger` | `#dc2626` | High priority, errors, validation failures |
| `--danger-bg` | `#fef2f2` | Error banners only |

### Dark theme tokens

| Token | Value |
|---|---|
| `--bg` | `#0a0a0a` |
| `--surface` | `#141414` |
| `--surface-raised` | `#1c1c1c` |
| `--border` | `#27272a` |
| `--border-strong` | `#3f3f46` |
| `--text-primary` | `#fafafa` |
| `--text-secondary` | `#a1a1aa` |
| `--text-tertiary` | `#71717a` |
| `--brand` | `#818cf8` |
| `--brand-hover` | `#a5b4fc` |
| `--brand-bg` | `#1e1b4b` |
| `--ai` | `#a78bfa` |
| `--ai-soft` | `#c4b5fd` |
| `--ai-bg` | `#1e1b3a` |
| `--success` | `#22c55e` |
| `--warning` | `#f59e0b` |
| `--danger` | `#ef4444` |

### Color usage rules
1. **Brand indigo (`#5b6cff`) = user-initiated actions only** (submit, assign, navigate, focus). Replaces the old AntD `#1677ff`.
2. **AI violet (`#7c3aed`) = AI-touched moments only** (sigil prefix, left-border on AI blocks, match scores, AI buttons). Never as a full panel background. Never on user actions.
3. **Hairline borders replace shadows.** Cards get `border: 1px solid var(--border)`, never `box-shadow`. Modals/dropdowns can use shadows only when elevation is semantically necessary.
4. **Status colors (green/orange/red)** follow standard helpdesk semantics. Used ONLY for status (resolved/pending/error), never for decoration or branding.
5. **No decorative gradients.** Including the AI panel header. The old `linear-gradient(135deg, #722ed1 0%, #531dab 100%)` pattern is banned. Skeleton loading shimmer is the only allowed gradient exception.
6. **No `bg` variants as full panel fills.** `--ai-bg`, `--success-bg`, `--warning-bg`, `--danger-bg` are for compact badges and banners only — never as panel-spanning backgrounds.

## The AI Marker System (the big idea)

This replaces the old "AI = purple panel background" pattern. AI-generated content earns its identity through structure, not color flooding.

### The four marks

1. **`✦` Sigil prefix (Unicode `U+2726`, FOUR TEARDROP-SPOKED ASTERISK)** — applied to:
   - AI panel headers (e.g., `✦ AI 诊断助手`)
   - Table rows where AI has prepared a suggestion (prefix in title cell)
   - AI-generated KB article badges
   - Any AI-authored field label (`✦ 优先级原因`)

2. **2px left border (`var(--ai)`)** — applied to:
   - AI side panels (the whole panel container)
   - Inline AI content blocks within a card (e.g., `priorityReason` block)
   - Table rows where AI has prepared a suggestion (`box-shadow: inset 2px 0 0 var(--ai)` on the first cell)

3. **Italic body for AI-generated text** — applied to:
   - All AI-generated paragraph/body content
   - Latin characters, numbers, and code switch to italic
   - Chinese characters remain upright (italic CJK is hostile to readability) — sigil + left border carry the semantic for CJK
   - Headings inside AI content reset to `font-style: normal` for hierarchy
   - Code (`<code>`) inside AI content resets to `font-style: normal` for legibility

4. **AI accent color for marker elements only** — sigil glyphs, left borders, match scores, AI button fills. NEVER as a full surface fill.

### Anti-patterns (banned)

- ❌ Purple or decorative gradient headers (the old `linear-gradient(135deg, #722ed1, #531dab)` pattern)
- ❌ Full-width `--ai-bg` background on a panel body
- ❌ Italic on non-AI content (e.g., quotes, emphasis, captions)
- ❌ Italic on Chinese characters
- ❌ Sigil without context (decorative use)
- ❌ Mixing brand indigo and AI violet on the same element

## Spacing
- **Base unit:** 8px
- **Density:** Comfortable. Engineers must see 15-20 ticket rows above the fold on a 1080p screen. KPI cards collapsible if user opts in.
- **Scale:**
  - 2xs: 4px — icon padding, badge spacing
  - xs: 8px — compact inline gaps, button gap
  - sm: 12px — form field spacing, card internal vertical padding
  - md: 16px — default card padding, inter-component spacing
  - lg: 24px — section spacing, page-level horizontal padding
  - xl: 32px — major section breaks
  - 2xl: 48px — page-level vertical breathing room

## Layout
- **Approach:** Light-first, command-driven, hairline-disciplined
- **App shell:** Fixed light sidebar (240px, `--surface`, 1px right border `--border`) + fixed top bar (52px, `--bg`, 1px bottom border) + scrollable content (24px padding, `--bg`)
- **Sidebar contents:**
  - Logo block: 28px square logo `--text-primary` background, white "智" glyph + product name "智能工单"
  - Group sections (e.g., 工单管理 / 知识库 / 系统) with 11px uppercase tertiary labels
  - Nav items: 7px 10px padding, 13px text, `--text-secondary` default, `--brand` text + `--brand-bg` background when active, hover = `--surface-raised` background
  - Optional badge counters (e.g., "待审核文章 [7]") use `--ai-bg` + `--ai` text in 10px pill
- **Top bar:**
  - Left: breadcrumb (`--text-tertiary` separator slashes, `--text-primary` current segment)
  - Right (left to right): `⌘K` command palette button, user avatar dropdown
- **Command palette (`⌘K`):**
  - Triggered by `⌘K` (Mac) or `Ctrl+K` (Win)
  - Modal centered, 640px wide, 1px border, soft shadow allowed for elevation
  - Searches: tickets (by ID/title/description), KB articles (by title/content), pages (by name), actions (assign/resolve/refresh)
  - Renders results as 14px rows with category prefix tag
- **Grid:** 1fr content with sidebars/panels typically `360px` fixed. KPI grids: 4 columns at desktop.
- **Max content width:** No max (fills available width on desktop monitors).
- **Border radius:**
  - sm: 4px — badges, square priority chips
  - md: 6px — buttons, inputs, KB suggestion cards, status badges
  - lg: 8px — cards, panels, KPI tiles, table containers
  - xl: 12px — login card, modal containers
  - full: 999px — avatars, status dot containers, badge counters

## Motion
- **Approach:** Functional minimal. Motion communicates state change, never decorates.
- **Easing:** enter `ease-out` / exit `ease-in` / hover `ease` (default 0.12s)
- **Duration:**
  - micro: 100-120ms — hover, focus ring, button press
  - short: 150-180ms — fade-in panels, tooltip, dropdown
  - medium: 200-240ms — modal entrance, sidebar collapse, theme switch
- **Specific animations:**
  - Theme toggle: `transition: background-color 0.15s, color 0.15s` on `body`
  - Panel reveals: opacity 0→1, 180ms ease-out
  - Loading: skeleton shimmer (linear-gradient sweep, 1.5s loop) — only one variant project-wide
  - AI panel content: fade-in 200ms after async load
- **Rules:**
  - No bounce, spring, or playful curves
  - No animation on initial page load (instant render)
  - Respect `prefers-reduced-motion: reduce` — kill all transitions to 0.01ms
  - No animations longer than 300ms anywhere

## Component Patterns

### KPI Stat Card
- `border: 1px solid var(--border)`, `border-radius: 8px`, `padding: 20px`
- `background: var(--surface-raised)`
- Label: 11px uppercase, letter-spacing 0.08em, `--text-tertiary`, weight 500
- Value: **40px Inter Tight 600**, `font-variant-numeric: tabular-nums`, letter-spacing -0.025em
- Optional sub-metric: 12px `--text-tertiary` with `<strong>` for delta colored by direction

### Ticket Table
- Container: `border: 1px solid var(--border)`, `border-radius: 8px`, `background: var(--surface-raised)`
- Header: `--surface` background, 11px uppercase labels with letter-spacing 0.08em, `--text-tertiary`, weight 500
- Row height: ~42px (10px 14px padding, 13px text)
- Row hover: background `--surface`
- Priority column: square chip + label. High = `--danger`, Medium = `--warning`, Low = `--success`. 10×10px chip with 2px radius.
- Title column: 13px medium weight. AI-touched rows prefix with `<span class="sigil">✦</span>` + the row gets `box-shadow: inset 2px 0 0 var(--ai)` on the first cell.
- Status column: dot (6px, status-colored) + label
- Submitter: 13px regular
- Time column: 12px `--text-tertiary`, `font-variant-numeric: tabular-nums`

### AI Side Panel
- `border: 1px solid var(--border)`, `border-radius: 8px`, `background: var(--surface-raised)`
- **NO purple background fill. NO gradient header. NO box-shadow.**
- Panel head: 14px 16px padding, 1px bottom border `--border`. Contents: `✦` sigil (`--ai`, 14px) + title (13px semibold) + meta tag right-aligned (10px uppercase tertiary)
- Panel body: 16px padding
- Body content: 13px italic `--text-secondary`, line-height 1.7
  - `<strong>` resets to upright + `--text-primary` weight 500
  - `<h4>` resets to upright + `--text-primary` weight 600
  - `<code>` resets to upright + Geist Mono + `--surface` background
  - Links: `--brand`, no underline, upright

### AI Inline Block (within a card)
- `padding: 16px 16px 16px 18px`, `border-left: 2px solid var(--ai)`, no background fill
- Header: 12px uppercase `--ai`, weight 500, with sigil prefix (sigil renders upright)
- Body: 13px italic `--text-secondary`, line-height 1.7

### Status Pill (status indicator)
- 6px dot + 12px label, `--text-secondary`, weight 500
- Dot colors: open = `--brand`, in_progress = `--warning`, resolved = `--success`, closed = `--text-tertiary`

### Priority Badge
- 10px square chip (2px radius) + 12px label, weight 500
- Color matches the priority severity, applied to both chip and label

### Buttons
- All: 8px 14px padding, 6px radius, 13px text, weight 500, font-family inherit
- Primary (user action): `--brand` background, white text. Hover: `--brand-hover`.
- Secondary: `--surface-raised` background, `--text-primary` text, 1px `--border-strong` border. Hover: `--surface` background.
- AI: `--ai` background, white text. Hover: `filter: brightness(1.1)`.
- Ghost: transparent, `--text-secondary` text. Hover: `--surface` background, `--text-primary` text.
- All transitions: `all 0.12s`

### Form Inputs
- Height: ~32px (8px 12px padding)
- `background: var(--surface-raised)`, `border: 1px solid var(--border)`, `border-radius: 6px`, 13px text
- Focus: `border-color: var(--brand)`, `box-shadow: 0 0 0 3px var(--brand-bg)`, no ring outline
- Error state: border `--danger`, 12px `--danger` error text below

### Command Palette Trigger (in top bar)
- `padding: 5px 10px`, `background: var(--surface)`, `border: 1px solid var(--border)`, `border-radius: 6px`
- 12px text `--text-tertiary`
- Embedded `<kbd>` for the shortcut: Geist Mono 10px, 1px 5px padding, `--bg` background, 1px `--border`, 3px radius

### Logo (single component, parametric size)
- Square with `--text-primary` background, `--bg` (white in light, near-black in dark) glyph "智"
- Sizes: 28px (sidebar), 48px (login)
- Border radius: scales linearly (6px at 28px → 12px at 48px)

## Accessibility
- Desktop-only layout (1280px+ optimized; gracefully degrades to 1024px; below shows "请使用桌面浏览器访问" guard)
- Keyboard navigation: all interactive elements focusable, focus ring uses `--brand` color
- `⌘K` is the primary keyboard primitive — also support `⌘/` for help, `?` for shortcut sheet
- ARIA labels on all icon-only buttons and the sigil glyph (`aria-label="AI 生成内容标记"`)
- Minimum touch target: 32px (desktop tool — engineers use mice/trackpads, not touch)
- Color contrast: all text/background combinations meet WCAG AA in BOTH light and dark modes
- `prefers-reduced-motion: reduce` respected — all transitions clamped to 0.01ms
- `prefers-color-scheme` respected on first load (then user override persists in localStorage)
- Italic AI body: ensure font-feature-settings preserve readability; verify in Edge/Chrome/Safari/Firefox

## Implementation Notes (for the refactor)

### File structure
- `frontend/src/theme/tokens.ts` — exports all CSS variable names + JS-readable values for Ant Design ConfigProvider
- `frontend/src/theme/ConfigProvider.tsx` — wraps app, applies AntD theme override + handles `[data-theme]` toggle
- `frontend/src/theme/index.css` — defines `:root` and `[data-theme="dark"]` blocks, all base resets, AI marker classes (`.ai-text`, `.sigil`, `.ai-block`, `.ai-row`)
- `frontend/src/components/AiPanel.tsx` — shared side-panel component (used by TicketDetailPage and TicketCreatePage)
- `frontend/src/components/AiBlock.tsx` — inline AI content block with left border + sigil header
- `frontend/src/components/KpiCard.tsx` — shared KPI tile component
- `frontend/src/components/Logo.tsx` — parametric logo (size prop)
- `frontend/src/components/CommandPalette.tsx` — `⌘K` modal
- `frontend/src/components/PageHeader.tsx` — page title + actions row

### What to delete
- The `<style>` injection inside `AppLayout.tsx:112` — move to `index.css`
- The 22-line ReactMarkdown style mapping inside `TicketDetailPage.tsx:226-246` — move to scoped `.ai-markdown` selectors in `index.css`
- The purple gradient header in `TicketDetailPage.tsx:207`
- The purple-background demo account block in `LoginPage.tsx:67-75` — replace with neutral `--surface` + 1px `--border`

### Migration checklist
1. Wire ConfigProvider with new tokens (replaces AntD blue with brand indigo)
2. Replace all hardcoded `#XXXXXX` in inline styles with `var(--token-name)`
3. Replace all `boxShadow` on cards with `border: 1px solid var(--border)`
4. Convert sidebar from `theme="dark"` to light theme + manual styling per spec
5. Replace dual AI panels with one shared `<AiPanel>` component (no gradient)
6. Add italic + sigil + left-border treatment to AI text via `<AiBlock>` and ReactMarkdown wrapper
7. Bump KPI card values to 40px Inter Tight
8. Wire `⌘K` command palette + global hotkey listener
9. Add theme toggle in top bar + localStorage persistence + `prefers-color-scheme` initial
10. Test all pages in both light AND dark themes before claiming done

## Decisions Log
| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-03-30 | Initial v1: Industrial/Utilitarian + purple AI accent | Original system, AntD Pro defaults baseline |
| 2026-05-06 | **v2 redesign: "Operator's Console"** | User feedback: v1 read as "AntD Pro template," lacked product personality. Competitive research (Linear / Vercel / Raycast / Cursor / v0) showed "indie engineering tool" aesthetic is the differentiator for AI-native B2B in 2026. |
| 2026-05-06 | Light sidebar (Risk #1) | Every Chinese enterprise tool uses AntD Pro dark sidebar. Going light = instant differentiation signal. |
| 2026-05-06 | AI marker system: ✦ + italic + left border (Risk #2) | Eliminates "purple panel" pattern that was identified as AI slop in v1's own anti-pattern list. Italic for AI body is unique in Chinese SaaS — becomes visual fingerprint. CJK exempt from italic for readability. |
| 2026-05-06 | KPI numbers 40px Inter Tight (Risk #3) | Bloomberg Terminal-grade weight signals "this is a power tool." v1's 30px was too tepid. |
| 2026-05-06 | `⌘K` command palette (Risk #4) | Linear/Notion/Raycast standard. Engineers use it once, can't go back. Self-evangelism vector. |
| 2026-05-06 | Dark mode v1, not deferred (Risk #5) | All tokens are CSS variables — `[data-theme="dark"]` is near-zero marginal cost. Engineer audience strongly prefers dark. |
| 2026-05-06 | Brand indigo `#5b6cff` replaces AntD blue `#1677ff` | Every Chinese SaaS uses AntD blue. Indigo signals "not a template project" in 1 second of viewing. |
| 2026-05-06 | Inter Tight + Inter + PingFang + Geist Mono | Linear/Vercel typographic stack. Bunny Fonts for privacy. PingFang remains system fallback for 0ms CJK. |
| 2026-05-06 | Hairline borders replace shadows on cards | Linear's signal of "this is a serious tool, not a marketing site." |

## Design System Preview
- Preview file: [/tmp/design-consultation-preview-1778032822.html](file:///tmp/design-consultation-preview-1778032822.html)
- Includes: hero, palette, typography specimen, AI marker before/after, KPI cards, components, full mockups (queue / detail / create / login), light/dark toggle
- Generated: 2026-05-06 by `/design-consultation`

## Next Step (NOT done by this skill)
This file documents the design system. The frontend code does NOT yet match this spec. To implement:
1. Run `/plan-eng-review` to architect the migration
2. Implement per the migration checklist above (~2 days CC time)
3. Run `/design-review` after to verify compliance
