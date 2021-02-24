package com.example.tracker2

import android.util.Log
import java.io.File

const val PROJECT_PREFIX = "project"
const val LABEL_PREFIX = "label"

fun nextNameFrom(items: List<String>, prefix: String): String {
    val sorted = items.filter { it.startsWith(prefix) }.sorted()
    Log.i("FileManager", "$sorted")
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

    fun moveFileTo(file: File, projectName: String, label: String) {
        addLabel(projectName, label)
        val targetDir = labelDir(projectName, label)
        assert(targetDir.exists())
        val target = File(targetDir, file.name)
        assert(file.renameTo(target))
    }

    fun deleteLabel(projectName: String, label: String): Boolean {
        val allGone = labelDir(projectName, label).listFiles()!!.all {println("Trying to delete ${it}"); it.delete() }
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