package com.example.tracker2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_test.*
import java.io.File

class DummyTarget : ClassifierListener {
    override fun receiveClassification(msg: String) = 0
}

const val COMMAND_FLAG: String = "COMMAND"

class TestActivity : FileAccessActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        setupHistory();

        log_test.append("Log\n")

        test_to_manager.setOnClickListener {
            startActivity(Intent(this@TestActivity, ManagerActivity::class.java))
        }

        test_to_robot.setOnClickListener {
            val intent = Intent(this@TestActivity, MainActivity::class.java)
            if (command_tester.text.isEmpty()) {useOldText()}
            if (command_tester.text.isNotEmpty()) {
                val command = command_tester.text.toString()
                updateHistory(command)
                intent.putExtra(COMMAND_FLAG, command)
            }
            startActivity(intent)
        }

        use_old.setOnClickListener {
            useOldText()
        }

        run_test.setOnClickListener {
            log_test.append("Interpreting...\n")
            if (command_tester.text.isEmpty()) {useOldText()}
            val command = command_tester.text.toString()

            val result = interpret(command, outputDir, arrayListOf(DummyTarget()))
            log_test.append(result.cmdType.toString() + '\n')
            log_test.append(result.msg + '\n')
            if (result.cmdType == CommandType.CREATE_CLASSIFIER) {
                updateHistory(command)
                val assessment = result.classifier.assess()
                log_test.append(assessment.trim() + '\n')
            }

            scroller_test.post { scroller_test.fullScroll(View.FOCUS_DOWN) }
        }

        resetHistorySpinner()
    }

    private fun useOldText() {
        command_tester.text.clear()
        command_tester.text.insert(0, old_commands.selectedItem.toString())
    }

    private fun updateHistory(cmd: String) {
        history.add(cmd)
        val items = resetHistorySpinner()
        old_commands.setSelection(items.indexOf(cmd))
    }

    private fun resetHistorySpinner(): List<String> {
        val items = history.mostPopular()
        old_commands.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        return items
    }
}
