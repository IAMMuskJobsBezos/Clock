# Design Principles (all screens)

These rules come from what is consistent across every wireframe. They override
current app styling wherever the two conflict.

## Typography

- Primary time displays (current time, countdown, elapsed time) are the
  dominant element on screen — roughly 1/4 of screen height in the sketches
  (e.g. "9:45 AM", "3:11:06"). Target ≥ 64sp, scaling with system font size.
- List rows (alarm times, world-clock cities) use large text: ~30–36sp for the
  time, ~20sp for the secondary line (days, city, date).
- All other text ≥ 18sp. Nothing smaller than 18sp anywhere.
- Respect the Android system font-size setting; layouts must not clip at the
  largest accessibility font sizes (test at 200%).

## Touch targets

- Minimum touch target 56dp; primary action buttons are full-width (or
  half-width when paired) pill buttons ~64dp tall, as drawn
  ("Start Timer", "+ Add Alarm", "Cancel"/"Save").
- Paired buttons (Cancel/Save, Reset/Stop) sit side by side with a clear gap;
  destructive/leaving action on the left, confirming action on the right —
  matching every wireframe.

## Buttons say what they do

- Every button has an icon **and** a text label: "▶ Start Timer",
  "⏸ Stop Stopwatch", "↺ Reset", "✓ Save", "✗ Cancel", "+ Add Alarm",
  "+ Add City". No icon-only actions anywhere in the redesigned screens.
- Verbs name the object ("Start Stopwatch", not "Start") on the main action of
  each tab, exactly as sketched.

## Navigation

- Persistent bottom navigation bar on all four main screens with four
  fixed items, each icon + label: **Clock, Alarm, Timer, Stopwatch**. Active
  tab marked by color, not icon shape (decision #18): every tab — active or
  not — uses the same outline-style icon; the active tab's icon and label
  are **black**, inactive ones are **purple**.
- Swiping between tabs is **disabled** (decision #9) — accidental horizontal
  swipes changing screens is a common elderly-UX failure. Tabs change only by
  tapping the bottom nav.
- **No back arrow in any header, anywhere** (decision #16) — every screen's
  app bar is a plain static label reading "Clock", not a per-tab or
  per-screen title, and never shows a back icon. The system back
  gesture/button is the way to leave a full-screen editor without saving.
  Matches the sibling Fossify apps' single-label header pattern; the
  tradeoff is that Settings/About/Sort are unreachable from the main screen
  (no overflow menu either — see [alarm.md](alarm.md), [implementation-map.md](implementation-map.md)).
- Editing happens on **full screens**, not floating dialogs: alarm editor and
  Add City are drawn as full pages with explicit Cancel/Save. No editing via
  long-press, swipe actions, or hidden menus.

## Input

- Time entry uses large **scroll wheels** (hour / minute / AM-PM or
  hour / min / sec), never a keypad or clock-face picker. Wireframes include
  helper text ("Scroll to set time/alarm") — keep short inline hints like this.
- Time format follows the system 12/24-hour setting (decision #10): wheels get
  a 24-hour variant (0–23 hour column, no AM/PM column).
- On/off is a labeled toggle with the words "Off" and "On" beside the switch,
  as drawn on alarm rows and the Repeat control.
- Day-of-week selection is a row of large square chips S M T W T F S
  (decision #17) with helper text "Tap to select".

## Color & contrast

- High contrast text (≥ 7:1 for body text, WCAG AAA where feasible).
- Color is never the only signal — toggles also show Off/On words, active tab
  also shows an underline/label weight change.
- Keep Fossify's light/dark theming, but the elderly layouts must meet the
  contrast bar in both themes.
- **Stay on Fossify Commons** (decision #3): reuse Commons theming, colors,
  and components wherever possible — scaled up, not replaced. The app must
  still read as Fossify Clock.

## Language

- Plain words, no jargon, sentence case. Relative descriptions where they help:
  "Tomorrow", "+3hr" (world clock offset), full day names on alarm rows when
  space allows.
