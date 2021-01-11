package com.example.tracker2

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_label_project.*

class LabelProjectActivity : FileAccessActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_project)

        back_to_manager_button.setOnClickListener {
            startActivity(Intent(this@LabelProjectActivity, ManagerActivity::class.java))
        }
    }
}