package com.liempo.outdoor

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent.*
import android.speech.SpeechRecognizer.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class SpeechRecognitionModel(application: Application):
    AndroidViewModel(application), RecognitionListener {

    internal val recognizedText = MutableLiveData<String>()
    internal val isListening = MutableLiveData<Boolean>()
    internal val rmsValue = MutableLiveData<Float>()
    internal val error = MutableLiveData<String>()

    private val recognizer =
        createSpeechRecognizer(application).apply {
            setRecognitionListener(this@SpeechRecognitionModel)
        }

    private val intent by lazy {
        Intent(ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM)
            putExtra(EXTRA_CALLING_PACKAGE, application.packageName)
            putExtra(EXTRA_PARTIAL_RESULTS, true)
        }
    }

    fun startListening() {
        recognizer.startListening(intent)
        notifyListening(isRecording = true)
    }

    fun stopListening() {
        recognizer.stopListening()
    }

    private fun notifyListening(isRecording: Boolean) {
        isListening.value = isRecording
    }

    private fun updateResults(speechBundle: Bundle?) {
        recognizedText.value = speechBundle?.getStringArrayList(
            RESULTS_RECOGNITION)?.get(0)
    }

    override fun onRmsChanged(rms: Float) {
        this.rmsValue.value = rms
    }

    override fun onPartialResults(results: Bundle?) =
        updateResults(speechBundle = results)

    override fun onResults(results: Bundle?) =
        updateResults(speechBundle = results)

    override fun onEndOfSpeech() =
        notifyListening(isRecording = false)

    override fun onError(errorCode: Int) {
        error.value = when (errorCode) {
            ERROR_AUDIO -> "error_audio_error"
            ERROR_CLIENT -> "error_client"
            ERROR_INSUFFICIENT_PERMISSIONS -> "error_permission"
            ERROR_NETWORK -> "error_network"
            ERROR_NETWORK_TIMEOUT -> "error_timeout"
            ERROR_NO_MATCH -> "error_no_match"
            ERROR_RECOGNIZER_BUSY -> "error_busy"
            ERROR_SERVER -> "error_server"
            ERROR_SPEECH_TIMEOUT -> "error_timeout"
            else -> "error_unknown"
        }
    }

    override fun onReadyForSpeech(p0: Bundle?) {}
    override fun onBufferReceived(p0: ByteArray?) {}
    override fun onEvent(p0: Int, p1: Bundle?) {}
    override fun onBeginningOfSpeech() {}

}