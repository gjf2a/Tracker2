package com.example.tracker2

import android.graphics.*
import android.util.Log

fun getFloorRect(width: Int, height: Int): Rect {
    val rectWidth = width / 4
    val rectHeight = height / 3
    val left = (width - rectWidth) / 2
    val right = left + rectWidth
    val top = height - rectHeight
    val bottom = height
    return Rect(left, top, right, bottom)
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

open class Groundline<C: SimpleClassifier<ColorTriple, Boolean>>(images: ArrayList<Bitmap>, makeClassifier: (ArrayList<Pair<ColorTriple,Boolean>>)->C) : BitmapClassifier() {
    val isFloor = makeClassifier(makeLabeledColorsFrom(images))
    val overlayer = GroundlineOverlayer()
    val width = images[0].width
    val height = images[0].height

    override fun classify(image: Bitmap) {
        val x2y = ArrayList<Int>()
        val scaled = Bitmap.createScaledBitmap(image, width, height, false)
        for (x in 0 until width) {
            var y = height - 1
            while (y > 0) {
                val color = tripleFrom(x, y, scaled)
                if (!isFloor.labelFor(color)) {
                    break;
                } else {
                    y -= 1
                }
            }
            x2y.add(y)
        }
        overlayer.updateHeights(x2y, height)
        Log.i("Groundline", "result (${width}x${height} ${x2y.size}): $x2y")
        val best = highestPoint(x2y)
        notifyListeners("$best")
    }

    fun highestPoint(heights: ArrayList<Int>): Pair<Int,Int> {
        var xBest = 0
        var yBest = heights[0]
        for (x in 1 until heights.size) {
            if (heights[x] < yBest) {
                xBest = x
                yBest = heights[x]
            }
        }
        return Pair(xBest, yBest)
    }

    override fun assess(): String {
        return "Groundline ready\n"
    }

    override fun overlayers(): ArrayList<Overlayer> {
        return arrayListOf(overlayer)
    }
}

fun knnTrainer(labeled: ArrayList<Pair<ColorTriple, Boolean>>, k: Int): KNN<ColorTriple, Boolean, Long> {
    val result = KNN<ColorTriple, Boolean, Long>(::colorSSD, k)
    for (p in labeled) {
        result.addExample(p.first, p.second)
    }
    return result
}

class GroundlineKnn(images: ArrayList<Bitmap>, k: Int) :
    Groundline<KNN<ColorTriple, Boolean, Long>>(images, { knnTrainer(it, k)}) {
    override fun assess(): String {
        return "Total knn examples: ${isFloor.numExamples()}\n"
    }
}

fun colorSSD(ct1: ColorTriple, ct2: ColorTriple): Long {
    return squaredDiffInt(ct1.red, ct2.red) +
            squaredDiffInt(ct1.green, ct2.green) +
            squaredDiffInt(ct1.blue, ct2.blue)
}

class GroundlineOverlayer : Overlayer {
    private val overlayPaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
        }

    val heights = ArrayList<Int>()
    var maxHeight = 0

    override fun draw(canvas: Canvas) {
        for (x in 0 until heights.size - 1) {
            val x1Scaled = canvas.width * x.toFloat() / heights.size
            val x2Scaled = canvas.width * (x + 1).toFloat() / heights.size
            val y1Scaled = canvas.height * heights[x].toFloat() / maxHeight
            val y2Scaled = canvas.height * heights[x + 1].toFloat() / maxHeight
            canvas.drawLine(x1Scaled, y1Scaled, x2Scaled, y2Scaled, overlayPaint)
        }
    }

    fun updateHeights(updated: ArrayList<Int>, updatedHeight: Int) {
        heights.clear()
        heights.addAll(updated)
        maxHeight = updatedHeight
    }
}