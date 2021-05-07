package com.example.tracker2

import android.util.Log
import com.example.tracker2.MainActivity.Companion.TAG
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter


const val LOG_NAME = "_log"

class Logger(outputDir: File) {
    private val logFile = File("$outputDir${File.separator}$LOG_NAME")
    var active = true

    fun clear() {
        logFile.delete()
    }

    fun log(s: String) {
        if (active) {
            val writer = PrintWriter(FileWriter(logFile, true))
            writer.println("[$s]")
            writer.close()
        }
    }

    fun get() = logFile.readText()
}