---
name: android-typography-system
description: Enforce Google Sans semantic typography roles, six size primitives and exact line heights for Jetpack Compose. Use during repository-wide audits, feature changes, regression analysis, or design-system migrations in Kotlin Jetpack Compose projects.
---

# Google Sans Typography

Read repository root `AGENTS.md` first. Follow its safety gates, inventory conventions and evidence requirements.


## Implementation contract
1. Load `design-system/typography-registry.yaml` as source of truth. Use roles exactly: pageTitle 22/600/28; cardTitle 16/600/22; sectionLabel 12/500/16; bodyTitle 16/500/22; rowTitle 14/500/20; supporting 12/400/16; button 14/600/20; action 14/500/20; heroMetric 32/600/38; amountInput 28/600/34; inlineAmount 14/600/20; chartLabel 12/400/16 (size/weight/line height in sp except weight).
2. Primitive scale is `xs=12, sm=14, md=16, lg=22, xl=28, xxl=32`; sizes outside scale need review and documented exception (chart labels may be 11sp by approval).
3. Implement Kotlin registry as `TextStyle` values and named component wrappers; avoid per-feature repeated TextStyle/size/weight. For Google Sans load legitimately supplied/licensed fonts into `res/font`; do not bundle or download proprietary font binaries. Until provided, use configured fallback and explicitly report deviation.
4. Preserve Android fontScale accessibility. Never clamp font scale or switch sp to dp to force fit. Verify large font display, clipping, baseline, ellipsis, min touch target and bidi text.
5. Migrate by role rather than search/replace numeric font sizes. Run snapshot tests and inspect visual diffs before and after.

## Outputs
Update relevant inventory, `docs/qa/issues.csv`, and `docs/qa/report.md`; cite exact files/lines, test commands and execution status.
