# Decisions

Resolved 2026-07-17 with Emmett (interactive Q&A). Supersedes the earlier
open-questions list.

| # | Question | Decision |
| --- | --- | --- |
| 1 | Distribution | **Replace the UI in this fork.** No mode toggle, no separate flavor. Divergence from upstream Fossify is accepted. |
| 2 | "Rules .md" reference | No such file existed in Phone/Contacts. Rules captured once in this repo's `CLAUDE.md` (Fossify conventions, Commons design system, permissions policy, build loop). |
| 3 | Design system | Stay within the **Fossify Commons / Fossify Clock look** ("like the other apps") — Commons theming and components, scaled up per the spec. Not a new visual language. |
| 4 | Wireframe images | Kept **untracked** (`.gitignore`); spec text is what's committed. |
| 5 | Alarm extras (ringtone, vibrate, label, snooze duration) | **Not decided — revisit later.** Marked TBD in [alarm.md](alarm.md); for now they stay out of the editor and use current app defaults. |
| 6 | Alarm deletion | **"Delete Alarm" button inside the full-screen editor**, with a confirm step. No hidden gestures. |
| 7 | Timer count | **Single timer.** Multi-timer list removed; existing saved timers dropped on upgrade. |
| 8 | Ring screens | **Spec now, with Snooze** — see [ring-screens.md](ring-screens.md). |
| 9 | Tab swiping | **Disabled.** Bottom-nav taps are the only way to switch tabs. |
| 10 | Time format | **Follow system 12/24-hour setting.** Wheels get a 24-hour variant (0–23 hour wheel, no AM/PM column). |
| 11 | Stopwatch | **Whole seconds only; Lap disabled while paused; laps newest-first; sorting feature removed.** |
| 12 | Reset guard | **One tap, as drawn.** No confirmation on timer/stopwatch Reset. |
| 13 | V1 scope | **Widgets and Settings untouched in v1**; each gets its own pass later. |
| 14 | World-clock offset wording | Keep **"+3hr"** as drawn (compact, matches wireframe). |
| 15 | Build process | Iterative: build → emulator screenshot → compare to wireframes → adjust, per screen. See [implementation-map.md](implementation-map.md). |
| 6 (revised) | Alarm deletion | **Superseded 2026-07-18.** The Cancel button's slot now becomes "Delete Alarm" (with confirm) when editing an *existing* alarm; "Cancel" only appears when adding a new one. No separate standalone Delete button. The header has no back arrow (#16), so system back is the "leave without saving" path for an existing alarm. |
| 16 | Header navigation | **No back arrow in any header, anywhere** — the system back gesture/button is sufficient (phones already have one). The 4 main tabs (Clock/Alarm/Stopwatch/Timer) share one plain static label reading **"Clock"** that does not change per tab. Full-screen editors (Alarm editor, Add City) keep their own contextual title ("Alarm", "Add City") instead — more useful for knowing which screen you're on, and Emmett's original instruction was specifically about tab-switching within the main screen, not the editors. Matches the sibling Fossify apps' plain single-label header pattern. The previous main-tab overflow menu removal (Settings/About/Sort unreachable from the main screen) still stands. |
| 17 | Day chip shape | **Squares**, not the rounded rectangles or circles tried earlier — 38dp, 8dp gap between chips (wider spacing for thick-fingered users), 8dp corner radius, purple outline when unselected / filled purple with white text when selected. |
| 18 | Bottom tab bar coloring | Active and inactive tabs use the **same outline-style icon** (no filled/solid icon variant anywhere) — the only difference is tint: **black** for the active tab (icon + label), **purple** for inactive ones. Chosen so the active tab is unambiguous at a glance without changing icon shape. |
| 19 | Add City offset format | **Relative to device local time** ("+3hr", "-8hr"), not the zone's absolute UTC offset — confirms/restates decision #14 after a brief detour through an absolute "GMT±H" format. |
| 20 | World-clock city source | Full IANA zone list (`TimeZone.getAvailableIDs()`), not the original ~90-entry curated list — needed so search actually finds arbitrary real cities. City name shown plain (no offset prefix); region shown after the name only when the zone data has one ("Louisville, KY"); most zones don't and show just the city. |
| 21 | Add City selector | Purple circles (outline/fill), not checkboxes — same visual language as the day chips. |
| 22 | List row dividers | Thin, low-contrast divider line between Add City rows (`divider_grey` / `divider_height`, the existing Commons-wide divider tokens), extended edge-to-edge past the row's own side padding via negative margins, so rows read as clearly separated. |
| 23 | Toggle switch styling | Every toggle in the app (alarm Repeat switch, per-alarm enable switch) shares one look: **outlined purple, white track, when off; solid filled purple with white thumb, when on** — a very slight (15%) alpha reduction on the off-state purple only, for a touch less vibrancy. Implemented via direct `trackTintList`/`trackDecorationTintList`/`thumbTintList` on the switch (`styleToggleSwitch` in `extensions/MyMaterialSwitch.kt`), bypassing `MyMaterialSwitch.setColors()` — that method always alpha-fades the off-state color to 20-40%, which reads as grayed-out and can't be tuned into a crisp outline through its own API. |
| 24 | Timer minimum duration | The Timer setup wheels can never produce 0:00:00 — when Hour and Minute are both 0, the Second wheel's minimum becomes 1 (was 0), so the lowest settable timer is 1 second. |

## Verified working (live device testing, 2026-07-18)

- Alarm ring flow: notification fires with working Snooze/Stop actions; tapping opens the full-screen ring UI (giant time, outlined Snooze / filled Stop Alarm); Stop Alarm dismisses cleanly. No crashes.
- Timer finished flow: "Time's up" notification fires with a working Stop action; in-app finished state renders correctly. No crashes.
- Neither alert screen auto-launches full-screen while the device is unlocked and in use — that's expected Android behavior for `setFullScreenIntent` (only auto-launches full-screen when the screen is off/locked at trigger time), not a bug in this app.

## Still open (later)

- Alarm extras placement (#5) — decide before or during the Settings pass.
- Settings & widgets redesign (post-v1).
- Timer duration bug: the setup wheel's displayed seconds value doesn't
  always match what the timer actually starts with (observed: picker showed
  8s, timer started with 57s remaining) — not yet investigated (distinct
  from decision #24's minimum-duration fix, which only stops the wheel from
  reaching 0:00:00).
- Dark theme and 200% system font size — the whole session's work was
  verified in light theme at default font scale only; neither has been
  checked yet.
- Alarm list row alignment — Emmett flagged pencil/time/day-text as
  inconsistently aligned across rows; investigated once and couldn't
  reproduce in the two reachable row states (no way to set a label through
  the current UI). Worth another look if it recurs.
