# Android Compose QA report

## Scope and device

This run completed the 20-case device matrix on the connected realme RMX2156 (Android 12 / API 31), using com.focusfirst version 1.0.1, targetSdk 35. The device is 1080x2400 at 480 dpi (360 dp logical width). Runtime captures and UI trees are in app/build/device-qa/, which is ignored build output.

The source audit and fixes for ISS-001 through ISS-003 remain in the working tree. This run did not modify application source.

## Automated verification

- ./gradlew.bat :app:testDebugUnitTest --rerun-tasks --console=plain — BUILD SUCCESSFUL; 12 tests, 0 failures, 0 errors, 0 skipped.
- ./gradlew.bat :app:connectedDebugAndroidTest --console=plain — BUILD SUCCESSFUL; the connected RMX2156 ran 7 instrumented tests, 0 failures, including Home title/logo, timer prominence, ring-label clearance, and system Back navigation.
- Earlier after the code fixes, testDebugUnitTest assembleDebug lintDebug --rerun-tasks also passed; lint reported 118 warnings and 3 hints. The build still reports existing Gradle/Kotlin deprecation warnings.

## Device matrix

| Result | Count | Cases |
|---|---:|---|
| PASS | 7 | QA-04, QA-08, QA-09, QA-13, QA-14, QA-15, QA-17 |
| FAIL | 7 | QA-02, QA-03, QA-05, QA-06, QA-16, QA-18, QA-20 |
| BLOCKED | 5 | QA-01, QA-07, QA-11, QA-12, QA-19 |
| N/A | 1 | QA-10 |
| NOT_TESTED | 0 | Every matrix case has an outcome; blocked and unsupported conditions are recorded above. |

### Passes

- QA-04: landscape timer controls are reachable after scrolling.
- QA-08: forced RTL mirrors layout at the closest supported large font scale; RTL was restored off.
- QA-09: the timer starts and counts down with Wi-Fi/mobile data disabled and airplane mode off.
- QA-13: denied DND access opens Android settings from Grant and returns to a usable app.
- QA-14/15: EULA Back and acceptance paths reach Home; a fresh reinstall also showed EULA before acceptance.
- QA-17: at font scale 1.35, planner keyboard, task entry, submission, and scrolling work.

### Failures

- QA-02/03/05: at scale 1.35 and the device maximum 1.6, timer/session text truncates and bottom navigation overlaps or clips against system navigation. Font scale 2.0 is not available on this device.
- QA-06: the in-app Light mode row is a static placeholder; Toki remains dark.
- QA-16: after force-stopping an active timer, relaunch shows idle 25:00 despite saved preferences still indicating a running timer and future end time.
- QA-18: the widget launches Toki but stays at 09:10 after Toki returns to idle 25:00.
- QA-20: with TalkBack enabled, the Home tree exposed the timer summary label, but Start was not an actionable node and all three bottom-tab labels had [0,0] bounds. The spoken utterance order could not be independently captured.

### Blocked and not applicable

- QA-01: the Realme display-size control is clipped to zero-height bounds; wm size cannot be changed on this device. The narrow-width case could not be set up.
- QA-07: dynamic color is not exposed; AMOLED mode is Pro-gated.
- QA-11/12/19: Focus Guard and blocked-app behavior are behind the Pro gate. Usage Access remained at the default app-op; no purchase was made.
- QA-10: N/A on API 31; Android's notification runtime permission begins at API 33.

## Open device findings

- ISS-004 (P2): bottom navigation overlaps system navigation at default/high font scales; its TalkBack nodes have zero bounds.
- ISS-005 (P1): process death loses the active timer state.
- ISS-006 (P2): widget content is stale after app restart.
- ISS-007 (P2): Start is not exposed as an actionable accessibility node.
- ISS-008 (P2): Light mode is an unimplemented placeholder.
- ISS-009 (P2): stopping a partial session leaves 00:00 and “0 minutes remaining” feedback.

ISS-001 and ISS-002 remain unit-tested fixes; deep-link association and duration handling were not exercised end-to-end on the device. QA-17 confirms the planner entry and basic task form, but not every task state or persistence path.

