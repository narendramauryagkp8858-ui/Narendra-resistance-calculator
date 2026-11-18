package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.BandCount
import com.example.model.ResistorColor
import kotlin.math.abs

class ResistorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bandCount: BandCount = BandCount.FOUR
    private var bandColors: List<ResistorColor> = BandCount.FOUR.defaultBands
    private var activeBandIndex: Int = 0
    private var onBandSelectedListener: ((Int) -> Unit)? = null

    private val bandHitRegions = mutableListOf<RectF>()
    private val bodyPath = Path()
    private val bandRect = RectF()
    private val pointerPath = Path()

    private val leadPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bandBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val activeIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val primaryColor: Int by lazy {
        val typedValue = android.util.TypedValue()
        if (context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            ContextCompat.getColor(context, R.color.md_theme_primary)
        }
    }

    init {
        isClickable = true
        isFocusable = true
    }

    fun setOnBandSelectedListener(listener: (Int) -> Unit) {
        onBandSelectedListener = listener
    }

    fun updateState(
        bandCount: BandCount,
        bands: List<ResistorColor>,
        activeIndex: Int
    ) {
        this.bandCount = bandCount
        this.bandColors = bands.take(bandCount.count)
        this.activeBandIndex = activeIndex.coerceIn(0, bandCount.count - 1)
        val bandSummary = bandColors.joinToString(", ") { context.getString(it.nameRes) }
        contentDescription = "${bandCount.count}-band resistor: $bandSummary"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val padH = 14f * density
        val centerY = h * 0.46f

        val leadHeight = 9f * density
        val bodyLeft = w * 0.14f
        val bodyRight = w * 0.86f
        val bodyWidth = bodyRight - bodyLeft
        val bodyHalfH = (h * 0.30f).coerceAtMost(38f * density)
        val waistHalfH = bodyHalfH * 0.78f

        // 1. Draw metallic axial leads
        leadPaint.shader = LinearGradient(
            0f, centerY - leadHeight / 2f,
            0f, centerY + leadHeight / 2f,
            intArrayOf(0xFF64748B.toInt(), 0xFFE2E8F0.toInt(), 0xFF94A3B8.toInt(), 0xFF475569.toInt()),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(
            padH,
            centerY - leadHeight / 2f,
            w - padH,
            centerY + leadHeight / 2f,
            leadHeight / 2f,
            leadHeight / 2f,
            leadPaint
        )

        // 2. Build dog-bone axial resistor body path
        val capWidth = bodyWidth * 0.16f
        val capCorner = 12f * density
        bodyPath.reset()
        bodyPath.moveTo(bodyLeft + capCorner, centerY - bodyHalfH)
        bodyPath.lineTo(bodyLeft + capWidth, centerY - bodyHalfH)
        bodyPath.cubicTo(
            bodyLeft + capWidth + 10f * density, centerY - bodyHalfH,
            bodyLeft + capWidth + 12f * density, centerY - waistHalfH,
            bodyLeft + capWidth + 24f * density, centerY - waistHalfH
        )
        bodyPath.lineTo(bodyRight - capWidth - 24f * density, centerY - waistHalfH)
        bodyPath.cubicTo(
            bodyRight - capWidth - 12f * density, centerY - waistHalfH,
            bodyRight - capWidth - 10f * density, centerY - bodyHalfH,
            bodyRight - capWidth, centerY - bodyHalfH
        )
        bodyPath.lineTo(bodyRight - capCorner, centerY - bodyHalfH)
        bodyPath.quadTo(bodyRight, centerY - bodyHalfH, bodyRight, centerY - bodyHalfH + capCorner)
        bodyPath.lineTo(bodyRight, centerY + bodyHalfH - capCorner)
        bodyPath.quadTo(bodyRight, centerY + bodyHalfH, bodyRight - capCorner, centerY + bodyHalfH)
        bodyPath.lineTo(bodyRight - capWidth, centerY + bodyHalfH)
        bodyPath.cubicTo(
            bodyRight - capWidth - 10f * density, centerY + bodyHalfH,
            bodyRight - capWidth - 12f * density, centerY + waistHalfH,
            bodyRight - capWidth - 24f * density, centerY + waistHalfH
        )
        bodyPath.lineTo(bodyLeft + capWidth + 24f * density, centerY + waistHalfH)
        bodyPath.cubicTo(
            bodyLeft + capWidth + 12f * density, centerY + waistHalfH,
            bodyLeft + capWidth + 10f * density, centerY + bodyHalfH,
            bodyLeft + capWidth, centerY + bodyHalfH
        )
        bodyPath.lineTo(bodyLeft + capCorner, centerY + bodyHalfH)
        bodyPath.quadTo(bodyLeft, centerY + bodyHalfH, bodyLeft, centerY + bodyHalfH - capCorner)
        bodyPath.lineTo(bodyLeft, centerY - bodyHalfH + capCorner)
        bodyPath.quadTo(bodyLeft, centerY - bodyHalfH, bodyLeft + capCorner, centerY - bodyHalfH)
        bodyPath.close()

        // 3. Fill resistor body (tan ceramic for 3/4-band, precision blue for 5/6-band)
        val isPrecision = bandCount == BandCount.FIVE || bandCount == BandCount.SIX
        val topTint = if (isPrecision) 0xFF93C5FD.toInt() else 0xFFF5E6CA.toInt()
        val midTint = if (isPrecision) 0xFF60A5FA.toInt() else 0xFFE6C896.toInt()
        val botTint = if (isPrecision) 0xFF2563EB.toInt() else 0xFFC8A165.toInt()
        val outlineTint = if (isPrecision) 0xFF1D4ED8.toInt() else 0xFF9A7B4F.toInt()

        bodyPaint.shader = LinearGradient(
            0f, centerY - bodyHalfH,
            0f, centerY + bodyHalfH,
            intArrayOf(topTint, midTint, botTint),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(bodyPath, bodyPaint)

        // 4. Clip to body path to draw color bands + top specular sheen
        val saveCount = canvas.save()
        canvas.clipPath(bodyPath)

        val positions = computeBandFractions(bandCount)
        val bandWidth = (bodyWidth * if (bandCount.count >= 5) 0.056f else 0.068f)
            .coerceAtLeast(10f * density)

        bandHitRegions.clear()
        bandColors.forEachIndexed { index, resistorColor ->
            val cx = bodyLeft + bodyWidth * positions[index]
            val left = cx - bandWidth / 2f
            val right = cx + bandWidth / 2f
            bandRect.set(left, centerY - bodyHalfH - 2f, right, centerY + bodyHalfH + 2f)

            configureBandShader(resistorColor, bandRect)
            canvas.drawRect(bandRect, bandPaint)

            if (resistorColor == ResistorColor.WHITE || resistorColor == ResistorColor.BLACK) {
                bandBorderPaint.color = if (resistorColor == ResistorColor.WHITE) {
                    0x440F172A
                } else {
                    0x33FFFFFF
                }
                bandBorderPaint.strokeWidth = 1f * density
                canvas.drawLine(left, centerY - bodyHalfH, left, centerY + bodyHalfH, bandBorderPaint)
                canvas.drawLine(right, centerY - bodyHalfH, right, centerY + bodyHalfH, bandBorderPaint)
            }

            // Expand touch region comfortably for 48dp+ accessibility
            val touchHalfW = (bandWidth * 1.4f).coerceAtLeast(22f * density)
            bandHitRegions.add(
                RectF(
                    cx - touchHalfW,
                    0f,
                    cx + touchHalfW,
                    h
                )
            )
        }

        // Specular sheen across upper cylinder
        highlightPaint.shader = LinearGradient(
            0f, centerY - bodyHalfH,
            0f, centerY - bodyHalfH * 0.25f,
            0x44FFFFFF,
            0x00FFFFFF,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(bodyLeft, centerY - bodyHalfH, bodyRight, centerY - bodyHalfH * 0.25f, highlightPaint)

        canvas.restoreToCount(saveCount)

        // 5. Outer contour stroke on body
        bodyStrokePaint.color = outlineTint
        bodyStrokePaint.strokeWidth = 1.5f * density
        canvas.drawPath(bodyPath, bodyStrokePaint)

        // 6. Active band highlight ring & pointer arrow below resistor
        if (activeBandIndex in positions.indices) {
            val activeCx = bodyLeft + bodyWidth * positions[activeBandIndex]
            activeIndicatorPaint.color = primaryColor
            activeIndicatorPaint.style = Paint.Style.STROKE
            activeIndicatorPaint.strokeWidth = 2.4f * density

            val ringHalfW = bandWidth / 2f + 3f * density
            val ringTop = centerY - bodyHalfH - 4f * density
            val ringBottom = centerY + bodyHalfH + 4f * density
            canvas.drawRoundRect(
                activeCx - ringHalfW,
                ringTop,
                activeCx + ringHalfW,
                ringBottom,
                5f * density,
                5f * density,
                activeIndicatorPaint
            )

            // Crisp pointer triangle below the active band
            activeIndicatorPaint.style = Paint.Style.FILL
            val tipY = ringBottom + 4f * density
            val baseY = tipY + 8f * density
            val halfBase = 6f * density
            pointerPath.reset()
            pointerPath.moveTo(activeCx, tipY)
            pointerPath.lineTo(activeCx - halfBase, baseY)
            pointerPath.lineTo(activeCx + halfBase, baseY)
            pointerPath.close()
            canvas.drawPath(pointerPath, activeIndicatorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            var bestIndex = -1
            var bestDist = Float.MAX_VALUE
            bandHitRegions.forEachIndexed { index, rect ->
                if (rect.contains(x, event.y)) {
                    val dist = abs(x - rect.centerX())
                    if (dist < bestDist) {
                        bestDist = dist
                        bestIndex = index
                    }
                }
            }
            if (bestIndex != -1) {
                performClick()
                onBandSelectedListener?.invoke(bestIndex)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun configureBandShader(color: ResistorColor, rect: RectF) {
        when (color) {
            ResistorColor.GOLD -> {
                bandPaint.shader = LinearGradient(
                    0f, rect.top, 0f, rect.bottom,
                    intArrayOf(0xFFFDE68A.toInt(), 0xFFD4AF37.toInt(), 0xFF927012.toInt()),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            ResistorColor.SILVER -> {
                bandPaint.shader = LinearGradient(
                    0f, rect.top, 0f, rect.bottom,
                    intArrayOf(0xFFF1F5F9.toInt(), 0xFFCBD5E1.toInt(), 0xFF64748B.toInt()),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            else -> {
                bandPaint.shader = LinearGradient(
                    0f, rect.top, 0f, rect.bottom,
                    intArrayOf(
                        lightenColor(color.colorInt, 0.16f),
                        color.colorInt,
                        darkenColor(color.colorInt, 0.22f)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        }
    }

    private fun computeBandFractions(bandCount: BandCount): FloatArray = when (bandCount) {
        BandCount.THREE -> floatArrayOf(0.24f, 0.40f, 0.56f)
        BandCount.FOUR -> floatArrayOf(0.21f, 0.36f, 0.51f, 0.78f)
        BandCount.FIVE -> floatArrayOf(0.18f, 0.30f, 0.42f, 0.54f, 0.79f)
        BandCount.SIX -> floatArrayOf(0.16f, 0.27f, 0.38f, 0.49f, 0.69f, 0.86f)
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - factor)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
