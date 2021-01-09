package com.example.tracker2

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_manager.*
import java.io.File

class FileLoop {
    internal var files: ArrayList<File> = ArrayList()
    var i: Int = 0

    fun refresh(dir: File) {
        files.clear()
        dir.walkTopDown().filter { it.extension == "jpg" }.toCollection(files)
        i %= files.size
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
        if (empty()) {
            return "No files present"
        } else {
            return files[i].name
        }
    }

    fun currentImage(): Bitmap? {
        if (empty()) {
            return null
        } else {
            return BitmapFactory.decodeFile(files[i].toString())
        }
    }
}

class ManagerActivity : FileAccessActivity() {
    var files: FileLoop = FileLoop()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager)

        files.refresh(outputDir)
        showCurrentFile()

        go_to_image_button.setOnClickListener {
            startActivity(Intent(this@ManagerActivity, MainActivity::class.java))
        }

        left_picture_button.setOnClickListener { files.prev(); showCurrentFile() }
        right_picture_button.setOnClickListener { files.next(); showCurrentFile() }
    }

    fun showCurrentFile() {
        filename_text.text = files.currentName()
        files.currentImage()?.let { current_image.setImageBitmap(it) }
    }
}