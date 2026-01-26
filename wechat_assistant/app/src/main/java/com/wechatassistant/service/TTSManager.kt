package com.wechatassistant.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    
    companion object {
        private const val TAG = "TTSManager"
    }
    
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.CHINESE)
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                          result != TextToSpeech.LANG_NOT_SUPPORTED
            
            if (isInitialized) {
                tts.setSpeechRate(1.0f)
                tts.setPitch(1.0f)
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS language not supported")
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }
    
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isInitialized) {
            tts.speak(text, queueMode, null, null)
        } else {
            Log.w(TAG, "TTS not initialized, cannot speak: $text")
        }
    }
    
    fun announceIncomingCall(callerName: String) {
        val announcement = "${callerName}的视频通话"
        speak(announcement, TextToSpeech.QUEUE_FLUSH)
    }
    
    fun announceCallAction(action: String) {
        speak(action, TextToSpeech.QUEUE_ADD)
    }
    
    fun stop() {
        tts.stop()
    }
    
    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
    
    fun isSpeaking(): Boolean {
        return tts.isSpeaking
    }
}