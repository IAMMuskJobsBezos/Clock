# Elderly-Friendly Clock — Spec Overview

Redesign of Fossify Clock for elderly users, based on hand-drawn wireframes
(local copies in `wireframes/`, untracked; originals in
`~/Downloads/clock-wireframes` — only `use as well.jpeg` and the nine
`Screenshot 2026-07-17 …` files are source material).

## Goal

Keep the four core functions — Clock, Alarm, Timer, Stopwatch — but make every
screen usable by someone with reduced vision, reduced fine-motor control, and
low tolerance for hidden or ambiguous UI. Fewer features on screen, bigger
targets, everything labeled in words — while staying on the Fossify Commons
design system so it still looks like Fossify Clock.

**This fork's UI is replaced outright** (no "simple mode" toggle, no separate
flavor) — decision #1 in [decisions.md](decisions.md).

## Spec files

| File | Covers |
| --- | --- |
| [design-principles.md](design-principles.md) | Type scale, touch targets, contrast, navigation, language rules for every screen |
| [design-tokens.md](design-tokens.md) | Exact colors, typography, spacing, radii, motion timings from the ElderBerry Design System handoff |
| [clock.md](clock.md) | Clock tab: big local time, world-clock list, Add City screen |
| [alarm.md](alarm.md) | Alarm tab: alarm list, full-screen alarm editor with scroll wheels |
| [timer.md](timer.md) | Timer tab: wheel picker, running/paused states |
| [stopwatch.md](stopwatch.md) | Stopwatch tab: start/pause/lap/reset states |
| [ring-screens.md](ring-screens.md) | Alarm ringing, timer finished, snooze (not wireframed; specced to the same principles) |
| [implementation-map.md](implementation-map.md) | How each spec maps onto the existing Fossify code, plus the build/verify loop |
| [decisions.md](decisions.md) | All resolved product decisions and the few items deferred |

## Scope

**V1:** the four tabs, full-screen editors (alarm, add-city), ring screens,
bottom navigation.

**Later (unchanged in v1):** widgets, Settings screen, per-alarm extras
(ringtone/vibrate/label/snooze placement — TBD, decision #5), data model,
backup/export.

## Status

**Structure/behavior complete as of 2026-07-18**, then **restyled to the
ElderBerry Design System handoff on 2026-08-09** (decision #25) — see
[design-tokens.md](design-tokens.md) for the exact values now in effect.
Product/behavior decisions from the first pass (single timer, full-screen
editors, alarm scheduling, etc.) are unchanged; only the visual layer moved.

The restyle included a new `WheelPickerView` (physics-matched picker wheel,
replacing the stock NumberPicker library) used on the Alarm editor and Timer
setup screens, and an `AngledGradientDrawable` for the ring screen's 160°
gradient. Verified on a physical Pixel 9a (light + a dark-theme spot check):
Clock, Add City, Alarm list + editor, Stopwatch/Timer idle & running all
match the handoff closely. Not yet seen live: the ring screen firing for
real (restyled in code, not device-verified this session — see
decisions.md's "Still open").

Genuinely still open: 200% font size hasn't been checked; the dark palette
is derived, not spec'd, and needs a real contrast pass; a pre-existing Timer
duration display bug hasn't been investigated; alarm extras
(ringtone/vibrate/label/snooze) and the Settings/widgets redesign remain
deferred by design (decision #5, #13). See decisions.md's "Still open"
section for the full list.
