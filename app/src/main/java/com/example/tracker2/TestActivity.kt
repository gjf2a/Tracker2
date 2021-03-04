package com.example.tracker2

import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_test.*

class DummyMessage : MessageTarget {
    override fun sendString(msg: String): Boolean {
        return true
    }
}

const val COMMAND_FLAG: String = "COMMAND"

class TestActivity : FileAccessActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        log_test.append("Log\n")

        test_to_manager.setOnClickListener {
            startActivity(Intent(this@TestActivity, ManagerActivity::class.java))
        }

        test_to_robot.setOnClickListener {
            val intent = Intent(this@TestActivity, MainActivity::class.java)
            if (command_tester.text.isNotEmpty()) {
                intent.putExtra(COMMAND_FLAG, command_tester.text.toString())
            }
            startActivity(intent)
        }

        run_test.setOnClickListener {
            log_test.append("Interpreting...\n")
            val result = interpret(command_tester.text.toString(), outputDir, DummyMessage())
            log_test.append(result.cmdType.toString() + '\n')
            log_test.append(result.msg + '\n')
            if (result.cmdType == CommandType.CREATE_CLASSIFIER) {
                val assessment = result.classifier.assess()
                log_test.append(assessment.trim() + '\n')
            }

            scroller_test.post { scroller_test.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
