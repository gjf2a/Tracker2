package com.example.tracker2

import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList
import kotlin.math.pow


class KMeans<T, N>(k: Int, val distance: (T, T) -> N, data: ArrayList<T>, mean: (ArrayList<T>) -> T)
    : Iterable<T>
        where N: Number, N: Comparable<N> {
    val means: ArrayList<T> = iterate(k, data, distance, mean)

    fun k(): Int {return means.size}

    fun classification(sample: T): Int {
        return classify(means, sample, distance)
    }

    override fun iterator(): Iterator<T> {
        return means.iterator()
    }
}

fun <T, N> classify(objs: ArrayList<T>, t: T, distance: (T,T)->N): Int
        where N: Number, N: Comparable<N> {
    val distances = objs.map { distance(it, t) }
    var best: Int = 0
    for (i in 1 until distances.size) {
        if (distances[i] < distances[best]) {
            best = i
        }
    }
    return best
}

fun <T, N> plus_plus_randomized(k: Int, data: ArrayList<T>, distance: (T, T) -> N): ArrayList<T>
        where N: Number, N: Comparable<N> {
    val result = ArrayList<T>()
    result.add(data[ThreadLocalRandom.current().nextInt(k)])
    for (i in 0 until k-1) {
        val distro = Distribution<T>()
        for (d in data) {
            distro.add(d, 1 + distance(result.last(), d).toDouble().pow(2))
        }
        result.add(distro.random_pick())
    }
    return result
}

fun <T, N> non_randomized_init(k: Int, data: ArrayList<T>, distance: (T, T) -> N): ArrayList<T>
        where N: Number, N: Comparable<N> {
    val result = ArrayList<T>()
    // TODO: Actually implement this.
    return result
}

fun <T, N> iterate(k: Int, data: ArrayList<T>, distance: (T, T) -> N, mean: (ArrayList<T>) -> T): ArrayList<T>
        where N: Number, N: Comparable<N>{
    val result = plus_plus_randomized(k, data, distance)
    while (true) {
        val classifications = ArrayList<ArrayList<T>>()
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

class KMeansClassifier<T,L,N> (k: Int, val distance: (T, T) -> N, data: ArrayList<T>,
                             dataLabels: ArrayList<L>, mean: (ArrayList<T>) -> T) : Iterable<T>
        where N: Number, N: Comparable<N> {
    val means = ArrayList<T>()
    val labels = ArrayList<L>()

    init {
        assert(data.size == dataLabels.size)

        val kmeans = KMeans(k, distance, data, mean)
        val meanCounts = ArrayList<Histogram<L>>()
        for (i in 0 until kmeans.k()) {
            meanCounts.add(Histogram())
        }

        for (i in 0 until dataLabels.size) {
            val bestMean = kmeans.classification(data[i])
            meanCounts[bestMean].bump(dataLabels[i])
        }

        for (i in 0 until kmeans.k()) {
            if (meanCounts[i].totalCount > 0) {
                means.add(kmeans.means[i])
                labels.add(meanCounts[i].pluralityLabel())
            }
        }
    }

    override fun iterator(): Iterator<T> {
        return means.iterator()
    }

    fun labelFor(example: T): L {
        val mean = classify(means, example, distance)
        return labels[mean]
    }
}