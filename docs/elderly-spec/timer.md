# Timer Tab

Wireframe: [`timer-stopwatch-flows`](wireframes/timer-stopwatch-flows.jpeg)
(top row: setup → running → paused).

## States

### 1. Setup (no timer running)

1. Header "Timer".
2. **Three scroll wheels**: Hour | Minute | Second with column headers,
   selected value centered and largest (sketch shows 4:05:12 selected).
3. **"▶ Start Timer"** full-width pill button.
4. Bottom navigation (Timer active).

### 2. Running

1. Large countdown display **H:MM:SS** ("3:11:06") with Hour/Min/Sec column
   labels above.
2. Two side-by-side buttons: **"↺ Reset"** and **"⏸ Stop"**
   (sketch annotates Stop as *pause* — it pauses, it does not discard).

### 3. Paused

Same as running, but the right button becomes **"▶ Start"** (resume).
Reset returns to the setup state with the previously chosen duration.

## Behavior

- **Single timer at a time** (decision #7). Replaces the current multi-timer
  list (`item_timer`, `TimerFragment`); existing saved timers are dropped on
  upgrade.
- Countdown continues in the background/killed app exactly as today
  (foreground `TimerService`).
- When the timer fires, the "time's up" surface is specced in
  [ring-screens.md](ring-screens.md).
- Reset while running: stops and returns to setup, one tap, no confirmation
  (decision #12).
- Wheels follow the 12/24-hour rule in
  [design-principles](design-principles.md) (hour wheel is 0–23 either way
  for durations — Hour/Min/Sec, no AM/PM).
