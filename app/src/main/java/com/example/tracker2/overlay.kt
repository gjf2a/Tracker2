package com.example.tracker2

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

interface Overlayer {
    fun draw(canvas: Canvas)
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
}