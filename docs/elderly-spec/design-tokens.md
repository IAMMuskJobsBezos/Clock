# Design Tokens — ElderBerry Design System

Copied from the 2026-08-09 high-fidelity handoff (see
[decisions.md](decisions.md#elderberry-design-system-handoff-2026-08-09)).
This is the exact spec; implementation resources (`eb_colors.xml`, font
files, dimens) should resolve to these values, not approximations.

Frame reference: 420×900px phone canvas, 38px screen corner radius. Vertical
budget: 44px status bar + 64px title row + flexible scroll body + action dock
(52px button + 14/18px padding) + 101px tab bar. On real devices, use
platform safe-area insets instead of hard-coding these.

## Color (light — shipped surface)

| Token | Value | Use |
|---|---|---|
| bg | `#ffffff` | app + sheet background |
| text | `#15121a` | primary ink, centered wheel cell |
| display ink | `#1a181d` | hero clock digits + meridiem |
| sub | `#6f6b75` | secondary labels, status bar |
| date | `#8b878f` | clock date line |
| faint | `#a8a4ae` | off-center wheel cells |
| accent | `#6a3a7e` | offsets, active tab, fills, links |
| line | `#e8e6ea` | separators, tab-bar rule |
| inputBg | `#f4f3f5` | search field |
| btnBg / btnText | `#6a3a7e` / `#ffffff` | filled buttons |
| ghostBorder | `#d6d3d9` | toggle track (off) |
| tabOn / tabOff | `#6a3a7e` / `#7c7883` | tab bar |
| knobOn | `#ffffff` | toggle knob (on) |
| toggle knob (off) | `#d8cbdf` (pale plum, chosen default) | toggle knob off-state |
| ringing gradient | `linear-gradient(160deg,#6b4574 0%,#4a2c58 55%,#2a1633 100%)` | alarm/timer overlay |

Dark theme: no dark palette shipped in the handoff beyond the ringing
gradient (which is already dark). Derive a dark surface the same way the
rest of Fossify Commons does — invert bg/ink roles, keep `accent` = `#6a3a7e`
(check contrast on dark bg; lighten if needed), keep hue relationships.
Verify contrast on-device (task: build+screenshot in dark theme).

Secondary-button style in use app-wide (Cancel/Delete, Reset, Lap, day
chips): **plum outline** — border + text `#6a3a7e`, transparent fill. (WCAG
≈ 8.4:1 on white, AAA.)

Next-alarm note style in use: **gray chip** — text `#4a4550`, fill
`#f2f1f4`, no border, radius 100px, padding 8px 16px.

## Typography

- **Poppins** — all UI text and labels. Weights in play: 600, 700.
  Sizes: 26 (title), 24 (section label / chip), 21 (picker + column label),
  20 (lap label, sheet offset), 19 (city name), 18 (button, alarm repeat
  line), 17 (on/off), 16 (tab label), 15 (status bar).
- **Roboto 500** — every time readout, always with
  `font-variant-numeric: tabular-nums` (Android: `fontFeatureSettings="tnum"`
  or monospaced-digit fallback). Sizes: 84 (hero clock), 76
  (stopwatch/timer), 64 (ringing), 42 (wheel center), 32 (alarm row), 30
  (city row), 28 (wheel neighbors + meridiem), 24 (lap time), 15 (status
  bar).
- Source: Google Fonts `Poppins:wght@600;700` and `Roboto:wght@500`, bundled
  as app font resources (not fetched at runtime).
- Sizes above are px in the 420-wide reference frame; convert to sp 1:1 (the
  frame is authored at a 1x-ish density) and let them scale with system font
  size per [design-principles.md](design-principles.md).

## Spacing

6, 8, 10, 12, 14, 16, 18, 20, 24, 26, 32, 44px. Screen gutter is 24dp
throughout.

## Radii

100dp (all pills: buttons, toggles, search field), 14dp (day chips), 50%
(selector dots and toggle knobs).

## Sizes

- Buttons: 52dp tall.
- Toggle: 74×42dp pill, 30dp knob.
- Day chips: 56dp tall.
- Search field: 60dp tall.
- Wheel viewport: 176dp tall, 58dp row height.
- Tab icons: 32dp. Button icons: 18–20dp. Row icons: 22dp (edit/pencil),
  20dp (bell), 34dp (Add City selector dot).
- Separator weight: 2.5dp everywhere (`line` color).

## Motion

Picker wheel only; everything else is instant (no transitions on tab switch,
no hover states — touch-first).

- Momentum decay: **0.84**/frame (`v *= decay`).
- Settle lerp: **0.22**/frame (`position += (target - position) * 0.22`
  until within 0.002, then snap).
- Velocity smoothing: `vel = vel*0.65 + (Δy/Δt)*0.35`.
- Initial per-frame velocity on release: `v = -(vel/58) * 16`, clamped to
  ±2.2.
- Mouse/scroll-wheel-equivalent (Android: not applicable, touch only).
- Clamped wheels (AM/PM): 0.35 rubber-band overshoot during drag; momentum
  zeroes on hitting a bound; out-of-range cells not rendered.

Full wheel geometry/behavior spec: see "The Picker Wheel" section copied
below.

### The Picker Wheel (core interaction component)

Used in six places: timer Hour/Minute/Second, alarm Hour/Minute/AM-PM.

**Structure**: column. Label on top (Poppins 600 21dp, `sub` color), then a
176dp-tall viewport with clipped overflow, 6dp margin-top. Five cells
(indices −2…+2 around the current value), each `ITEM_H = 58dp` tall,
vertically centered, offset by `(cellIndex − floatPosition) * 58dp`
translateY.

**Cell appearance as a function of distance `dy` (dp from center)**:
- `near = max(0, 1 − |dy| / 58)`
- font size = `28 + 14 * near` dp (42dp dead center, 28dp at neighbors)
- opacity = `max(0, 1 − |dy| / 118.9)`
- color = `text` (`#15121a`) when `near > 0.5`, else `faint` (`#a8a4ae`)
- Always Roboto 500, tabular numerals.

**State**: each wheel holds a float position, not an integer. Displayed
value = `mod(round(position), size)`.

**Gestures** (`touch-action:none` equivalent — intercept touch, no
scrollbar):
- **Drag**: `position -= deltaY / 58` per move event; commit rounded value
  as it crosses each stop. Track smoothed velocity against the last pointer
  position (not last committed stop): `vel = vel*0.65 + (Δy/Δt)*0.35`.
- **Release**: momentum — `v = -(vel/58)*16` clamped ±2.2, then each frame
  `position += v; v *= 0.84`.
- **Settle**: once `|v| < 0.004`, lerp to nearest integer
  (`position += (target-position)*0.22`) until within 0.002, then snap and
  commit.
- **Clamped wheels** (AM/PM, 2-stop non-cyclic): bound to `[0, size-1]` with
  0.35 rubber-band overshoot on drag; momentum zeroes at a bound;
  out-of-range cells not rendered.

## Assets

Icons: inline SVG, 24×24 viewBox, `fill:none`, `stroke:currentColor`,
`stroke-width:2.2` (2.4–2.6 on X and check glyphs), round caps/joins.
Android: vector drawables at the same geometry, `strokeWidth` scaled to
match, tinted via `android:tint`/`app:tint` to the token color per context
(don't hardcode fill colors in the drawable).

Tab bar icons specifically:
- Clock = circle + hands.
- Alarm = circle + hands + two angled bell-leg strokes.
- Stopwatch = circle (cy 14, r 8) + single hand + crown bar across the top.
- Timer = hourglass (4 paths).

## Shadow

`0 6px 16px rgba(0,0,0,.10)` — defined in the handoff but unused on the flat
button treatment actually shipped. Skip unless a later revision calls for
elevation.
