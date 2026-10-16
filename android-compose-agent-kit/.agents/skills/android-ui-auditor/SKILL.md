---
name: android-ui-auditor
description: Audit every Jetpack Compose screen and component for rendering, accessibility, responsiveness, and state correctness. Use during repository-wide audits, feature changes, regression analysis, or design-system migrations in Kotlin Jetpack Compose projects.
---

# UI Component Auditor

Read repository root `AGENTS.md` first. Follow its safety gates, inventory conventions and evidence requirements.


## Process
1. Discover routes from NavHost, composable destinations, deep links, activity entry points, dialogs, sheets and nested navigation graphs. Record every screen in `docs/qa/screen-inventory.csv`; do not equate files with screens.
2. For each screen, enumerate all visible and conditional UI elements, modifiers, clickable nodes, focusable fields, panels, dialogs, menus, sheets, lists and chart elements. Record `screen_id`, `component_id`, `source_file:line`, entry conditions, states, and coverage.
3. Inspect default, loading, empty, populated, error, offline, disabled, permission-denied and high-content states where applicable. Test long text, large font scaling, RTL, landscape, gesture/nav-bar insets, IME, light/dark, different widths and API levels.
4. Check clipping, text overflow, tap targets (48dp recommended), accessible labels/semantics, screen-reader order, focus traversal, contrast, density-dependent spacing, consistency, duplicate actions and visual regressions.
5. Use Compose UI tests (`createAndroidComposeRule`/`onNode`), screenshot tests if project provides a configured framework, and emulator/device inspection. Do not claim runtime testing unless executed.
6. Capture reproducible steps, expected vs actual, screenshot/test evidence, severity, owner, fix and post-fix verification in `docs/qa/issues.csv`. Every inventoried component gets Verified / Fail / Blocked / Not Tested status.
## Safety
Do not restyle entire app during bug audit. For fixes, change smallest possible scope, add regression test, rerun affected user journeys and note unrelated pre-existing failures. Cross-check `design-system/typography-registry.yaml`, `color-tokens.yaml` and `icon-registry.yaml`.

## Outputs
Update relevant inventory, `docs/qa/issues.csv`, and `docs/qa/report.md`; cite exact files/lines, test commands and execution status.
