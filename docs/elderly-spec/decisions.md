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
| 22 | List row dividers | Thin, low-contrast divider line between Add City rows (`divider_grey` / `divider_height`, the existing Commons-wide divider tokens), reaching the true screen edge on both sides (0 to full width, pixel-verified) — the row's own content (selector circle, title, offset) carries its own start/end margins instead of the row root having padding, since ConstraintLayout doesn't reliably let a negative margin push a child past its parent's own padding. |
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
- 200% system font size — the ElderBerry restyle (2026-08-09) hasn't been
  checked at large accessibility font scales yet; the picker wheel's font
  size does scale with `fontScale` (see `WheelPickerView`), but the rest of
  the screens haven't been verified not to clip.
- Dark theme — spot-checked on-device 2026-08-09 (Alarm tab) and reads
  correctly (readable, consistent, right structure), but the dark palette in
  `eb_colors.xml` (values-night) is **derived, not spec'd** — the handoff
  only shipped a light surface — so it hasn't been checked screen-by-screen
  or contrast-audited.
- Ring screen (`AlarmActivity`) — restyled in code (gradient background,
  bell icon, stacked buttons) but not seen firing live on-device this
  session; the activity isn't `adb`-exported and driving the full alarm-fire
  flow through the custom picker wheel via shell taps was too slow/imprecise
  to set up in the time available. Verify next session by letting a real
  alarm ring.
- Alarm list row alignment — Emmett flagged pencil/time/day-text as
  inconsistently aligned across rows; investigated once and couldn't
  reproduce in the two reachable row states (no way to set a label through
  the current UI). Worth another look if it recurs.

## ElderBerry Design System handoff (2026-08-09)

