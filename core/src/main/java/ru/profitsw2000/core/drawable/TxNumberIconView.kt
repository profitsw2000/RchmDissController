package ru.profitsw2000.core.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import ru.profitsw2000.core.R

class TxNumberIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    private var txNumber: String = "0"
    private var txIconColor: Int = Color.BLACK

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TxNumberIconView)
        txNumber = typedArray.getString(R.styleable.TxNumberIconView_txNumber) ?: "0"
        txIconColor = typedArray.getColor(R.styleable.TxNumberIconView_txIconColor, Color.GRAY)
        typedArray.recycle()

        setImageResource(R.drawable.tx_icon)
        setColorFilter(txIconColor)
    }

    fun setIconColor(color: Int) {
        txIconColor = color
        setColorFilter(color)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        textPaint.textSize = height*0.25f

        val x = width * 0.8f
        val y = height/2f - (textPaint.descent() + textPaint.ascent())/2f

        canvas.drawText(txNumber, x, y, textPaint)
    }
}