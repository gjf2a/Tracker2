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

    fun next() {
        i = (i + 1) % files.size
    }

    fun prev() {
        i = (i + files.size - 1) % files.size
    }

    fun currentName(): File {
        return files[i]
    }

    fun currentImage(): Bitmap {
        return BitmapFactory.decodeFile(files[i].toString())
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
        filename_text.text = files.currentName().name
        current_image.setImageBitmap(files.currentImage())
    }

    fun showFiles() {
        for (file in files.files) {
            Log.i("GJF", "File:" + file.name)
        }
    }
}