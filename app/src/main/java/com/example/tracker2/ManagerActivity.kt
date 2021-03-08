package com.example.tracker2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_manager.*
import java.io.File

class ManagerActivity : FileAccessActivity() {
    var files: FileLoop = FileLoop()
    lateinit var manager: FileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager)

        files.refresh(outputDir)
        manager = FileManager(outputDir)
        if (manager.allProjects().isEmpty()) {
            manager.addProject(manager.makeProjectName())
        }

        showCurrentFile()

        selected_project.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, manager.allProjects())
        selected_project.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                // Intentionally left blank
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selected_label.adapter = ArrayAdapter(baseContext, android.R.layout.simple_spinner_item, manager.allLabelsIn(p0!!.getItemAtPosition(p2).toString()))
            }
        }

        go_to_image_button.setOnClickListener {
            startActivity(Intent(this@ManagerActivity, MainActivity::class.java))
        }

        go_to_project_label.setOnClickListener {
            startActivity(Intent(this@ManagerActivity, LabelProjectActivity::class.java))
        }

        go_to_test_button.setOnClickListener {
            startActivity(Intent(this@ManagerActivity, TestActivity::class.java))
        }

        view_unclassified.isChecked = true
        view_unclassified.setOnCheckedChangeListener { _, b -> refreshFiles(b) }

        left_picture_button.setOnClickListener { files.prev(); showCurrentFile() }
        right_picture_button.setOnClickListener { files.next(); showCurrentFile() }

        pick_view.setOnClickListener { files.refresh(selectedDir()); showCurrentFile() }

        move_picture_button.setOnClickListener {
            if (files.currentFile() != null) {
                manager.moveFileTo(files.currentFile()!!, projectName(), labelName())
                refreshFiles(view_unclassified.isChecked)
            }
        }

        floor_sample.setOnCheckedChangeListener { _, b -> showCurrentFile() }

        delete_picture_button.setOnClickListener { files.currentFile()?.delete(); showCurrentFile() }
    }

    private fun viewingDir(viewingUnclassified: Boolean): File {
        return if (viewingUnclassified) {outputDir} else {selectedDir()}
    }

    private fun refreshFiles(viewingUnclassified: Boolean) {
        files.refresh(viewingDir(viewingUnclassified))
        showCurrentFile()
    }

    private fun safelyString(value: Any?): String {
        if (value == null) {
            return "[None]"
        } else {
            return value.toString();
        }
    }

    private fun projectName(): String {
        return safelyString(selected_project.selectedItem)
    }

    private fun labelName(): String {
        return safelyString(selected_label.selectedItem)
    }

    private fun selectedDir(): File {
        return manager.labelDir(projectName(), labelName())
    }

    private fun showCurrentFile() {
        Log.i("ManagerActivity", "showCurrentFile()")
        photo_directory.text = "${viewingDir(view_unclassified.isChecked).toString()}${File.separatorChar}${files.currentName()}"
        if (files.currentImage() == null) {
            current_image.setImageResource(android.R.color.darker_gray)
            photo_filename.text = "None"
        } else {
            val width = files.currentImage()!!.width
            val height = files.currentImage()!!.height
            val index = if (files.i == 0) {files.size()} else {files.i}
            val category = if (view_unclassified.isChecked) {"Unclassified"} else {"${projectName()}:${labelName()}"}
            photo_filename.text = "$category ($index/${files.size()}) ${width}x${height}"
            current_image.setImageBitmap(files.currentImage())
            if (floor_sample.isChecked) {
                rectangle_overlay.addOverlayer(RectangleOverlayer(arrayListOf(::get_floor_rect, ::get_upper_left_rect, ::get_upper_right_rect)))
                Log.i("ManagerActivity", "Adding rectangle overlay")
            } else {
                rectangle_overlay.clearOverlayers()
                Log.i("ManagerActivity", "Removing rectangle overlay")
            }
            runOnUiThread { rectangle_overlay.invalidate() }
        }
    }
}