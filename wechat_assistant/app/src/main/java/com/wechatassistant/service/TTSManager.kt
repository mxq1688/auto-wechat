package com.wechatassistant.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    
    companion object {
        private const val TAG = "TTSManager"
    }
    
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false
    private var onSpeechDone: (() -> Unit)? = null
    private var onSpeechStart: (() -> Unit)? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    
    // 初始化前的待播报队列
    private val pendingSpeak = mutableListOf<String>()
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 尝试多种中文 Locale
            val locales = listOf(Locale.CHINA, Locale.CHINESE, Locale("zh", "CN"), Locale.getDefault())
            var success = false
            for (locale in locales) {
                val result = tts.setLanguage(locale)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    success = true
                    Log.d(TAG, "TTS language set to: $locale")
                    break
                }
            }
            
            isInitialized = success
            
            if (isInitialized) {
                tts.setSpeechRate(1.0f)
                tts.setPitch(1.0f)
                
                // 设置播报进度监听
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS speaking started: $utteranceId")
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS speaking done: $utteranceId")
                        releaseAudioFocus()
                        onSpeechDone?.invoke()
                    }
                    
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS speaking error: $utteranceId")
                        releaseAudioFocus()
                        onSpeechDone?.invoke()
                    }
                })
                
                Log.d(TAG, "TTS initialized successfully")
                
                // 播放待播报队列
                if (pendingSpeak.isNotEmpty()) {
                    Log.d(TAG, "Playing ${pendingSpeak.size} pending speaks")
                    val pending = pendingSpeak.toList()
                    pendingSpeak.clear()
                    for (text in pending) {
                        speak(text)
                    }
                }
            } else {
                Log.e(TAG, "TTS language not supported for any Chinese locale")
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }
    
    /**
     * 设置播报完成回调（用于恢复录音等）
     */
    fun setOnSpeechDoneListener(listener: (() -> Unit)?) {
        onSpeechDone = listener
    }
    
    /**
     * 设置播报开始回调（用于暂停录音等）
     */
    fun setOnSpeechStartListener(listener: (() -> Unit)?) {
        onSpeechStart = listener
    }
    
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        Log.d(TAG, "speak() called: text='$text', initialized=$isInitialized")
        
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized yet, queuing: $text")
            pendingSpeak.add(text)
            return
        }
        
        // 请求音频焦点
        requestAudioFocus()
        
        // 确保媒体音量足够
        ensureVolume()
        
        // 通知开始播报
        onSpeechStart?.invoke()
        
        // 选择可用的音频流：优先 STREAM_MUSIC，音量为0则回退 STREAM_ALARM
        val musicVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val stream = if (musicVol > 0) AudioManager.STREAM_MUSIC else AudioManager.STREAM_ALARM
        val streamName = if (stream == AudioManager.STREAM_MUSIC) "MUSIC" else "ALARM"
        
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, stream)
        }
        val utteranceId = "tts_${System.currentTimeMillis()}"
        val result = tts.speak(text, queueMode, params, utteranceId)
        Log.d(TAG, "TTS speak result: $result, stream=$streamName (0=SUCCESS, -1=ERROR)")
    }
    
    /**
     * 确保播放音量不为 0
     */
    private fun ensureVolume() {
        // 检查 STREAM_MUSIC
        val musicVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        Log.d(TAG, "STREAM_MUSIC volume: $musicVol/$musicMax")
        
        if (musicVol == 0) {
            // MUSIC 为 0，确保 ALARM 有音量作为后备
            val alarmVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val alarmMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            if (alarmVol == 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmMax * 2 / 3, 0)
                Log.d(TAG, "STREAM_ALARM volume was 0, set to ${alarmMax * 2 / 3}")
            } else {
                Log.d(TAG, "STREAM_ALARM volume: $alarmVol/$alarmMax (fallback)")
            }
        }
    }
    
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
            Log.d(TAG, "Audio focus requested (ASSISTANT)")
        }
    }
    
    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
                Log.d(TAG, "Audio focus released")
            }
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
        releaseAudioFocus()
    }
    
    fun isSpeaking(): Boolean {
        return tts.isSpeaking
    }
}