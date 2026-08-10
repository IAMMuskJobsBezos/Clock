package org.fossify.clock.views

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.core.content.res.ResourcesCompat
import org.fossify.clock.R
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The conveyor picker wheel from docs/elderly-spec/design-tokens.md ("The Picker Wheel").
 * Renders five cells around a continuous float position and drives them with drag momentum,
 * exactly mirroring the handoff's constants rather than approximating with a stock NumberPicker.
 *
 * Two modes:
 * - Cyclic (default): values wrap, used for Hour/Minute/Second.
 * - Clamped (non-cyclic, [setClamped]): a two-stop wheel like AM/PM - bounded with rubber-band
 *   overshoot on drag, momentum zeroes at a bound, out-of-range cells aren't drawn.
 */
class WheelPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // Row geometry widened from the handoff's 58dp (Emmett's explicit call, see
        // docs/elderly-spec/decisions.md) - a bigger drag surface per value, easier for fat
        // fingers to hit. NEAR/OPACITY divisors keep the same ratio to ITEM_H_DP as before so
        // the falloff curve (which cells look "selected" vs. faded) is unchanged in shape, just
        // scaled up with the taller rows.
        private const val ITEM_H_DP = 78f
        private const val NEAR_DIVISOR_DP = 78f
        private const val OPACITY_DIVISOR_DP = 160f
        private const val FONT_MIN_DP = 28f
        private const val FONT_MAX_ADD_DP = 14f
        private const val VEL_SMOOTH_OLD = 0.65f
        private const val VEL_SMOOTH_NEW = 0.35f
        // Release momentum: OverScroller's own fling model turned out to still scale distance
        // almost linearly with release speed (tried it - a 4x faster flick landed ~18x farther,
        // same "dramatic" problem), so the coast distance is capped by an explicit soft
        // saturation (tanh) of our own instead: a light release still coasts proportionally
        // less, but past a moderate speed, harder/faster releases stop making much difference -
        // MAX_COAST_ROWS is the steady ceiling every real flick approaches, SATURATION_SPEED_PXPS
        // is how much release speed it takes to get most of the way there. OverScroller is still
        // used for the animation *to* that computed target - its startScroll() ease-out curve is
        // what gives the actual glide its smooth, standard feel. Per Emmett's explicit call
        // ("regardless of the velocity... has to dramatically decrease... smoother and
        // steadier"), see decisions.md.
        private const val MAX_COAST_ROWS = 4.5f
        private const val SATURATION_SPEED_PXPS = 1400f
        // Slowed down further (200/500/55 -> 320/780/85) - same formula, just less snappy
        // overall. Emmett's explicit call.
        private const val SETTLE_DURATION_MIN_MS = 320
        private const val SETTLE_DURATION_MAX_MS = 780
        private const val SETTLE_DURATION_PER_ROW_MS = 85
        // Asymmetric "detent" around the centered value, applied only while actively dragging
        // (not during release momentum, which should stay smooth) - see onTouchEvent. Within
        // STICKY_ZONE of an integer, a drag that would move *away* from it (leaving the bolded value)
        // is damped down to LEAVE_DAMPING; a drag that moves *toward* one (settling into the
        // next bolded value) gets ENTER_ASSIST instead - so leaving a value takes a deliberate
        // drag, but landing on the next one snaps in quick. Emmett's explicit call.
        private const val STICKY_ZONE = 0.22f
        private const val LEAVE_DAMPING = 0.3f
        private const val ENTER_ASSIST = 1.25f
        // Low-pass filter on the raw per-frame drag distance before it moves anything - so a
        // shaky/tremoring hand's small back-and-forth noise contributes less than a deliberate,
        // sustained drag does. Emmett's explicit call.
        // More smoothing than before (0.45/0.55 -> 0.65/0.35) - direct drag was still tracking
        // the finger too instantly/reactively. Emmett's explicit call.
        private const val DY_SMOOTH_OLD = 0.65f
        private const val DY_SMOOTH_NEW = 0.35f
        private const val RUBBER_BAND = 0.35f
    }

    var onValueChangeListener: ((Int) -> Unit)? = null
    var formatter: (Int) -> String = { it.toString() }

    private var size = 60
    private var clamped = false
    private var position = 0f
    private var committedValue = -1

    private val density = resources.displayMetrics.density
    // Accessibility: track the system font-size multiplier so the wheel's numerals scale with
    // it the same way every other elderly-scale text size does (design-principles.md).
    private val fontScale = resources.configuration.fontScale

    private val itemH get() = ITEM_H_DP * density
    private val nearDivisor get() = NEAR_DIVISOR_DP * density
    private val opacityDivisor get() = OPACITY_DIVISOR_DP * density

    private val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.eb_text)
    private val faintColor = androidx.core.content.ContextCompat.getColor(context, R.color.eb_faint)
    private val typeface: Typeface? = runCatching {
        ResourcesCompat.getFont(context, R.font.eb_roboto_medium)
    }.getOrNull()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = this@WheelPickerView.typeface
        try {
            fontFeatureSettings = "tnum"
        } catch (ignored: Exception) {
        }
    }

    // Drag/velocity tracking
    private var dragging = false
    private var lastTouchY = 0f
    private var lastTouchTimeMs = 0L
    private var vel = 0f
    private var smoothedDy = 0f

    // Momentum/settle animation - see startMomentum().
    private val scroller = OverScroller(context)
    private val frameTick = object : Runnable {
        override fun run() {
            if (stepAnimation()) {
                postOnAnimation(this)
            }
        }
    }

    fun setClamped(isClamped: Boolean) {
        clamped = isClamped
    }

    /** Configures the wheel; [initialValue] is 0-based. */
    fun configure(itemCount: Int, initialValue: Int, isClamped: Boolean = false) {
        size = itemCount
        clamped = isClamped
        position = initialValue.toFloat()
        committedValue = wrapIndex(initialValue)
        invalidate()
    }

    var value: Int
        get() = wrapIndex(position.roundToInt())
        set(v) {
            stopAnimations()
            position = v.toFloat()
            commitIfChanged()
            invalidate()
        }

    private fun wrapIndex(i: Int): Int {
        if (clamped) {
            return i.coerceIn(0, size - 1)
        }
        return ((i % size) + size) % size
    }

    private fun commitIfChanged() {
        val newValue = wrapIndex(position.roundToInt())
        if (newValue != committedValue) {
            committedValue = newValue
            onValueChangeListener?.invoke(newValue)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                stopAnimations()
                dragging = true
                lastTouchY = event.rawY
                lastTouchTimeMs = System.currentTimeMillis()
                vel = 0f
                smoothedDy = 0f
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return true
                val now = System.currentTimeMillis()
                val dt = max(1L, now - lastTouchTimeMs).toFloat()

                // Low-pass filter the raw per-frame drag first, so a shaky hand's small
                // back-and-forth jitter carries less weight than a deliberate, sustained drag -
                // everything below is driven off this smoothed value, not the raw touch delta.
                val rawDy = event.rawY - lastTouchY
                smoothedDy = smoothedDy * DY_SMOOTH_OLD + rawDy * DY_SMOOTH_NEW

                var delta = -smoothedDy / itemH
                // Asymmetric detent around the centered value: a drag that would move away from
                // it (leaving the bolded value) is damped way down; a drag that moves toward one
                // (settling into the next bolded value) is left full-speed or slightly assisted.
                // So leaving a value takes a deliberate drag, but landing on the next one snaps
                // in quick. Emmett's explicit call, see decisions.md.
                val distToNearest = position - position.roundToInt()
                if (abs(distToNearest) < STICKY_ZONE) {
                    val movingAway = (distToNearest >= 0f && delta > 0f) || (distToNearest < 0f && delta < 0f)
                    delta *= if (movingAway) LEAVE_DAMPING else ENTER_ASSIST
                }
                if (clamped) {
                    val raw = position + delta
                    if (raw < 0f) {
                        delta *= RUBBER_BAND
                    } else if (raw > size - 1) {
                        delta *= RUBBER_BAND
                    }
                }
                position += delta
                commitIfChanged()

                val instantVel = (smoothedDy / dt) * 16f // px per ~frame(16ms), matches the handoff's tick
                vel = vel * VEL_SMOOTH_OLD + instantVel * VEL_SMOOTH_NEW

                lastTouchY = event.rawY
                lastTouchTimeMs = now
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                startMomentum()
            }
        }
        return true
    }

    private fun startMomentum() {
        val startYpx = (position * itemH).roundToInt()
        // vel is smoothedDy/dt scaled to "px per ~16ms frame" (see onTouchEvent); real px/sec is
        // -vel/16ms * 1000. Position increases as the finger moves *up*, opposite sign to
        // smoothedDy/vel, hence the negation.
        val velocityYpx = -vel * 62.5f

        val coastRows = MAX_COAST_ROWS * kotlin.math.tanh(abs(velocityYpx) / SATURATION_SPEED_PXPS)
        val targetIndexRaw = position + (if (velocityYpx >= 0f) coastRows else -coastRows)
        val targetIndex = targetIndexRaw.roundToInt().let {
            if (clamped) it.coerceIn(0, size - 1) else it
        }

        val targetYpx = targetIndex * itemH
        val rows = abs(targetIndex - position)
        val duration = (SETTLE_DURATION_MIN_MS + rows * SETTLE_DURATION_PER_ROW_MS)
            .toInt()
            .coerceIn(SETTLE_DURATION_MIN_MS, SETTLE_DURATION_MAX_MS)

        // Animate straight to the exact target value with OverScroller's own smooth
        // ease-out curve - always lands exactly on a value, and a hard/fast release vs. a
        // gentler one mostly just changes which value it was already headed for, not how
        // dramatically far or fast the animation itself feels.
        scroller.forceFinished(true)
        scroller.startScroll(0, startYpx, 0, (targetYpx - startYpx).roundToInt(), duration)
        removeCallbacks(frameTick)
        postOnAnimation(frameTick)
    }

    private fun stopAnimations() {
        removeCallbacks(frameTick)
        scroller.forceFinished(true)
    }

    /** Returns true while the animation should keep ticking. */
    private fun stepAnimation(): Boolean {
        val stillScrolling = scroller.computeScrollOffset()
        position = scroller.currY / itemH
        commitIfChanged()
        invalidate()
        return stillScrolling
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        val base = floor(position).toInt()

        for (i in -2..2) {
            val idx = base + i
            if (clamped && (idx < 0 || idx > size - 1)) {
                continue
            }

            val dy = (idx - position) * itemH
            val absDy = abs(dy)
            val near = max(0f, 1f - absDy / nearDivisor)
            val opacity = max(0f, 1f - absDy / opacityDivisor)
            if (opacity <= 0f) continue

            paint.textSize = (FONT_MIN_DP + FONT_MAX_ADD_DP * near) * density * fontScale
            paint.color = if (near > 0.5f) textColor else faintColor
            paint.alpha = (opacity * 255).toInt()

            val displayIdx = wrapIndex(idx)
            val text = formatter(displayIdx)
            val textY = centerY + dy - (paint.ascent() + paint.descent()) / 2f
            canvas.drawText(text, width / 2f, textY, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimations()
    }
}
