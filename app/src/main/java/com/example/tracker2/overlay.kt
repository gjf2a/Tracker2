package com.example.tracker2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.tracker2.MainActivity.Companion.TAG

interface Overlayer {
    fun draw(canvas: Canvas)
}

class RectangleOverlayer(val imageWidth: Int, val imageHeight: Int, val rectWidth: Int, val rectHeight: Int) : Overlayer {
    private val rectPaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
        }

    override fun draw(canvas: Canvas) {
        val xScale = canvas.width.toFloat() / imageWidth.toFloat()
        val yScale = canvas.height.toFloat() / imageHeight.toFloat()
        val left = ((imageWidth - rectWidth) / 2).toFloat() * xScale
        val right = left + rectWidth.toFloat() * xScale
        val top = (imageHeight - rectHeight).toFloat() * yScale
        val bottom = imageHeight.toFloat() * yScale
        canvas.drawRect(left, top, right, bottom, rectPaint)
    }
}

// From https://stackoverflow.com/a/63097279/906268
class Overlay constructor(context: Context?, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private val overlayers = ArrayList<Overlayer>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (overlayer in overlayers) {
            overlayer.draw(canvas)
        }
    }

    fun addOverlayer(overlayer: Overlayer) {
        overlayers.add(overlayer)
    }

    fun clearOverlayers() {
        overlayers.clear()
    }
}
