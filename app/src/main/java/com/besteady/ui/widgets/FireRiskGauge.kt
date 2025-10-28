package com.besteady.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class FireRiskGauge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val greenColor = 0xFF4CAF50.toInt()
    private val yellowColor = 0xFFFFC107.toInt()
    private val redColor = 0xFFFF4444.toInt()
    private val trackColor = 0x22000000
    private val needleColor = 0xFF000000.toInt()

    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = trackColor
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = needleColor
    }

    private var strokeWidthPx: Float = dp(14f)
    private var needleLengthRatio: Float = 0.8f

    private var animator: ValueAnimator? = null

    var currentRiskAngle: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 180f)
            invalidate()
        }

    fun animateToAngle(targetAngle: Float, durationMs: Long, onEnd: (() -> Unit)? = null) {
        animator?.cancel()
        val start = currentRiskAngle
        val end = targetAngle.coerceIn(0f, 180f)
        animator = ValueAnimator.ofFloat(start, end).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                currentRiskAngle = value
            }
            doOnEnd { onEnd?.invoke() }
            start()
        }
    }

    fun startFireProgress(durationMs: Long = 25_000L) {
        animateToAngle(180f, durationMs)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = dp(280f).toInt()
        val desiredHeight = (desiredWidth / 2f + dp(24f)).toInt()

        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val widthF = width.toFloat()
        val heightF = height.toFloat()

        val radius = min(widthF, heightF * 2f) / 2f - strokeWidthPx
        val centerX = widthF / 2f
        val centerY = heightF - dp(8f)

        // Track
        trackPaint.strokeWidth = strokeWidthPx
        canvas.drawArc(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            180f,
            180f,
            false,
            trackPaint
        )

        // Zones: Green 0-60, Yellow 60-120, Red 120-180 (angles are from 180 baseline)
        zonePaint.strokeWidth = strokeWidthPx
        zonePaint.color = greenColor
        canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 180f, 60f, false, zonePaint)
        zonePaint.color = yellowColor
        canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 240f, 60f, false, zonePaint)
        zonePaint.color = redColor
        canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 300f, 60f, false, zonePaint)

        // Needle
        val needleAngleRad = Math.toRadians((180f + currentRiskAngle).toDouble())
        val needleLength = radius * needleLengthRatio

        val tipX = centerX + (needleLength * cos(needleAngleRad)).toFloat()
        val tipY = centerY + (needleLength * sin(needleAngleRad)).toFloat()

        val baseWidth = dp(10f)
        val baseRadius = dp(6f)

        val leftAngle = Math.toRadians((180f + currentRiskAngle - 90f).toDouble())
        val rightAngle = Math.toRadians((180f + currentRiskAngle + 90f).toDouble())
        val baseLeftX = centerX + (baseWidth * 0.5f * cos(leftAngle)).toFloat()
        val baseLeftY = centerY + (baseWidth * 0.5f * sin(leftAngle)).toFloat()
        val baseRightX = centerX + (baseWidth * 0.5f * cos(rightAngle)).toFloat()
        val baseRightY = centerY + (baseWidth * 0.5f * sin(rightAngle)).toFloat()

        val needlePath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseLeftX, baseLeftY)
            lineTo(baseRightX, baseRightY)
            close()
        }
        canvas.drawPath(needlePath, needlePaint)

        // Needle hub
        canvas.drawCircle(centerX, centerY, baseRadius, needlePaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}


