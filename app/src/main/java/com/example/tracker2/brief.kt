package com.example.tracker2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.*

class KnnBriefClassifier(val talker: ArduinoTalker, k: Int, projectName: String, files: FileManager,
                         val scaleWidth: Int, val scaleHeight: Int, val numPairs: Int) : BitmapClassifier {
    var knn: KNN<BRIEFDescriptor,String,Int> = KNN(::BRIEFdistance, k)

    init {
        for (label in files.projectDir(projectName).listFiles()!!) {
            if (label.isDirectory) {
                for (imageFile in label.listFiles()!!.filter { it.extension == "jpg" }) {
                    val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(imageFile.path), scaleWidth, scaleHeight, false)
                    knn.addExample(BRIEFDescriptor(numPairs, bitmap), label.name)
                }
            }
        }
    }

    override fun classify(image: Bitmap) {
        val label = knn.labelFor(BRIEFDescriptor(numPairs, Bitmap.createScaledBitmap(image, scaleWidth, scaleHeight, false)))
        talker.sendString(label)
    }

}

class BRIEFDescriptor(var numPairs: Int, img: Bitmap) {
    var bits = BitSet(numPairs)

    init {
        val totalPixels = img.width * img.height
        if (numPairs < totalPixels) {numPairs = totalPixels}
        val interval = totalPixels / numPairs
        for (i in 0 until totalPixels step interval) {
            val xy = getXYFrom(i, img.width)
            val pixel1 = img.getPixel(xy.first, xy.second)
            val pixel2 = img.getPixel((xy.first + img.width/3) % img.width, (xy.second + img.height/3) % img.height)
            val bigRed = get8bits(pixel1, 16) > get8bits(pixel2, 16)
            val bigGreen = get8bits(pixel1, 8) > get8bits(pixel2, 8)
            val bigBlue = get8bits(pixel1, 0) > get8bits(pixel2, 0)
            bits.set(i, bigRed and bigGreen or bigGreen and bigBlue or bigBlue and bigRed)
        }
    }
}

fun getXYFrom(index: Int, width: Int): Pair<Int,Int> {
    return Pair(index % width, index / width)
}

fun BRIEFdistance(b1: BRIEFDescriptor, b2: BRIEFDescriptor): Int {
    val b3 = b1.bits.get(0, b1.bits.size())
    b3.xor(b2.bits)
    var count = 0
    for (i in 0 until b3.size()) {
        if (b3.get(i)) {
            count += 1
        }
    }
    return count
}