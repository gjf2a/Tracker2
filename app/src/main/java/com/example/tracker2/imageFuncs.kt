package com.example.tracker2

import android.graphics.Bitmap

class ColorTriple(var red: Int, var green: Int, var blue: Int)

operator fun ColorTriple.plusAssign(other: ColorTriple) {
    red += other.red
    green += other.green
    blue += other.blue
}

operator fun ColorTriple.divAssign(scalar: Int) {
    red /= scalar
    green /= scalar
    blue /= scalar
}

fun tripleFrom(x: Int, y: Int, image: Bitmap): ColorTriple {
    val pixel = image.getPixel(x, y)
    return ColorTriple(get8bits(pixel, 16), get8bits(pixel, 8), get8bits(pixel, 0))
}

fun get8bits(color: Int, rightShift: Int): Int {
    return (color shr rightShift) and 0xff
}

fun squaredDiffInt(c1: Int, c2: Int): Long {
    val diff = (c1 - c2).toLong();
    return diff * diff
}

fun singlePixelSSD(color1: Int, color2: Int): Long {
    var sum: Long = 0
    for (rightShift in 0..24 step 8) {
        sum += squaredDiffInt(get8bits(color1, rightShift), get8bits(color2, rightShift))
    }
    return sum
}

fun colorSSD(ct1: ColorTriple, ct2: ColorTriple): Long {
    return squaredDiffInt(ct1.red, ct2.red) +
            squaredDiffInt(ct1.green, ct2.green) +
            squaredDiffInt(ct1.blue, ct2.blue)
}

fun colorMean(colors: ArrayList<ColorTriple>): ColorTriple {
    val total = ColorTriple(0, 0, 0)
    for (color in colors) {
        total += color
    }
    total /= colors.size
    return total
}

