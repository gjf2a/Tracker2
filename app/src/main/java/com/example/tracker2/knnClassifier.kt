package com.example.tracker2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

fun bitmapSSD(img1: Bitmap, img2: Bitmap): Long {
    var sum: Long = 0
    for (x in 0 until img1.width) {
        for (y in 0 until img1.height) {
            sum += singlePixelSSD(img1.getPixel(x, y), img2.getPixel(x, y))
        }
    }
    return sum
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

class KnnClassifier(k: Int, projectName: String, files: FileManager, val scaleWidth: Int, val scaleHeight: Int) : BitmapClassifier() {
    var knn: KNN<Bitmap,String,Long> = KNN(::bitmapSSD, k)

    init {
        for (label in files.projectDir(projectName).listFiles()!!) {
            if (label.isDirectory) {
                for (imageFile in label.listFiles()!!.filter { it.extension == "jpg" }) {
                    val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(imageFile.path), scaleWidth, scaleHeight, false)
                    knn.addExample(bitmap, label.name)
                }
            }
        }
    }

    override fun classify(image: Bitmap) {
        Log.i("KnnClassifier", "About to classify")
        val label = knn.labelFor(Bitmap.createScaledBitmap(image, scaleWidth, scaleHeight, false))
        Log.i("KnnClassifier", label)
        notifyListeners(label)
    }

    override fun assess(): String {
        return knn.assess().summary()
    }

    fun numExamples(): Int {
        return knn.numExamples()
    }
}
