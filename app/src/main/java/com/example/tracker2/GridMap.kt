package com.example.tracker2

import java.lang.StringBuilder
import java.util.*

fun cell2index(xCell: Int, yCell: Int, cellsPerSide: Int): Int {
    val x = xCell + cellsPerSide/2
    val y = yCell + cellsPerSide/2
    return y * cellsPerSide + x
}

class GridMap(val cellsPerMeter: Double, var metersPerSide: Double = 2.5) {
    private var cells = makeCells()
    fun cellsPerSide() = (cellsPerMeter * metersPerSide).toInt()
    fun totalCells() = cellsPerSide() * cellsPerSide()

    private fun makeCells() = BitSet(totalCells())

    private fun meter2cell(meter: Double): Int = (meter * cellsPerMeter).toInt()
    private fun meters2index(xMeter: Double, yMeter: Double): Int =
        meter2cell(yMeter + metersPerSide/2) * cellsPerSide() + meter2cell(xMeter + metersPerSide/2)

    fun setAll() {
        cells.set(0, totalCells(), true)
    }

    fun set(xMeter: Double, yMeter: Double, width: Double, height: Double, value: Boolean) {
        var m = yMeter
        while (m < yMeter + height) {
            var start = meters2index(xMeter, m)
            var end = meters2index(xMeter + width, m)
            while (start < 0 || start >= totalCells() || end < 0 || end >= totalCells()) {
                resize()
                start = meters2index(xMeter, m)
                end = meters2index(xMeter + width, m)
            }
            cells.set(start, end, value)
            m += 1.0/cellsPerMeter
        }
    }

    private fun resize() {
        val oldCells = cells
        val oldSize = cellsPerSide()
        val oldStart = -oldSize/2
        metersPerSide *= 2
        cells = makeCells()
        for (x in oldStart until oldStart + oldSize) {
            for (y in oldStart until oldStart + oldSize) {
                cells.set(cell2index(x, y, cellsPerSide()), oldCells.get(cell2index(x, y, oldSize)))
            }
        }
    }

    fun isFilled(xMeter: Double, yMeter: Double): Boolean = cells.get(meters2index(xMeter, yMeter))

    override fun toString(): String {
        val builder = StringBuilder()
        for (y in 0 until cellsPerSide()) {
            for (x in 0 until cellsPerSide()) {
                builder.append(if (cells.get(y * cellsPerSide() + x)) {"*"} else {"."})
            }
            builder.append('\n')
        }
        return builder.toString()
    }
}

fun mapFrom(groundline: ArrayList<Int>, cellsPerMeter: Double, converter: PixelConverter): GridMap {
    val map = GridMap(cellsPerMeter)
    for (x in groundline.indices) {
        val x1 = converter.xPixel2distance(x, groundline[x])
        val x2 = converter.xPixel2distance(x + 1, groundline[x])
        val y1 = converter.yPixel2distance(groundline[x])
        val y2 = converter.yPixel2distance(groundline[x] - 1)
        if (y1 < MAX_DISTANCE_METERS) {
            map.set(x1, y1, x2 - x1, y2 - y1, true)
        }
    }
    return map
}

const val MAX_DISTANCE_METERS = 2.0

fun solveForX(y: Int, x1: Int, y1: Int, x2: Int, y2: Int): Double =
    x1 + (y - y1).toDouble() * (x2 - x1).toDouble() / (y2 - y1).toDouble()

class PixelConverter(val meter1: CalibrationLine, val meter2: CalibrationLine,
                     val imgWidth: Int, val imgHeight: Int) {

    private fun calib2yPixel(calibY: Int): Int = scale2int(calibY, imgHeight)

    private val meter1Pixel = calib2yPixel(meter1.height)
    private val meter2Pixel = calib2yPixel(meter2.height)

    fun xPixel2distance(xPixel: Int, yPixel: Int): Double {
        val x = (CALIBRATION_MAX * xPixel / imgWidth) - (CALIBRATION_MAX / 2)
        val y = CALIBRATION_MAX * yPixel / imgHeight
        val widthAtY = widthAt(y)
        return x.toDouble() / widthAtY
    }

    fun yPixel2distance(y: Int): Double {
        return when {
            y >= meter1Pixel -> yScale(y, imgHeight, meter1Pixel)
            y >= meter2Pixel -> 1.0 + yScale(y, meter1Pixel, meter2Pixel)
            else -> MAX_DISTANCE_METERS
        }
    }

    private fun yScale(y: Int, offsetBottom: Int, offsetTop: Int): Double {
        val offset = offsetBottom - y
        val range = offsetBottom - offsetTop
        return offset.toDouble() / range.toDouble()
    }

    private fun widthAt(height: Int): Double {
        val left = solveForX(height, meter1.xLeft(), meter1.height, meter2.xLeft(), meter2.height)
        val right = solveForX(height, meter1.xRight(), meter1.height, meter2.xRight(), meter2.height)
        return right - left
    }
}

