package com.example.tracker2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log

class ColorBlob(images: ArrayList<Bitmap>, maxColors: Int) : BitmapClassifier() {
    val width = images[0].width
    val height = images[0].height
    val matches = KMeansClassifier(maxColors, ::colorSSD, makeLabeledColorsFrom(images), ::colorMean)
    val overlayer = ThresholdOverlayer()
    //val overlayer = BlobWindowOverlayer()

    override fun classify(image: Bitmap) {
        val scaled = Bitmap.createScaledBitmap(image, width, height, false)!!
        val thresh = ThresholdImage(scaled) { bitmap, x, y -> matches.labelFor(tripleFrom(x, y, bitmap))}
        val defaultBlob = Blob(width/2, height/2, 1, 1, 1, width, height)
        val biggest = thresh.getBlobs().fold(defaultBlob) {biggest: Blob, blob: Blob -> if (blob.count > biggest.count) {blob} else {biggest}}
        overlayer.thresholdImage = thresh
        //overlayer.blob = biggest
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

    var blob = Blob(0, 0, 1, 1, 1, 1, 1)

    override fun draw(canvas: Canvas) {
        Log.i("BlobWindowOverlayer", "${blob.toString()}: ${blob.toRect(canvas)}" )
        canvas.drawRect(blob.toRect(canvas), rectPaint)
    }
}

class ThresholdOverlayer : Overlayer {
    private val rectPaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
        }

    var thresholdImage = ThresholdImage(10, 10)
    override fun draw(canvas: Canvas) {
        for (y in 0 until thresholdImage.height) {
            for (x in 0 until thresholdImage.width) {
                if (thresholdImage.get(x, y)) {
                    canvas.drawPoint(scale(x, thresholdImage.width, canvas.width).toFloat(), scale(y, thresholdImage.height, canvas.height).toFloat(), rectPaint)
                }
            }
        }
    }


}