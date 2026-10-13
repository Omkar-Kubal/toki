---
name: android-logic-auditor
description: Trace every Compose interaction through ViewModel, use cases, repositories, services and persisted state; identify and repair missing or incorrect logic. Use during repository-wide audits, feature changes, regression analysis, or design-system migrations in Kotlin Jetpack Compose projects.
---

# Logic Trace Auditor

Read repository root `AGENTS.md` first. Follow its safety gates, inventory conventions and evidence requirements.


## Process
1. Index Composable UI actions (`onClick`, `onValueChange`, swipe, long-click, submit, lifecycle effects, nav actions), caller graphs and state producers. Link each component to handlers and downstream effects, not only named functions.
2. Trace UI → event/intent → ViewModel/state holder → domain use case → repository/API/Room/DataStore/work scheduler → result → state update → UI feedback. Track alternate destination components using shared state.
3. Classify handlers: complete, partial, no-op/TODO, unreachable, uncaught failure, incorrect persistence, inconsistent multi-screen state, race, duplicate submission, cancellation gap, lifecycle leak or permission/security problem.
4. Check loading/error/empty/success invariants; concurrency and dispatcher usage; repository error mapping; idempotence and transactional writes; collection with lifecycle; navigation one-off events; process death; privacy of logs.
5. Prefer focused fix with unit tests (fake repositories), ViewModel tests and integration/Compose tests. For database/network behavior use fake/test backends, avoid destructive production requests. Verify expected data end state.
6. Maintain `docs/qa/logic-trace.csv` for each action and `issues.csv` for defects, with file:line and proof. `Unverified` is mandatory when a runtime side effect cannot be observed.
## Stop conditions
Never assume backend exists because a button has an onClick. Never call a speculative issue confirmed. Avoid schema migrations, remote writes, dependency replacements and destructive fixes without explicit approval.

## Outputs
Update relevant inventory, `docs/qa/issues.csv`, and `docs/qa/report.md`; cite exact files/lines, test commands and execution status.
