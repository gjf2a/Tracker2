package com.example.tracker2

import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*
import kotlin.math.pow

val kmeansSamples = arrayListOf(1, 2, 3, 1001, 1002, 1003, 2001, 2002, 2003, 3001, 3002, 3003)
val kmeansLabels = arrayListOf('a', 'a', 'a', 'b', 'b', 'b', 'c', 'c', 'c', 'd', 'd', 'd')

fun intDist(x: Int, y: Int): Double {
    return (x - y).toDouble().pow(2)
}

fun intMean(nums: ArrayList<Int>): Int {
    return nums.sum() / nums.size
}

class UnitTests {
    @Test
    fun histogramTest() {
        val h = Histogram<Char>()
        for (i in 0 until 20) {
            h.bump('a')
        }
        for (i in 0 until 30) {
            h.bump('b')
        }
        assert(h.get('a') == 20)
        assert(h.get('b') == 30)
        assert(h.get('c') == 0)
        assert(h.portion('a') == 0.4)
        assert(h.portion('b') == 0.6)
        assert(h.pluralityLabel() == 'b')
    }

    fun is_approx_eq(value: Double, target: Double, tolerance: Double): Boolean {
        val min = target - tolerance/2
        val max = min + tolerance
        return value in min..max
    }

    @Test
    fun distroTest() {
        val h = Histogram<Char>()
        val d = Distribution<Char>()
        d.add('a', 2.0)
        d.add('b', 4.0)
        for (i in 0 until 10000) {
            h.bump(d.randomPick())
        }
        assert(is_approx_eq(h.portion('a'), 0.33, 0.1))
        assert(is_approx_eq(h.portion('b'), 0.67, 0.1))
    }

    @Test
    fun kmeansTest() {
        val means = KMeans(4, ::intDist, kmeansSamples) { it.sum() / it.size}
        for (target in arrayOf(2, 1002, 2002, 3002)) {
            assert(means.contains(target))
        }
    }

    @Test
    fun knnTest() {
        val classifier = KNN<Int, Char, Double>(::intDist, 3)
        for (i in kmeansLabels.indices) {
            classifier.addExample(kmeansSamples[i], kmeansLabels[i])
        }
        for (test in arrayOf(1002, 3002, 2, 2002).zip(arrayOf('b', 'd', 'a', 'c'))) {
            assert(classifier.labelFor(test.first) == test.second)
        }
    }

    @Test
    fun kmeansUnderflowTest() {
        val means = KMeans(kmeansSamples.size + 1, ::intDist, kmeansSamples) { it.sum() / it.size}
        for (sample in kmeansSamples) {
            assert(means.means.contains(sample))
        }
    }

    @Test
    fun kmeansClassifierTest() {
        val kmeansData = kmeansSamples.zip(kmeansLabels)
        val classifier = KMeansClassifierAggregated(4, ::intDist, kmeansData, ::intMean)
        for (p in arrayOf(Pair(50, 'a'), Pair(400, 'a'), Pair(600, 'b'), Pair(1400, 'b'),
            Pair(1800, 'c'), Pair(2100, 'c'), Pair(2700, 'd'))) {
            assert(classifier.labelFor(p.first) == p.second)
        }
    }

    @Test
    fun kmeansClassifier2Test() {
        val kmeansData = kmeansSamples.zip(kmeansLabels)
        val classifier = KMeansClassifier(2, ::intDist, kmeansData, ::intMean)
        for (p in arrayOf(Pair(50, 'a'), Pair(400, 'a'), Pair(600, 'b'), Pair(1400, 'b'),
            Pair(1800, 'c'), Pair(2100, 'c'), Pair(2700, 'd'))) {
            assert(classifier.labelFor(p.first) == p.second)
        }
    }

    @Test
    fun swapRemoveTest() {
        val nums = arrayListOf(0, 1, 2, 3)
        assert(swapRemove(1, nums) == 1)
        assert(nums.size == 3)
        assert(swapRemove(2, nums) == 2)
        assert(nums.size == 2)
        assert(swapRemove(1, nums) == 3)
        assert(nums.size == 1)
        assert(swapRemove(0, nums) == 0)
        assert(nums.size == 0)
    }

