package com.example.tracker2

import java.util.*
import kotlin.collections.ArrayList

class KNN<T,L>(val distance: (T,T) -> Double, val k: Int) {
    val examples = ArrayList<Pair<T,L>>()

    fun addExample(example: T, label: L) {
        examples.add(Pair(example,label))
    }

    fun labelFor(example: T): L {
        val distances = examples.map { Pair(distance(it.first, example), it.second) }.sortedBy { it.first}
        val votes = Histogram<L>()
        for (i in 0 until numToCheck()) {
            votes.bump(distances[i].second)
        }
        return votes.pluralityLabel()
    }

    fun numToCheck(): Int {
        return if (k < examples.size) {k} else {examples.size}
    }
}