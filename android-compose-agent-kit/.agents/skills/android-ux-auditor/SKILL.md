---
name: android-ux-auditor
description: Inspect navigation, user journeys, back stacks, gestures, state restoration and interaction friction throughout a Compose Android app. Use during repository-wide audits, feature changes, regression analysis, or design-system migrations in Kotlin Jetpack Compose projects.
---

# UX Flow Auditor

Read repository root `AGENTS.md` first. Follow its safety gates, inventory conventions and evidence requirements.


## Process
1. Enumerate user entry paths: launcher, deep links, notifications, widgets, onboarding, authentication, tab destinations, and restoration after process death.
2. Build directed graph of destinations and transitions from NavHost, event handlers and state-driven routing; annotate guards, permissions, loading, success, failure, cancel, dismiss and retry.
3. Trace each primary task end-to-end, from entry to confirmation/exit, including every sheet and navigation edge. Verify Back/predictive Back, home, tabs, duplicate taps, cancellation, offline retry, rotation, process recreation and state retention.
4. Assess discoverability, information scent, action hierarchy, redundant steps, accidental destructive actions, undo, feedback, keyboard behavior, focus, pagination and accessibility.
5. Record transitions in `docs/qa/flow-inventory.csv` and issues with UX impact, reproducible evidence and targeted recommendations. Keep preference-based recommendations distinct from objectively broken behavior.
6. Write before/after task paths and predicted benefit; validate changes with UI tests and device runs whenever available. Mark unexecuted flows Not Tested.
## Quality gate
No orphan destinations, nonfunctional controls, misleading success states, lost unsaved edits, unexpected navigation stack growth or dead-end errors without explicit documentation.

## Outputs
Update relevant inventory, `docs/qa/issues.csv`, and `docs/qa/report.md`; cite exact files/lines, test commands and execution status.
