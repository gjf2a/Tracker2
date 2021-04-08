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

fun suffixStartingAnyOf(candidates: List<String>, msg: String): String {
    for (candidate in candidates) {
        val suffix = suffixStartingWith(candidate, msg)
        if (suffix.isNotEmpty()) {
            return suffix
        }
    }
    return ""
}

interface ClassifierListener {
    fun receiveClassification(msg: String)
}

enum class CommandType {
    CREATE_CLASSIFIER, PAUSE_CLASSIFIER, COMMENT, RESUME_CLASSIFIER, SPEAK, MESSAGE, ERROR
}

data class InterpreterResult(val cmdType: CommandType, val classifier: BitmapClassifier, val resumeIndex: Int, val msg: String)

fun simpleResult(cmd: CommandType, msg: String) =
    InterpreterResult(cmd, DummyClassifier(), 0, msg)

fun relayResult(cmd: CommandType, command: List<String>) =
    simpleResult(cmd, command.subList(1, command.size).joinToString(separator = " "))

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
        return if (comment.isNotEmpty()) {
            simpleResult(CommandType.COMMENT, comment)
        } else {
            val command = suffixStartingAnyOf(listOf("cv ", "say "), msg.trim()).split(" ")
            if (command.isNotEmpty()) {
                when {
                    command[0] == "cv" -> {
                        when {
                            command.size == 6 && command[1] == "knn" -> interpretKnn(command, manager, listeners)
                            command.size == 7 && command[1] == "knn_brief" -> interpretBrief(command, manager, listeners)
                            command.size == 6 && command[1] == "kmeans" -> interpretKmeans(command, manager, listeners)
                            command.size >= 8 && command[1] == "groundline" -> interpretGroundline(::GroundlineKmeans, command, manager, listeners)
                            command.size >= 19 && command[1] == "particle" -> interpretParticle(command, manager, listeners)
                            command.size == 2 && command[1] == "pause" -> simpleResult(CommandType.PAUSE_CLASSIFIER, "Classifier paused")
                            command.size == 3 && command[1] == "resume" -> resumeResult(Integer.parseInt(command[2]))
                            else -> simpleResult(CommandType.ERROR, "Unrecognized cv cmd: '$command'")
                        }
                    }
                    command[0] == "say" -> relayResult(CommandType.SPEAK, command)
                    command[0] == "msg" -> relayResult(CommandType.MESSAGE, command)
                    else -> {
                        simpleResult(CommandType.ERROR, "Unrecognized cmd type: '${command[0]}'")
                    }
                }
            } else {
                simpleResult(CommandType.ERROR,"Unrecognized cmd: '$command'")
            }
        }
    } catch (nfe: NumberFormatException) {
        return simpleResult(CommandType.ERROR, "Integer expected")
    }
}

fun interpretParticle(command: List<String>, manager: FileManager, listeners: List<ClassifierListener>): InterpreterResult {
    val maxColors = command[2].toInt()
    val minNotFloor = command[3].toInt()
    val maxJump = command[4].toInt()
    val numParticles = command[5].toInt()
    val noiseMin = PolarCoord(command[6].toDouble(), command[7].toDouble())
    val noiseMax = PolarCoord(command[8].toDouble(), command[9].toDouble())
    val cellsPerMeter = command[10].toDouble()
    val width = command[11].toInt()
    val height = command[12].toInt()
    val meter1 = CalibrationLine(command[14].toInt(), command[13].toInt())
    val meter2 = CalibrationLine(command[16].toInt(), command[15].toInt())
    val project = command[17]
    if (manager.projectExists(project)) {
        val label = command[18]
        if (manager.labelExists(project, label)) {
            val choices = ArrayList<Bitmap>()
            for (i in getPhotoNumbers(command, 19, project, label, manager)) {
                choices.add(manager.loadImage(project, label, i, width, height))
            }
            val particle = ParticleFilterClassifier(choices, maxColors, minNotFloor, maxJump,
                numParticles, noiseMin, noiseMax, cellsPerMeter, meter1, meter2)
            particle.addListeners(listeners)
            return createClassifier(particle, "Activating Particle Filter: maxColors=$maxColors minNotFloor=$minNotFloor maxJump=$maxJump numParticles=$numParticles noise=($noiseMin,$noiseMax) cellsPerMeter=$cellsPerMeter img:(${width}x${height} meter1=$meter1 meter2=$meter2")
        } else {
            return simpleResult(CommandType.ERROR,"Label '$label' does not exist")
        }
    } else {
        return simpleResult(CommandType.ERROR, "Project '$project' does not exist")
    }
}

fun interpretKmeans(command: List<String>, manager: FileManager, listeners: List<ClassifierListener>): InterpreterResult {
    val k = Integer.parseInt(command[2])
    val project = command[3]
    return if (manager.projectExists(project)) {
        val width = Integer.parseInt(command[4])
        val height = Integer.parseInt(command[5])
        val kmeans = KmeansBitmapClassifier(k, project, manager, width, height)
        kmeans.addListeners(listeners)
        createClassifier(kmeans, "Activating kMeans classifer; k=$k; project=$project\n")
    } else {
        simpleResult(CommandType.ERROR,"Project $project does not exist")
    }
}

fun <C: SimpleClassifier<ColorTriple, Boolean>, G: Groundline<C>> interpretGroundline
            (makeGroundline: (ArrayList<Bitmap>, Int, Int, Int)->G,
             command: List<String>,
             manager: FileManager,
             listeners: List<ClassifierListener>): InterpreterResult {
    val maxColors = Integer.parseInt(command[2])
    val minNotFloor = Integer.parseInt(command[3])
    val project = command[4]
    if (manager.projectExists(project)) {
        val label = command[5]
        if (manager.labelExists(project, label)) {
            val width = Integer.parseInt(command[6])
            val height = Integer.parseInt(command[7])
            val choices = ArrayList<Bitmap>()
            var start = 8
            val maxJump = if (command.size >= 8 && command[8].startsWith("maxJump=")) {
                start = 9
                Integer.parseInt(command[8].split("=")[1])
            } else {
                height + 1
            }
            for (i in getPhotoNumbers(command, start, project, label, manager)) {
                choices.add(manager.loadImage(project, label, i, width, height))
            }
            val groundline = makeGroundline(choices, maxColors, minNotFloor, maxJump)
            groundline.addListeners(listeners)
            return createClassifier(groundline,
                "Activating Groundline: $maxColors $project $label (${width}x${height})")
        } else {
            return simpleResult(CommandType.ERROR,"Label '$label' does not exist")
        }
    } else {
        return simpleResult(CommandType.ERROR, "Project '$project' does not exist")
    }
}

fun getPhotoNumbers(command: List<String>, start: Int, project: String, label: String, manager: FileManager): ArrayList<Int> {
    val result = ArrayList<Int>()
    if (command.size > start) {
        for (i in start until command.size) {
            result.add(Integer.parseInt(command[i]))
        }
    } else {
        for (i in 1..manager.imageFilesFor(project, label).size) {
            result.add(i)
        }
    }
    return result
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