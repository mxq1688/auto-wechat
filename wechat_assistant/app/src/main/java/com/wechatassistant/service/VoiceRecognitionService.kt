package com.wechatassistant.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceRecognitionService(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceRecognition"
    }
    
    interface VoiceCommandListener {
        fun onCommandRecognized(command: String)
        fun onError(error: String)
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var commandListener: VoiceCommandListener? = null
    
    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognitionListener()
        }
    }
    
    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            
            override fun onError(error: Int) {
                Log.e(TAG, "Recognition error: $error")
                isListening = false
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    processCommand(matches[0])
                }
                isListening = false
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    fun startListening() {
        if (!isListening && speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            }
            speechRecognizer?.startListening(intent)
            isListening = true
        }
    }
    
    private fun processCommand(command: String) {
        Log.d(TAG, "Command: $command")
        commandListener?.onCommandRecognized(command)
    }
    
    fun setCommandListener(listener: VoiceCommandListener) {
        commandListener = listener
    }
    
    fun destroy() {
        speechRecognizer?.destroy()
    }
}