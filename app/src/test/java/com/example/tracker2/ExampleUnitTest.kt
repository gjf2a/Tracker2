package com.example.tracker2

import org.junit.Test
import kotlin.math.absoluteValue
import kotlin.math.pow

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
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
}
