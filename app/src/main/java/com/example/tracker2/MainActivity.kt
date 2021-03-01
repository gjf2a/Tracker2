package com.example.tracker2

// This program is derived from the example given at:
// https://codelabs.developers.google.com/codelabs/camerax-getting-started

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbManager
import android.media.Image
import android.net.Uri
import android.renderscript.*
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService

const val START: String = "start"
const val STOP: String = "stop"

open class FileAccessActivity : AppCompatActivity() {

    protected lateinit var outputDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        outputDir = getOutputDirectory()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
}

interface MessageReceiver {
    fun message(msg: String)
}

class MainActivity : FileAccessActivity(), TextListener, MessageReceiver {
    var imageCapture: ImageCapture? = null
    lateinit var cameraExecutor: ExecutorService
    lateinit var view: PreviewView
    lateinit var analyzer: BitmapAnalyzer1
    lateinit var talker: ArduinoTalker
    lateinit var reader: TextReader
    var incoming = MessageHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraSetup(viewFinder)

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener { takePhoto() }

        to_manager_button.setOnClickListener {
            startActivity(Intent(this@MainActivity, ManagerActivity::class.java))
        }

        log.append("Log\n")
        log.append("CPUS: ${Runtime.getRuntime().availableProcessors()}\n")

        start_robot.setOnClickListener { safeSend(START) }
        stop_robot.setOnClickListener { safeSend(STOP) }

        tester.setOnClickListener {
            findCommandsIn("cv knn 3 Gameroom 20 15\n")
        }

