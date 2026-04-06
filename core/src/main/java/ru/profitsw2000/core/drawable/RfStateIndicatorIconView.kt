package ru.profitsw2000.core.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import ru.profitsw2000.core.R

class RfStateIndicatorIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr){

    private var rfStateText: String = ""
    private var rfStateIconColor: Int = Color.BLACK
    private var rfStateIconPadding: Float = 16f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val frameDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 12f
        setStroke(8, Color.BLACK)
    }

    init {
        val attributeSet = context.obtainStyledAttributes(attrs, R.styleable.RfStateIndicatorIconView)
        rfStateText = attributeSet.getString(R.styleable.RfStateIndicatorIconView_rfStateText) ?: ""
        rfStateIconColor = attributeSet.getColor(R.styleable.RfStateIndicatorIconView_rfStateIconColor, Color.BLACK)
        rfStateIconPadding = attributeSet.getDimension(R.styleable.RfStateIndicatorIconView_rfStateIconPadding, 20f)
        attributeSet.recycle()
        updateStyles()
    }

    private fun updateStyles() {
        textPaint.color = rfStateIconColor
        frameDrawable.setStroke(8, rfStateIconColor)
        invalidate()
    }

    fun setLabelColor(color: Int) {
        rfStateIconColor = color
        updateStyles()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        textPaint.textSize = MeasureSpec.getSize(heightMeasureSpec)*0.5f
        val textWidth = textPaint.measureText(rfStateText)

        val desiredWidth = (textWidth + rfStateIconPadding*2).toInt()
        val desiredHeight = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        frameDrawable.setBounds(0, 0,width, height)
        frameDrawable.draw(canvas)

        val x = width/2f
        val y = height/2f - (textPaint.descent() + textPaint.ascent())/2f
        canvas.drawText(rfStateText, x, y, textPaint)
    }

}

