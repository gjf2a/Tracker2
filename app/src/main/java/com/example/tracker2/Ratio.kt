package com.example.tracker2

class Ratio(val numerator: Int, val denominator: Int) {
    fun format(): String {
        return "$numerator/$denominator (${"%.2f".format(100.0 * numerator.toFloat() / denominator.toFloat())}%)"
    }
}