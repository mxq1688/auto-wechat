package com.wechatassistant.service

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Vosk 离线语音识别服务
 * 完全免费、离线、不需要网络
 * 模型已打包在 APK 的 assets 目录中
 */
class VoskVoiceService(private val context: Context) {
    
    companion object {
        private const val TAG = "VoskVoice"
        // assets 中的模型目录名
        private const val MODEL_ASSETS_PATH = "model-zh-cn"
        private const val SAMPLE_RATE = 16000.0f
    }
    
    interface VoskListener {
        fun onResult(text: String)
        fun onPartialResult(text: String)
        fun onError(error: String)
        fun onModelDownloadProgress(progress: Int)
        fun onModelReady()
    }
    
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var listener: VoskListener? = null
    private var isModelLoading = false
    private var isListening = false
    
    fun setListener(listener: VoskListener) {
        this.listener = listener
    }
    
    /**
     * 检查模型是否已解压到内部存储
     */
    fun isModelReady(): Boolean {
        val modelDir = File(context.filesDir, MODEL_ASSETS_PATH)
        return modelDir.exists() && modelDir.isDirectory && 
               File(modelDir, "am/final.mdl").exists()
    }
    
    /**
     * 初始化模型（从 assets 解压或直接加载）
     */
    fun initModel(callback: (Boolean) -> Unit) {
        if (model != null) {
            callback(true)
            return
        }
        
        if (isModelLoading) {
            Log.d(TAG, "Model is already loading...")
            return
        }
        
        isModelLoading = true
        
        // 使用 Vosk 的 StorageService 从 assets 解压模型
        StorageService.unpack(context, MODEL_ASSETS_PATH, MODEL_ASSETS_PATH,
            { model: Model ->
                // 模型加载成功
                this.model = model
                isModelLoading = false
                Log.d(TAG, "Model loaded successfully from assets")
                listener?.onModelReady()
                callback(true)
            },
            { exception: IOException ->
                // 加载失败
                Log.e(TAG, "Failed to load model from assets", exception)
                isModelLoading = false
                listener?.onError("模型加载失败: ${exception.message}")
                callback(false)
            }
        )
    }
    
    /**
     * 开始语音识别
     */
    fun startListening(): Boolean {
        if (model == null) {
            Log.e(TAG, "Model not loaded")
            listener?.onError("模型未加载")
            return false
        }
        
        if (isListening) {
            Log.d(TAG, "Already listening")
            return true
        }
        
        try {
            val recognizer = Recognizer(model, SAMPLE_RATE)
            
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    hypothesis?.let {
                        val text = parseResult(it)
                        if (text.isNotEmpty()) {
                            Log.d(TAG, "Partial: $text")
                            listener?.onPartialResult(text)
                        }
                    }
                }
                
                override fun onResult(hypothesis: String?) {
                    hypothesis?.let {
                        val text = parseResult(it)
                        if (text.isNotEmpty()) {
                            Log.d(TAG, "Final: $text")
                            listener?.onResult(text)
                        }
                    }
                }
                
                override fun onFinalResult(hypothesis: String?) {
                    hypothesis?.let {
                        val text = parseResult(it)
                        if (text.isNotEmpty()) {
                            Log.d(TAG, "Final result: $text")
                            listener?.onResult(text)
                        }
                    }
                }
                
                override fun onError(exception: Exception?) {
                    Log.e(TAG, "Recognition error", exception)
                    listener?.onError("识别错误: ${exception?.message}")
                }
                
                override fun onTimeout() {
                    Log.d(TAG, "Recognition timeout")
                }
            })
            
            isListening = true
            Log.d(TAG, "Started listening")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            listener?.onError("启动识别失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopListening() {
        try {
            speechService?.stop()
            speechService = null
            isListening = false
            Log.d(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping", e)
        }
    }
    
    /**
     * 暂停识别
     */
    fun pause() {
        speechService?.setPause(true)
    }
    
    /**
     * 恢复识别
     */
    fun resume() {
        speechService?.setPause(false)
    }
    
    /**
     * 释放资源
     */
    fun destroy() {
        stopListening()
        model?.close()
        model = null
    }
    
    /**
     * 解析 Vosk JSON 结果
     */
    private fun parseResult(json: String): String {
        return try {
            val obj = JSONObject(json)
            obj.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }
    
    fun isListening(): Boolean = isListening
}
