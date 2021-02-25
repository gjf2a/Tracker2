package com.example.tracker2

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.pow

class RGBHistogramMaker(bitmap: Bitmap, num_clusters: Int) {
    val clusters = KMeans(num_clusters, {c1, c2 -> rgbDistance(c1, c2)}, rgbBitmapList(bitmap), {rgbMean(it)})

    fun makeFrom(bitmap: Bitmap): Histogram<Int> {
        val result = Histogram<Int>()
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                result.bump(clusters.classification(bitmap.getPixel(x, y)))
            }
        }
        return result
    }
}

fun rgbBitmapList(bitmap: Bitmap): ArrayList<Int> {
    val result = ArrayList<Int>()
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            result.add(bitmap.getPixel(x, y))
        }
    }
    return result
}

fun rgbMean(colors: ArrayList<Int>): Int {
    return Color.rgb(RGB.RED.mean(colors), RGB.GREEN.mean(colors), RGB.BLUE.mean(colors))
}

fun rgbDistance(c1: Int, c2: Int): Double {
    return RGB.values().map { squared_diff(it.part(c1), it.part(c2)) }.sum()
}

fun squared_diff(c1: Int, c2: Int): Double {
    val diff = c1 - c2;
    return (diff * diff).toDouble()
}

fun squared_diff(c1: Double, c2: Double): Double {
    return (c1 - c2).pow(2)
}

enum class RGB {
    RED {
        override fun part(encoded: Int): Int {
            return Color.red(encoded)
        }
    },
    GREEN {
        override fun part(encoded: Int): Int {
            return Color.green(encoded)
        }
    },
    BLUE {
        override fun part(encoded: Int): Int {
            return Color.blue(encoded)
        }
    };

    abstract fun part(encoded: Int): Int

    fun mean(colors: ArrayList<Int>): Int {
        return colors.map {part(it)}.sum() / colors.size
    }
}