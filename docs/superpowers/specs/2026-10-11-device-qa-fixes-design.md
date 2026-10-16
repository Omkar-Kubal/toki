# Device QA Fixes Design

## Goal

Resolve the confirmed RMX2156 findings for bottom navigation insets, timer restoration and lifecycle logic, TalkBack actions, Light mode, and Stop feedback. Remove the home-screen widget feature as requested for ISS-006.

The success criteria are that navigation and primary timer actions remain visible and operable with TalkBack and large text; running and paused Pomodoro and Flow timers restore correctly; Light mode persists across launches; stopped timers report a ready state without saying zero minutes remain; and no widget provider or stale widget state remains in the app.

| Finding | Confirmed issue |
|---|---|
| ISS-004 | Bottom navigation overlaps or clips at tested font scales and has zero-size TalkBack bounds. |
| ISS-005 | A running timer returns as idle after process death despite a future saved end timestamp. |
| ISS-006 | Widget state remains stale after app recovery or Stop; remove the widget feature. |
| ISS-007 | The Home Start action is not exposed as actionable to TalkBack. |
| ISS-008 | The Light mode Settings row is a static placeholder. |
| ISS-009 | Stopping a partial session can announce “0 minutes remaining.” |

## Context and constraints

- Device evidence comes from the realme RMX2156, Android 12 / API 31, at 360 dp logical width. Confirmed font-scale observations include 0.9, 1.35, and 1.6.
- The app uses edge-to-edge Compose with a custom `Scaffold` bottom bar. The current custom bar has fixed vertical padding and no explicit navigation-bar inset handling.
- `FocusFirstTheme` already contains static Light and Dark schemes. `MainActivity` currently passes only the AMOLED preference; the root theme defaults to Dark.
- `SettingsRepository` already has a DataStore `KEY_THEME_MODE`, but the Settings UI presents Light mode as a noninteractive placeholder.
- The foreground service previously persisted only `timer_was_running`, `timer_end_time_ms`, and `timer_phase`; recovery also needs mode, phase, original duration, pause status, and session timestamps.
- Google Sans font files are not present in this repository. The typography registry requires licensed project-owned font resources, so the system sans-serif fallback is used while the semantic registry tokens are applied.
- `TimerUiText` currently labels idle duration as “remaining”; a late zero tick after Stop can produce “0 minutes remaining.”
- Preserve all existing working-tree changes. Do not change unrelated tasks, navigation routes, billing, or timer completion policy.

## Design

### Navigation and accessibility

Keep the existing custom bottom navigation and apply one explicit system navigation-bar inset policy to its bottom-bar container. Avoid adding the same inset twice through both the bar and Scaffold. Validate that each tab's interactive and semantic bounds are nonzero and sit above the system navigation area at the tested font scales.

Give each tab a single accessible node with its visible label, `Tab` role, selected state, and click action. The Home primary timer control must expose the current action (“Start”, “Pause”, or “Resume”) as an actionable button to accessibility services. Preserve the existing visual design and normal touch behavior.

### Persisted Light/Dark selection

Turn the existing Light mode row into a switch backed by `SettingsRepository.themeMode`. Persist explicit `Light` and `Dark` values and collect the setting at the app root so changing it updates all app screens. Use the current static palette definitions. Treat absent, legacy `System`, and invalid values as Dark to preserve the app's current appearance for existing installs. When Light is selected, the Light scheme takes precedence over the AMOLED-dark preference; the AMOLED preference remains stored and takes effect again when Dark is selected.

### Timer recovery and lifecycle

On app startup, reconcile the persisted timer record before showing Home as idle. For a finite countdown marked running with a valid future end timestamp, compute remaining seconds from `endTimeMs - now`, restore its phase and countdown state, and start/reconnect the foreground service so countdown, notification, alarm, and Home remain aligned. Persist the finite timer's mode, phase, original total duration, and session start time so recovery does not infer elapsed time from a changed Settings value. A paused finite timer restores as paused from its saved remaining seconds and does not start the service; Resume sets a fresh end timestamp from the saved remaining time.

