package com.auroratv.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * One layer of the procedural mountain scene. Four separate views are stacked by HomeActivity:
 * SKY, FAR, MID (behind the clock) and FG (in front of the clock) - that separation is what makes
 * the foreground ridge overlap the clock digits and what lets the layers move independently.
 */
class ScenicLayer(context: Context, private val kind: Int) : View(context) {

    companion object {
        const val SKY = 0
        const val FAR = 1
        const val MID = 2
        const val FG = 3
    }

    var palette: Palette = Palettes.SUNSET
        set(value) {
            field = value
            rebuild()
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ridge = Path()
    private val trees = Path()
    private var fill: Shader? = null
    private var glow: Shader? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    private fun ridged(t: Float, seed: Float): Float {
        var sum = 0f
        var amp = 1f
        var freq = 2.2f
        var norm = 0f
        for (i in 0 until 5) {
            val phase = seed * (i + 1) * 0.37f
            val v = 1f - abs(sin((t * freq + phase) * Math.PI.toFloat()))
            sum += v * v * amp
            norm += amp
            amp *= 0.5f
            freq *= 2.1f
        }
        return sum / norm
    }

    private fun ridgeY(x: Float): Float {
        val w = width.toFloat()
        val h = height.toFloat()
        val t = x / w
        return when (kind) {
            FAR -> h * (0.50f - 0.20f * ridged(t, 1.3f))
            MID -> h * (0.57f - 0.22f * ridged(t, 4.1f))
            else -> {
                val d = (t - 0.5f) / 0.3f
                h * (0.46f - 0.12f * ridged(t, 2.7f) - 0.03f * exp(-d * d))
            }
        }
    }

    private fun rebuild() {
        if (width == 0 || height == 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val p = palette
        ridge.reset()
        trees.reset()

        if (kind == SKY) {
            fill = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(p.skyTop, p.skyMid, p.skyHorizon, p.skyHorizon),
                floatArrayOf(0f, 0.42f, 0.70f, 1f),
                Shader.TileMode.CLAMP
            )
            val sx = w * 0.72f
            val sy = h * 0.43f
            glow = RadialGradient(
                sx, sy, h * 0.5f,
                intArrayOf(
                    ColorUtils.setAlphaComponent(p.sunGlow, 210),
                    ColorUtils.setAlphaComponent(p.sunGlow, 70),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            )
            return
        }

        fill = when (kind) {
            FAR -> LinearGradient(
                0f, h * 0.28f, 0f, h * 0.62f,
                p.far, ColorUtils.blendARGB(p.far, p.skyHorizon, 0.55f), Shader.TileMode.CLAMP
            )
            MID -> LinearGradient(0f, h * 0.33f, 0f, h * 0.70f, p.midRim, p.mid, Shader.TileMode.CLAMP)
            else -> LinearGradient(0f, h * 0.34f, 0f, h, p.fg, p.fgBottom, Shader.TileMode.CLAMP)
        }

        val step = 8f
        ridge.moveTo(0f, h)
        var x = 0f
        while (x < w) {
            ridge.lineTo(x, ridgeY(x))
            x += step
        }
        ridge.lineTo(w, ridgeY(w))
        ridge.lineTo(w, h)
        ridge.close()

        if (kind == FG) {
            val u = h / 1080f
            var tx = 6f * u
            var i = 0
            while (tx < w) {
                val y0 = ridgeY(tx)
                val th = (26f + 30f * abs(sin(i * 12.9898f))) * u
                val hw = th * 0.28f
                trees.moveTo(tx - hw, y0 + 6f * u)
                trees.lineTo(tx, y0 - th)
                trees.lineTo(tx + hw, y0 + 6f * u)
                trees.close()
                tx += (16f + 10f * abs(sin(i * 4.1f))) * u
                i++
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val p = palette
        val w = width.toFloat()
        val h = height.toFloat()
        if (kind == SKY) {
            paint.shader = fill
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = glow
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
            paint.color = p.sun
            canvas.drawCircle(w * 0.72f, h * 0.43f, h * 0.032f, paint)
            if (p.stars) {
                val u = h / 1080f
                for (i in 0 until 80) {
                    val sx = ((i * 0.61803f) % 1f) * w
                    val sy = ((i * 0.75487f + 0.13f) % 1f) * h * 0.45f
                    val r = (0.8f + (i % 3) * 0.5f) * u
                    paint.color = Color.argb(120 + (i * 37) % 110, 255, 255, 255)
                    canvas.drawCircle(sx, sy, r, paint)
                }
            }
            return
        }
        paint.shader = fill
        canvas.drawPath(ridge, paint)
        if (kind == FG) {
            paint.shader = null
            paint.color = p.fg
            canvas.drawPath(trees, paint)
        }
    }
}