A high-fidelity design handoff ("ElderBerry Design System", an HTML/CSS
prototype — not tracked in this repo, lived at
`~/Downloads/design_handoff_elderberry_clock/`) replaced the earlier
hand-sketch wireframes as the visual source of truth. Exact colors,
typography, spacing, radii, motion timings, and copy from that handoff are
now the spec — copied into [design-tokens.md](design-tokens.md). Product/
behavior decisions above (single timer, full-screen editors, no gestures,
alarm scheduling semantics, etc.) are unaffected; only the visual layer
changed. Where the handoff's visuals disagree with an earlier decision below,
**the handoff wins** — confirmed with Emmett 2026-08-09 rather than
re-litigating each one individually, since the handoff is explicitly final
fidelity ("colors, typography, spacing, radii, motion timings, and copy are
final").

| # | Question | Decision |
| --- | --- | --- |
| 25 | Visual spec source | **The ElderBerry handoff supersedes the wireframe-derived visual details in design-principles.md and the decisions below where they conflict.** Specifically: |
| 16 (revised) | Header title | **Superseded.** The title row now reads the active tab's name ("Clock" / "Alarm" / "Stopwatch" / "Timer") per the handoff, not a constant "Clock" on all four tabs. Still no back arrow anywhere; full-screen editors keep "Add City" / "Alarm" as before. |
| 17 (revised) | Day chip shape | **Superseded.** Rounded rectangles, 14px radius, 56px tall, per the handoff — not squares. |
| 18 (revised) | Bottom tab bar coloring | **Superseded.** Active tab (icon + label) is **purple** `#6a3a7e`; inactive is **gray** `#7c7883` — not black/purple. Icon shape still doesn't change between active/inactive. |

Everything else in decisions #1–24 stands as written.

## Post-handoff polish pass (2026-08-09, same day)

A round of fixes/refinements on top of the handoff restyle, from live on-device review:

| # | Question | Decision |
| --- | --- | --- |
| 26 | Row divider placement | Bug fix. Dividers in the world-clock, alarm, lap, and Add City lists were anchored to the row's own bottom padding boundary (via a `marginTop` that had no effect without a matching top constraint), so they sat flush against the row's content with almost no gap, then left a double-wide gap before the next row. Fixed by dropping root `paddingVertical` on all four row layouts and anchoring the divider `top_toBottomOf` the last content view instead - it now sits equidistant between this row's content and the next row's. |
| 27 (revised) | Add City button fill | **Superseded same day.** First tried transparent/ghost (see below); Emmett clarified he meant the button itself stays solid/filled (`ElderlyButton.Primary`), it's the *list* that should run behind it - the `ElderlyButton.Ghost` style still exists but isn't used by this button anymore. The list-runs-full-height-behind-the-button layout change stands. |
| 28 | Bottom nav icons | Bug fix. The tab bar was using the app's existing chunky filled Material-style icons (960×960 viewBox), not the handoff's thin stroke-line icons (24×24 viewBox, stroke-width 2.2, round caps) - visually a different icon language. Rebuilt as `ic_tab_*_vector` matching the handoff's SVG paths exactly, with a new custom tab-item layout (32dp icon, 6dp gap, 16sp/700 label) replacing Commons' stock one. |
| 29 | Pill button height | **60dp**, not the handoff's 52dp - +4dp top/+4dp bottom, Emmett's explicit call. Applies to `eb_button_height` and `eb_ringing_button_height`. |
| 30 | Pressed-state feedback | Every pill button (Primary/Secondary/Ghost/Ringing) now inverts its colors while held (e.g. Primary's filled-plum/white-text becomes white-bg/plum-text) via pressed-state `ColorStateList`s, so elderly users get an unambiguous "yes, that registered" cue on tap - Emmett's explicit call. Bottom nav tabs got a colored ripple back (was `tabRippleColor="@null"`). Day chips and the alarm/repeat toggle already invert on tap as part of their normal selected-state styling, so they weren't touched. |
| 31 | Wheel picker row spacing/sensitivity | Row height (`ITEM_H_DP` / `eb_wheel_viewport_height`) widened from the handoff's 58dp/176dp to 78dp/234dp - a bigger drag surface for fat-finger scrolling, shared by the Alarm editor and Timer setup wheels (same `WheelPickerView` + dimens, so one change reaches both). Momentum tuned down (`DECAY` 0.84→0.78, `RELEASE_VELOCITY_SCALE` 16→10, `RELEASE_VELOCITY_CLAMP` 2.2→1.5) so a flick coasts less and is easier to stop on an exact value - both per Emmett's explicit call. |
| 32 (revised) | Timer start transition | Tapping Start Timer animates into the running countdown: the wheels' neighboring numbers fade away, the selected H/M/S values fly from their wheel position to their landing spot in the countdown display (measured off the real, already-autosized countdown view so the handoff is pixel-exact), and a colon fades in on each side moving along the same path. **The Hour/Minute/Second header labels no longer move** - Emmett's follow-up call: both `timer_setup_header` and `timer_running_header` now share the identical column padding (the nudge values that used to only apply to the running row), so a label sits at the exact same pixel position in both states and there's nothing to animate there at all. Implemented in `TimerFragment.playStartTransition()` with floating overlay `TextView`s driven by a single `ValueAnimator`. Best-effort on the vertical centering math for the flying digits (approximated from font metrics rather than a live layout pass per frame, to avoid a one-frame lag) - worth a closer look if it reads as off on a future pass. |

## Second polish pass (2026-08-09, same day, after live device review of the first pass)

| # | Question | Decision |
| --- | --- | --- |
| 33 | Stopwatch tab icon | Bug fix, two rounds. First: the handoff's own SVG draws the center hand-tick and the button cap as two disconnected segments with a gap, and its r=8 circle is ~20% narrower than the other three tab icons - both fixed in decision #28's rebuild. Second (this pass): the fix accidentally dropped the hand-tick entirely, leaving an empty-looking dial - restored as one continuous line from the dial center through the rim to the button cap, so it reads as both hand and crown stem. |
| 34 | Tab label sizing | Bug fix. Removing AutofitHelper (decision #28) fixed the size-mismatch but exposed that "Stopwatch" (the longest label) clipped at a flat 16sp against its real column width, and a hand-picked smaller flat size (14sp) still clipped it once - Material's TabLayout reserves its own internal per-tab padding even with `tabMinWidth=0dp`, so naive `barWidth / tabCount` math overestimates available space. Fixed properly: `MainActivity.equalizeTabLabelSizes()` measures each tab's *actual* laid-out column width post-layout and computes the largest size all four labels fit at together, applied uniformly - adapts correctly to any device/density instead of a hardcoded guess. Currently lands at a small margin above 14sp; nudged closer to the edge (0.9 → 0.95 of the measured column) per Emmett's "slightly bigger" follow-up. |
| 35 | Add City search field text position | Bug fix. Text/hint sat high in the pill - `android:gravity="center_vertical"` alone didn't fully fix it; `includeFontPadding` (default true) adds extra space above the glyph's ascent that skews the visual center, and the default EditText style likely added its own asymmetric vertical padding. Fixed with `includeFontPadding="false"` + explicit `paddingVertical="0dp"` alongside the gravity, matching how every other text element in the app is already set up. |
| 36 | Add City search field border | **Removed** - Emmett's explicit call, overriding the handoff's 2dp `eb_line` stroke. `search_field_background.xml` is now just the rounded fill, no stroke. |
| 37 | Tab label size, round 2 | Emmett asked for it "slightly bigger" twice more after decision #34 landed - `equalizeTabLabelSizes()`'s safety margin nudged from 0.9 → 0.95 → 0.97 of the measured column width (still computed dynamically per device, not a hardcoded size). |
| 38 | Wheel picker momentum/precision, round 2 | Still felt jumpy after decision #31's first pass. Momentum cut much further (`DECAY` 0.78→0.62, `RELEASE_VELOCITY_SCALE` 10→4, `RELEASE_VELOCITY_CLAMP` 1.5→0.7, velocity smoothing increased) so a flick barely carries past where the finger actually dragged. Also added a "sticky zone" (`STICKY_ZONE`/`STICKY_DAMPING` in `WheelPickerView`): drags within ~22% of an item-height of the centered value are damped to 35% effect, giving the current value some free-play/detent before the wheel starts tracking the finger 1:1 - Emmett's idea ("the center one gets more space that's theirs"). On-device test: a 2-row-equivalent drag now lands almost exactly 2 rows away with no overshoot (previously overshot by 5+ rows on a similar drag). |

| 39 | Wheel picker momentum, round 3 | Still felt jumpy - Emmett's diagnosis was that the fling distance still scaled too much with exactly how hard/fast the release was, and asked for it to feel like a standard, steady scroll instead. Rewrote release momentum from scratch: no more hand-tuned decay constants at all. `WheelPickerView` now computes a target value via a soft-saturating (tanh) function of release speed - `MAX_COAST_ROWS` is a steady ceiling every real flick approaches (a gentle flick and a 4x-faster hard one land at the same distance past a moderate speed), then animates straight to that value using `android.widget.OverScroller.startScroll()` for a standard-feeling ease-out glide (duration scaled by distance). Direct on-device test: a 250ms swipe and a 60ms swipe over the same distance both landed exactly 5 rows away (previously the fast one landed ~7x farther than the slow one). Also asked directly whether the original Fossify `NumberPicker` scroll logic was still in play anywhere - it isn't; `WheelPickerView` fully replaced it for the Alarm/Timer wheels back in the initial ElderBerry restyle (the `com.shawnlin.numberpicker.NumberPicker` dependency only remains for one unrelated dialog, `dialog_my_time_picker.xml`). OverScroller is the same underlying mechanism that stock `NumberPicker` itself is built on, so this is close in spirit to "the original logic" without giving up the spec-matched custom rendering. The STICKY_ZONE/LEAVE_DAMPING/ENTER_ASSIST detent and DY_SMOOTH shaky-hand filtering from decision #38 are unchanged - they govern the direct-drag feel, not release momentum, and weren't part of this round's complaint. |

Everything else in decisions #1–32 stands as written.

## Typography: numerals (2026-08-13, from the Phone app)

| # | Question | Decision |
| --- | --- | --- |
| 40 | Font for numbers | **Digits are set in Roboto, everything else in Poppins** - Emmett's call, made against the Phone app but stated as a general rule ("any number will be in roboto and the others will be the other font"). Scope, when asked how far it reaches: only text that is *nothing but* digits - keypad keys, a typed or displayed phone number, the 911 pill, a call timer. Text that mixes words and digits ("09:56 AM", "Outgoing · 3 Calls") stays wholly Poppins; digits are not picked out mid-string. Weight is matched to the Poppins it replaces rather than dropped to a single weight - the Phone app maps Poppins 600/700/800 to Roboto Medium/Bold/Black, so a number looks exactly as heavy as it did before, just in Roboto's letterforms. Note this differs from the Clock handoff's "always Roboto 500" for time readouts, which stands for Clock. Views with fixed content pick the face off a style; views that only learn at bind time whether they hold a name or a number (a Recents row title, a caller label) choose in code. |

## Contacts: New/Edit Contact screen (2026-08-16, from the Contacts app)

Handoff: `design_handoff_contacts_new_contact` — README plus three per-state
boards. Where the boards (authored later the same day) disagreed with the
README, the boards won: avatar initial 57sp/800 (not 40), filled field value
22sp/700 while the placeholder stays 20sp/600, Save carries a 2dp border of its
own fill colour so it measures identically to Cancel.

| # | Question | Decision |
| --- | --- | --- |
| 41 | Caps field labels vs the 18sp floor | The handoff draws the "FIRST NAME" labels above filled fields at 14px. **The 18sp floor from design-principles.md wins** — Emmett's explicit call. It is the only value in the handoff deliberately not matched, and the reason is the same one the floor exists for. |
| 42 | Field label capitalisation | **Title Case** — "First Name", "Phone Number", "Zip Code", "Show Optional Fields", "New Contact" — per the boards, overriding the sentence-case rule in design-principles.md § Language. English strings only; other locales keep their Commons translations. |
| 43 | 911 pill, one shape across the suite | The Contacts pill is sized to **the Phone fork's pill**, not to the contacts board: 20dp/8dp padding, 8dp icon gap, 24dp icon, 22sp label in Roboto Black (decision #40 — digits only). Emmett's call: the emergency affordance must be identical wherever it appears, so Phone is the reference and other apps match it. Contacts keeps the handoff's `#c62828` red; Phone's is `#c1272d` — visually indistinguishable, worth unifying on a later pass. |
| 44 | 911 asterisk glyph | Rebuilt. The old `ic_emergency_star` was a thin-armed asterisk that read much lighter than the heavy six-armed glyph in the boards. Replaced with three blunt-ended bars crossed at 60°, arms ~4.5× as long as thick, ink filling ~0.7 of the box the way a 24px glyph does. **Applied to every 911 button in the suite** — the drawable is kept identical in Contacts and Phone. |
| 45 | Press feedback on the optional-fields toggle | The Show/Hide Optional Fields pill takes **no press state at all** — Emmett's explicit call, carving it out of decision #30. It is a disclosure control rather than a solid-fill action button; Cancel/Save/911 keep the press-invert. |

## Contacts + Phone follow-up pass (2026-08-16, same day)

| # | Question | Decision |
| --- | --- | --- |
| 46 | One 911 red | **Superseded #43's note.** Both apps now use `#c62828` — the contacts handoff's value — in **both themes**. Phone's `#c1272d` is gone, and so is its dark-mode lift to `#e04a50`: `eb_emergency` has no night variant in either app, because the emergency affordance must be the identical colour everywhere it appears. |
| 47 | Phone's "+ New Contact" pill | The pill floats **over** the contacts list instead of sitting on an opaque strip of its own, so rows scroll visibly behind it — Emmett's explicit call, and the same treatment decision #27 settled on for Clock's Add City button. `ContactsFragment` pads the list's bottom by the pill's measured footprint so the last contact can still be scrolled clear. |
| 48 (revises #40) | Titles are always Poppins | **Carve-out from the numerals rule.** A row or screen *title* stays Poppins even when it is a bare number, because a title names the thing rather than reading out a value. Under #40 an unsaved caller's Recents row rendered in Roboto Bold — visibly lighter and narrower than the saved contact directly above it, so one list read as two kinds of row. Emmett: "the title of ANY call/thing should be the same bold and color that a contact gets." Applies to the Recents row title, the Recents detail screen title, and a Contacts row for a contact saved without a name (`applyTitleTypeface*`). Genuine numeric readouts — keypad input, a Phone Number field value, the 911 pill, call timers — still follow #40. |
| 49 | Recents avatar alignment | Bug fix. The Recents row anchored its title to the **avatar's top** with no vertical chain, while the avatar centred itself in the row — so with two lines of text beside it the avatar sat visibly high, unlike the Contacts row. Title + status are now a packed vertical chain centred in the row, exactly as `item_contact_row` already did. |
| 50 | Keypad key fill | The digit keys drop their plum tint for a neutral grey (`#e7e7e7`, same lightness as the `#e6e2e9` it replaces; `#3a3a3c` in dark) — Emmett's explicit call. They also get their own neutral pressed colour rather than sharing `eb_soft_fill_pressed`, so a key cannot pick the tint back up on touch. The "Add To Contacts" soft pill keeps its tint; only the keys were in scope. |

## Contacts: focus-to-compose animation (2026-08-16, from FOCUS_ANIMATION.md)

| # | Question | Decision |
| --- | --- | --- |
| 51 | Collapse-on-focus | Per the signed-off spec: any field taking focus collapses the avatar + name block (height → 0, opacity → 0, `translateY` → -12dp, padding → 0) over **520 ms** on CSS `ease` (`PathInterpolator(.25,.1,.25,1)`), opacity clearing at 0.8 of that; the Cancel/Save row's vertical padding rises 12dp → 16dp over the same curve; the header title takes over the name, live per keystroke, and returns to "Contacts" at rest even when the contact has one. One `ValueAnimator` drives a single fraction that every value is derived from, so nothing can drift. 130 ms blur grace period stops the flicker when moving between fields. Reduce-motion applies the end state directly. |
| 52 | Two Android deviations from the spec | (a) The expanded height is **measured**, not the spec's fixed 190px max-height. The CSS constraint behind that number — `auto` is not animatable — does not apply on Android, and a hardcoded 190dp would clip the block at large system font sizes, which this suite cannot afford. (b) The spec's "focus lost" has no direct Android equivalent: a field keeps focus after the keyboard closes, so the block would never come back. **Putting the keyboard away drops focus**, which is how a user signals they are done typing. |

## Suite-wide: the typed line never hides behind the keyboard (2026-08-16)

| # | Question | Decision |
| --- | --- | --- |
| 53 | Caret vs keyboard | **Universal rule, every app.** The line being typed always stays a fixed distance (24dp) clear of the keyboard: as a field wraps onto a new line or a box grows, the scroll container follows the caret instead of letting it slide underneath the IME. Emmett's explicit call — scrolling back up by hand every line or two is exactly the fiddly correction this suite exists to remove. Installed with one call per screen, on the scroll container, after its fields exist: `keepTypedLineClearOfKeyboard(keepClearPx)`. It walks the container's `EditText` descendants for caret movement and watches the container itself for resizes. |
| 54 | Why not `requestRectangleOnScreen` | The platform's own "reveal this area" call looks like the obvious tool and **does not work here**: `ScrollView`/`NestedScrollView` measure their visible area as the full view height and ignore bottom padding, so on an edge-to-edge screen — where the keyboard is drawn *over* the container rather than shrinking it — they decide the caret is already visible and never scroll. The helper instead measures how much of the container the keyboard actually covers (IME inset vs the container's position in the window), which is correct whether the window resizes or the keyboard overlays. Worth remembering before anyone "simplifies" it back. |

**Where it is installed.** Contacts → the new/edit contact editor. Notes → the note editor body. Those are the only two screens in the suite with a text field inside a scrolling container. Messages' compose box is bottom-anchored above the keyboard and grows upward, so it is structurally immune; Phone, Clock, Camera and Gallery have only single-line fields in dialogs, which the framework already lifts clear. Any new scrolling text screen should call the helper.
