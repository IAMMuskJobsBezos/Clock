# Ring Screens (alarm firing / timer finished)

Not wireframed — specced to the same [design principles](design-principles.md)
because these are the highest-stakes moments for this audience. Confirmed
direction: **Snooze included** (decision #8).

## Alarm ringing (`AlarmActivity`)

Full screen, shown over lockscreen, screen on:

1. Current time, app-largest type (same scale as Clock tab).
2. Alarm's schedule line beneath (e.g. "7:30 AM Alarm").
3. Two huge stacked buttons, each ≥ 96dp tall, full width:
   - **"⏰ Snooze"** (top) — snoozes for the app-default duration; button
     label includes the duration ("Snooze — 10 minutes").
   - **"✓ Stop Alarm"** (bottom) — dismisses.
4. No swipe-to-dismiss gestures, no small icon buttons. Buttons visually
   distinct (Snooze secondary style, Stop primary/filled) so a half-awake
   tap on either is never ambiguous.

Notification actions (when shown as heads-up instead of full screen) use the
same two labeled actions.

## Timer finished

Full screen or high-priority notification (matching current behavior):

1. **"Time's up"** in large type, with the original duration beneath.
2. One huge full-width button: **"✓ Stop Timer"**.
3. No snooze/repeat for timers.

## Snooze reminder (`SnoozeReminderActivity`)

Keep behavior; restyle any visible surface to the same type/target scale.
