package org.fossify.clock.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.FragmentTimerBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.createNewTimer
import org.fossify.clock.extensions.getFormattedDuration
import org.fossify.clock.extensions.hideTimerNotification
import org.fossify.clock.extensions.secondsToMillis
import org.fossify.clock.extensions.timerHelper
import org.fossify.clock.models.Timer
import org.fossify.clock.models.TimerEvent
import org.fossify.clock.models.TimerState
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.extensions.updateTextColors
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Single timer, no list - see docs/elderly-spec/timer.md and decision #7.
 * A default Idle timer row always exists (AppDatabase.insertDefaultTimer); this fragment
 * always operates on the first timer row and drops any extras it finds.
 */
class TimerFragment : Fragment() {
    private lateinit var binding: FragmentTimerBinding
    private var currentTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireContext().updateTextColors(binding.root)
        setupPickerColors()
        loadTimer()
    }

    private fun setupPickerColors() {
        val textColor = requireContext().getProperTextColor()
        binding.apply {
            arrayOf(timerSetupHourPicker, timerSetupMinutePicker, timerSetupSecondPicker).forEach {
                it.textColor = textColor
                it.selectedTextColor = textColor
                it.dividerColor = textColor
            }

            timerSetupHourPicker.setOnValueChangedListener { _, _, _ -> enforceMinimumDuration() }
            timerSetupMinutePicker.setOnValueChangedListener { _, _, _ -> enforceMinimumDuration() }
        }
    }

    // A timer can never be set to 0:00:00 - when hour and minute are both 0, the second wheel
    // can't offer 0 either, so it never bottoms out at a no-op timer.
    private fun enforceMinimumDuration() {
        binding.apply {
            val atZero = timerSetupHourPicker.value == 0 && timerSetupMinutePicker.value == 0
            timerSetupSecondPicker.minValue = if (atZero) 1 else 0
            if (atZero && timerSetupSecondPicker.value == 0) {
                timerSetupSecondPicker.value = 1
            }
        }
    }

    private fun loadTimer() {
        val safeActivity = activity as? SimpleActivity ?: return
        safeActivity.timerHelper.getTimers { timers ->
            safeActivity.runOnUiThread {
                if (activity == null) {
                    return@runOnUiThread
                }

                if (timers.isEmpty()) {
                    val newTimer = safeActivity.createNewTimer()
                    safeActivity.timerHelper.insertOrUpdateTimer(newTimer) { loadTimer() }
                    return@runOnUiThread
                }

                if (timers.size > 1) {
                    safeActivity.timerHelper.deleteTimers(timers.drop(1))
                }

                currentTimer = timers.first()
                render()
            }
        }
    }

    private fun render() {
        val timer = currentTimer ?: return
        when (val state = timer.state) {
            is TimerState.Idle -> renderSetup(timer)
            is TimerState.Running -> renderRunning(timer, state.tick, isPaused = false)
            is TimerState.Paused -> renderRunning(timer, state.tick, isPaused = true)
            is TimerState.Finished -> renderRunning(timer, 0L, isPaused = true, isFinished = true)
        }
    }

    private fun renderSetup(timer: Timer) {
        binding.apply {
            timerSetupHolder.beVisible()
            timerRunningHolder.beGone()

            timerSetupHourPicker.value = timer.seconds / 3600
            timerSetupMinutePicker.value = (timer.seconds / 60) % 60
            timerSetupSecondPicker.value = timer.seconds % 60
            enforceMinimumDuration()

            timerStart.setOnClickListener {
                startTimer(timer)
            }
        }
    }

    private fun startTimer(timer: Timer) {
        val hours = binding.timerSetupHourPicker.value
        val minutes = binding.timerSetupMinutePicker.value
        val seconds = binding.timerSetupSecondPicker.value
        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        if (totalSeconds <= 0) {
            return
        }

        withNotificationPermission {
            val safeActivity = activity as? SimpleActivity ?: return@withNotificationPermission
            safeActivity.config.timerSeconds = totalSeconds
            val updatedTimer = timer.copy(seconds = totalSeconds)
            safeActivity.timerHelper.insertOrUpdateTimer(updatedTimer) {
                EventBus.getDefault().post(
                    TimerEvent.Start(timer.id!!, totalSeconds.toLong().secondsToMillis)
                )
            }
        }
    }

    private fun renderRunning(timer: Timer, tick: Long, isPaused: Boolean, isFinished: Boolean = false) {
        binding.apply {
            timerSetupHolder.beGone()
            timerRunningHolder.beVisible()

            timerCountdown.text = tick.getFormattedDuration(forceShowHours = true)

            timerReset.setOnClickListener {
                EventBus.getDefault().post(TimerEvent.Reset(timer.id!!))
                requireContext().hideTimerNotification(timer.id!!)
            }

            if (isPaused) {
                timerPlayPause.text = getString(R.string.start)
                timerPlayPause.setIconResource(org.fossify.commons.R.drawable.ic_play_vector)
                timerPlayPause.setOnClickListener {
                    val resumeDuration = if (isFinished) timer.seconds.secondsToMillis else tick
                    withNotificationPermission {
                        EventBus.getDefault().post(TimerEvent.Start(timer.id!!, resumeDuration))
                    }
                }
            } else {
                timerPlayPause.text = getString(R.string.stop)
                timerPlayPause.setIconResource(org.fossify.commons.R.drawable.ic_pause_vector)
                timerPlayPause.setOnClickListener {
                    EventBus.getDefault().post(TimerEvent.Pause(timer.id!!, tick))
                }
            }
        }
    }

    private fun withNotificationPermission(action: () -> Unit) {
        val safeActivity = activity as? SimpleActivity ?: return
        safeActivity.handleNotificationPermission { granted ->
            if (granted) {
                action()
            } else {
                PermissionRequiredDialog(
                    activity = safeActivity,
                    textId = org.fossify.commons.R.string.allow_notifications_reminders,
                    positiveActionCallback = { safeActivity.openNotificationSettings() }
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(@Suppress("unused") event: TimerEvent.Refresh) {
        loadTimer()
    }
}
