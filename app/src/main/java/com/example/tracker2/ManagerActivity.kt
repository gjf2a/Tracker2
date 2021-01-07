package com.example.tracker2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_manager.*

class ManagerActivity : FileAccessActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager)
        go_to_image_button.setOnClickListener {
            startActivity(Intent(this@ManagerActivity, MainActivity::class.java))
        }

        left_picture_button.setOnClickListener { Log.i("GJF", "Left button click") }
        right_picture_button.setOnClickListener { Log.i("GJF", "Right button click") }
    }


}