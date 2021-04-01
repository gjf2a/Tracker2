package com.example.tracker2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_manager.*
import java.io.File

class PhotoManager(
    val currentImage: ImageView,
    val goLeft: Button,
    val goRight: Button,
    val pickView: Button,
    val photoDir: TextView,
    val viewUnclassified: CheckBox,
    val selectedProject: Spinner,
    val selectedLabel: Spinner,
    val photoFile: TextView,
    val outputDir: File,
    val uiRunner: (() -> Unit) -> Unit
) {

    var files: FileLoop = FileLoop()
    val manager: FileManager = FileManager(outputDir)

    init {
        files.refresh(outputDir)
        if (manager.allProjects().isEmpty()) {
            manager.addProject(manager.makeProjectName())
        }
    }

    fun setup(activity: AppCompatActivity, baseContext: Context) {
        selectedProject.adapter =
            ArrayAdapter(activity, android.R.layout.simple_spinner_item, manager.allProjects())
        selectedProject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                // Intentionally left blank
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedLabel.adapter = ArrayAdapter(
                    baseContext,
                    android.R.layout.simple_spinner_item,
                    manager.allLabelsIn(p0!!.getItemAtPosition(p2).toString())
                )
            }
        }

        viewUnclassified.isChecked = true
        viewUnclassified.setOnCheckedChangeListener { _, b -> refreshFiles(b) }

        goLeft.setOnClickListener { files.prev(); showCurrentFile() }
        goRight.setOnClickListener { files.next(); showCurrentFile() }

        pickView.setOnClickListener { files.refresh(selectedDir()); showCurrentFile() }
    }

    fun refreshFiles(viewingUnclassified: Boolean) {
        files.refresh(viewingDir(viewingUnclassified))
        showCurrentFile()
    }

    private fun viewingDir(viewingUnclassified: Boolean): File {
        return if (viewingUnclassified) {outputDir} else {selectedDir()}
    }

    private fun selectedDir(): File {
        return manager.labelDir(projectName(), labelName())
    }

    fun projectName(): String {
        return safelyString(selectedProject.selectedItem)
    }

    fun labelName(): String {
        return safelyString(selectedLabel.selectedItem)
    }

    private fun safelyString(value: Any?): String {
        if (value == null) {
            return "[None]"
        } else {
            return value.toString();
        }
    }

    fun showCurrentFile() {
        uiRunner {
            photoDir.text =
                "${viewingDir(viewUnclassified.isChecked).toString()}${File.separatorChar}${files.currentName()}"
            if (files.currentImage() == null) {
                currentImage.setImageResource(android.R.color.darker_gray)
                photoFile.text = "None"
            } else {
                val width = files.currentImage()!!.width
                val height = files.currentImage()!!.height
                val index = if (files.i == 0) {
                    files.size()
                } else {
                    files.i
                }
                val category = if (viewUnclassified.isChecked) {
                    "Unclassified"
                } else {
                    "${projectName()}:${labelName()}"
                }
                photoFile.text = "$category ($index/${files.size()}) ${width}x${height}"
                currentImage.setImageBitmap(files.currentImage())
            }
        }
    }
}

class ManagerActivity : FileAccessActivity() {
    lateinit var photos: PhotoManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager)

        photos = PhotoManager(
            current_image, left_picture_button, right_picture_button,
            pick_view, photo_directory, view_unclassified, selected_project, selected_label,
            photo_filename, outputDir, {
                runOnUiThread {
                    if (floor_sample.isChecked) {
                        rectangle_overlay.addOverlayer(
                            RectangleOverlayer(
                                arrayListOf(
                                    ::getFloorRect,
                                    ::getUpperLeftRect,
                                    ::getUpperRightRect
                                )
                            )
                        )
                        Log.i("ManagerActivity", "Adding rectangle overlay")
                    } else {
                        rectangle_overlay.clearOverlayers()
                        Log.i("ManagerActivity", "Removing rectangle overlay")
                    }
                    it()
                    rectangle_overlay.invalidate()
                }})
        photos.setup(this, baseContext)
        photos.showCurrentFile()

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
        view_unclassified.setOnCheckedChangeListener { _, b -> photos.refreshFiles(b) }

        move_picture_button.setOnClickListener {
            if (photos.files.currentFile() != null) {
                photos.manager.moveFileTo(photos.files.currentFile()!!, photos.projectName(), photos.labelName())
                photos.refreshFiles(view_unclassified.isChecked)
            }
        }

        floor_sample.setOnCheckedChangeListener { _, b -> photos.showCurrentFile() }

        delete_picture_button.setOnClickListener { photos.files.currentFile()?.delete(); photos.showCurrentFile() }
    }
}