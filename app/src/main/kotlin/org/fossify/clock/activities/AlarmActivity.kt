package org.fossify.clock.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.view.WindowManager
import org.fossify.clock.R
import org.fossify.clock.databinding.ActivityAlarmBinding
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.clock.helpers.getPassedSeconds
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.AlarmEvent
import org.fossify.clock.views.AngledGradientDrawable
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.isOreoMr1Plus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Two huge stacked buttons, no swipe-to-dismiss gesture - see
 * docs/elderly-spec/ring-screens.md.
 */
class AlarmActivity : SimpleActivity() {
    private var alarm: Alarm? = null
    private val tickHandler = Handler(Looper.getMainLooper())

    private val binding by viewBinding(ActivityAlarmBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        showOverLockscreen()
        applyRingingGradient()

        val id = intent.getIntExtra(ALARM_ID, -1)
        alarm = dbHelper.getAlarmWithId(id)
        if (alarm == null) {
            finish()
            return
        }

        tickCurrentTime()
        binding.reminderText.text = getString(org.fossify.commons.R.string.alarm)

        setupButtons()
        EventBus.getDefault().register(this)
    }

    // linear-gradient(160deg, #6b4574 0%, #4a2c58 55%, #2a1633 100%), see
    // docs/elderly-spec/design-tokens.md.
    private fun applyRingingGradient() {
        binding.root.background = AngledGradientDrawable(
            angleDeg = 160f,
            colors = intArrayOf(
                androidx.core.content.ContextCompat.getColor(this, R.color.eb_ringing_start),
                androidx.core.content.ContextCompat.getColor(this, R.color.eb_ringing_mid),
                androidx.core.content.ContextCompat.getColor(this, R.color.eb_ringing_end),
            ),
            positions = floatArrayOf(0f, 0.55f, 1f)
        )
    }

    private fun setupButtons() {
        val snoozeMinutes = config.snoozeTime
        val snoozeDurationText = resources.getQuantityString(
            org.fossify.commons.R.plurals.minutes, snoozeMinutes, snoozeMinutes
        )
        binding.reminderSnooze.text = getString(R.string.snooze_with_duration, snoozeDurationText)
        binding.reminderSnooze.setOnClickListener {
            dismissAlarmAndFinish(snoozeMinutes)
        }

        binding.reminderDismiss.setOnClickListener {
            dismissAlarmAndFinish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            AlarmClock.ACTION_DISMISS_ALARM -> dismissAlarmAndFinish()
            AlarmClock.ACTION_SNOOZE_ALARM -> {
                val durationMinutes = intent.getIntExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, -1)
                if (durationMinutes == -1) {
                    dismissAlarmAndFinish(config.snoozeTime)
                } else {
                    dismissAlarmAndFinish(durationMinutes)
                }
            }

            else -> {
                // no-op. user probably clicked the notification
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickHandler.removeCallbacksAndMessages(null)
        EventBus.getDefault().unregister(this)
    }

    private fun tickCurrentTime() {
        binding.reminderCurrentTime.text = getFormattedTime(
            passedSeconds = getPassedSeconds(),
            showSeconds = false,
            makeAmPmSmaller = false
        ).toString().uppercase()
        tickHandler.postDelayed({ tickCurrentTime() }, 1000L)
    }

    private fun dismissAlarmAndFinish(snoozeMinutes: Int = -1) {
        if (alarm != null) {
            if (snoozeMinutes != -1) {
                alarmController.snoozeAlarm(alarm!!.id, snoozeMinutes)
            } else {
                alarmController.stopAlarm(alarm!!.id)
            }
        }

        finishActivity()
    }

    private fun showOverLockscreen() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        if (isOreoMr1Plus()) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAlarmStoppedEvent(event: AlarmEvent.Stopped) {
        if (event.alarmId == alarm?.id && !isFinishing) {
            finishActivity()
        }
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
