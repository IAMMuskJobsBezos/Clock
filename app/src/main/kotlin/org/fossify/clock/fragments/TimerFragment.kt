package org.fossify.clock.fragments

import android.animation.ValueAnimator
import android.graphics.Paint
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import org.fossify.clock.views.WheelPickerView
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.openNotificationSettings
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.max

/**
 * Single timer, no list - see docs/elderly-spec/timer.md and decision #7.
 * A default Idle timer row always exists (AppDatabase.insertDefaultTimer); this fragment
 * always operates on the first timer row and drops any extras it finds.
 */
class TimerFragment : Fragment() {
    private lateinit var binding: FragmentTimerBinding
    private var currentTimer: Timer? = null

    // Whether the Second wheel is currently restricted to 1-59 (Hour and Minute both at 0) -
    // see docs/elderly-spec/timer.md / decision #24.
    private var secondWheelRestricted = false

    // Guards render() while playStartTransition()'s overlay animation owns the screen, so an
    // EventBus-driven refresh can't cut it short - see decisions.md.
    private var isAnimatingStart = false
    private var startAnimator: ValueAnimator? = null
    private val startTransitionClones = mutableListOf<View>()

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

    override fun onDestroyView() {
        startAnimator?.cancel()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        setupPickers()
        loadTimer()
    }

    private fun setupPickers() {
        binding.apply {
            timerSetupHourPicker.formatter = { "%02d".format(it) }
            timerSetupMinutePicker.formatter = { "%02d".format(it) }
            timerSetupHourPicker.onValueChangeListener = { enforceMinimumDuration() }
            timerSetupMinutePicker.onValueChangeListener = { enforceMinimumDuration() }
        }
    }

    // The Second wheel's displayed value is a plain 0-based index; under the 1-59 restricted
    // mode (see configureSecondWheel) the actual seconds value is index+1.
    private fun currentSeconds(): Int {
        val raw = binding.timerSetupSecondPicker.value
        return if (secondWheelRestricted) raw + 1 else raw
    }

    private fun configureSecondWheel(seconds: Int, restricted: Boolean) {
        secondWheelRestricted = restricted
        binding.timerSetupSecondPicker.apply {
            if (restricted) {
                formatter = { (it + 1).toString() }
                configure(itemCount = 59, initialValue = (seconds - 1).coerceIn(0, 58))
            } else {
                formatter = { it.toString() }
                configure(itemCount = 60, initialValue = seconds.coerceIn(0, 59))
            }
        }
    }

