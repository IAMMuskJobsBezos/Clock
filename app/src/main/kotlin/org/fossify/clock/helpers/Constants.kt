package org.fossify.clock.helpers

import org.fossify.clock.extensions.isBitSet
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.MyTimeZone
import org.fossify.commons.helpers.FRIDAY_BIT
import org.fossify.commons.helpers.MONDAY_BIT
import org.fossify.commons.helpers.SATURDAY_BIT
import org.fossify.commons.helpers.SUNDAY_BIT
import org.fossify.commons.helpers.THURSDAY_BIT
import org.fossify.commons.helpers.TUESDAY_BIT
import org.fossify.commons.helpers.WEDNESDAY_BIT
import org.fossify.commons.helpers.isPiePlus
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

const val REPOSITORY_NAME = "Clock"

// shared preferences
const val SELECTED_TIME_ZONES = "selected_time_zones"
const val EDITED_TIME_ZONE_TITLES = "edited_time_zone_titles"
const val TIMER_SECONDS = "timer_seconds"
const val TIMER_VIBRATE = "timer_vibrate"
const val TIMER_SOUND_URI = "timer_sound_uri"
const val TIMER_SOUND_TITLE = "timer_sound_title"
const val TIMER_CHANNEL_ID = "timer_channel_id"
const val TIMER_LABEL = "timer_label"
const val TOGGLE_STOPWATCH = "toggle_stopwatch"
const val TIMER_MAX_REMINDER_SECS = "timer_max_reminder_secs"
const val ALARM_MAX_REMINDER_SECS = "alarm_max_reminder_secs"
const val ALARM_LAST_CONFIG = "alarm_last_config"
const val TIMER_LAST_CONFIG = "timer_last_config"
const val INCREASE_VOLUME_GRADUALLY = "increase_volume_gradually"
const val ALARMS_SORT_BY = "alarms_sort_by"
const val ALARMS_CUSTOM_SORTING = "alarms_custom_sorting"
const val TIMERS_SORT_BY = "timers_sort_by"
const val TIMERS_CUSTOM_SORTING = "timers_custom_sorting"
const val STOPWATCH_LAPS_SORT_BY = "stopwatch_laps_sort_by"
const val WAS_INITIAL_WIDGET_SET_UP = "was_initial_widget_set_up"
const val DATA_EXPORT_EXTENSION = ".json"
const val LAST_DATA_EXPORT_PATH = "last_alarms_export_path"
const val MIGRATE_FIRST_DAY_OF_WEEK = "migrate_first_day_of_week"

const val TABS_COUNT = 4
const val EDITED_TIME_ZONE_SEPARATOR = ":"
const val ALARM_ID = "alarm_id"
const val NOTIFICATION_ID = "notification_id"
const val DEFAULT_ALARM_MINUTES = 480
const val DEFAULT_MAX_ALARM_REMINDER_SECS = 300
const val DEFAULT_MAX_TIMER_REMINDER_SECS = 60
const val SIMPLE_PHONE = "Simple_Phone"
const val ALARM_NOTIFICATION_CHANNEL_ID = "Alarm_Channel"
const val UPCOMING_ALARM_CHANNEL_ID = "Early Alarm Dismissal"
const val MISSED_ALARM_NOTIFICATION_CHANNEL_ID = "missed_alarm_channel"
const val MISSED_ALARM_NOTIFICATION_TAG = "missed_alarm_tag"

const val OPEN_STOPWATCH_TAB_INTENT_ID = 9993
const val PICK_AUDIO_FILE_INTENT_ID = 9994
const val OPEN_ALARMS_TAB_INTENT_ID = 9996
const val OPEN_APP_INTENT_ID = 9997
const val ALARM_NOTIFICATION_ID = 9998
const val TIMER_RUNNING_NOTIFICATION_ID = 10000
const val STOPWATCH_RUNNING_NOTIFICATION_ID = 10001
const val UPCOMING_ALARM_INTENT_ID = 10002
const val UPCOMING_ALARM_NOTIFICATION_ID = 10003

