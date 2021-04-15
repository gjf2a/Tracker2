package com.example.tracker2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class ColorBlob(images: ArrayList<Bitmap>, maxColors: Int) : BitmapClassifier() {
    val width = images[0].width
    val height = images[0].height
    val matches = KMeansClassifier(maxColors, ::colorSSD, makeLabeledColorsFrom(images), ::colorMean)
    val overlayer = BlobWindowOverlayer()

    override fun classify(image: Bitmap) {
        val scaled = Bitmap.createScaledBitmap(image, width, height, false)!!
        val thresh = ThresholdImage(scaled) { bitmap, x, y -> matches.labelFor(tripleFrom(x, y, bitmap))}
        val defaultBlob = Blob(width/2, height/2, 1, 1, 1)
        val biggest = thresh.getBlobs().fold(defaultBlob) {biggest: Blob, blob: Blob -> if (blob.count > biggest.count) {blob} else {biggest}}
        overlayer.blob = biggest
        notifyListeners("${biggest.x} ${biggest.y}")
    }

    override fun assess() = "ColorBlob ready\n"

    override fun overlayers(): ArrayList<Overlayer> {
        return arrayListOf(overlayer)
    }
}

class BlobWindowOverlayer : Overlayer {
    private val rectPaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
        }

    var blob = Blob(0, 0, 1, 1, 1)

    override fun draw(canvas: Canvas) {
        canvas.drawRect(blob.toRect(), rectPaint)
    }
}