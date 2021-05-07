package com.example.tracker2

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_log_view.*

class LogViewActivity : FileAccessActivity() {

    lateinit var logger: Logger;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)

        logger = Logger(outputDir)

        log_viewer.append(logger.get())
    }
}