const val OPEN_TAB = "open_tab"
const val TAB_CLOCK = 1
const val TAB_ALARM = 2
const val TAB_STOPWATCH = 4
const val TAB_TIMER = 8
const val TAB_CLOCK_INDEX = 0
const val TAB_ALARM_INDEX = 1
const val TAB_STOPWATCH_INDEX = 2
const val TAB_TIMER_INDEX = 3

const val TIMER_ID = "timer_id"
const val INVALID_TIMER_ID = -1

// stopwatch sorting
const val SORT_BY_LAP = 1
const val SORT_BY_LAP_TIME = 2
const val SORT_BY_TOTAL_TIME = 4

const val STOPWATCH_LIVE_LAP_ID = Int.MAX_VALUE

// alarm and timer sorting
const val SORT_BY_CREATION_ORDER = 0
const val SORT_BY_ALARM_TIME = 1
const val SORT_BY_DATE_AND_TIME = 2
const val SORT_BY_TIMER_DURATION = 3

const val TODAY_BIT = -1
const val TOMORROW_BIT = -2

// stopwatch shortcut
const val STOPWATCH_SHORTCUT_ID = "stopwatch_shortcut_id"
const val STOPWATCH_TOGGLE_ACTION = "org.fossify.clock.TOGGLE_STOPWATCH"

// time formatting
const val FORMAT_12H = "h:mm a"
const val FORMAT_24H = "HH:mm"
const val FORMAT_12H_WITH_SECONDS = "h:mm:ss a"
const val FORMAT_24H_WITH_SECONDS = "HH:mm:ss"

private val DAY_BIT_MAP = mapOf(
    Calendar.SUNDAY to SUNDAY_BIT,
    Calendar.MONDAY to MONDAY_BIT,
    Calendar.TUESDAY to TUESDAY_BIT,
    Calendar.WEDNESDAY to WEDNESDAY_BIT,
    Calendar.THURSDAY to THURSDAY_BIT,
    Calendar.FRIDAY to FRIDAY_BIT,
    Calendar.SATURDAY to SATURDAY_BIT,
)

// Import/export
const val EXPORT_BACKUP_MIME_TYPE = "application/json"
val IMPORT_BACKUP_MIME_TYPES = buildList {
    add("application/json")
    if (!isPiePlus()) {
        // Workaround for https://github.com/FossifyOrg/Messages/issues/88
        add("application/octet-stream")
    }
}


fun getDefaultTimeZoneTitle(id: Int) = getAllTimeZones().firstOrNull { it.id == id }?.title ?: ""

fun getPassedSeconds(): Int {
    val calendar = Calendar.getInstance()
    val isDaylightSavingActive = TimeZone.getDefault().inDaylightTime(Date())
    var offset = calendar.timeZone.rawOffset
    if (isDaylightSavingActive) {
        offset += TimeZone.getDefault().dstSavings
    }
    return ((calendar.timeInMillis + offset) / 1000).toInt()
}

fun formatTime(
    showSeconds: Boolean,
    use24HourFormat: Boolean,
    hours: Int,
    minutes: Int,
    seconds: Int,
): String {
    val hoursFormat = if (use24HourFormat) "%02d" else "%01d"
    var format = "$hoursFormat:%02d"

    return if (showSeconds) {
        format += ":%02d"
        String.format(format, hours, minutes, seconds)
    } else {
        String.format(format, hours, minutes)
    }
}

fun getDayNumber(calendarDay: Int): Int = (calendarDay + 5) % 7

fun getTomorrowBit(): Int {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_WEEK, 1)
    val dayOfWeek = getDayNumber(calendar.get(Calendar.DAY_OF_WEEK))
    return 1 shl dayOfWeek
}

fun getTodayBit(): Int {
    val calendar = Calendar.getInstance()
    val dayOfWeek = getDayNumber(calendar.get(Calendar.DAY_OF_WEEK))
    return 1 shl dayOfWeek
}

fun getBitForCalendarDay(day: Int): Int {
    return DAY_BIT_MAP[day] ?: 0
}

