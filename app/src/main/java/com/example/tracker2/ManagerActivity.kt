package com.example.tracker2

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_manager.*
import java.io.File

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

        go_to_project_label.setOnClickListener {
            startActivity(Intent(this@ManagerActivity, LabelProjectActivity::class.java))
        }

        left_picture_button.setOnClickListener { files.prev(); showCurrentFile() }
        right_picture_button.setOnClickListener { files.next(); showCurrentFile() }
    }

    fun showCurrentFile() {
        filename_text.text = files.currentName()
        files.currentImage()?.let { current_image.setImageBitmap(it) }
    }
}