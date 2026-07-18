# Implementation Map

How the spec lands in the existing Fossify Clock codebase
(`app/src/main/kotlin/org/fossify/clock/`). This is a pointer map, not a task
plan. All product decisions are in [decisions.md](decisions.md).

## Build & verify loop (decision #15)

Every redesigned screen is verified visually, not just by compiling:
build → install on emulator → screenshot the screen in each specced state →
compare side-by-side with the matching image in `wireframes/` → adjust →
repeat until it matches the sketch's structure and the
[design principles](design-principles.md). Also re-screenshot at 200% system
font size and in dark theme before calling a screen done.

## Shell / navigation

| Spec | Existing code | Change |
| --- | --- | --- |
| Bottom nav, 4 labeled tabs | `activities/MainActivity.kt`, `res/layout/activity_main.xml` | Ensure icon + always-visible label per tab; **disable ViewPager swipe** (decision #9) |
| Global type/target scale | theme + `res/values/dimens` | New dimension set (time display, list row, button heights) used by all redesigned layouts |

## Clock

| Spec | Existing code | Change |
| --- | --- | --- |
| Big time + date, world list | `fragments/ClockFragment.kt`, `fragment_clock.xml`, `item_time_zone.xml` | Rework layout sizes; add relative-offset ("+3hr") text; drop seconds |
| Add City full screen | `dialogs/AddTimeZonesDialog.kt`, `dialog_select_time_zones.xml` | Replace dialog with a new full-screen activity (search + check list + Cancel/Save) |

## Alarm

| Spec | Existing code | Change |
| --- | --- | --- |
| Alarm list rows | `fragments/AlarmFragment.kt`, `adapters/AlarmsAdapter*`, `item_alarm.xml` | Bigger rows: pencil, big time, schedule line, worded Off/On toggle |
| Full-screen editor with wheels | `dialogs/EditAlarmDialog.kt`, `dialog_edit_alarm.xml`, `dialogs/MyTimePickerDialogDialog.kt` | Replace with new activity: 3 scroll wheels (2 in 24h mode), Repeat toggle + day chips, Delete Alarm (with confirm), Cancel/Save. Ringtone/vibrate/label/snooze fall back to defaults (TBD, decision #5) |

## Timer

| Spec | Existing code | Change |
| --- | --- | --- |
| Single timer, wheel setup, run/pause | `fragments/TimerFragment.kt`, `fragment_timer.xml`, `item_timer.xml`, `dialogs/EditTimerDialog.kt`, `services/TimerService` (keep) | Replace multi-timer list UI with single-timer state machine (setup → running → paused). Service/scheduling logic reused |

## Stopwatch

| Spec | Existing code | Change |
| --- | --- | --- |
| Big display, labeled buttons, laps | `fragments/StopwatchFragment.kt`, `fragment_stopwatch.xml`, `item_lap.xml`, `dialogs/ChangeTimerSortDialog.kt` etc. | Resize; replace icon buttons with labeled pills; remove lap sorting UI |

## Ring screens

| Spec | Existing code | Change |
| --- | --- | --- |
| [ring-screens.md](ring-screens.md) | `activities/AlarmActivity.kt`, `activity_alarm.xml`, `SnoozeReminderActivity.kt`, notification builders in `helpers/`/`services/` | Restyle to giant time + two huge labeled buttons (Snooze / Stop Alarm); timer-finished gets one "Stop Timer" button |

## Untouched in v1 (decision #13)

Widgets (`WidgetAnalogueConfigureActivity`, `WidgetDigitalConfigureActivity`),
`SettingsActivity`, databases/models, receivers, backup/export.

## Android permissions

No new Android permissions — the redesign is UI-only (rule captured in the
repo `CLAUDE.md`). Existing alarm/notification permissions stay as-is.
