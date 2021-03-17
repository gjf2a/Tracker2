package com.example.tracker2

import android.graphics.*
import android.util.Log

fun getFloorRect(width: Int, height: Int): Rect {
    val rectWidth = width / 4
    val rectHeight = height / 3
    val left = (width - rectWidth) / 2
    val right = left + rectWidth
    val top = height - rectHeight
    return Rect(left, top, right, height)
}

fun getUpperLeftRect(width: Int, height: Int): Rect {
    val rectWidth = width / 4
    val rectHeight = height / 4
    return Rect(0, 0, rectWidth, rectHeight)
}

fun getUpperRightRect(width: Int, height: Int): Rect {
    val rectWidth = width / 4
    val rectHeight = height / 4
    return Rect(width - rectWidth, 0, width, rectHeight)
}

fun makeLabeledColorsFrom(images: ArrayList<Bitmap>): ArrayList<Pair<ColorTriple, Boolean>> {
    val labeledData = ArrayList<Pair<ColorTriple, Boolean>>()
    for (image in images) {
        for (rectFunc in arrayListOf(::getUpperLeftRect, ::getUpperRightRect)) {
            addColorsFrom(image, rectFunc(image.width, image.height), labeledData,false)
        }
        addColorsFrom(image, getFloorRect(image.width, image.height), labeledData,true)
    }
    return labeledData
}

fun addColorsFrom(image: Bitmap, rect: Rect, labeledData: ArrayList<Pair<ColorTriple, Boolean>>, label: Boolean) {
    for (x in rect.left until rect.right) {
        for (y in rect.top until rect.bottom) {
            labeledData.add(Pair(tripleFrom(x, y, image), label))
        }
    }
}

open class Groundline<C: SimpleClassifier<ColorTriple, Boolean>>
    (images: ArrayList<Bitmap>, val minNotFloor: Int, makeClassifier: (ArrayList<Pair<ColorTriple,Boolean>>)->C) : BitmapClassifier() {
    val isFloor = makeClassifier(makeLabeledColorsFrom(images))
    val overlayer = GroundlineOverlayer()
    val width = images[0].width
    val height = images[0].height

    override fun classify(image: Bitmap) {
        val x2y = ArrayList<Int>()
        val scaled = Bitmap.createScaledBitmap(image, width, height, false)
        for (x in 0 until width) {
            x2y.add(findNotFloor(scaled, x))
        }
        val best = highestPoint(x2y)
        overlayer.updateHeights(x2y, height, best.first)
        Log.i("Groundline", "result (${width}x${height} ${x2y.size}): $x2y")
        notifyListeners("${best.first} ${best.second}")
    }

    private fun findNotFloor(scaled: Bitmap, x: Int): Int {
        var y = scaled.height - 1
        var notFloorStreak = 0
        while (y > 0 && notFloorStreak < minNotFloor) {
            notFloorStreak = if (!isFloor.labelFor(tripleFrom(x, y, scaled))) {
                notFloorStreak + 1
            } else {
                0
            }
            y -= 1
        }
        return y
    }

    override fun assess(): String {
        return "Groundline ready\n"
    }

    override fun overlayers(): ArrayList<Overlayer> {
        return arrayListOf(overlayer)
    }
}

// Pattern: Say width is 12;
// 6, 5, 7, 4, 8, 3, 9, 2, 10, 1, 11, 0
// 0, -1, +2, -3, +4, -5, +6, -7, +8, -9, +10, -11
fun highestPoint(heights: ArrayList<Int>): Pair<Int,Int> {
    var x = heights.size / 2
    var xBest = x
    var yBest = heights[xBest]
    for (offset in 1 until heights.size) {
        x += if (offset % 2 == 0) {offset} else {-offset}
        if (heights[x] < yBest) {
            xBest = x
            yBest = heights[x]
        }
    }
    return Pair(xBest, yBest)
}

fun knnTrainer(labeled: ArrayList<Pair<ColorTriple, Boolean>>, k: Int): KNN<ColorTriple, Boolean, Long> {
    val result = KNN<ColorTriple, Boolean, Long>(::colorSSD, k)
    for (p in labeled) {
        result.addExample(p.first, p.second)
    }
    return result
}

class GroundlineKnn(images: ArrayList<Bitmap>, k: Int, minNotFloor: Int)
    : Groundline<KNN<ColorTriple, Boolean, Long>>(images, minNotFloor, { knnTrainer(it, k)}) {
    override fun assess(): String {
        return "Total knn examples: ${isFloor.numExamples()}\n"
    }
}

class GroundlineKmeans(images: ArrayList<Bitmap>, k: Int, minNotFloor: Int)
    : Groundline<KMeansClassifier<ColorTriple, Boolean, Long>>(images, minNotFloor,
    { KMeansClassifier(k, ::colorSSD, it, ::colorMean) })

class GroundlineOverlayer : Overlayer {
    private val groundlinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
        }

    private val highestPaint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
        style = Paint.Style.STROKE
    }

    var heights = ArrayList<Int>()
    var imageHeight = 0
    var highestX = 0

    override fun draw(canvas: Canvas) {
        for (x in 0 until heights.size - 1) {
            val x1Scaled = canvas.width * x.toFloat() / heights.size
            val x2Scaled = canvas.width * (x + 1).toFloat() / heights.size
            val y1Scaled = canvas.height * heights[x].toFloat() / imageHeight
            val y2Scaled = canvas.height * heights[x + 1].toFloat() / imageHeight
            canvas.drawLine(x1Scaled, y1Scaled, x2Scaled, y2Scaled, groundlinePaint)
        }
        val x = highestX.toFloat() * canvas.width.toFloat() / heights.size.toFloat()
        canvas.drawLine(x, 0.0f, x, canvas.height.toFloat(), highestPaint)
    }

    fun updateHeights(updated: ArrayList<Int>, imageHeight: Int, highestX: Int) {
        this.heights = updated
        this.imageHeight = imageHeight
        this.highestX = highestX
    }
}