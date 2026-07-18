package org.fossify.clock.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.fossify.clock.R
import org.fossify.clock.activities.AlarmActivity
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.extensions.getOpenAlarmTabIntent
import org.fossify.clock.extensions.getSnoozePendingIntent
import org.fossify.clock.extensions.getStopAlarmPendingIntent
import org.fossify.clock.models.Alarm
import org.fossify.commons.extensions.notificationManager

/**
 * Helper class to handle alarm notifications in the app.
 * This includes creating notification channels, building notifications for active alarms,
 * and posting notifications for missed or replaced alarms.
 */
class AlarmNotificationHelper(private val context: Context) {

    /**
     * Builds and returns the active alarm notification to be shown in the foreground service.
     */
    fun buildActiveAlarmNotification(alarm: Alarm): Notification {
        val channelId = ALARM_NOTIFICATION_CHANNEL_ID
        val channel = NotificationChannel(
            channelId,
            context.getString(org.fossify.commons.R.string.alarm),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setBypassDnd(true)
            setSound(null, null)
        }

        context.notificationManager.createNotificationChannel(channel)

        val contentTitle = alarm.label.ifEmpty {
            context.getString(org.fossify.commons.R.string.alarm)
        }

        val contentText = context.getFormattedTime(
            passedSeconds = alarm.timeInMinutes * 60,
            showSeconds = false,
            makeAmPmSmaller = false
        ).toString().uppercase()

        val reminderIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, alarm.id, reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = context.getStopAlarmPendingIntent(alarm)
        val snoozeIntent = context.getSnoozePendingIntent(alarm)

        // Same two labeled actions as the full-screen ring UI when shown as heads-up instead -
        // see docs/elderly-spec/ring-screens.md.
        val snoozeMinutes = context.config.snoozeTime
        val snoozeDurationText = context.resources.getQuantityString(
            org.fossify.commons.R.plurals.minutes, snoozeMinutes, snoozeMinutes
        )

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_alarm_vector)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .addAction(
                org.fossify.commons.R.drawable.ic_snooze_vector,
                context.getString(R.string.snooze_with_duration, snoozeDurationText),
                snoozeIntent
            )
            .addAction(
                org.fossify.commons.R.drawable.ic_check_vector,
                context.getString(R.string.stop_alarm),
                dismissIntent
            )
            .setDeleteIntent(dismissIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .build()
    }

    /**
     * Creates the missed alarm notification channel.
     */
    private fun createMissedAlarmNotificationChannel() {
        val channel = NotificationChannel(
            MISSED_ALARM_NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.missed_alarm),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setSound(null, null)
        }

        context.notificationManager.createNotificationChannel(channel)
    }

    /**
     * Posts a notification for a missed alarm (auto-dismissed).
     */
    fun postMissedAlarmNotification(missedAlarm: Alarm) {
        createMissedAlarmNotificationChannel()
        val replacedTime = context.getFormattedTime(
            passedSeconds = missedAlarm.timeInMinutes * 60,
            showSeconds = false,
            makeAmPmSmaller = false
        )
        val contentIntent = context.getOpenAlarmTabIntent()
        val notification = NotificationCompat.Builder(context, MISSED_ALARM_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.missed_alarm))
            .setContentText(context.getString(R.string.alarm_timed_out))
            .setContentIntent(contentIntent)
            .setSubText(replacedTime)
            .setSmallIcon(R.drawable.ic_alarm_off_vector)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setShowWhen(false)
            .setAutoCancel(true)
            .build()

        context.notificationManager.notify(
            MISSED_ALARM_NOTIFICATION_TAG,
            missedAlarm.id,
            notification
        )
    }

    /**
     * Posts a notification for a replaced alarm (when a new alarm starts while another is active).
     */
    fun postReplacedAlarmNotification(replacedAlarm: Alarm) {
        createMissedAlarmNotificationChannel()

        val replacedTime = context.getFormattedTime(
            passedSeconds = replacedAlarm.timeInMinutes * 60,
            showSeconds = false,
            makeAmPmSmaller = false
        )
        val contentIntent = context.getOpenAlarmTabIntent()
        val notification = NotificationCompat.Builder(context, MISSED_ALARM_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.missed_alarm))
            .setContentText(context.getString(R.string.replaced_by_another_alarm))
            .setContentIntent(contentIntent)
            .setSubText(replacedTime)
            .setSmallIcon(R.drawable.ic_alarm_off_vector)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setShowWhen(false)
            .setAutoCancel(true)
            .build()

        context.notificationManager.notify(
            MISSED_ALARM_NOTIFICATION_TAG,
            replacedAlarm.id,
            notification
        )
    }
}
