package com.example.tracker2

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_label_project.*
import java.io.File

class LabelProjectActivity : FileAccessActivity() {
    lateinit var manager: FileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_project)

        manager = FileManager(outputDir)

        refreshSpinners()

        back_to_manager_button.setOnClickListener {
            startActivity(Intent(this@LabelProjectActivity, ManagerActivity::class.java))
        }

        new_project_button.setOnClickListener {
            manager.addProject(manager.makeProjectName())
            refreshSpinners()
        }

        new_label_button.setOnClickListener {
            manager.addLabel(projectName(), manager.makeLabelName(projectName()))
            refreshSpinners()
        }

        delete_project_button.setOnClickListener {
            manager.deleteProject(projectName())
            refreshSpinners()
        }

        delete_label_button.setOnClickListener {
            manager.deleteLabel(projectName(), labelName())
            refreshSpinners()
        }
    }

    private fun refreshSpinners() {
        current_project.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, manager.allProjects())
        current_label.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, manager.allLabelsIn(projectName()))
    }

    private fun projectName(): String {
        return current_project.selectedItem.toString()
    }

    private fun labelName(): String {
        return current_label.selectedItem.toString()
    }

    private fun selectedDir(): File {
        return manager.labelDir(projectName(), labelName())
    }
}