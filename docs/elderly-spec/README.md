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

**Clock app redesign complete as of 2026-07-18.** All four tabs, the Alarm
and Add City full-screen editors, and the alarm/timer ring screens are
implemented and match this spec — see
[decisions.md](decisions.md#verified-working-live-device-testing-2026-07-18)
for what's been confirmed on-device.

Genuinely still open: dark theme and 200% font size haven't been checked
(everything so far was verified in light theme at default scale only); a
pre-existing Timer duration display bug hasn't been investigated; alarm
extras (ringtone/vibrate/label/snooze) and the Settings/widgets redesign
remain deferred by design (decision #5, #13). See decisions.md's "Still
open" section for the full list.
