package com.example.tracker2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class CalibrationActivity : FileAccessActivity() {
    var files: FileLoop = FileLoop()
    lateinit var manager: FileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)
    }
}
