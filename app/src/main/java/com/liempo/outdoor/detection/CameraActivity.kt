package com.liempo.outdoor.detection

import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.*
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector
import kotlinx.android.synthetic.main.activity_camera.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

import com.liempo.outdoor.R
import java.util.*

class CameraActivity : AppCompatActivity() {

    private lateinit var detector: FirebaseVisionObjectDetector
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.plant(Timber.DebugTree())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)


        detector = FirebaseVision.getInstance()
            .onDeviceObjectDetector

        tts = TextToSpeech(this, OnInitListener {
            if (it == SUCCESS) {
                val lang = tts.setLanguage(Locale.US)

                if (lang == LANG_MISSING_DATA || lang == LANG_NOT_SUPPORTED) {
                    Timber.e("Missing data or not supported.")
                }
            }

            Timber.i("Initialized TTS successfully.")
        })

        texture.post { startCamera() }
    }

    private fun startCamera() {
        val metrics = DisplayMetrics().also { texture.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setTargetResolution(screenSize)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(windowManager.defaultDisplay.rotation)
            setTargetRotation(texture.display.rotation)
        }.build()

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            texture.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val analysisConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setTargetResolution(screenSize)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(windowManager.defaultDisplay.rotation)
            setTargetRotation(texture.display.rotation)

            // Optimization
            val thread = HandlerThread(
                "ObjDetectionThread").apply { start() }
            setCallbackHandler(Handler(thread.looper))
        }.build()

        val analysis = ImageAnalysis(analysisConfig).apply {
            analyzer = ImageAnalyzer()
        }

        CameraX.bindToLifecycle(this, preview, analysis)
    }

    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = texture.width / 2f
        val centerY = texture.height / 2f

        val rotationDegrees = when (texture.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.
            toFloat(), centerX, centerY)
        texture.setTransform(matrix)
    }

    inner class ImageAnalyzer : ImageAnalysis.Analyzer {

        private var isAnalyzing = AtomicBoolean(false)

        private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
        }

        @Suppress("DEPRECATION")
        override fun analyze(imageProxy: ImageProxy?, degrees: Int) {
            if (isAnalyzing.get()) return

            isAnalyzing.set(true)
            val mediaImage = imageProxy?.image
            val imageRotation = degreesToFirebaseRotation(degrees)
            if (mediaImage != null) {
                val image = FirebaseVisionImage
                    .fromMediaImage(mediaImage, imageRotation)
                detector.processImage(image).addOnSuccessListener {
                    isAnalyzing.set(false)
                    Timber.i("ResultSize: ${it.size}")

                    it.forEach { obj ->
                        val area = obj.boundingBox.width() *
                                obj.boundingBox.height()
                        if (area > 20000) tts.speak(
                            "Obstacle is near.",
                            QUEUE_FLUSH, null)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop(); tts.shutdown()
        }
    }
}
