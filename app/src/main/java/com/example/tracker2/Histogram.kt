package com.example.tracker2

class Histogram<T> : Iterable<MutableMap.MutableEntry<T,Int>> {
    var counts: HashMap<T,Int> = HashMap()
    var totalCount: Int = 0

    fun bump(key: T) {
        counts.put(key, get(key) + 1)
        totalCount += 1
    }

    fun get(key: T): Int {
        return counts.get(key) ?: 0
    }

    fun portion(key: T): Double {
        return get(key).toDouble() / totalCount.toDouble()
    }

    fun distance(other: Histogram<T>): Double {
        return counts.keys.union(other.counts.keys)
            .map { squared_diff(portion(it), other.portion(it)) }
            .sum()
    }

    fun pluralityLabel(): T {
        assert(!counts.isEmpty())
        val start = counts.entries.iterator().next()
        return counts.entries.fold(start, {acc, t -> if (acc.value < t.value) {t} else {acc} }).key
    }

    override fun iterator(): Iterator<MutableMap.MutableEntry<T, Int>> {
        return counts.entries.iterator()
    }

    fun minCount(): Int? {
        return counts.values.minOrNull()
    }
}