    // A timer can never be set to 0:00:00 - when hour and minute are both 0, the second wheel
    // can't offer 0 either, so it never bottoms out at a no-op timer (decision #24). Reconfigures
    // the wheel (rather than just clamping) since going from a 0-59 to a 1-59 cyclic range needs
    // a different item count.
    private fun enforceMinimumDuration() {
        binding.apply {
            val atZero = timerSetupHourPicker.value == 0 && timerSetupMinutePicker.value == 0
            if (atZero != secondWheelRestricted) {
                configureSecondWheel(max(1, currentSeconds()), restricted = atZero)
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
        if (isAnimatingStart) {
            return
        }

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

            val hours = timer.seconds / 3600
            val minutes = (timer.seconds / 60) % 60
            val seconds = timer.seconds % 60
            val restricted = hours == 0 && minutes == 0
            timerSetupHourPicker.configure(itemCount = 24, initialValue = hours)
            timerSetupMinutePicker.configure(itemCount = 60, initialValue = minutes)
            configureSecondWheel(if (restricted) max(1, seconds) else seconds, restricted = restricted)

            timerStart.setOnClickListener {
                startTimer(timer)
            }
        }
    }

    private fun startTimer(timer: Timer) {
        val hours = binding.timerSetupHourPicker.value
        val minutes = binding.timerSetupMinutePicker.value
        val seconds = currentSeconds()
        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        if (totalSeconds <= 0) {
            return
        }

        withNotificationPermission {
            playStartTransition(timer, totalSeconds)
        }
    }

    // Actually writes the new duration and kicks off the real countdown - split out of
    // startTimer() so playStartTransition() can call it once the visual transition lands,
    // rather than the DB write racing the animation. See decisions.md.
    private fun commitStartTimer(timer: Timer, totalSeconds: Int) {
        val safeActivity = activity as? SimpleActivity ?: return
        safeActivity.config.timerSeconds = totalSeconds
        val updatedTimer = timer.copy(seconds = totalSeconds)
        safeActivity.timerHelper.insertOrUpdateTimer(updatedTimer) {
            EventBus.getDefault().post(
                TimerEvent.Start(timer.id!!, totalSeconds.toLong().secondsToMillis)
            )
        }
    }

    // The "start" animation: the wheels' neighboring numbers fade away, the selected H/M/S
    // values fly from their wheel position to their landing spot in the countdown display, a
    // colon fades in on either side of them moving along the same path, and the Hour/Minute/
    // Second header labels slide from the setup layout to the running layout - Emmett's spec,
    // see decisions.md. Landing spots are measured off the real (already laid out, already
    // autosized) countdown TextView's Paint, not guessed, so the handoff to the real view at the
    // end is pixel-exact.
    private fun playStartTransition(timer: Timer, totalSeconds: Int) {
        if (isAnimatingStart || !isAdded) {
            return
        }

        isAnimatingStart = true
        binding.timerStart.isEnabled = false

        val targetText = (totalSeconds.toLong() * 1000L).getFormattedDuration(forceShowHours = true)

        binding.timerRunningHolder.alpha = 1f
        binding.timerRunningHolder.beVisible()
        binding.timerCountdown.text = targetText
        binding.timerCountdown.alpha = 0f
        // Hour/Minute/Second labels sit at the exact same on-screen position in both states
        // (timer_setup_header and timer_running_header share the same column padding - see
        // fragment_timer.xml), so the running row can just show immediately under the setup
        // row's identical labels with nothing to animate - no move, no crossfade, no pop.
        binding.timerRunningLabelHour.alpha = 1f
        binding.timerRunningLabelMinute.alpha = 1f
        binding.timerRunningLabelSecond.alpha = 1f
        binding.timerRunningButtonsRow.alpha = 0f
        binding.timerSetupHolder.bringToFront()

        // Wait one layout pass so timerCountdown's autosize/measurement is real before we read
        // its Paint and position off it.
        binding.timerCountdown.post {
            if (!isAdded) {
                isAnimatingStart = false
                commitStartTimer(timer, totalSeconds)
                return@post
            }
            runStartAnimation(timer, totalSeconds, targetText)
        }
    }

    private fun runStartAnimation(timer: Timer, totalSeconds: Int, targetText: String) {
        val root = binding.root as RelativeLayout
        val countdown = binding.timerCountdown

        val idx1 = targetText.indexOf(':')
        val idx2 = if (idx1 >= 0) targetText.indexOf(':', idx1 + 1) else -1
        if (idx1 < 0 || idx2 < 0) {
            // Format wasn't "H:MM:SS" shaped for some reason - skip the per-glyph choreography
            // and fall back to a plain crossfade rather than crashing on bad substring ranges.
            finishStartTransition(timer, totalSeconds)
            return
        }

        val hourStr = targetText.substring(0, idx1)
        val minuteStr = targetText.substring(idx1 + 1, idx2)
        val secondStr = targetText.substring(idx2 + 1)

        val measurePaint = Paint(countdown.paint)
        val targetSizePx = countdown.textSize
        val wHour = measurePaint.measureText(hourStr)
        val wColon = measurePaint.measureText(":")
        val wMinute = measurePaint.measureText(minuteStr)
        val wSecond = measurePaint.measureText(secondStr)

        val countdownLeft = countdown.xRelativeTo(root) +
            (countdown.width - (wHour + wColon + wMinute + wColon + wSecond)) / 2f
        val countdownCenterY = countdown.yRelativeTo(root) + countdown.height / 2f

        fun segCenterX(before: Float, w: Float) = countdownLeft + before + w / 2f
        val hourTargetX = segCenterX(0f, wHour)
        val colon1TargetX = segCenterX(wHour, wColon)
        val minuteTargetX = segCenterX(wHour + wColon, wMinute)
        val colon2TargetX = segCenterX(wHour + wColon + wMinute, wColon)
        val secondTargetX = segCenterX(wHour + wColon + wMinute + wColon, wSecond)

        val density = resources.displayMetrics.density
        val fontScale = resources.configuration.fontScale
        // Matches WheelPickerView's own center-cell font size (FONT_MIN_DP + FONT_MAX_ADD_DP).
        val startSizePx = (28f + 14f) * density * fontScale

        fun wheelCenter(wheel: WheelPickerView): Pair<Float, Float> {
            return (wheel.xRelativeTo(root) + wheel.width / 2f) to (wheel.yRelativeTo(root) + wheel.height / 2f)
        }

        val (hourStartX, hourStartY) = wheelCenter(binding.timerSetupHourPicker)
        val (minuteStartX, minuteStartY) = wheelCenter(binding.timerSetupMinutePicker)
        val (secondStartX, secondStartY) = wheelCenter(binding.timerSetupSecondPicker)

        val textColor = ContextCompat.getColor(requireContext(), R.color.eb_display_ink)
        val typeface = countdown.typeface

        fun makeClone(text: String, startX: Float, startY: Float): TextView {
            return TextView(requireContext()).apply {
                this.text = text
                this.typeface = typeface
                setTextColor(textColor)
                includeFontPadding = false
                setTextSize(TypedValue.COMPLEX_UNIT_PX, startSizePx)
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                root.addView(this)
                x = startX
                y = startY
                startTransitionClones.add(this)
            }
        }

        val hourWheelText = binding.timerSetupHourPicker.let { it.formatter(it.value) }
        val minuteWheelText = binding.timerSetupMinutePicker.let { it.formatter(it.value) }
        val secondWheelText = binding.timerSetupSecondPicker.let { it.formatter(it.value) }

        val hourClone = makeClone(hourWheelText, hourStartX, hourStartY)
        val minuteClone = makeClone(minuteWheelText, minuteStartX, minuteStartY)
        val secondClone = makeClone(secondWheelText, secondStartX, secondStartY)
        val colon1Clone = makeClone(":", (hourStartX + minuteStartX) / 2f, (hourStartY + minuteStartY) / 2f)
        val colon2Clone = makeClone(":", (minuteStartX + secondStartX) / 2f, (minuteStartY + secondStartY) / 2f)
        colon1Clone.alpha = 0f
        colon2Clone.alpha = 0f

        binding.timerStart.alpha = 0f

        // Positions the pivot of a clone at (cx, cy), matching the same ascent/descent-centered
        // convention WheelPickerView.onDraw uses for its own center cell, so the handoff at
        // t=1 lines up exactly with the real countdown TextView's glyph placement.
        fun place(clone: TextView, cx: Float, cy: Float, sizePx: Float) {
            measurePaint.textSize = sizePx
            val w = measurePaint.measureText(clone.text.toString())
            val ascent = measurePaint.ascent()
            val descent = measurePaint.descent()
            clone.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
            clone.x = cx - w / 2f
            clone.y = cy + (ascent - descent) / 2f
        }

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 380L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val t = va.animatedValue as Float
                val sizePx = startSizePx + (targetSizePx - startSizePx) * t

                place(hourClone, hourStartX + (hourTargetX - hourStartX) * t, hourStartY + (countdownCenterY - hourStartY) * t, sizePx)
                place(minuteClone, minuteStartX + (minuteTargetX - minuteStartX) * t, minuteStartY + (countdownCenterY - minuteStartY) * t, sizePx)
                place(secondClone, secondStartX + (secondTargetX - secondStartX) * t, secondStartY + (countdownCenterY - secondStartY) * t, sizePx)

                val colon1Cy = (hourStartY + minuteStartY) / 2f + (countdownCenterY - (hourStartY + minuteStartY) / 2f) * t
                val colon2Cy = (minuteStartY + secondStartY) / 2f + (countdownCenterY - (minuteStartY + secondStartY) / 2f) * t
                place(colon1Clone, colon1TargetX, colon1Cy, sizePx)
                place(colon2Clone, colon2TargetX, colon2Cy, sizePx)
                val colonAlpha = ((t - 0.3f) / 0.7f).coerceIn(0f, 1f)
                colon1Clone.alpha = colonAlpha
                colon2Clone.alpha = colonAlpha

                // Neighboring wheel numbers fade away fast, revealing just the clone that's
                // already sitting over the selected value.
                val wheelAlpha = (1f - t / 0.4f).coerceIn(0f, 1f)
                binding.timerWheelRow.alpha = wheelAlpha

                val buttonsFadeIn = ((t - 0.5f) / 0.5f).coerceIn(0f, 1f)
                binding.timerRunningButtonsRow.alpha = buttonsFadeIn
            }
            doOnEndCompat { finishStartTransition(timer, totalSeconds) }
        }
        startAnimator = animator
        animator.start()
    }

    private fun finishStartTransition(timer: Timer, totalSeconds: Int) {
        startTransitionClones.forEach { (binding.root as RelativeLayout).removeView(it) }
        startTransitionClones.clear()
        startAnimator = null

        binding.timerCountdown.alpha = 1f
        binding.timerRunningLabelHour.alpha = 1f
        binding.timerRunningLabelMinute.alpha = 1f
        binding.timerRunningLabelSecond.alpha = 1f
        binding.timerRunningButtonsRow.alpha = 1f
        binding.timerWheelRow.alpha = 1f
        binding.timerStart.alpha = 1f
        binding.timerStart.isEnabled = true
        binding.timerSetupHolder.beGone()

        isAnimatingStart = false
        commitStartTimer(timer, totalSeconds)
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

/** Position of [this] relative to [ancestor], walking up the parent chain summing view.x. */
private fun View.xRelativeTo(ancestor: View): Float {
    var x = 0f
    var v: View? = this
    while (v != null && v !== ancestor) {
        x += v.x
        v = v.parent as? View
    }
    return x
}

/** Position of [this] relative to [ancestor], walking up the parent chain summing view.y. */
private fun View.yRelativeTo(ancestor: View): Float {
    var y = 0f
    var v: View? = this
    while (v != null && v !== ancestor) {
        y += v.y
        v = v.parent as? View
    }
    return y
}

private fun ValueAnimator.doOnEndCompat(action: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) {
            removeListener(this)
            action()
        }
    })
}
