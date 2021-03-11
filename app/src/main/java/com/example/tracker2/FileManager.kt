package com.example.tracker2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

const val PROJECT_PREFIX = "project"
const val LABEL_PREFIX = "label"

fun nextNameFrom(items: List<String>, prefix: String): String {
    val sorted = items.filter { it.startsWith(prefix) }.sorted()
    return if (sorted.isEmpty()) {
        "${prefix}1"
    } else {
        val number = Integer.parseInt(sorted.last().substring(prefix.length)) + 1
        "${prefix}${number}"
    }
}

class FileManager(var baseDir: File) {
    fun allProjects(): List<String> {
        return baseDir.listFiles()!!.filter { it.isDirectory }.map { it.name }
    }

    fun makeProjectName(): String {
        return nextNameFrom(allProjects(), PROJECT_PREFIX)
    }

    fun makeLabelName(project: String): String {
        return nextNameFrom(allLabelsIn(project), LABEL_PREFIX)
    }

    fun projectExists(projectName: String): Boolean {
        return File(baseDir, projectName).exists()
    }

    fun labelExists(projectName: String, labelName: String): Boolean {
        return projectExists(projectName) && File(projectDir(projectName), labelName).exists()
    }

    fun projectDir(projectName: String): File {
        return File(baseDir, projectName)
    }

    fun labelDir(projectName: String, label: String): File {
        return File(projectDir(projectName), label)
    }

    fun allLabelsIn(projectName: String): List<String> {
        return projectDir(projectName).listFiles()!!.map { it.name }
    }

    fun addProject(projectName: String) {
        if (!projectExists(projectName)) {
            val newProject = File(baseDir, projectName)
            newProject.mkdir()
            addLabel(projectName, makeLabelName(projectName))
            addLabel(projectName, makeLabelName(projectName))
        }
    }

    fun addLabel(projectName: String, label: String) {
        addProject(projectName)
        if (!labelExists(projectName, label)) {
            val newLabel = File(projectDir(projectName), label)
            newLabel.mkdir()
        }
    }

    fun imageFilesFor(projectName: String, label: String): ArrayList<File> {
        val result = ArrayList<File>()
        for (file in labelDir(projectName, label).listFiles()!!.filter { it.extension == "jpg" }) {
            result.add(file)
        }
        return result
    }

    fun numImagesFor(projectName: String, label: String): Int {
        return imageFilesFor(projectName, label).size
    }

    fun loadImage(projectName: String, label: String, index: Int, scaledWidth: Int, scaledHeight: Int): Bitmap {
        val i = index % numImagesFor(projectName, label)
        return bitmapFor(imageFilesFor(projectName, label)[i], scaledWidth, scaledHeight)
    }

    fun bitmapFor(imageFile: File, scaledWidth: Int, scaledHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(BitmapFactory.decodeFile(imageFile.path), scaledWidth, scaledHeight, false)
    }

    fun allProjectImages(projectName: String, scaledWidth: Int, scaledHeight: Int): ArrayList<Pair<Bitmap,String>> {
        val result = ArrayList<Pair<Bitmap,String>>()
        for (label in allLabelsIn(projectName)) {
            for (imageFile in imageFilesFor(projectName, label)) {
                result.add(Pair(bitmapFor(imageFile, scaledWidth, scaledHeight), label))
            }
        }
        return result
    }

    fun moveFileTo(file: File, projectName: String, label: String) {
        addLabel(projectName, label)
        val targetDir = labelDir(projectName, label)
        assert(targetDir.exists())
        val target = File(targetDir, file.name)
        assert(file.renameTo(target))
    }

    fun deleteLabel(projectName: String, label: String): Boolean {
        val allGone = labelDir(projectName, label).listFiles()!!.all {println("Trying to delete ${it}"); it.delete() }
        println("allGone: $allGone")
        return allGone && labelDir(projectName, label).delete()
    }

    fun deleteProject(projectName: String): Boolean {
        val allGone = projectDir(projectName).listFiles()!!.all {println("Trying to delete ${it}");  deleteLabel(projectName, it.name) }
        return allGone && projectDir(projectName).delete()
    }

    fun renameProject(oldName: String, newName: String) {
        projectDir(oldName).renameTo(projectDir(newName))
    }

    fun renameLabel(projectName: String, oldName: String, newName: String) {
        labelDir(projectName, oldName).renameTo(labelDir(projectName, newName))
    }
}