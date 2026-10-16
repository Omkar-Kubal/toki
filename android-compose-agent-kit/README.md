# Android Compose QA Agent Kit

Repository-based instructions for six independent skills + `AGENTS.md` orchestrator. **No application source included or audited**; integration required.

## Install
Copy `AGENTS.md`, `.agents/skills/`, `design-system/`, `docs/qa/`, `scripts/` into your Android repository root. Coding agents must read `AGENTS.md` and matching skills. Some agents only discover skills from their own configured skills path; add a link/copy of `.agents/skills` if required. AGENTS.md is a convention, not an autonomous runtime.

## Commands to agent
- `Read AGENTS.md and run AUDIT_ONLY across all modules. Fill inventories and issues; mark runtime checks unverified if no emulator.`
- `Audit checkout flow: UI → navigation → ViewModel → repository → persisted result; give evidence and severity.`
- `Propose typography/color/icon migration only; do not edit app until approved.`
- `Implement approved P1 fixes with regression tests and update docs/qa/report.md.`

## Design integration
`design-system/*yaml` contain source-of-truth registries and starter themes. `design-system/kotlin-examples/` provides **illustrative** Compose samples, not a ready-to-compile app module. Replace package name and wire to existing MaterialTheme architecture. Provide licensed Google Sans font files yourself. Verify official Hugeicons Kotlin compatibility, license and actual asset IDs before using icon adapter.

## Evidence standard
Use inventories and case IDs. Each defect needs file:line, steps, actual/expected, root cause hypothesis/evidence, proposed fix, test evidence, status and cross-screen impact. Static-only verdict is NOT_TESTED for runtime.
