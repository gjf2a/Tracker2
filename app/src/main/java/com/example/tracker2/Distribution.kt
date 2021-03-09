package com.example.tracker2

import org.jetbrains.annotations.TestOnly
import java.util.*
import java.util.concurrent.ThreadLocalRandom

// Based on: https://stackoverflow.com/questions/6737283/weighted-randomness-in-java#:~:text=Add%20the%20weights%20beginning%20with,to%20your%20running%20weight%20counter.&text=Then%20you%20just%20have%20to,to%20get%20a%20valid%20number.&text=will%20give%20you%20the%20random%20weighted%20item.

class Distribution<T> {
    var distro: TreeMap<Double,T> = TreeMap()
    var totalWeight: Double = 0.0

    fun add(value: T, weight: Double) {
        distro.put(totalWeight, value)
        totalWeight += weight
    }

    fun random_pick(): T {
        val num = ThreadLocalRandom.current().nextDouble(totalWeight)
        return distro.floorEntry(num)!!.value
    }

    override fun toString(): String {
        return distro.toString()
    }
}