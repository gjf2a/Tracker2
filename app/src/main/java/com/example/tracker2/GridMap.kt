package com.example.tracker2

import java.util.*

class GridMap(val cellsPerMeter: Double, val metersPerSide: Double) {
    val cellsPerSide = (cellsPerMeter * metersPerSide).toInt()
    private val cells = BitSet(cellsPerSide * cellsPerSide)

    private fun meter2cell(meter: Double): Int = (meter * cellsPerMeter).toInt()

    private fun meters2index(xMeter: Double, yMeter: Double): Int =
        meter2cell(yMeter + metersPerSide/2) * cellsPerSide + meter2cell(xMeter + metersPerSide/2)

    fun set(xMeter: Double, yMeter: Double, width: Double, height: Double, value: Boolean) {
        var m = yMeter
        while (m < yMeter + height) {
            cells.set(meters2index(xMeter, m), meters2index(xMeter + width, m), value)
            m += 1.0/cellsPerMeter
        }
    }

    fun isFilled(xMeter: Double, yMeter: Double): Boolean = cells.get(meters2index(xMeter, yMeter))

    fun print() {
        for (y in 0 until cellsPerSide) {
            for (x in 0 until cellsPerSide) {
                print("${if (cells.get(y * cellsPerSide + x)) {"*"} else {"."}}")
            }
            println()
        }
    }
}

const val MAX_DISTANCE_METERS = 2.0

fun solveForX(y: Int, x1: Int, y1: Int, x2: Int, y2: Int): Double =
    x1 + (y - y1).toDouble() * (x2 - x1).toDouble() / (y2 - y1).toDouble()

class PixelConverter(val meter1: CalibrationLine, val meter2: CalibrationLine,
                     val imgWidth: Int, val imgHeight: Int) {

    private fun calib2xPixel(calibX: Int): Int = scale2int(calibX, imgWidth)
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

