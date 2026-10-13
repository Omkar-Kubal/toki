---
name: android-color-system
description: Create and enforce primitive and semantic design tokens for Compose light/dark themes, states and components. Use during repository-wide audits, feature changes, regression analysis, or design-system migrations in Kotlin Jetpack Compose projects.
---

# Semantic Color System

Read repository root `AGENTS.md` first. Follow its safety gates, inventory conventions and evidence requirements.


## Implementation contract
1. Inventory hardcoded Color literals, theme MaterialTheme references, painter tints, chart palettes and alpha layers. Preserve brand colors unless supplied; use `design-system/color-tokens.yaml` as placeholder baseline, not an approved brand specification.
2. Define primitives separately from semantic aliases. Prefer roles such as `surface.canvas/card/raised`, `text.primary/secondary/disabled/inverse`, `border.subtle/strong/focus`, `action.primary/onPrimary/secondary`, `status.success/warning/error/info`, `chart.series1..6`, and accessible dark equivalents.
3. Provide Kotlin `@Immutable` semantic color data class with Compose CompositionLocal and Theme wrapper, mapping all existing uses incrementally; do not replace Material3 dynamic color policy without app decision.
4. For every UI state test enabled, disabled, pressed, selected, focused, loading and error; avoid opacity-only distinctions. Evaluate contrast using actual composited colors (WCAG AA 4.5:1 normal text, 3:1 large text/UI where relevant).
5. Ban literals in feature UI via lint/code review policy; allow literals only under theme primitives, previews, tests, or documented exemptions. Verify screenshot diffs in light/dark and high contrast contexts.
6. Output migration map old → new, orphan/unused tokens, compatibility risks and exceptions. No broad color replacement without preview snapshots and approval.

## Outputs
Update relevant inventory, `docs/qa/issues.csv`, and `docs/qa/report.md`; cite exact files/lines, test commands and execution status.
