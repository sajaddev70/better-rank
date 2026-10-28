package com.marvelous.betterrank.mediaManager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.util.Log
import java.util.Locale

object SpeechToTextManager {

    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening(context: Context, listener: OnSpeechResultListener) {

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onSpeechError(getErrorMessage(0))
            return
        }


        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Listen to you")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(p0: Bundle?) {
                listener.onSpeechReady()
            }

            override fun onBeginningOfSpeech() {

                listener.onSpeechBeginning()
            }

            override fun onRmsChanged(p0: Float) {
                listener.onSpeechRmsChanged(p0)
            }

            override fun onBufferReceived(p0: ByteArray?) {
//                Log.d("","")
            }

            override fun onEndOfSpeech() {
//                Log.d("","")
            }

            override fun onError(error: Int) {
                listener.onSpeechError(getErrorMessage(error))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                listener.onSpeechResult(matches?.joinToString(" ") ?: "")
            }

            override fun onPartialResults(p0: Bundle?) {
//                Log.d("","")
            }
            override fun onEvent(p0: Int, p1: Bundle?) {
//                Log.d("","")
            }
        })

        speechRecognizer?.startListening(recognizerIntent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    interface OnSpeechResultListener {
        fun onSpeechReady()
        fun onSpeechBeginning()
        fun onSpeechRmsChanged(p0: Float)
        fun onSpeechResult(result: String)
        fun onSpeechError(errorMessage: String)
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            -1->"Google Play Services are not available."
            0->"Speech recognition is not available on this device"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Connection timed out. Please try again."
            SpeechRecognizer.ERROR_NETWORK -> "Network error. Please check your connection."
            SpeechRecognizer.ERROR_AUDIO -> "Error capturing audio. Please check your microphone."
            SpeechRecognizer.ERROR_SERVER -> "Server error. Please try again later."
            SpeechRecognizer.ERROR_CLIENT -> "Client error. Please restart the app."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected. Please try again."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found. Please speak clearly."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please wait."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied. Please allow microphone access."
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests. Please wait and try again."
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server disconnected. Please check your internet connection."
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported."
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable on this device."
            else -> "Unknown error occurred. Please try again."
        }
    }
}



