package com.example.tracker2

import android.content.Intent
import android.os.Bundle
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

        view_unclassified.isChecked = true
        view_unclassified.setOnCheckedChangeListener { compoundButton, b -> files.refresh(if (b) {outputDir} else {selected_dir()}) }

        left_picture_button.setOnClickListener { files.prev(); showCurrentFile() }
        right_picture_button.setOnClickListener { files.next(); showCurrentFile() }

        pick_view.setOnClickListener { files.refresh(selected_dir()) }

        move_picture_button.setOnClickListener {
            if (files.currentFile() != null) {
                manager.moveFileTo(files.currentFile()!!, project_name(), label_name())
            }
        }

        delete_picture_button.setOnClickListener { files.currentFile()?.delete(); showCurrentFile() }
    }

    private fun project_name(): String {
        return selected_project.selectedItem.toString()
    }

    private fun label_name(): String {
        return selected_label.selectedItem.toString()
    }

    private fun selected_dir(): File {
        return manager.labelDir(project_name(), label_name())
    }

    fun showCurrentFile() {
        filename_text.text = files.currentName()
        if (files.currentImage() == null) {
            current_image.setImageResource(android.R.color.black)
        } else {
            current_image.setImageBitmap(files.currentImage())
        }
    }
}