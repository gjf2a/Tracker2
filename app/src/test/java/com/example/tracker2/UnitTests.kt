package com.example.tracker2

import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*
import kotlin.math.pow

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
            h.bump(d.random_pick())
        }
        assert(is_approx_eq(h.portion('a'), 0.33, 0.1))
        assert(is_approx_eq(h.portion('b'), 0.67, 0.1))
    }

    @Test
    fun kmeansTest() {
        val values = arrayListOf(1, 2, 3, 1001, 1002, 1003, 2001, 2002, 2003, 3001, 3002, 3003)
        val means = KMeans(4, {x, y -> (x - y).toDouble().pow(2)}, values, {nums -> nums.sum() / nums.size})
        for (mean in means) {
            print("$mean; ")
        }
        println()
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
}
