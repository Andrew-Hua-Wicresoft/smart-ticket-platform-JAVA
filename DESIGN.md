# Design System — 智能工单系统

## Product Context
- **What this is:** AI-powered IT support ticket system that auto-suggests solutions, learns from resolutions, and builds a self-improving knowledge base
- **Who it's for:** Internal users at a Chinese company. Three roles: customers (submit tickets), engineers (resolve with AI assistance), admins (analytics)
- **Space/industry:** Chinese enterprise IT helpdesk (peers: Feishu Helpdesk, Udesk, Meiqia, DingTalk workorder)
- **Project type:** Internal workspace tool (React 18 + Ant Design 5 + TypeScript)

## Aesthetic Direction
- **Direction:** Industrial/Utilitarian
- **Decoration level:** Minimal (typography and spacing do the work, no decorative elements)
- **Mood:** Clean, structured, data-dense. A workspace engineers want to live in. Bloomberg Terminal clarity meets Feishu's clean lines. The AI layer (purple) is the one deliberate visual break from utility.
- **Reference sites:** Ant Design Pro (preview.pro.ant.design), Feishu Helpdesk (feishu.cn), Udesk

## Typography
- **Display/Hero:** System font stack — same as body (internal tool, not brand-facing)
- **Body:** `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', sans-serif`
- **UI/Labels:** Same as body
- **Data/Tables:** Same as body with `font-variant-numeric: tabular-nums` for number alignment
- **Code:** `SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace`
- **Loading:** No external fonts. System fonts load in 0ms. PingFang SC renders natively on macOS, Microsoft YaHei on Windows.
- **Scale:**
  - 12px — captions, metadata, timestamps, AI confidence scores
  - 14px — body text, table cells, form inputs, buttons (base size)
  - 16px — subtitles, ticket titles in detail view
  - 20px — page titles, section headers
  - 24px — major section headers (used sparingly)
  - 30px — dashboard KPI numbers (tabular-nums, weight 600)
- **Weights:** 400 (body), 500 (subtitles, table headers, nav items), 600 (page titles, KPI numbers)

## Color
- **Approach:** Restrained with one intentional accent layer
- **Primary:** `#1677ff` — Ant Design 5 default blue. Brand actions: buttons, links, active states, selected items.
- **Primary hover:** `#4096ff`
- **Primary background:** `#e6f4ff` — selected row highlights, info badges
- **AI Accent:** `#722ed1` — reserved ONLY for AI-generated content. Suggestions, confidence scores, analysis badges, generated KB articles. This is the product's visual differentiator.
- **AI Accent hover:** `#9254de`
- **AI Surface:** `#f9f0ff` — background for AI suggestion panels and AI-generated content areas
- **AI Border:** `#d3adf7` — borders for AI surface containers
- **Success:** `#52c41a` — resolved tickets, published KB articles, positive metrics
- **Success background:** `#f6ffed`
- **Warning:** `#faad14` — pending actions, in-progress status, approaching SLA
- **Warning background:** `#fffbe6`
- **Error:** `#ff4d4f` — high priority tickets, validation errors, system failures
- **Error background:** `#fff2f0`
- **Neutrals:**
  - `#ffffff` — card/container backgrounds
  - `#fafafa` — table header, hover rows
  - `#f5f5f5` — page/layout background
  - `#f0f0f0` — dividers, subtle borders
  - `#d9d9d9` — input borders, disabled states
  - `#bfbfbf` — placeholder text (very light)
  - `#8c8c8c` — tertiary text, timestamps
  - `#595959` — secondary text, descriptions
  - `#262626` — primary text
  - `#141414` — headings, emphasis
- **Dark mode:** Deferred to v2. Strategy: invert neutral ramp, reduce color saturation 10-20%, use `#1f1f1f` as container background, `#141414` as layout background.

### Color Usage Rules
1. Blue (`#1677ff`) = user's own actions (submit, assign, navigate)
2. Purple (`#722ed1`) = AI did this (suggestions, analysis, generated content)
3. Never use purple for user-initiated actions
4. Never use blue for AI-generated content
5. Semantic colors (green/orange/red) follow Ant Design conventions exactly

## Spacing
- **Base unit:** 8px (Ant Design standard)
- **Density:** Comfortable — engineers scan ticket queues fast without feeling cramped. Target: 15-20 tickets visible above the fold in queue view.
- **Scale:**
  - 2xs: 4px — tight gaps (between badge and text, icon padding)
  - xs: 8px — compact gaps (between inline elements, small padding)
  - sm: 12px — standard gaps (form field spacing, card internal padding top/bottom)
  - md: 16px — default gaps (between components, card padding left/right, content margins)
  - lg: 24px — section spacing (between card groups, main content padding)
  - xl: 32px — major section breaks
  - 2xl: 48px — page-level spacing

