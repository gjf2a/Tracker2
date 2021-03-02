package com.example.tracker2

import java.lang.StringBuilder
import java.util.*

class MessageHolder : Iterator<String> {
    private var partialMessage = ArrayDeque<Char>()
    private var lines = ArrayDeque<String>()

    fun receive(text: String) {
        for (c in text) {
            if (c == '\n') {
                val line = StringBuilder()
                while (partialMessage.isNotEmpty()) {
                    line.append(partialMessage.removeFirst())
                }
                lines.addLast(line.toString())
            } else {
                partialMessage.addLast(c)
            }
        }
    }

    override fun hasNext(): Boolean {
        return lines.isNotEmpty()
    }

    override fun next(): String {
        return lines.removeFirst()
    }
}