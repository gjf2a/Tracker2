package com.example.tracker2

import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList
import kotlin.math.pow


class KMeans<T, N>(k: Int, val distance: (T, T) -> N, data: List<T>, mean: (ArrayList<T>) -> T)
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

fun <T, N> plus_plus_randomized(k: Int, data: List<T>, distance: (T, T) -> N): ArrayList<T>
        where N: Number, N: Comparable<N> {
    val result = ArrayList<T>()
    val candidates = ArrayList<T>(data)
    val first = ThreadLocalRandom.current().nextInt(data.size)
    result.add(swapRemove(first, candidates))

    for (i in 0 until k-1) {
        val distro = Distribution<Int>()
        for (j in 0 until candidates.size) {
            distro.add(j, 1 + closestDistanceTo(result, candidates[j], distance).toDouble().pow(2))
        }
        val pick = distro.random_pick()
        result.add(swapRemove(pick, candidates))
    }
    return result
}

fun <T> swapRemove(target: Int, list: ArrayList<T>): T {
    return if (target < list.size - 1) {
        val result = list[target]
        list[target] = list.removeAt(list.size - 1)
        result
    } else {
        list.removeAt(list.size - 1)
    }
}

fun <T, N> closestDistanceTo(selected: ArrayList<T>, candidate: T, distance: (T, T) -> N): N
    where N: Number, N: Comparable<N> {
    var min = distance(candidate, selected[0])
    for (i in 1 until selected.size) {
        val d = distance(candidate, selected[i])
        if (d < min) {
            min = d
        }
    }
    return min
}

fun <T, N> iterate(k: Int, data: List<T>, distance: (T, T) -> N, mean: (ArrayList<T>) -> T): ArrayList<T>
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

class KMeansClassifier<T,L,N> (k: Int, val distance: (T, T) -> N, labeledData: ArrayList<Pair<T,L>>,
                               mean: (ArrayList<T>) -> T) : Iterable<T>, SimpleClassifier<T, L>
        where N: Number, N: Comparable<N> {
    val means = ArrayList<T>()
    val labels = ArrayList<L>()

    init {
        val dataLabels = labeledData.unzip()

        val kmeans = KMeans(k, distance, dataLabels.first, mean)
        val meanCounts = ArrayList<Histogram<L>>()
        for (i in 0 until kmeans.k()) {
            meanCounts.add(Histogram())
        }

        for (i in 0 until labeledData.size) {
            val bestMean = kmeans.classification(dataLabels.first[i])
            meanCounts[bestMean].bump(dataLabels.second[i])
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

    override fun labelFor(example: T): L {
        val mean = classify(means, example, distance)
        return labels[mean]
    }
}