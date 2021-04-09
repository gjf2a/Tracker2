package com.example.tracker2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

abstract class BitmapClassifier {
    private val listeners = java.util.ArrayList<ClassifierListener>()
    private val messages = java.util.ArrayDeque<String>()
    protected var altDisplay = false

    fun addListener(listener: ClassifierListener) {
        listeners.add(listener)
    }

    fun addListeners(listeners: Iterable<ClassifierListener>) {
        for (listener in listeners) {
            addListener(listener)
        }
    }

    fun notifyListeners(msg: String) {
        for (listener in listeners) {
            listener.receiveClassification(msg)
        }
    }

    fun relayMessageTo(msg: String) {
        messages.addLast(msg)
    }

    fun messagesWaiting() = messages.isNotEmpty()

    fun retrieveMessage(): String = messages.removeFirst()

    fun showAlternative() {
        altDisplay = true
    }

    fun showVideo() {
        altDisplay = false
    }

    abstract fun classify(image: Bitmap)
    abstract fun assess(): String
    open fun overlayers(): java.util.ArrayList<Overlayer> {return java.util.ArrayList()}
}

class DummyClassifier : BitmapClassifier() {
    override fun classify(image: Bitmap) {}

    override fun assess(): String {
        return "No assessment\n"
    }
}

fun bitmapSSD(img1: Bitmap, img2: Bitmap): Long {
    var sum: Long = 0
    for (x in 0 until img1.width) {
        for (y in 0 until img1.height) {
            sum += colorSSD(tripleFrom(x, y, img1), tripleFrom(x, y, img2))
        }
    }
    return sum
}

fun bitmapMean(images: ArrayList<Bitmap>): Bitmap {
    val width = images[0].width
    val height = images[0].height
    val sum = Array(width) {Array(height) {ColorTriple(0, 0, 0)}}
    for (image in images) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                sum[x][y] += tripleFrom(x, y, image)
            }
        }
    }
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) {
        for (y in 0 until height) {
            result.setPixel(x, y, (sum[x][y] / images.size).toIntColor())
        }
    }
    return result
}

class KnnClassifier(k: Int, projectName: String, files: FileManager, val scaleWidth: Int,
                    val scaleHeight: Int) : BitmapClassifier() {
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

class KmeansBitmapClassifier(k: Int, projectName: String, files: FileManager, val scaleWidth: Int,
                             val scaleHeight: Int) : BitmapClassifier() {
    var kmeans = KMeansClassifier(k, ::bitmapSSD,
        files.allProjectImages(projectName, scaleWidth, scaleHeight), ::bitmapMean)

    override fun classify(image: Bitmap) {
        val label = kmeans.labelFor(Bitmap.createScaledBitmap(image, scaleWidth, scaleHeight, false))
        notifyListeners(label)
    }

    override fun assess(): String {
        return kmeans.assessment
    }

}