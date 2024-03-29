package com.example.tracker2

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.lang.StringBuilder
import java.util.*
import kotlin.math.*

data class Heading(private var degrees: Int) {
    init {
        degrees = ((degrees % 360) + 360) % 360
    }

    fun radians(): Double = Math.toRadians(degrees.toDouble())

    operator fun unaryMinus() = Heading(degrees + 180)
    operator fun plus(other: Heading) = Heading(degrees + other.degrees)
    operator fun minus(other: Heading) = Heading(degrees - other.degrees)
}

fun xy2polar(x: Double, y: Double) = PolarCoord(sqrt(x*x+y*y), atan2(y, x))

data class PolarCoord(val r: Double, val theta: Double) {
    fun x() = r * cos(theta)
    fun y() = r * sin(theta)

    fun rotated(rotation: Double) = PolarCoord(r, theta + rotation)

    operator fun plus(other: PolarCoord): PolarCoord {
        val xSum = x() + other.x()
        val ySum = y() + other.y()
        return PolarCoord(sqrt(xSum.pow(2.0) + ySum.pow(2.0)), atan2(ySum, xSum))
    }
}

data class RobotPosition(val x: Double = 0.0, val y: Double = 0.0, val heading: Heading = Heading(0)) {
    fun updatedBy(motion: PolarCoord) =
        RobotPosition(x + motion.x(), y + motion.y(),
            heading + Heading(Math.toDegrees(motion.theta).toInt()))
}

fun groundlinePolarCoordFrom(position: RobotPosition, groundlineX: Int, groundlineY: Int, converter: PixelConverter): PolarCoord {
    val basePolar = xy2polar(converter.xPixel2distance(groundlineX, groundlineY), converter.yPixel2distance(groundlineY)).rotated(position.heading.radians())
    return xy2polar(position.x + basePolar.x(), position.y + basePolar.y())
}

fun cell2index(xCell: Int, yCell: Int, cellsPerSide: Int): Int {
    val x = xCell + cellsPerSide/2
    val y = yCell + cellsPerSide/2
    return y * cellsPerSide + x
}

class GridMapFilter(cellsPerMeter: Double, metersPerSide: Double, position: RobotPosition, groundline: ArrayList<Int>, converter: PixelConverter) {
    val clearMap = GridMap(cellsPerMeter, metersPerSide)
    val filledMap = GridMap(cellsPerMeter, metersPerSide)

    init {
        clearMap.setAll()
        for (x in groundline.indices) {
            val mapPoint1 = groundlinePolarCoordFrom(position, x, groundline[x], converter)
            val mapPoint2 =
                groundlinePolarCoordFrom(position, x + 1, groundline[x] - 1, converter)
            clearMap.setLine(position.x, position.y, mapPoint1.x(), mapPoint1.y(), mapPoint2.x() - mapPoint1.x(), mapPoint2.y() - mapPoint1.y(), false)
        }
        for (x in groundline.indices) {
            val mapPoint1 = groundlinePolarCoordFrom(position, x, groundline[x], converter)
            val mapPoint2 =
                groundlinePolarCoordFrom(position, x + 1, groundline[x] - 1, converter)
            if (converter.yPixel2distance(groundline[x]) < MAX_DISTANCE_METERS) {
                filledMap.set(mapPoint1.x(), mapPoint1.y(), mapPoint2.x() - mapPoint1.x(), mapPoint2.y() - mapPoint1.y(), true)
                clearMap.set(mapPoint1.x(), mapPoint1.y(), mapPoint2.x() - mapPoint1.x(), mapPoint2.y() - mapPoint1.y(), true)
            }
        }
    }

    fun applyTo(map: GridMap) {
        map.intersect(clearMap)
        map.union(filledMap)
    }

    fun similarityTo(map: GridMap): Double {
        val clearFilter = clearMap.copy()
        clearFilter.flipAll()
        val totalPointsOfConcern = clearFilter.numFilledCells() + filledMap.numFilledCells()

        val filledFilter = filledMap.copy()
        filledFilter.intersect(map)

        val mapInvert = map.copy()
        mapInvert.flipAll()
        clearFilter.intersect(mapInvert)

        return (clearFilter.numFilledCells() + filledFilter.numFilledCells()).toDouble() / totalPointsOfConcern.toDouble()
    }
}

class GridMap(val cellsPerMeter: Double, var metersPerSide: Double = 2.5) {
    private var cells = makeCells()
    fun cellsPerSide() = (cellsPerMeter * metersPerSide).toInt()
    fun totalCells() = cellsPerSide() * cellsPerSide()

