package com.example.tracker2

import java.io.File

class FileManager(val baseDir: File) {
    fun allProjects(): List<String> {
        return baseDir.listFiles()!!.filter { it.isDirectory }.map { it.name }
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
}