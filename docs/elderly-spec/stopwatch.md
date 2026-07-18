# Stopwatch Tab

Wireframe: [`timer-stopwatch-flows`](wireframes/timer-stopwatch-flows.jpeg)
(bottom row: idle → running → paused).

## States

### 1. Idle

1. Header "Stopwatch".
2. Large elapsed display **00:00:00** with Hour/Min/Sec column labels.
3. **"▶ Start Stopwatch"** full-width pill button.
4. Bottom navigation (Stopwatch active).

### 2. Running

1. Elapsed display counting up ("00:00:02").
2. **Lap list** below the display: "Lap 1   00:00:11" — most recent on top,
   large rows, scrolls when long.
3. Buttons: **"↺ Reset"** and **"⏱ Lap"** side by side, and a full-width
   **"⏸ Stop Stopwatch"** below (annotated *pause* — does not clear).

### 3. Paused

Elapsed display frozen ("00:00:08"); buttons become **"↺ Reset"**,
**"⏱ Lap"** — shown but **disabled/grayed** while paused (decision #11; the
sketch draws it active, treated as an artifact) — and full-width
**"▶ Start Stopwatch"** (resume).

## Behavior

- Reset clears elapsed time and laps and returns to Idle. Immediate, no
  confirmation (decision #12).
- Display precision is **whole seconds** — no centiseconds (decision #11).
- Lap sorting options are removed; laps are always **newest-first**
  (decision #11).
- Keeps running in the background as today (`StopwatchService`).
