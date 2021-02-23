package com.example.tracker2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

class FileLoop {
    internal var files: ArrayList<File> = ArrayList()
    var i: Int = 0

    fun refresh(dir: File) {
        files.clear()

        for (file in dir.listFiles()!!) {
            if (file.extension == "jpg") {
                files.add(file)
            }
        }

        if (files.size > 0) {
            i %= files.size
        }
    }

    fun empty(): Boolean {
        return files.size == 0
    }

    fun next() {
        if (!empty()) i = (i + 1) % files.size
    }

    fun prev() {
        if (!empty()) i = (i + files.size - 1) % files.size
    }

    fun currentName(): String {
        return if (empty()) {
            "No files present"
        } else {
            files[i].name
        }
    }

    fun currentFile(): File? {
        return if (empty()) {
            null
        } else {
            files[i]
        }
    }

    fun currentImage(): Bitmap? {
        return if (empty()) {
            null
        } else {
            BitmapFactory.decodeFile(files[i].toString())
        }
    }
}