The inventories now distinguish runtime evidence from untested states. Of 69 component rows, 19 received runtime evidence (3 FAIL, 1 BLOCKED, 4 PASS, 11 partial), 4 are preview-only N/A, and 46 remain NOT_TESTED. Home, Settings, Tasks, and Insights received partial device review; the widget check failed; Licenses remain untested. Focus Guard and Blocked Activity are blocked by entitlement.

## Device restoration and side effects

System settings were restored to Dark mode, font scale 0.9, Wi-Fi on, airplane mode off, auto-rotate enabled, RTL off, and TalkBack off. The Toki widget was removed. The app is open on idle Home at 25:00. Mobile data remains off; its original state was not recorded, and the device had no cellular service during this run.

The connected instrumentation task uninstalled the debug package during cleanup. Reinstalling it cleared local app data, including the QA-created task and session count; that data could not be recovered. QA-14 was repeated, and the app was left at Home. Current state is recorded in app/build/device-qa/device-qa-final-state.txt.

## Typography registry alignment — 2026-10-11

`app/src/main/java/com/focusfirst/ui/theme/Type.kt:20-53` exposes the Google Sans roles from `design-system/typography-registry.yaml` and maps every Material 3 typography slot back to those roles. Compose text across Home, timer dialogs/sheets, Tasks, Insights, Settings, Focus Guard, Blocked Activity, licensing, and Pro surfaces uses semantic roles. The Home timer now uses the registered 48sp `timerHeroMetric`; the blocked-activity countdown retains the standard 32sp `heroMetric`. Primary CTAs use the registry's reviewed 16sp exception. Font sizes remain in `sp` and no font-scale clamp was added.

Verification:

- `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain` — **BUILD SUCCESSFUL**; the unit-test task was up to date, app assemble completed, and lint completed.
- Installed the debug APK with `adb install -r` on RMX2156 / Android 12. At the device's existing font scale 0.9, idle and live Home were visually reviewed in `app/build/device-qa/typography-home-default.png` and `typography-live-default.png`. The live hierarchy exposed the timer text and separate Stop, Pause, and Cancel buttons; Cancel returned Home to idle without recording a completed session.
- Large-scale verification remains **NOT_TESTED** in this run. `adb shell settings put system font_scale 1.35` was denied by the device, and the Realme font-size control did not accept the attempted selection. The original scale remained 0.9. Existing high-scale truncation and bottom-navigation finding ISS-004 remains open; this update does not claim it is fixed.

The connected RMX2156 was also used to visually recheck the Settings overview, Insights empty-data view, Tasks planner, and Home task sheet at font scale 0.9. Evidence is saved as `typography-settings.png`, `typography-insights.png`, `typography-tasks.png`, and `typography-task-sheet-final.png`. The latest APK's task-sheet empty state contains no mascot artwork. This was a visual typography pass, not exhaustive interaction or accessibility coverage; those broader states remain marked PARTIAL or NOT_TESTED in the component inventory.

## Home layout follow-up — 2026-10-12

The Home header now sits immediately below the status-bar inset and remains outside the scrolling content. It includes the launcher mark beside “Toki” and the Settings affordance. The timer body scrolls independently while the app-shell bottom navigation remains fixed. The duration range labels render below the arc stroke, and the ring value uses the 48sp timer hero role.

On the connected RMX2156 at font scale 0.9, the idle and live layouts were captured as `app/build/device-qa/home-idle-final.png` and `app/build/device-qa/home-live.png`. The live countdown and the Stop, Pause, and Cancel controls were visible. Device interaction verified Pause changes to Resume, Resume returns to running, and Cancel returns Home to idle 25:00. `home-paused.xml`, `home-resumed.xml`, and `home-idle-final.xml` record the accessibility trees. The existing high-font-scale/system-navigation overlap and TalkBack tab-bound issues (ISS-004/ISS-007) remain open; this pass does not claim to resolve them.

Verification: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain` completed successfully; 26 unit tests passed, debug assembly passed, and lint completed with 120 warnings and 3 hints. `python android-compose-agent-kit/scripts/check_contracts.py` passed. `git diff --check` passed; Git emitted only existing LF-to-CRLF working-tree warnings.