If the record is missing, malformed, or already expired, do not start a service from it. Clear stale timer recovery fields. Expired recovery must not fabricate a completed session or a partial session. An expired timer can be started afresh by the user. Persist run/pause status separately from the active-session marker; never resume a paused timer merely because that marker is present.

Flow is a count-up timer. Persist its accumulated elapsed seconds and current run start timestamp; on recovery, add the elapsed wall time since that run began. Pausing snapshots the elapsed duration and resume starts a new run interval. Flow does not schedule a finite completion alarm.

Remove the Glance receiver, provider metadata, resources, service updates, dependencies, and widget-specific strings. Clear timer recovery state on stop, and ignore late stop ticks so they cannot replace idle feedback.

### Idle feedback after Stop

Keep Stop's current timer and session-saving behavior. Change the idle accessibility summary so it reports readiness and the selected duration, not remaining time. For example, an idle 25-minute focus timer may announce “Focus ready, 25 minutes set”; a stopped zero-valued tick must still produce ready feedback and must never announce “0 minutes remaining.” Running and paused timers continue to announce their remaining time.

## Error handling and state rules

- Invalid phase names, a missing end timestamp for a running finite timer, and nonpositive remaining durations are treated as stale recovery data and cleared without crashing. A paused timer is valid only when its saved remaining duration is positive.
- A future finite end timestamp is authoritative for countdown remaining time; ordinary one-second ticks continue from that derived value.
- Widget update failure is logged and must not prevent timer restoration or app launch.
- Theme preference parsing is tolerant: only explicit `Light` selects Light; every other stored value resolves to Dark.
- Changing theme does not reset the timer or alter timer service state.

## Verification

### Automated tests

- Add focused tests for timer recovery decisions: valid future end, exact expiry, expired end, missing/zero/invalid timestamp, invalid phase, and paused state. Assert that expired or malformed records clear rather than start, and that a paused record remains paused.
- Add a recovery test proving remaining time is derived from the saved end timestamp and total duration is preserved independently of current Settings values.
- Add theme preference tests for the Dark default, Light persistence, legacy `System` fallback, and invalid-value fallback.
- Add Flow recovery tests for running elapsed time and paused elapsed time.
- Add `TimerUiText` tests for idle positive duration, idle zero duration after Stop, and unchanged running/paused “remaining” descriptions.
- Add Compose semantics coverage for all three bottom tabs and the primary timer button: visible labels, roles, selected state, click action, and nonzero bounds.

### RMX2156 device checks

- Re-run Home and Settings at font scales 0.9, 1.35, and 1.6. Verify the bottom bar clears the system navigation area, tab labels do not clip, and tabs are tappable.
- With TalkBack enabled, inspect the tab order and confirm the tab names/selected state and Start/Pause/Resume actions are actionable.
- Set Light mode, verify the Settings and Home palettes change, close/reopen the app, and confirm Light remains selected. Switch back to Dark and verify the AMOLED option still works when available.
- Start a finite countdown, force-stop the app, relaunch, and verify Home and notification/service reflect the remaining time computed from the saved end timestamp. Also test an expired recovery record and confirm it clears to ready without inserting a session.
- For Pomodoro and Flow running, pause, resume, and Stop, verify restored state, elapsed-time accounting, and clear ready feedback. Confirm the app no longer declares an app-widget provider.
- Re-run the existing unit-test and connected instrumentation commands. Record device outcomes and any blocked states in the QA matrix/report; the connected instrumentation task may uninstall the package and clear app data, so capture or account for that side effect before invoking it.

## Scope exclusions

- No redesign of the timer screen or navigation bar.
- Flow restores accumulated elapsed time after process death; it remains an open-ended count-up session.
- No change to billing, focus guard, task association, database schema, or network behavior.
- No claim that the full 20-case device matrix is revalidated by these targeted checks; only affected cases and relevant regressions will be rerun.
