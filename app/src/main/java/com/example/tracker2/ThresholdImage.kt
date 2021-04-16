package com.example.tracker2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import java.util.*
import kotlin.math.max
import kotlin.math.min

val neighbors = arrayOf(Point(0, -1), Point(0, 1), Point(1, 0), Point(-1, 0))

data class Point(val x: Int, val y: Int) {
    fun neighbors(width: Int, height: Int) = neighbors
        .map { Point(x + it.x, y + it.y) }
        .filter { it.x in 0 until width && it.y in 0 until height }

    fun min(other: Point) = Point(min(x, other.x), min(y, other.y))
    fun max(other: Point) = Point(max(x, other.x), max(y, other.y))
}

class ThresholdImage(val width: Int, val height: Int) {
    val totalPoints = width * height
    private val cells = BitSet(totalPoints)

    constructor(src: Bitmap, pixelClassifier: (Bitmap, Int, Int) -> Boolean) : this(src.width, src.height) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                set(x, y, pixelClassifier(src, x, y))
            }
        }
    }

    private fun index(x: Int, y: Int) = y * width + x

    fun set(x: Int, y: Int, value: Boolean) {
        cells.set(index(x, y), value)
    }

    fun get(x: Int, y: Int) = cells.get(index(x, y))

    fun get(p: Point) = get(p.x, p.y)

    fun getBlobs(): ArrayList<Blob> {
        val blobs = ArrayList<Blob>()
        val pixelLabels = Array(height) {Array(width) {0}}
        var label = 1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (get(x, y) && pixelLabels[y][x] == 0) {
                    pixelLabels[y][x] = label
                    blobs.add(makeBlobAt(x, y, label, pixelLabels))
                }
                label += 1
            }
        }
        return blobs
    }

    private fun makeBlobAt(x: Int, y: Int, label: Int, pixelLabels: Array<Array<Int>>): Blob {
        val queue = ArrayDeque<Point>()
        queue.addLast(Point(x, y))
        var xTotal = x
        var yTotal = y
        var count = 1
        var upperLeft = Point(x, y)
        var lowerRight = Point(x, y)
        while (queue.isNotEmpty()) {
            val candidate = queue.removeFirst()!!
            for (neighbor in candidate.neighbors(width, height)) {
                if (get(neighbor) && pixelLabels[neighbor.y][neighbor.x] == 0) {
                    xTotal += neighbor.x
                    yTotal += neighbor.y
                    count += 1
                    upperLeft = upperLeft.min(neighbor)
                    lowerRight = lowerRight.max(neighbor)
                    pixelLabels[neighbor.y][neighbor.x] = label
                    queue.add(neighbor)
                }
            }
        }
        return Blob(xTotal / count, yTotal / count, lowerRight.x - upperLeft.x, lowerRight.y - upperLeft.y, count, width, height)
    }
}

fun scale(origValue: Int, origSize: Int, targetSize: Int) = targetSize * origValue / origSize

data class Blob(val x: Int, val y: Int, val boundingWidth: Int, val boundingHeight: Int, val count: Int, val pixelWidth: Int, val pixelHeight: Int) {
    fun toRect(canvas: Canvas): Rect {
        val xStart = scale(x - boundingWidth / 2, pixelWidth, canvas.width)
        val xEnd = scale(x + boundingWidth / 2, pixelWidth, canvas.width)
        val yStart = scale(y - boundingHeight / 2, pixelHeight, canvas.height)
        val yEnd = scale(y + boundingHeight / 2, pixelHeight, canvas.height)
        return Rect(xStart, yStart, xEnd, yEnd)
    }
}