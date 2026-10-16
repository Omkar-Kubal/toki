# Toki Home device QA — 2026-10-11

## Scope and evidence

This pass checks the selected dark Home design and its live focus controls on the connected RMX2156. It covers idle, running, paused, resumed, cancel confirmation, and return to idle.

- Source visual: `docs/design/toki-home-idle-reference.png` (853 × 1844 px).
- Current full-view comparison: `docs/design/toki-home-idle-comparison-rmx2156-current.png`.
- Current focused comparison: `docs/design/toki-home-focused-comparison-rmx2156-current.png`.
- Idle implementation: `docs/design/toki-home-idle-device-rmx2156-latest.png`.
- Running implementation: `docs/design/toki-home-live-device-rmx2156-latest.png`.
- Paused implementation: `docs/design/toki-home-paused-device-rmx2156.png`.
- Resumed implementation: `docs/design/toki-home-resumed-device-rmx2156.png`.
- Cancel confirmation: `docs/design/toki-home-cancel-confirm-device-rmx2156.png`.
- Device: RMX2156, Android 12, 1080 × 2400 physical pixels at 480 dpi (3×; 360 × 800 dp).
- App: `com.focusfirst`, debug build installed from this worktree.
- The source has no system bars and its source density is unknown. Comparison boards scale the device capture to the source width while preserving its aspect ratio; this supports visual review, not pixel-level measurement. Device screenshots include Android status and navigation bars.
- The source shows a selected next task (`test4`); the device had no selected task, so it shows “Choose a task.” No task was created for QA.

## Full-view comparison

The full-view board shows the same dark monochrome timer-first hierarchy: Toki header, ring and duration, preset selector, primary action, queued-task area, and bottom navigation. Google Sans is bundled and used by the app. The mascot is absent.

The floating pill navigation is a deliberate update requested after the source mockup. Its Home, Tasks, and Progress destinations remained visible in idle, running, paused, and resumed captures. System bars are device chrome, not part of the source mockup.

## Focused comparison

The timer-ring and navigation regions are shown together in the focused board. The countdown, ring, presets, and action hierarchy remain legible at the device viewport. The 5 min and 60 min labels sit closer to the ring endpoints than in the source; they remain readable and do not obscure the timer. This is a P3 fidelity refinement, not a blocking issue.

## Interaction verification

1. Idle Home showed 25:00 and Start focus.
2. Start focus opened the live view and the countdown advanced.
3. Pause changed the center control to Resume. The displayed 24:37 remained unchanged through an additional 3-second check.
4. Resume changed the center control back to Pause and the countdown continued to 24:32 after 3 seconds.
5. Cancel opened “Cancel this session?” with the message that progress would be discarded. Confirming Cancel session returned to idle at 25:00; no session was saved.
6. The live controls are Stop, Pause/Resume, and Cancel. Each control measured 144 × 144 physical pixels, equal to 48 × 48 dp. The Stop control exposes the label “Stop”; it was not activated because it may save a session.

## Required fidelity surfaces

- Fonts and typography: Google Sans is bundled in regular, medium, semibold, and bold weights. The large timer remains prominent, with no visible clipping or awkward wrapping at this viewport.
- Spacing and layout rhythm: The timer, presets, primary action, queued-task row, and floating navigation retain clear separation. Device/system bars add chrome absent from the source.
- Colors and visual tokens: The app uses the adapted dark monochrome palette, with white primary text and muted gray surfaces and controls.
- Image quality and asset fidelity: The mascot is removed as requested. The visible controls use crisp Material icons; no image asset is needed for this screen.
- Copy and content: Timer and action copy match the intended flow. “Choose a task” is the expected empty selection state; the source’s `test4` is user data and was not fabricated.

## Findings

- No actionable P0, P1, or P2 differences remain for the Home surface at this viewport.
- P3: Move the 5 min and 60 min endpoint labels slightly below the ring ends to match the source more closely.
- Accessibility evidence: UI hierarchy exposes named Settings, Start focus, Stop, Pause/Resume, Cancel, and navigation controls. The 48 dp timer-control targets meet the app’s intended compact size. Contrast was visually reviewed only; TalkBack, text scaling, and automated contrast checks were not run.

## Comparison history

- Initial device capture was blocked by Toki’s first-run consent. After the user accepted it, this pass captured and inspected the clean Home and live timer states. No UI code changes were made during this QA pass; the new files are current-run screenshots, comparison boards, and this report.

## Evidence limits

This is a single-device, single-viewport Home review. The pass verifies foreground start, pause, resume, and cancel behavior; it does not exercise process-death recovery, device reboot recovery, timer completion, or the Stop action.

## Final result

final result: passed

## Tasks and Home integration — 2026-10-12

### Device and build

- Device: RMX2156, Android 12, 1080 × 2400 physical pixels at 480 dpi.
- Installed `com.focusfirst` debug build 1.0.1 with `adb install -r`; existing app data was preserved.
- `:app:testDebugUnitTest` and `:app:assembleDebug` both passed after the duration fix.

### Interaction verification

1. Opened Tasks from the floating bottom navigation and verified the week strip, task rows, completion controls, and fixed Add task action.
2. Created “Device task flow” for Mon, Oct 12 with a note, Focus tag, and 15-minute duration. The task appeared in Today with the saved 15-minute duration.
3. Starting that task initially opened Home at 25:00. Root cause: the matching-preset path used the stale idle timer duration. Added an explicit duration override to the start path and a regression test.
4. Reinstalled the updated build without clearing data. Starting the same task now opened Home at 15:00, showed “Task: Device task flow,” and counted down to 14:57.
5. Canceled the QA timer and confirmed cancellation; Home returned to idle. Marked the QA task complete; it moved to Completed, and Home returned to “Choose a task.”

The QA task remains in the app as a completed sample. The timer is idle.

### Device captures

- Creation sheet: `docs/design/toki-task-creation-device-rmx2156.png`
- Tasks list: `docs/design/toki-tasks-device-rmx2156.png`
- Live task timer: `docs/design/toki-task-live-device-rmx2156.png`
- Completed task: `docs/design/toki-task-completed-device-rmx2156.png`

### Not exercised

The date-picker interaction, 60-minute Pro gating, and new-tag creation were not tested on-device. The existing unit suite covers task date conversion, week dates, and timer-link values.
