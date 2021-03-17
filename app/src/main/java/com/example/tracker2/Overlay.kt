package com.example.tracker2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View

interface Overlayer {
    fun draw(canvas: Canvas)
}

class RectangleOverlayer(val rectFuncs: ArrayList<(Int,Int) -> Rect>) : Overlayer {
    private val rectPaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
        }

    override fun draw(canvas: Canvas) {
        for (rectFunc in rectFuncs) {
            canvas.drawRect(rectFunc(canvas.width, canvas.height), rectPaint)
        }
    }
}

// From https://stackoverflow.com/a/63097279/906268
class Overlay constructor(context: Context?, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private val overlayers = ArrayList<Overlayer>()
    private val receivers = ArrayList<MessageReceiver>()

    override fun onDraw(canvas: Canvas) {
        try {
            super.onDraw(canvas)
            for (overlayer in overlayers) {
                overlayer.draw(canvas)
            }
        } catch (e: Exception) {
            for (receiver in receivers) {
                receiver.message("Overlay Exception: ${e.message}")
            }
        }
    }

    fun addMessageReceiver(receiver: MessageReceiver) {
        receivers.add(receiver)
    }

    fun addOverlayer(overlayer: Overlayer) {
        overlayers.add(overlayer)
    }

    fun replaceOverlayers(overlayers: ArrayList<Overlayer>) {
        clearOverlayers()
        this.overlayers.addAll(overlayers)
    }

    fun clearOverlayers() {
        overlayers.clear()
    }
}
