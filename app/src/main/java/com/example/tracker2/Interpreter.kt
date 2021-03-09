package com.example.tracker2

import android.graphics.Bitmap
import java.io.File
import java.lang.NumberFormatException

// I don't know why it gets stray "cv" text at the start, but this is meant to compensate for it.
fun suffixStartingWith(suffixStart: String, msg: String): String {
    for (i in msg.length - suffixStart.length downTo 0) {
        if (msg.substring(i, i + suffixStart.length) == suffixStart) {
            return msg.substring(i)
        }
    }
    return ""
}

interface ClassifierListener {
    fun receiveClassification(msg: String)
}

enum class CommandType {
    CREATE_CLASSIFIER, PAUSE_CLASSIFIER, COMMENT, RESUME_CLASSIFIER, ERROR
}

data class InterpreterResult(val cmdType: CommandType, val classifier: BitmapClassifier, val resumeIndex: Int, val msg: String)

fun simpleResult(cmd: CommandType, msg: String): InterpreterResult {
    return InterpreterResult(cmd, DummyClassifier(), 0, msg)
}

fun resumeResult(id: Int): InterpreterResult {
    return InterpreterResult(
        CommandType.RESUME_CLASSIFIER,
        DummyClassifier(),
        id,
        "Resuming classifier $id"
    )
}

fun createClassifier(classifier: BitmapClassifier, msg: String): InterpreterResult {
    return InterpreterResult(CommandType.CREATE_CLASSIFIER, classifier, 0, msg)
}

fun interpret(msg: String, outputDir: File, listeners: List<ClassifierListener>): InterpreterResult {
    try {
        val manager = FileManager(outputDir)
        val comment = suffixStartingWith("#", msg.trim())
        if (comment.isNotEmpty()) {
            return simpleResult(CommandType.COMMENT, comment)
        } else {
            val command = suffixStartingWith("cv ", msg.trim()).split(" ")
            if (command.isNotEmpty() && command[0] == "cv") {
                if (command.size == 6 && command[1] == "knn") {
                    return interpretKnn(command, manager, listeners)
                } else if (command.size == 7 && command[1] == "knn_brief") {
                    return interpretBrief(command, manager, listeners)
                } else if (command.size >= 8 && command[1] == "groundline") {
                    return interpretGroundline(command, manager, listeners)
                } else if (command.size == 2 && command[1] == "pause") {
                    return simpleResult(CommandType.PAUSE_CLASSIFIER, "Classifier paused")
                } else if (command.size == 3 && command[1] == "resume") {
                    return resumeResult(Integer.parseInt(command[2]))
                } else {
                    return simpleResult(CommandType.ERROR,"Unrecognized cv cmd: '$command'")
                }
            } else {
                return simpleResult(CommandType.ERROR,"Unrecognized cmd: '$command'")
            }
        }
    } catch (nfe: NumberFormatException) {
        return simpleResult(CommandType.ERROR, "Integer expected")
    }
}

fun interpretGroundline(command: List<String>, manager: FileManager, listeners: List<ClassifierListener>): InterpreterResult {
    val k = Integer.parseInt(command[2])
    val project = command[3]
    if (manager.projectExists(project)) {
        val label = command[4]
        if (manager.labelExists(project, label)) {
            val width = Integer.parseInt(command[5])
            val height = Integer.parseInt(command[6])
            val choices = ArrayList<Bitmap>()
            for (i in 7 until command.size) {
                choices.add(
                    manager.loadImage(
                        project,
                        label,
                        Integer.parseInt(command[i]),
                        width,
                        height
                    )
                )
            }
            val groundline = Groundline(choices, k)
            groundline.addListeners(listeners)
            return createClassifier(groundline,
                "Activating Groundline: $k $project $label (${width}x${height} $choices")
        } else {
            return simpleResult(CommandType.ERROR,"Label '$label' does not exist")
        }
    } else {
        return simpleResult(CommandType.ERROR, "Project '$project' does not exist")
    }
}

fun interpretBrief(command: List<String>, manager: FileManager, listeners: List<ClassifierListener>): InterpreterResult {
    val k = Integer.parseInt(command[2])
    val project = command[3]
    return if (manager.projectExists(project)) {
        val width = Integer.parseInt(command[4])
        val height = Integer.parseInt(command[5])
        val numPairs = Integer.parseInt(command[6])
        val knn = KnnBriefClassifier(k, project, manager, width, height, numPairs)
        knn.addListeners(listeners)
        createClassifier(knn,
            "Activating BRIEF kNN classifier; k=$k; project = $project; numPairs = $numPairs\n")
    } else {
        simpleResult(CommandType.ERROR, "Project $project does not exist")
    }
}

fun interpretKnn(command: List<String>, manager: FileManager, listeners: List<ClassifierListener>): InterpreterResult {
    val k = Integer.parseInt(command[2])
    val project = command[3]
    return if (manager.projectExists(project)) {
        val width = Integer.parseInt(command[4])
        val height = Integer.parseInt(command[5])
        val knn = KnnClassifier(k, project, manager, width, height)
        knn.addListeners(listeners)
        createClassifier(knn, "Activating kNN classifer; k=$k; project=$project\n")
    } else {
        simpleResult(CommandType.ERROR,"Project $project does not exist")
    }
}