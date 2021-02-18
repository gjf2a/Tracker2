package com.example.tracker2

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_robot_control.*
import java.lang.Exception


const val START_BYTE: Byte = -1
const val STOP_BYTE: Byte = -2


class RobotControlActivity : AppCompatActivity(), LineListener {
    lateinit var talker: ArduinoTalker
    lateinit var reader: LineReader

    private fun makeConnection() {
        log.append("Attempting to connect...\n")
        talker = ArduinoTalker(this@RobotControlActivity.getSystemService(Context.USB_SERVICE) as UsbManager)
        if (talker.connected()) {
            log.append("Connected\n")
            reader = LineReader(talker)
            reader.addListener(this)
            reader.start()
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

        start_robot.setOnClickListener { safeSend(START_BYTE) }
        stop_robot.setOnClickListener { safeSend(STOP_BYTE) }

        makeConnection()
    }

    private fun safeSend(b: Byte) {
        try {
            talker.sendOneByte(b)
        } catch (e: Exception) {
            log.append("Exception when sending $b: $e\n")
        }
    }

    override fun receive(line: String) {
        this@RobotControlActivity.runOnUiThread {
            log.append(line)
            scroller.post { scroller.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
