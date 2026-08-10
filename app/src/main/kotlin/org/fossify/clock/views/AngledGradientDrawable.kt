package org.fossify.clock.views

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A linear gradient at an arbitrary CSS-style angle (0deg = up, clockwise), since
 * [android.graphics.drawable.GradientDrawable] is limited to eight fixed orientations. Used for
 * the ring-screen background: `linear-gradient(160deg, #6b4574 0%, #4a2c58 55%, #2a1633 100%)`.
 */
class AngledGradientDrawable(
    private val angleDeg: Float,
    private val colors: IntArray,
    private val positions: FloatArray
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val ux = sin(angleRad).toFloat()
        val uy = -cos(angleRad).toFloat()
        val length = w * abs(ux) + h * abs(uy)
        val cx = w / 2f
        val cy = h / 2f
        val x0 = cx - ux * length / 2f
        val y0 = cy - uy * length / 2f
        val x1 = cx + ux * length / 2f
        val y1 = cy + uy * length / 2f
        paint.shader = LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(bounds, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.OPAQUE"))
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
