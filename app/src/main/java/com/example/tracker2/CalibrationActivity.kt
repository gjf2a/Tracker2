package com.example.tracker2

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_calibration.*

const val CALIBRATION_DELTA = 2
const val CALIBRATION_MAX = 100

fun scale2float(value: Int, targetRange: Int): Float = (value * targetRange).toFloat() / CALIBRATION_MAX
fun scale2int(value: Int, targetRange: Int): Int = (value * targetRange) / CALIBRATION_MAX

data class CalibrationLine(var height: Int, var width: Int) {
    fun up() {
        height = (height - CALIBRATION_DELTA).coerceAtLeast(0)
    }

    fun down() {
        height = (height + CALIBRATION_DELTA).coerceAtMost(CALIBRATION_MAX)
    }

    fun widen() {
        width = (width + CALIBRATION_DELTA).coerceAtMost(CALIBRATION_MAX)
    }

    fun shorten() {
        width = (width - CALIBRATION_DELTA).coerceAtLeast(CALIBRATION_DELTA)
    }

    fun xLeft(): Int = (CALIBRATION_MAX - width) / 2
    fun xRight(): Int = xLeft() + width

    fun draw(canvas: Canvas, paint: Paint) {
        val scaledWidth = scale2float(width, canvas.width)
        val x1 = (canvas.width - scaledWidth) / 2
        val x2 = x1 + scaledWidth
        val y = scale2float(height, canvas.height)
        canvas.drawLine(x1, y, x2, y, paint)
    }
}

class CalibrationOverlayer(val meter1: CalibrationLine, val meter2: CalibrationLine) : Overlayer {
    private val paint1 =
        Paint().apply {
            isAntiAlias = true
            color = Color.CYAN
            style = Paint.Style.STROKE
        }

    private val paint2 =
        Paint().apply {
            isAntiAlias = true
            color = Color.RED
            style = Paint.Style.STROKE
        }

    override fun draw(canvas: Canvas) {
        meter1.draw(canvas, paint1)
        meter2.draw(canvas, paint2)
    }
}

class CalibrationActivity : FileAccessActivity() {
    lateinit var photos: PhotoManager
    private val lines = arrayOf(CalibrationLine(70, 50), CalibrationLine(30, 50))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        calibration_overlay.addOverlayer(CalibrationOverlayer(lines[0], lines[1]))

        photos = PhotoManager(calibration_image, left_calibration, right_calibration,
            pick_calibration_view, photo_directory_calibration, view_calibration_unclassified,
            selected_calibration_project, selected_calibration_label, photo_calibration_filename,
            outputDir) {runOnUiThread {it()}}

        photos.setup(this, baseContext)
        photos.showCurrentFile()

        calibration_to_manager.setOnClickListener {
            startActivity(Intent(this@CalibrationActivity, ManagerActivity::class.java))
        }

        line_up.setOnClickListener { update { it.up() } }
        line_down.setOnClickListener { update {it.down()} }
        line_short.setOnClickListener { update {it.shorten()} }
        line_wide.setOnClickListener { update {it.widen()} }

        line_choice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                refresh(position)
            }
        }

        refresh(line_choice.selectedItemPosition)
    }

    fun update(updater: (CalibrationLine) -> Unit) {
        val pos = line_choice.selectedItemPosition
        updater(lines[pos])
        refresh(pos)
    }

    fun refresh(position: Int) {
        calibration_width.text = "Width: ${lines[position].width}"
        calibration_height.text = "Height: ${lines[position].height}"
        calibration_overlay.invalidate()
    }
}
