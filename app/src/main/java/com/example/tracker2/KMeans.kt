package com.example.tracker2

import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList
import kotlin.math.pow


class KMeans<T>(k: Int, distance: (T, T) -> Double, data: ArrayList<T>, mean: (ArrayList<T>) -> T) : Iterable<T> {
    val distance: (T, T) -> Double = distance
    val means: ArrayList<T> = iterate(k, data, distance, mean)

    fun k(): Int {return means.size}

    fun classification(sample: T): Int {
        return classify(means, sample, distance)
    }

    override fun iterator(): Iterator<T> {
        return means.iterator()
    }
}

fun <T> classify(objs: ArrayList<T>, t: T, distance: (T,T)->Double): Int {
    val distances = objs.map { distance(it, t) }
    var best: Int = 0
    for (i in 1 until distances.size) {
        if (distances[i] < distances[best]) {
            best = i
        }
    }
    return best
}

fun <T> plus_plus_randomized(k: Int, data: ArrayList<T>, distance: (T, T) -> Double): ArrayList<T> {
    var result = ArrayList<T>()
    result.add(data[ThreadLocalRandom.current().nextInt(k)])
    for (i in 0 until k-1) {
        var distro = Distribution<T>()
        for (d in data) {
            distro.add(d, 1 + distance(result.last(), d).pow(2))
        }
        result.add(distro.random_pick())
    }
    return result
}

fun <T> iterate(k: Int, data: ArrayList<T>, distance: (T, T) -> Double, mean: (ArrayList<T>) -> T): ArrayList<T> {
    var result = plus_plus_randomized(k, data, distance)
    while (true) {
        var classifications = ArrayList<ArrayList<T>>()
        for (i in 0 until k) {
            classifications.add(ArrayList())
        }
        for (d in data) {
            classifications[classify(result, d, distance)].add(d)
        }
        val prev = result
        for (i in 0 until classifications.size) {
            result[i] = if (classifications[i].isEmpty()) {
                prev[i]
            } else {
                mean(classifications[i])
            }
        }
        if (result.zip(prev).all { it.first == it.second }) {
            return result
        }
    }
}
