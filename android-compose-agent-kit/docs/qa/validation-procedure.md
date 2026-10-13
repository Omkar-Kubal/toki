# Verification playbook

1. Confirm `git status --short`, app flavors, emulator availability, configuration and isolated test credentials. Save initial baseline.
2. Populate screen, component, flow and logic inventories by reading navigation graphs and source. Define finite state-case IDs for conditional branches.
3. Run `./gradlew tasks` to discover actual task names. Run available compile, unit tests and lint on debug flavor; record exact commands and failures.
4. Add targeted Compose UI tests for click, back, focus/IME, state feedback, empty/error conditions. Use navigation tests with TestNavHostController only if already configured or approved.
5. Execute Android instrumented tests on emulator/device, plus manual/screenshot validation at multiple font scales, sizes, themes, and orientations; record actual execution evidence.
6. For business logic, verify persisted state and error recovery via fake data stores or test backend; confirm impact on every UI consumer and navigation destination.
7. Audit WCAG contrast and accessibility semantics; validate localized/RTL content where app supports it.
8. Fix only approved defects with isolated patches. Rerun focused tests and affected downstream journeys; track regressions separately.
9. Reconcile all inventories: each case PASS/FAIL/BLOCKED/NOT_TESTED/N/A; report totals and evidence. Document limitations and change/rollback plan.

Failure to run device tests is not proof of correctness. Block release for unresolved P0 or agreed P1 issues; user decides risk exceptions.
