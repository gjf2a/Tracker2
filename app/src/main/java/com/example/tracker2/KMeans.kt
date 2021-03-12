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

fun <T, N> plusPlusRandomized(k: Int, data: List<T>, distance: (T, T) -> N): ArrayList<T>
        where N: Number, N: Comparable<N> {
    val result = ArrayList<T>()
    val candidates = ArrayList<T>(data)
    while (candidates.size < k) {
        candidates.add(data[ThreadLocalRandom.current().nextInt(data.size)])
    }
    val first = ThreadLocalRandom.current().nextInt(candidates.size)
    result.add(swapRemove(first, candidates))

    for (i in 0 until k-1) {
        val distro = Distribution<Int>()
        for (j in 0 until candidates.size) {
            distro.add(j, 1 + closestDistanceTo(result, candidates[j], distance).toDouble().pow(2))
        }
        val pick = distro.randomPick()
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
    val result = plusPlusRandomized(k, data, distance)
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

class KMeansClassifier1<T,L,N> (k: Int, val distance: (T, T) -> N, labeledData: List<Pair<T,L>>,
                                mean: (ArrayList<T>) -> T) : Iterable<T>, SimpleClassifier<T, L>
        where N: Number, N: Comparable<N> {
    val means = ArrayList<T>()
    val labels = ArrayList<L>()
    val assessment: String

    init {
        val dataLabels = labeledData.unzip()
        val kmeans = KMeans(k, distance, dataLabels.first, mean)
        val numLabels = numDistinctLabels(labeledData)
        var labelK = (2 * numLabels - 1)
            .coerceAtMost(minExamplesPerLabel(labeledData))
            .coerceAtLeast(1)
        labelK += if (labelK % 2 == 0) {1} else {0}
        val labeler = KNN<T, L, N>(distance, labelK)
        labeler.addAllExamples(labeledData)

        for (kmean in kmeans.means) {
            means.add(kmean)
            labels.add(labeler.labelFor(kmean))
        }

        assessment = assess(labeledData, this).summary()
    }

    override fun iterator(): Iterator<T> {
        return means.iterator()
    }

    override fun labelFor(example: T): L {
        val mean = classify(means, example, distance)
        return labels[mean]
    }
}

class KMeansClassifier2<T,L,N> (k: Int, val distance: (T, T) -> N, labeledData: List<Pair<T,L>>,
                                mean: (ArrayList<T>) -> T) : Iterable<T>, SimpleClassifier<T, L>
        where N: Number, N: Comparable<N> {
    val means = ArrayList<T>()
    val labels = ArrayList<L>()
    val assessment: String

    init {
        val clusters = clustersByLabel(labeledData, k, distance, mean)
        for (labelCluster in clusters) {
            for (cluster in labelCluster.value) {
                means.add(cluster)
                labels.add(labelCluster.key)
            }
        }
        assessment = assess(labeledData, this).summary()
    }

    override fun iterator(): Iterator<T> {
        return means.iterator()
    }

    override fun labelFor(example: T): L {
        val mean = classify(means, example, distance)
        return labels[mean]
    }
}

fun <T,L,N> clustersByLabel(labeledData: List<Pair<T,L>>, clustersPerLabel: Int, distance: (T, T) -> N, mean: (ArrayList<T>)->T): HashMap<L,KMeans<T,N>>
    where N: Number, N: Comparable<N> {
    val result = HashMap<L,KMeans<T,N>>()
    for (labelData in examplesByLabel(labeledData)) {
        result[labelData.key] = KMeans(clustersPerLabel, distance, labelData.value, mean)
    }
    return result
}

fun <T,L> examplesByLabel(labeledData: List<Pair<T,L>>): HashMap<L,ArrayList<T>> {
    val result = HashMap<L,ArrayList<T>>()
    for (dataLabel in labeledData) {
        if (!result.containsKey(dataLabel.second)) {
            result[dataLabel.second] = ArrayList()
        }
        result[dataLabel.second]!!.add(dataLabel.first)
    }
    return result
}

fun <T,L> numDistinctLabels(labeledData: List<Pair<T,L>>): Int {
    val labels = HashSet<L>()
    for (dataLabel in labeledData) {
        labels.add(dataLabel.second)
    }
    return labels.size
}

fun <T,L> minExamplesPerLabel(labeledData: List<Pair<T,L>>): Int {
    val counts = Histogram<L>()
    for (dataLabel in labeledData) {
        counts.bump(dataLabel.second)
    }
    return counts.minCount()!!
}