## Layout
- **Approach:** Grid-disciplined (Ant Design Pro pattern)
- **Structure:** Fixed left sidebar (220px, dark `#001529`) + fixed top header (48px) + scrollable content area
- **Grid:** Single-column content with 16px gap grid for card layouts. KPI cards: 4-column grid.
- **Max content width:** No max (fills available space, typical 1200-1600px on desktop monitors)
- **Border radius:**
  - sm: 4px — tags, badges, small elements
  - md: 6px — buttons, inputs, dropdowns
  - lg: 8px — cards, panels, containers
  - full: 9999px — avatars, status dots
- **Sidebar navigation:**
  - Dark background `#001529` with white text at 65% opacity
  - Active item: full `#1677ff` blue background, white text
  - Group labels: 11px uppercase, 30% opacity
  - Logo area: 48px height with colored icon + product name
  - Role-based: CUSTOMER sees (提交工单, 我的工单, 知识库), ENGINEER sees (工单队列, 我的工单, 知识库, 待审核文章), ADMIN sees all + 数据分析

## Motion
- **Approach:** Intentional (functional, not decorative)
- **Easing:** enter(ease-out) exit(ease-in) move(ease-in-out)
- **Duration:**
  - micro: 50-100ms — button hover, focus ring
  - short: 150-200ms — fade-in for panels, tooltip appear, tab switch
  - medium: 200-300ms — sidebar expand/collapse, modal entrance
- **Specific animations:**
  - Panel reveals: opacity 0 to 1, 200ms ease-out
  - Loading states: skeleton shimmer (linear-gradient sweep, 1.5s loop)
  - Sidebar transitions: slide, 200ms ease-in-out
  - AI suggestion appear: fade-in 200ms (AI panel loads after main content)
- **Rules:**
  - No bounce, spring, or playful animation
  - No animation on initial page load (content appears instantly)
  - Skeleton shimmer for any async data fetch
  - Reduce motion: respect `prefers-reduced-motion` media query

## Shadows
- **sm:** `0 1px 2px 0 rgba(0,0,0,0.03), 0 1px 6px -1px rgba(0,0,0,0.02), 0 2px 4px 0 rgba(0,0,0,0.02)` — cards, table wrappers
- **md:** `0 6px 16px 0 rgba(0,0,0,0.08), 0 3px 6px -4px rgba(0,0,0,0.12), 0 9px 28px 8px rgba(0,0,0,0.05)` — modals, dropdowns, elevated panels

## Component Patterns

### KPI Stat Cards
- White background, sm shadow, lg border-radius
- Label: 12px tertiary color
- Value: 30px weight-600 tabular-nums
- Sub-metric: 12px with colored up/down indicators

### Ticket Table
- White container with sm shadow
- Header row: `#fafafa` background, 12px uppercase labels
- Row height: ~42px (comfortable scan density)
- Priority column: colored square badges (high=red, medium=orange, low=green)
- AI column: purple dot + percentage

### AI Suggestion Panel
- `#f9f0ff` background, `#d3adf7` border
- Header: purple sparkle icon + text
- Items: white cards with purple border, hover = darker purple border
- Match score: purple text, right-aligned

### Status Pills
- Dot (6px circle) + text label
- OPEN: blue dot, IN_PROGRESS: orange dot, RESOLVED: green dot, CLOSED: gray dot

### Form Inputs
- Height: 32px
- Border: `#d9d9d9`, focus: `#1677ff` with 2px blue ring
- Error state: red border, 12px red error text below
- Textarea: same style, min-height 120px

## Accessibility (v1 baseline)
- Desktop-only layout
- Keyboard navigation: all interactive elements focusable
- ARIA labels on icon-only buttons
- Minimum touch target: 44px
- Color contrast: all text/background combinations meet WCAG AA (verified by Ant Design defaults)

## Design System Preview
- Preview file: `/tmp/design-consultation-preview-1711800000.html`
- Includes light/dark mode toggle, all components, and two product mockups

## Decisions Log
| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-03-30 | Initial design system created | Created by /design-consultation based on competitive research (Feishu, Udesk, Meiqia, Ant Design Pro) |
| 2026-03-30 | System fonts over custom fonts | Internal tool. 0ms load time, native CJK rendering (PingFang SC / Microsoft YaHei) |
| 2026-03-30 | Purple AI layer (#722ed1) | No Chinese helpdesk competitor uses a dedicated AI color. Visual differentiation + trust signal |
| 2026-03-30 | Comfortable density (not minimal) | Engineers need 15-20 tickets visible. Feishu's airy spacing wastes screen real estate for power users |
| 2026-03-30 | Dark sidebar (#001529) | Ant Design Pro default. Creates clear visual boundary between navigation and content |
| 2026-03-30 | No dark mode in v1 | Deferred to v2. CSS custom properties are in place for easy implementation |
| 2026-03-30 | Ant Design 5 defaults as baseline | No custom design tokens in v1. Framework defaults are battle-tested for Chinese enterprise |