    private fun makeCells() = BitSet(totalCells())

    private fun cell2index(xCell: Int, yCell: Int) = yCell * cellsPerSide() + xCell
    private fun meter2cell(meter: Double): Int = (meter * cellsPerMeter).toInt()
    private fun meters2index(xMeter: Double, yMeter: Double): Int =
        meter2cell(yMeter + metersPerSide/2) * cellsPerSide() + meter2cell(xMeter + metersPerSide/2)

    fun setAll() {
        cells.set(0, totalCells(), true)
    }

    fun flipAll() {
        cells.flip(0, totalCells())
    }

    fun numFilledCells(): Int = cells.cardinality()

    fun copy(): GridMap {
        val result = GridMap(cellsPerMeter, metersPerSide)
        result.cells.or(cells)
        return result
    }

    fun intersect(other: GridMap) {
        align(other)
        cells.and(other.cells)
    }

    fun union(other: GridMap) {
        align(other)
        cells.or(other.cells)
    }

    fun set(xMeter: Double, yMeter: Double, width: Double, height: Double, value: Boolean) {
        var row = yMeter
        var stop = yMeter + height
        if (row > stop) {
            val temp = row
            row = stop
            stop = temp
        }
        val width = abs(width)
        while (row < stop) {
            var start = meters2index(xMeter, row)
            var end = meters2index(xMeter + width, row)
            while (start < 0 || start >= totalCells() || end < 0 || end >= totalCells()) {
                resize()
                start = meters2index(xMeter, row)
                end = meters2index(xMeter + width, row)
            }
            cells.set(start, end + 1, value)
            row += 1.0/cellsPerMeter
        }
    }

    private fun align(other: GridMap) {
        if (other.metersPerSide < this.metersPerSide) {
            other.resize(this.metersPerSide)
        } else if (this.metersPerSide < other.metersPerSide) {
            this.resize(other.metersPerSide)
        }
    }

    private fun resize(newMetersPerSide: Double = metersPerSide * 2) {
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

    fun getCell(xCell: Int, yCell: Int) = cells.get(cell2index(xCell, yCell))

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

    override fun equals(other: Any?) = other is GridMap && other.cellsPerMeter == cellsPerMeter && other.metersPerSide == metersPerSide && other.cells == cells

    fun setFrom(position: RobotPosition, groundline: ArrayList<Int>, converter: PixelConverter) {
        val filter = GridMapFilter(cellsPerMeter, metersPerSide, position, groundline, converter)
        filter.applyTo(this)
    }

    fun setLine(xMeter1: Double, yMeter1: Double, xMeter2: Double, yMeter2: Double, width: Double, height: Double, value: Boolean) {
        if (xMeter1 > xMeter2) {
            setLine(xMeter2, yMeter2, xMeter1, yMeter1, width, height, value)
        } else {
            val yContinue: (Double) -> Boolean = if (yMeter2 > yMeter1) {{it < yMeter2}} else {{it > yMeter2}}
            val yStep = if (yMeter2 > yMeter1) {height} else {-height}
            val dx = xMeter2 - xMeter1
            val dy = abs(yMeter2 - yMeter1)
            var a = 0.0
            var b = 0.0
            while (xMeter1 + a < xMeter2 && yContinue(yMeter1 + b)) {
                set(xMeter1 + a, yMeter1 + b, width, height, value)
                when {
                    dy == 0.0 -> a += width
                    dx == 0.0 -> b += yStep
                    a/dx < b/dy -> a += width
                    else -> b += yStep
                }
            }
        }
    }

    override fun hashCode(): Int {
        var result = cellsPerMeter.hashCode()
        result = 31 * result + metersPerSide.hashCode()
        result = 31 * result + cells.hashCode()
        return result
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

class MapOverlayer(var map: GridMap) : Overlayer {
    val clearPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.STROKE
    }

    val fillPaint = Paint().apply {
            isAntiAlias = true
            color = Color.RED
            style = Paint.Style.STROKE
        }

    override fun draw(canvas: Canvas) {
        for (x in 0 until canvas.width) {
            val xCell = x * map.cellsPerSide() / canvas.width
            for (y in 0 until canvas.height) {
                val yCell = y * map.cellsPerSide() / canvas.height
                canvas.drawPoint(x.toFloat(), y.toFloat(), if (map.getCell(xCell, yCell)) {fillPaint} else {clearPaint})
            }
        }
    }
}