    @Test
    fun fileManagerTest() {
        val projectName = "testProject"
        val filename = "target"
        val filetext1 = "This is a test"
        val filetext2 = "Also a test"
        val label1 = "a"
        val label2 = "b"

        println("Manager base: ${File(".").absolutePath}")
        val manager = FileManager(File("."))
        assert(!manager.projectExists(projectName))
        manager.addProject(projectName)
        assert(manager.projectExists(projectName))

        assert(!manager.labelExists(projectName, label1))
        manager.addLabel(projectName, label1)
        assert(manager.labelExists(projectName, label1))

        val dummyFile = File(filename)
        val writer = PrintWriter(FileWriter(dummyFile))
        writer.println(filetext1)
        writer.close()
        assert(dummyFile.exists())
        manager.moveFileTo(dummyFile, projectName, label1)

        val movedDummy = File(manager.labelDir(projectName, label1), dummyFile.name)
        assert(movedDummy.exists())
        assert(!dummyFile.exists())
        val dummyScanner = Scanner(movedDummy)
        val line = dummyScanner.nextLine()
        assert(line == filetext1)
        dummyScanner.close()

        val writer2 = PrintWriter(FileWriter(dummyFile))
        writer2.println(filetext2)
        writer2.close()
        manager.moveFileTo(dummyFile, projectName, label2)

        assert(manager.allLabelsIn(projectName).contains(label1))
        assert(manager.allLabelsIn(projectName).contains(label2))
        assert(manager.allProjects().contains(projectName))

        assert(manager.deleteLabel(projectName, label1))
        assert(!manager.allLabelsIn(projectName).contains(label1))

        assert(manager.deleteProject(projectName))
        assert(!manager.allProjects().contains(projectName))
    }

    @Test
    fun historyTest() {
        val file = File("testfile")
        val history = CommandHistory(file.name)
        history.add(" ")
        for (i in 1..10) {
            history.add(" ")
            history.add("two")
            if (i % 2 == 0) {
                history.add("three")
            }
            if (i % 3 == 0) {
                history.add("one")
            }
        }

        val sorted = history.mostPopular()
        assert(sorted.size == 3)
        assert(sorted[0] == "two")
        assert(sorted[1] == "three")
        assert(sorted[2] == "one")

        val history2 = CommandHistory(file.name)
        assert(history == history2)

        file.delete()
    }

    @Test
    fun minHeightTest() {
        val example1 = arrayListOf(27, 27, 27, 27, 27, 13, 13, 13, 13, 13, 13, 11, 11, 10, 10, 10, 10, 11, 10, 12, 12, 12, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 27, 27, 27, 27)
        val highest1 = highestPoint(example1)
        assert(highest1 == Pair(18, 10))

        val example2 = arrayListOf(16, 13, 16, 13, 16, 12, 14, 13, 13, 10, 10, 10, 13, 9, 9, 9, 9, 9, 9, 9, 10, 10, 11, 11, 12, 12, 12, 12, 12, 12, 12, 5, 1, 1, 5, 6, 8, 9, 10, 12)
        val highest2 = highestPoint(example2)
        assert(highest2 == Pair(32, 1))
    }

    @Test
    fun pixelConverterTest() {
        val converter = PixelConverter(CalibrationLine(60, 64), CalibrationLine(52, 30), 40, 30)
        val groundline1 = arrayListOf(17, 17, 17, 17, 17, 16, 16, 17, 16, 15, 15, 16, 15, 15, 15, 15, 15, 15, 15, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 21, 20, 21, 21, 21, 21, 21, 21, 21, 22, 22)
        val groundline2 = arrayListOf(26, 26, 24, 24, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 23, 26, 24, 15, 21, 16, 19, 20, 20, 21, 23, 22, 5, 11, 2, 4, 3, 1, 1, 1, 10, 17, 17)
        val groundline3 = arrayListOf(18, 17, 17, 7, 18, 17, 17, 17, 17, 17, 18, 18, 16, 15, 15, 15, 16, 15, 15, 15, 15, 17, 17, 17, 18, 18, 18, 17, 18, 18, 18, 18, 16, 14, 14, 14, 14, 14, 17, 17)
        for (groundline in arrayOf(groundline1, groundline2, groundline3)) {
            for (x in 0 until groundline.size) {
                val x1 = converter.xPixel2distance(x, groundline[x])
                val y1 = converter.yPixel2distance(groundline[x])
                println("($x, ${groundline[x]}): ($x1, $y1)")
            }
        }
    }

    @Test
    fun solveForXTest() {
        assert(solveForX(0, 1, 2, 1, 3) == 1.0)
        assert(solveForX(3, 1, 1, 2, 2) == 3.0)
    }
}
