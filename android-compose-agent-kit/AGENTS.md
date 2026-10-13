# Android Compose Quality Orchestrator

## Operating contract
Target Kotlin / Jetpack Compose source repository. This file coordinates six scoped skills under `.agents/skills/`. Read all applicable local `AGENTS.md` documents, inspect existing architecture and tests, and do not assume any prescribed folder/package layout. Do not claim coverage before code inventory + executed testing evidence.

## Mode / approvals
Default mode = **AUDIT_ONLY**. Never edit application code, migrate tokens, modify dependencies, create/delete data, or run production API operations in audit-only mode. For user-requested implementation, propose ordered changes and risks; approve migration/DB/network and large multi-module refactors separately. Preserve existing design until explicit redesign scope. Do not commit/push, alter secrets or external services without authorization.

## Skill selection
- UI components, visual defects, accessibility → `.agents/skills/android-ui-auditor/SKILL.md`
- Navigation, journeys, back stack, screen transitions → `.agents/skills/android-ux-auditor/SKILL.md`
- UI event through persisted/backend effect → `.agents/skills/android-logic-auditor/SKILL.md`
- Color/theme/contrast/hardcoded colors → `.agents/skills/android-color-system/SKILL.md`
- Google Sans semantic fonts → `.agents/skills/android-typography-system/SKILL.md`
- Hugeicons catalog/mapping/sizes → `.agents/skills/android-icon-system/SKILL.md`

## Full audit sequence
1. **Preflight**: `git status`, identify modules/build variants/SDK, tests, Gradle wrapper, app entry points and device/emulator availability. Record baseline failures and environment limitations. Never discard local changes.
2. **Discovery**: exhaustively enumerate navigation routes, Composables, overlays, conditional components, user actions, business-data boundaries, and shared UI. Populate screen, component, flow and logic inventories before making overall coverage assertions. Audit adaptive/wearable/TV variants only if present.
3. **Static review**: inspect UI + UX + logic per screen; reconcile cross-screen dependencies, duplicated reusable Composables and each action to its handler. Consult all three design registries.
4. **Runtime verification**: run available static analysis/tests, emulator/device explorations, screenshot/Compose UI tests and accessibility inspection. Mark every case as PASS, FAIL, BLOCKED or NOT_TESTED; static checks alone do not constitute end-to-end verification.
5. **Prioritize**: P0 crash/data loss/security; P1 broken primary user flow; P2 functional/visual inconsistency; P3 polish. Record confidence, steps and evidence; avoid guessed bugs.
6. **Fix (only if permitted)**: isolate smallest change, add tests first where feasible, retain original state, avoid unrelated modifications, run targeted tests then expanded regression for affected routes. Recheck light/dark + font scaling and all relevant dependencies.
7. **Report**: update `docs/qa/report.md`, inventory CSVs, issues CSV; summarize coverage numerator/denominator and excluded/unverified scope. Attach repro steps and tests, mark unresolved risks.

## Component definition / coverage
A component includes composable leaf, action surface, field, list item type, conditional branch, dialog, bottom sheet, menu, snackbar, accessible interaction, and reusable UI piece. A page is not fully covered just because screenshot looks correct. Track component states and action paths separately. Coverage = VERIFIED applicable cases / ALL inventoried applicable cases; report BLOCKED/NOT_TESTED counts alongside.

## Verification matrix
Dimensions: min/typical/max screen width, orientations supported by manifest, font scales 1.0/1.3/2.0 where supported, light/dark, system bars/IME, LTR/RTL where supported, low connectivity, first run/returning, permissions and process restoration. Mark non-applicable cases N/A with reason. Test matrix is capability- and device-dependent; never manufacture passes.

## Commands (adapt to repo; never assert execution without running)
`./gradlew tasks`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew connectedDebugAndroidTest`; variant/module task names differ. Before long or expensive runs verify target + available emulator. Capture commands, exit code and relevant output in report.

## Design constraints
Typography source: `design-system/typography-registry.yaml`; colors: `design-system/color-tokens.yaml`; icons: `design-system/icon-registry.yaml`. These are design *contracts and example implementation snippets*, not automatically wired into app. Do not import Kotlin examples until package names, font licensing, icon source and architecture validated.

## Definition of done
No high-severity confirmed issues open within agreed scope, all changed actions have trace evidence, affected unit/UI/snapshot tests passed in available environment, explicit NOT_TESTED entries for inaccessible states/devices, token migration documented, final changed-file list and rollback path supplied.
