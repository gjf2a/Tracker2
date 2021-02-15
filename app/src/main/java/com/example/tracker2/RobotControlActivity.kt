package com.example.tracker2

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_robot_control.*


class RobotControlActivity : AppCompatActivity() {
    lateinit var talker: ArduinoTalker

    private fun makeConnection() {
        log.append("Attempting to connect...\n")
        talker = ArduinoTalker(this@RobotControlActivity.getSystemService(Context.USB_SERVICE) as UsbManager)
        if (talker.connected()) {
            log.append("Connected\n")
        } else {
            log.append("Not connected\n")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_robot_control)
        log.append("Log\n")

        robot2manager.setOnClickListener {
            startActivity(Intent(this@RobotControlActivity, ManagerActivity::class.java))
        }

        connect.setOnClickListener {
            makeConnection()
        }

        makeConnection()
    }
}
