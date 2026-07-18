# Clock Tab

Wireframes: [`clock-main-a`](wireframes/clock-main-a.png),
[`clock-main-b`](wireframes/clock-main-b.png),
[`add-city`](wireframes/add-city.png).

## Main screen

Top-to-bottom:

1. **Header** — static app-section label "Clock" (this label doesn't change
   per tab anywhere in the app — see [design-principles.md](design-principles.md)).
2. **Local time block** — dominant element:
   - Time, e.g. **9:45 AM** (largest text in the app).
   - Full date below: "Monday, May 11".
   - No seconds shown (sketches show none).
3. **World clock list** — one card per added city:
   - City's current time large ("10:11 AM") with relative offset right-aligned
     ("+3hr").
   - City name + region below ("Monterey, California").
   - List scrolls if more cities than fit (scroll arrows sketched in
     clock-main-b).
   - No inline delete/edit on the row — managing cities happens via the Add
     City screen: unchecking a city there and saving removes it from the list.
4. **"+ Add City" button** — full-width pill.
5. Bottom navigation (Clock active).

## Add City screen (full screen, replaces `AddTimeZonesDialog`)

No back arrow in the header (decision #16) — leave via Cancel or the system
back gesture.

1. Header "Clock" (static, same as every screen).
2. **Search field** — plain grey rounded field, no outline/border, hint text
   "Search for a city" sitting inside as an ordinary placeholder (not a
   Material floating label on the border). Sourced from the **full IANA time
   zone list** (`TimeZone.getAvailableIDs()`, filtered to real geographic
   zones), not a small curated set — searching for any real city should find
   it.
3. **Results/selected list** — one row per city: a purple circle selector
   (outlined when unselected, filled solid purple when selected — no
   checkmark glyph), city name (plus region when the zone data actually has
   one, e.g. "Louisville, KY" — most zones don't and show just the city),
   and the offset ("+3hr") at the end. A thin low-contrast divider line
   separates each row. Tapping a row toggles its circle.
4. **Cancel / Save** buttons at the bottom. Save persists the selection set;
   Cancel discards.

## Behavior notes

- Offsets are relative to the device's local time and update with DST; the
  compact "+3hr" form is kept as drawn (decision #14) — relative to the
  device, not the zone's absolute UTC offset.
- Time format follows the system/app 12/24-hour setting (decision #10);
  wireframes show 12-hour with AM/PM.
- Because city IDs are generated dynamically from the live IANA list rather
  than a fixed curated one, world-clock selections saved before that change
  don't reliably map to the same city afterward. Not a concern pre-launch.