        makeConnection()
    }

    private fun makeConnection() {
        log.append("Attempting to connect...\n")
        talker = ArduinoTalker(this@MainActivity.getSystemService(Context.USB_SERVICE) as UsbManager)
        if (talker.connected()) {
            log.append("Connected\n")
            reader = TextReader(talker)
            reader.addListener(this)
            reader.start()
        } else {
            log.append("Not connected\n")
        }
    }

    private fun safeSend(msg: String) {
        try {
            if (talker.sendString(msg)) {
                log.append(">$msg\n")
            }
        } catch (e: Exception) {
            Log.i("MainActivity", "Exception when sending '$msg': $e")
            log.append("Exception when sending '$msg': $e\n")
        }
    }

    override fun receive(text: String) {
        this@MainActivity.runOnUiThread {
            findCommandsIn(text)
            log.append(text)
            scroller.post { scroller.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun findCommandsIn(text: String) {
        try {
            incoming.receive(text)
            val manager = FileManager(outputDir)
            for (message in incoming) {
                Log.i("MainActivity", "Processing '$message'")
                val command = message.trim().split(" ")
                if (command.isNotEmpty() && command[0] == "cv") {
                    if (command.size == 6 && command[1] == "knn") {
                        val k = Integer.parseInt(command[2])
                        val project = command[3]
                        if (manager.projectExists(project)) {
                            val width = Integer.parseInt(command[4])
                            val height = Integer.parseInt(command[5])
                            val knn = KnnClassifier(talker, k, project, manager, width, height)
                            val id = analyzer.addClassifier(knn)
                            Log.i(TAG, "Number of examples: ${knn.numExamples()}")
                            analyzer.resetFPSCalculation()
                            log.append("Activating kNN classifer; k=$k; project=$project")
                            talker.sendString("$id")
                        } else {
                            log.append("Project $project does not exist")
                        }
                    } else if (command.size == 7 && command[1] == "knn_brief") {
                        val k = Integer.parseInt(command[2])
                        val project = command[3]
                        if (manager.projectExists(project)) {
                            val width = Integer.parseInt(command[4])
                            val height = Integer.parseInt(command[5])
                            val numPairs = Integer.parseInt(command[6])
                            val knn = KnnBriefClassifier(talker, k, project, manager, width, height, numPairs)
                            val id = analyzer.addClassifier(knn)
                            analyzer.resetFPSCalculation()
                            log.append("Activating BRIEF kNN classifier; k=$k; project = $project; numPairs = $numPairs")
                            talker.sendString("$id")
                        } else {
                            log.append("Project $project does not exist")
                        }
                    } else if (command.size == 2 && command[1] == "pause") {
                        analyzer.currentClassifier = 0
                        talker.sendString("Deactivating classifier")
                        log.append("Classifier paused; FPS ${analyzer.currentFPS()}")
                    } else if (command.size == 3 && command[1] == "resume") {
                        val id = Integer.parseInt(command[2])
                        if (id >= analyzer.classifiers.size || id < 0) {
                            talker.sendString("Id $id not valid")
                        } else {
                            analyzer.currentClassifier = id
                            analyzer.resetFPSCalculation()
                        }
                    } else {
                        talker.sendString("Unrecognized cv cmd: '$text'")
                    }
                }
            }
        } catch (e: Exception) {
            Log.i("MainActivity", "Exception receiving '$text': $e")
            e.printStackTrace()
            log.append("Exception when receiving '$text': $e\n")
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDir,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    fun cameraSetup(myView: PreviewView) {
        view = myView
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            analyzer = BitmapAnalyzer1(YuvBitmapConverter(applicationContext), this)

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, analyzer)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val TAG = "CameraXBasic"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun message(msg: String) {
        log.append(msg.trim() + '\n')
    }
}

interface BitmapClassifier {
    fun classify(image: Bitmap)
}

class DummyClassifier : BitmapClassifier {
    override fun classify(image: Bitmap) {}
}

class BitmapAnalyzer1(val converter: YuvBitmapConverter, val complaintsTo: MessageReceiver) : ImageAnalysis.Analyzer {
    var classifiers = ArrayList<BitmapClassifier>()
    var currentClassifier = 0
    var frameCount: Long = 0
    var startTime: Long = 0

    init {
        classifiers.add(DummyClassifier())
        resetFPSCalculation()
    }

    fun addClassifier(b: BitmapClassifier): Int {
        classifiers.add(b)
        currentClassifier = classifiers.size - 1
        return currentClassifier
    }

    fun resetFPSCalculation() {
        frameCount = 0
        startTime = System.currentTimeMillis()
    }

    fun currentFPS(): Double {
        val elapsedTime = System.currentTimeMillis() - startTime
        return 1000 * frameCount.toDouble() / elapsedTime.toDouble()
    }

    override fun analyze(image: ImageProxy) {
        try {
            val bitmap = converter.convert(image.image!!)
            Log.i("BitmapAnalyzer1", "Bitmap: (${bitmap.width}, ${bitmap.height})")
            classifiers.get(currentClassifier).classify(bitmap)
            frameCount += 1
            Log.i("BitmapAnalyzer1", "FPS: ${currentFPS()}")
        } catch (e: java.lang.Exception) {
            Log.i("BitmapAnalyzer1", "Exception when classifying: $e")
            e.printStackTrace()
            complaintsTo.message("Exception when classifying: $e")
        } finally {
            image.close()
        }
    }

}


// From https://blog.minhazav.dev/how-to-convert-yuv-420-sp-android.media.Image-to-Bitmap-or-jpeg/
// My translation into Kotlin
class YuvBitmapConverter(context: Context) {
    private val rs: RenderScript = RenderScript.create(context)
    private val script: ScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    lateinit var incoming: Allocation
    lateinit var outgoing: Allocation

    fun convert(image: Image): Bitmap {
        val yuvBytes = yuv420ToByteArray(image)
        if (!::incoming.isInitialized) {
            val yuvType = Type.Builder(rs, Element.U8(rs)).setX(yuvBytes.size)
            val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(image.width).setY(image.height)
            incoming = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
            outgoing = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        }

        incoming.copyFrom(yuvBytes)
        script.setInput(incoming)
        script.forEach(outgoing)

        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        outgoing.copyTo(bitmap)
        return bitmap
    }
}

// Filling in a gap from the example above
fun yuv420ToByteArray(image: Image): ByteArray {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    return nv21
}