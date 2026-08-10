# Alarm Tab

Wireframes: [`alarm-list-a`](wireframes/alarm-list-a.png),
[`alarm-list-b`](wireframes/alarm-list-b.png),
[`alarm-list-c`](wireframes/alarm-list-c.png),
[`alarm-edit-a`](wireframes/alarm-edit-a.png),
[`alarm-edit-b`](wireframes/alarm-edit-b.png),
[`alarm-edit-c-repeat-off`](wireframes/alarm-edit-c-repeat-off.png).

## Alarm list

1. Header "Alarm" — the app bar shows the active tab's label
   (see [design-principles.md](design-principles.md)).
2. **One full-width row per alarm**, each containing:
   - Edit affordance: pencil icon (one sketch labels it "Edit") on the left —
     tapping the row/pencil opens the editor.
   - Alarm time, very large: "12:00 AM".
   - Schedule line below the time: repeat days ("Tue, Wed, Thu, Fri"), or
     "Tomorrow" for a one-shot alarm, or full day name ("Tuesday").
   - **On/Off toggle** on the right with the words "Off"/"On" flanking the
     switch. Toggling does not open the editor.
3. **"+ Add Alarm"** full-width pill button below the list.
4. Bottom navigation (Alarm active).

No swipe-to-delete, no long-press menus. Deletion happens inside the editor
(decision #6, revised).

## Alarm editor (full screen, replaces `EditAlarmDialog`)

Used for both Add and Edit. No back arrow in the header (decision #16) — the
system back gesture/button is the way to leave without saving.

1. Header "Alarm".
2. **Time picker — three scroll wheels**: Hour | Minute | AM/PM, with column
   headers, current value centered and largest. Helper text underneath:
   "Scroll to set time/alarm".
3. **Repeat control**:
   - Label "Repeat" + Off/On labeled toggle.
   - When **On**: a row of seven **square** day chips **S M T W T F S**
     (38dp, 8dp gap between chips, 8dp corner radius, outlined purple when
     unselected / filled purple with white text when selected) with helper
     "Tap to select". When **Off**: the day chips are hidden entirely.
4. **Bottom-left button is context-dependent** (decision #6, revised):
   - **Adding a new alarm** ⇒ "✗ Cancel", discards and closes.
   - **Editing an existing alarm** ⇒ "🗑 Delete Alarm", with a confirm step
     ("Delete this alarm? Yes / No"). There is no separate standalone delete
     button — this slot *is* the delete action once an alarm exists, freeing
     the header's absent back arrow to be the "leave without saving" path.
5. **✓ Save** on the right, always present.

With the system in 24-hour mode, the wheels become Hour (0–23) | Minute with
no AM/PM column (decision #10).

## Behavior

- Save with Repeat Off ⇒ one-shot alarm; list row shows "Today"/"Tomorrow" as
  appropriate.
- Save with Repeat On + selected days ⇒ recurring alarm; row lists the days.
- Fields the current dialog exposes but the wireframes omit — ringtone,
  vibrate, label, snooze — are **not** shown and fall back to app defaults.
  Where (if anywhere) they resurface is **TBD — deliberately deferred**
  (decision #5); revisit with the later Settings pass.
- The alarm ringing / snooze screens are specced in
  [ring-screens.md](ring-screens.md).
