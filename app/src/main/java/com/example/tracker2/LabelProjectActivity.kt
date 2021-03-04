package com.example.tracker2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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

        current_project.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                refreshSpinnerFrom(current_label, manager.allLabelsIn(parent.getItemAtPosition(position).toString()))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // another interface callback
            }
        }

        new_project_button.setOnClickListener {
            manager.addProject(manager.makeProjectName())
            refreshSpinners()
        }

        rename_project_button.setOnClickListener {
            if (update_project_name.text.isNotEmpty()) {
                val newName = despacify(update_project_name.text.toString())
                if (newName.isNotEmpty()) {
                    manager.renameProject(projectName(), newName)
                    refreshSpinners()
                }
                update_project_name.text.clear()
            }
        }

        rename_label_button.setOnClickListener {
            if (update_label_name.text.isNotEmpty()) {
                val newName = despacify(update_label_name.text.toString())
                if (newName.isNotEmpty()) {
                    manager.renameLabel(projectName(), labelName(), newName)
                    refreshSpinners()
                }
                update_label_name.text.clear()
            }
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
        refreshSpinnerFrom(current_project, manager.allProjects())
        refreshSpinnerFrom(current_label, manager.allLabelsIn(projectName()))
    }

    private fun refreshSpinnerFrom(spinner: Spinner, items: List<String>) {
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
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

fun despacify(s: String): String {
    return s.trim().replace(' ', '_').replace('\n', '_')
}