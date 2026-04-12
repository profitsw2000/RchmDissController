package ru.profitsw2000.core.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import ru.profitsw2000.core.R

class RfChannelNumberIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    private var rfChannelNumber: String = "0"
    private var rfChannelIconColor: Int = Color.BLACK
    private var crossColor: Int = Color.RED // Цвет крестика по умолчанию
    private var showCross: Boolean = false   // Флаг отображения

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = rfChannelIconColor
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RfChannelNumberIconView)
        rfChannelNumber = typedArray.getString(R.styleable.RfChannelNumberIconView_rfChannelNumber) ?: "0"
        rfChannelIconColor = typedArray.getColor(R.styleable.RfChannelNumberIconView_rfIconColor, Color.GRAY)
        crossColor = typedArray.getColor(R.styleable.RfChannelNumberIconView_rfCrossColor, Color.RED)
        showCross = typedArray.getBoolean(R.styleable.RfChannelNumberIconView_rfCrossColor, false)

        val iconRes = typedArray.getResourceId(
            R.styleable.RfChannelNumberIconView_rfChannelIcon,
            R.drawable.tx_icon
        )
        typedArray.recycle()

        setImageResource(iconRes)
        setColorFilter(rfChannelIconColor)
    }

    fun setIconColor(color: Int) {
        rfChannelIconColor = color
        setColorFilter(color)
        invalidate()
    }
    
    fun setRfChannelNumber(number: String) {
        rfChannelNumber = number
        invalidate()
    }

    fun setCrossVisible(isVisible: Boolean) {
        showCross = isVisible
        invalidate()
    }

    fun setCrossColor(color: Int) {
        crossColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        textPaint.textSize = height*0.5f
        textPaint.color = rfChannelIconColor

        val x = width * 0.7f
        val y = height/2f - (textPaint.descent() + textPaint.ascent())/2f

        canvas.drawText(rfChannelNumber, x, y, textPaint)

        if (showCross) {
            drawCross(canvas)
        }
    }

    private fun drawCross(canvas: Canvas) {
        crossPaint.color = crossColor
        crossPaint.strokeWidth = height * 0.075f // Толщина линий крестика

        val size = height * 0.075f // Размер крестика
        val centerX = width * 0.45f // Смещение влево (противоположно цифре)
        val centerY = height * 0.5f

        // Рисуем две линии крестика
        canvas.drawLine(centerX - size, centerY - size, centerX + size, centerY + size, crossPaint)
        canvas.drawLine(centerX + size, centerY - size, centerX - size, centerY + size, crossPaint)
    }
}