fun getCurrentDayMinutes(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

// A handful of IANA zone IDs have a real middle segment that is a genuine US state/region
// (e.g. "America/Indiana/Indianapolis"), rather than a country subdivision we'd have to guess
// at. Abbreviate just these known cases; anything else with a 3-segment ID falls back to the
// unabbreviated region name rather than fabricating a state that isn't in the data.
private val KNOWN_US_STATE_ABBREVIATIONS = mapOf(
    "Indiana" to "IN",
    "Kentucky" to "KY",
    "North_Dakota" to "ND"
)

// Legacy/duplicate top-level links that shadow the canonical Continent/City zone for the same
// offset (e.g. "US/Pacific" duplicates "America/Los_Angeles") - excluded so the same city
// doesn't effectively appear twice under two different display names.
private val LEGACY_ZONE_PREFIXES = setOf("Etc", "SystemV", "US", "Canada", "Mexico", "Brazil", "Chile")

/**
 * Every real IANA city/region time zone Android ships with, not a curated ~90-entry subset -
 * see docs/elderly-spec/clock.md and the "ALL cities should show up when searched" request.
 * Display name is "City" for a plain Continent/City zone, or "City, Region" when the zone id
 * has a genuine middle region segment (Continent/Region/City). No state/region is fabricated
 * for zones that don't have one in the data.
 */
fun getAllTimeZones(): ArrayList<MyTimeZone> {
    val now = Date()
    return TimeZone.getAvailableIDs()
        .filter { it.contains('/') }
        .distinct()
        .mapNotNull { zoneId ->
            val segments = zoneId.split('/')
            if (segments.size < 2 || segments[0] in LEGACY_ZONE_PREFIXES) {
                return@mapNotNull null
            }

            val city = segments.last().replace('_', ' ')
            val displayName = if (segments.size >= 3) {
                val regionRaw = segments[segments.size - 2]
                val region = KNOWN_US_STATE_ABBREVIATIONS[regionRaw] ?: regionRaw.replace('_', ' ')
                "$city, $region"
            } else {
                city
            }

            val offsetMs = TimeZone.getTimeZone(zoneId).getOffset(now.time)
            Triple(displayName, zoneId, offsetMs)
        }
        // A few cities have both a plain Continent/City id and a legacy backward-compatible
        // Continent/Region/City alias for the same real place (e.g. "America/Louisville" and
        // "America/Kentucky/Louisville") - same display-name-minus-region and same offset, so
        // keep only the more specific (more path segments) one instead of listing it twice.
        .sortedByDescending { (_, zoneId, _) -> zoneId.count { it == '/' } }
        .distinctBy { (displayName, _, offsetMs) -> displayName.substringBefore(", ") to offsetMs }
        .sortedWith(compareBy({ it.third }, { it.first }))
        .mapIndexed { index, (displayName, zoneId, _) -> MyTimeZone(index + 1, displayName, zoneId) }
        .toCollection(ArrayList())
}

fun getTimeOfNextAlarm(alarm: Alarm): Calendar? {
    return getTimeOfNextAlarm(alarm.timeInMinutes, alarm.days)
}

fun getTimeOfNextAlarm(alarmTimeInMinutes: Int, days: Int): Calendar? {
    val nextAlarmTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarmTimeInMinutes / 60)
        set(Calendar.MINUTE, alarmTimeInMinutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return when (days) {
        TODAY_BIT -> nextAlarmTime // do nothing, alarm is today
        TOMORROW_BIT -> nextAlarmTime.apply { add(Calendar.DAY_OF_MONTH, 1) }
        else -> {
            val now = Calendar.getInstance()
            repeat(8) {
                val currentDay = getDayNumber(nextAlarmTime.get(Calendar.DAY_OF_WEEK))
                if (days.isBitSet(currentDay) && now < nextAlarmTime) {
                    return nextAlarmTime
                } else {
                    nextAlarmTime.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            null
        }
    }
}

fun updateNonRecurringAlarmDay(alarm: Alarm) {
    if (alarm.isRecurring()) return
    alarm.days = if (alarm.timeInMinutes > getCurrentDayMinutes()) {
        TODAY_BIT
    } else {
        TOMORROW_BIT
    }
}
