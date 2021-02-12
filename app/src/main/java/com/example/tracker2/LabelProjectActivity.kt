package com.example.tracker2

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_label_project.*
import java.io.File

class LabelProjectActivity : FileAccessActivity() {
    lateinit var manager: FileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_project)

        manager = FileManager(outputDir)

        back_to_manager_button.setOnClickListener {
            startActivity(Intent(this@LabelProjectActivity, ManagerActivity::class.java))
        }

        new_project_button.setOnClickListener { manager.addProject("dummy") }
    }



    private fun project_name(): String {
        return current_project.selectedItem.toString()
    }

    private fun label_name(): String {
        return current_label.selectedItem.toString()
    }

    private fun selected_dir(): File {
        return manager.labelDir(project_name(), label_name())
    }
}