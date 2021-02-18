package com.example.tracker2

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_robot_control.*
import java.lang.Exception


const val START: String = "start"
const val STOP: String = "stop"


class RobotControlActivity : AppCompatActivity(), TextListener {
    lateinit var talker: ArduinoTalker
    lateinit var reader: TextReader

    private fun makeConnection() {
        log.append("Attempting to connect...\n")
        talker = ArduinoTalker(this@RobotControlActivity.getSystemService(Context.USB_SERVICE) as UsbManager)
        if (talker.connected()) {
            log.append("Connected\n")
            reader = TextReader(talker)
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

        start_robot.setOnClickListener { safeSend(START) }
        stop_robot.setOnClickListener { safeSend(STOP) }

        makeConnection()
    }

    private fun safeSend(msg: String) {
        try {
            if (talker.sendString(msg)) {
                log.append(">$msg\n")
            }
        } catch (e: Exception) {
            log.append("Exception when sending '$msg': $e\n")
        }
    }

    override fun receive(text: String) {
        this@RobotControlActivity.runOnUiThread {
            log.append(text)
            scroller.post { scroller.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
