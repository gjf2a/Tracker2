package com.example.tracker2

import java.io.File

// I don't know why it gets stray "cv" text at the start, but this is meant to compensate for it.
fun suffixStartingWith(suffixStart: String, msg: String): String {
    for (i in msg.length - suffixStart.length downTo 0) {
        if (msg.substring(i, i + suffixStart.length) == suffixStart) {
            return msg.substring(i)
        }
    }
    return msg
}

interface MessageTarget {
    fun sendString(msg: String): Boolean
}

enum class CommandType {
    CREATE_CLASSIFIER, PAUSE_CLASSIFIER, RESUME_CLASSIFIER, ERROR
}

data class InterpreterResult(val cmdType: CommandType, val classifier: BitmapClassifier, val index: Int, val msg: String)

fun interpret(msg: String, outputDir: File, talker: MessageTarget): InterpreterResult {
    val manager = FileManager(outputDir)
    val command = suffixStartingWith("cv ", msg.trim()).split(" ")
    if (command.isNotEmpty() && command[0] == "cv") {
        if (command.size == 6 && command[1] == "knn") {
            val k = Integer.parseInt(command[2])
            val project = command[3]
            if (manager.projectExists(project)) {
                val width = Integer.parseInt(command[4])
                val height = Integer.parseInt(command[5])
                val knn = KnnClassifier(talker, k, project, manager, width, height)
                return InterpreterResult(CommandType.CREATE_CLASSIFIER, knn, 0, "Activating kNN classifer; k=$k; project=$project\n")
            } else {
                return InterpreterResult(CommandType.ERROR, DummyClassifier(), 0, "Project $project does not exist")
            }
        } else if (command.size == 7 && command[1] == "knn_brief") {
            val k = Integer.parseInt(command[2])
            val project = command[3]
            if (manager.projectExists(project)) {
                val width = Integer.parseInt(command[4])
                val height = Integer.parseInt(command[5])
                val numPairs = Integer.parseInt(command[6])
                val knn = KnnBriefClassifier(talker, k, project, manager, width, height, numPairs)
                return InterpreterResult(CommandType.CREATE_CLASSIFIER, knn, 0, "Activating BRIEF kNN classifier; k=$k; project = $project; numPairs = $numPairs\n")
            } else {
                return InterpreterResult(CommandType.ERROR, DummyClassifier(), 0, "Project $project does not exist")
            }
        } else if (command.size == 2 && command[1] == "pause") {
            talker.sendString("0")
            return InterpreterResult(CommandType.PAUSE_CLASSIFIER, DummyClassifier(), 0, "Classifier paused")
        } else if (command.size == 3 && command[1] == "resume") {
            val id = Integer.parseInt(command[2])
            return InterpreterResult(CommandType.RESUME_CLASSIFIER, DummyClassifier(), id, "")
        } else {
            return InterpreterResult(CommandType.ERROR, DummyClassifier(), 0, "Unrecognized cv cmd: '$command'")
        }
    } else {
        return InterpreterResult(CommandType.ERROR, DummyClassifier(), 0, "Unrecognized cmd: '$command'")
    }
}