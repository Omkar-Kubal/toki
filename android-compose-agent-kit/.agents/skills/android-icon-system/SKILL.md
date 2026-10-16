---
name: android-icon-system
description: Centralize Hugeicons semantic icons and sizing for Jetpack Compose without assuming an unverified Android package API. Use during repository-wide audits, feature changes, regression analysis, or design-system migrations in Kotlin Jetpack Compose projects.
---

# Hugeicons Registry

Read repository root `AGENTS.md` first. Follow its safety gates, inventory conventions and evidence requirements.


## Implementation contract
1. Read `design-system/icon-registry.yaml`. Inventory every icon usage and action meaning; assign semantic roles and categorize navigation, primary-action, editing, finance/content, status, system and charts.
2. Choose a verified, licensed Hugeicons distribution compatible with Kotlin/Compose. Check actual artifact name, classes and available icon IDs in project or official current docs before writing imports. Do not assume JavaScript names exist in Kotlin. Document dependency coordinates and attribution/licensing.
3. Implement a single icon adapter (`AppIcon` + `AppIcons` mapping) that resolves semantic names to verified underlying assets (VectorDrawable or supported library ImageVector). Preserve tint, size, contentDescription and semantics. Use decorative null descriptions only when redundant with adjacent accessible text.
4. Default sizes: xs=16dp, sm=20dp, md=24dp, lg=28dp, xl=32dp; typical navigation 24dp, row trailing 20dp, toolbar 24dp. Maintain ≥48dp touch area for interactive icons. Stroke widths/variants must match icon source capabilities, never fabricate arbitrary styles.
5. Audit missing assets, collisions, inconsistent meanings, variant/style mismatch and overuse of icons. Validate selected/unselected state, RTL mirroring only when semantically correct, dark tint and accessibility.
6. Do not distribute Hugeicons files or proprietary font assets without license verification. Until a source exists, emit mapping placeholders and a clearly marked integration TODO, not noncompiling imports.

## Outputs
Update relevant inventory, `docs/qa/issues.csv`, and `docs/qa/report.md`; cite exact files/lines, test commands and execution status.
