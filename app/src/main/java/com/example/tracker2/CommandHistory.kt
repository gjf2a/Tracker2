package com.example.tracker2

import java.io.File
import java.io.PrintStream
import java.util.*
import kotlin.collections.HashMap

class HistoryItem(val command: String, var numUses: Int = 1) {
    override fun toString(): String = "$command:$numUses"

    override fun equals(other: Any?): Boolean = (other is HistoryItem) && command == other.command && numUses == other.numUses

    override fun hashCode(): Int = command.hashCode() + numUses
}

class CommandHistory(private val filename: String) {
    private val history = HashMap<String,HistoryItem>()

    init {
        val file = File(filename)
        if (file.exists()) {
            val reader = Scanner(file)
            while (reader.hasNextLine()) {
                val line = reader.nextLine().split(":")
                history[line[0]] = HistoryItem(line[0], Integer.parseInt(line[1]))
            }
        }
    }

    override fun equals(other: Any?): Boolean = (other is CommandHistory) && history == other.history && filename == other.filename

    fun add(command: String) {
        val trimmed = command.trim()
        if (trimmed.isNotEmpty()) {
            if (history.containsKey(trimmed) && history[trimmed] != null) {
                history[trimmed]!!.numUses += 1
            } else {
                history[trimmed] = HistoryItem(trimmed)
            }

            val fileOut = PrintStream(File(filename))
            for (stored in history.values) {
                fileOut.println(stored)
            }
            fileOut.close()
        }
    }

    fun mostPopular(): List<String> {
        val items = ArrayList<HistoryItem>(history.values)
        items.sortBy { -it.numUses }
        return items.map { it.command }
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + history.hashCode()
        return result
    }
}