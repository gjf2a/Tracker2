package com.example.tracker2

import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

interface SimpleClassifier<T, L> {
    fun labelFor(example: T): L
}

class KNN<T,L,D: Comparable<D>>(val distance: (T,T) -> D, val k: Int) : SimpleClassifier<T, L> {
    private val examples = ArrayList<Pair<T,L>>()

    fun addExample(example: T, label: L) {
        examples.add(Pair(example,label))
    }

    fun addAllExamples(examples: List<Pair<T,L>>) {
        this.examples.addAll(examples)
    }

    override fun labelFor(example: T): L {
        val distances = examples.map { Pair(distance(it.first, example), it.second) }.sortedBy { it.first}
        val votes = Histogram<L>()
        for (i in 0 until numToCheck()) {
            votes.bump(distances[i].second)
        }
        return votes.pluralityLabel()
    }

    private fun numToCheck(): Int {
        return if (k < examples.size) {k} else {examples.size}
    }

    fun numExamples(): Int {
        return examples.size
    }

    private fun without(example: T): KNN<T,L,D> {
        val result = KNN<T,L,D>(distance, k)
        result.examples.addAll(examples.filter { it.first != example })
        return result
    }

    fun assess(): ConfusionMatrix<L> {
        val result = ConfusionMatrix<L>()
        for (example in examples) {
            val tester = without(example.first)
            result.resultFor(example.second, tester.labelFor(example.first))
        }
        return result
    }
}

class ConfusionMatrix<L> {
    var numCorrectFor = Histogram<L>()
    var numIncorrectFor = Histogram<L>()
    var labels = TreeSet<L>()

    fun resultFor(target: L, predicted: L) {
        labels.add(target)
        labels.add(predicted)
        if (target == predicted) {numCorrectFor} else {numIncorrectFor}.bump(target)
    }

    fun summary(): String {
        val result = StringBuilder()
        for (label in labels) {
            result.append("${label.toString()}: ${Ratio(numCorrectFor.get(label), numCorrectFor.get(label) + numIncorrectFor.get(label)).format()}\n")
        }
        return result.toString()
    }
}