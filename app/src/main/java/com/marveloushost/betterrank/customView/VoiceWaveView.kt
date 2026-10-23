package com.marveloushost.betterrank.customView

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class VoiceWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val barHeights = mutableListOf<Float>()
    private var barWidth = 10f
    private var barSpacing = 15f
    private var maxHeight = 0f
    private var maxBars = 0
    private var minBarHeight = 4f
    private var cornerRadius = 20f

    private var valueAnimator: ValueAnimator? = null

    fun reset() {
        barHeights.clear()
        invalidate()
    }

    fun updateRms(rms: Float) {

        val normalizedRms = rms.coerceIn(0f, 10f) / 10f
        val newBarHeight = (normalizedRms * maxHeight).coerceAtLeast(minBarHeight)


        if (maxBars > 0 && barHeights.size >= maxBars) {
            barHeights.removeAt(0)
        }
        barHeights.add(newBarHeight)


        valueAnimator?.cancel()
        val startValues = barHeights.toList().toFloatArray()
        val endValues = barHeights.toFloatArray()
        valueAnimator = ValueAnimator.ofObject(FloatArrayEvaluator(), startValues, endValues)
        valueAnimator?.duration = 2000
        valueAnimator?.addUpdateListener { animation ->
            val animatedValues = animation.animatedValue as FloatArray

            for (i in animatedValues.indices) {
                if (i < barHeights.size) {
                    barHeights[i] = animatedValues[i]
                }
            }
            invalidate()
        }
        valueAnimator?.start()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        maxHeight = h * 0.8f
        maxBars = (w / (barWidth + barSpacing)).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var startX = width.toFloat()
        for (barHeight in barHeights.reversed()) {
            canvas.drawRoundRect(
                startX - barWidth,
                height / 2f - barHeight / 2f,
                startX,
                height / 2f + barHeight / 2f,
                cornerRadius,
                cornerRadius,
                paint
            )
            startX -= (barWidth + barSpacing)
        }
    }

    fun setBarColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setBarStyle(width: Float, spacing: Float) {
        barWidth = width
        barSpacing = spacing
        invalidate()
    }

    fun setMinBarHeight(minHeight: Float) {
        minBarHeight = minHeight
        invalidate()
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }

    class FloatArrayEvaluator : TypeEvaluator<FloatArray> {
        override fun evaluate(fraction: Float, startValue: FloatArray, endValue: FloatArray): FloatArray {
            val result = FloatArray(startValue.size)
            for (i in startValue.indices) {
                result[i] = startValue[i] + fraction * (endValue[i] - startValue[i])
            }
            return result
        }
    }
}

