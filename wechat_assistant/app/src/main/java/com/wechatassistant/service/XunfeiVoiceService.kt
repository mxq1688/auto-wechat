package com.wechatassistant.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import com.wechatassistant.voice.VoiceCommandProcessor
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread

/**
 * 讯飞语音识别服务 - 使用 WebSocket API
 */
class XunfeiVoiceService(private val context: Context) {
    
    companion object {
        private const val TAG = "XunfeiVoice"
        
        // 讯飞 API 配置
        private const val APP_ID = "295dbfc8"
        private const val API_KEY = "9b7288124af2970cf54d7b34bcc8b47e"
        private const val API_SECRET = "NzlhZTdjYzAzOWYyNDI2MjI2MjU3Mzc3"
        
        // WebSocket 地址
        private const val HOST_URL = "wss://iat-api.xfyun.cn/v2/iat"
        
        // 音频参数
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_SIZE = 1280 // 每帧 40ms 的数据
    }
    
    interface VoiceListener {
        fun onResult(text: String, isLast: Boolean)
        fun onError(error: String)
        fun onReady()
    }
    
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var listener: VoiceListener? = null
    private val okHttpClient = OkHttpClient.Builder().build()
    private val commandProcessor = VoiceCommandProcessor()
    
    private var commandListener: VoiceRecognitionService.VoiceCommandListener? = null
    
    // 累积识别结果
    private val recognizedTextBuilder = StringBuilder()
    
    fun setCommandListener(listener: VoiceRecognitionService.VoiceCommandListener) {
        this.commandListener = listener
    }
    
    fun startListening() {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            commandListener?.onError("没有录音权限")
            return
        }
        
        // 清空之前的识别结果
        recognizedTextBuilder.clear()
        
        Log.d(TAG, "Starting Xunfei voice recognition...")
        connectWebSocket()
    }
    
    fun stopListening() {
        Log.d(TAG, "Stopping voice recognition")
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        webSocket?.close(1000, "User stopped")
        webSocket = null
    }
    
    fun destroy() {
        stopListening()
        okHttpClient.dispatcher.executorService.shutdown()
    }
    
    private fun connectWebSocket() {
        val url = getAuthUrl()
        Log.d(TAG, "Connecting to: $url")
        
        val request = Request.Builder().url(url).build()
        
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                commandListener?.onWaitingForCommand()
                startRecording()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
                commandListener?.onError("连接失败: ${t.message}")
                stopListening()
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
            }
        })
    }
    
    private fun handleMessage(message: String) {
        try {
            val json = JSONObject(message)
            val code = json.getInt("code")
            
            if (code != 0) {
                val errorMsg = json.optString("message", "Unknown error")
                Log.e(TAG, "API error: $code - $errorMsg")
                commandListener?.onError("识别错误: $errorMsg")
                return
            }
            
            val data = json.optJSONObject("data") ?: return
            val result = data.optJSONObject("result") ?: return
            val ws = result.optJSONArray("ws") ?: return
            
            val sb = StringBuilder()
            for (i in 0 until ws.length()) {
                val wsItem = ws.getJSONObject(i)
                val cw = wsItem.optJSONArray("cw") ?: continue
                for (j in 0 until cw.length()) {
                    val cwItem = cw.getJSONObject(j)
                    sb.append(cwItem.optString("w", ""))
                }
            }
            
            val text = sb.toString()
            val status = data.optInt("status", 0)
            val isLast = status == 2
            
            if (text.isNotEmpty()) {
                // 累加识别结果
                recognizedTextBuilder.append(text)
                Log.d(TAG, "Recognized: $text (isLast=$isLast, total=${recognizedTextBuilder})")
            }
            
            if (isLast) {
                // 识别结束，处理完整的命令
                val fullText = recognizedTextBuilder.toString().trim()
                    .replace("。", "").replace("，", "").replace("？", "")  // 去除标点
                Log.d(TAG, "Final recognized text: $fullText")
                
                if (fullText.isNotEmpty()) {
                    processCommand(fullText)
                }
                
                // 清空累积结果
                recognizedTextBuilder.clear()
                
                // 重新开始监听
                stopListening()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startListening()
                }, 500)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
        }
    }
    
    private fun processCommand(text: String) {
        Log.d(TAG, "Processing command: $text")
        commandListener?.onCommandRecognized(text)
        
        val command = commandProcessor.parseCommand(text)
        if (command != null) {
            Log.d(TAG, "Command parsed: type=${command.type}, contact=${command.contactName}")
            commandListener?.onCommandExecuted(command)
        }
    }
    
    private fun startRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )
        
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed")
            commandListener?.onError("录音初始化失败")
            return
        }
        
        audioRecord?.startRecording()
        isRecording = true
        
        thread {
            val buffer = ByteArray(FRAME_SIZE)
            var frameIndex = 0
            
            while (isRecording) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    sendAudioFrame(buffer, frameIndex)
                    frameIndex++
                }
            }
            
            // 发送结束帧
            sendEndFrame()
        }
    }
    
    private fun sendAudioFrame(data: ByteArray, frameIndex: Int) {
        val status = when (frameIndex) {
            0 -> 0  // 第一帧
            else -> 1  // 中间帧
        }
        
        val frame = JSONObject().apply {
            put("common", JSONObject().apply {
                put("app_id", APP_ID)
            })
            put("business", JSONObject().apply {
                put("language", "zh_cn")
                put("domain", "iat")
                put("accent", "mandarin")
                put("vad_eos", 3000)  // 静音检测 3 秒
                put("dwa", "wpgs")  // 动态修正
            })
            put("data", JSONObject().apply {
                put("status", status)
                put("format", "audio/L16;rate=16000")
                put("encoding", "raw")
                put("audio", Base64.encodeToString(data, Base64.NO_WRAP))
            })
        }
        
        webSocket?.send(frame.toString())
    }
    
    private fun sendEndFrame() {
        val frame = JSONObject().apply {
            put("data", JSONObject().apply {
                put("status", 2)  // 结束帧
                put("format", "audio/L16;rate=16000")
                put("encoding", "raw")
                put("audio", "")
            })
        }
        webSocket?.send(frame.toString())
    }
    
    /**
     * 生成鉴权 URL
     */
    private fun getAuthUrl(): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        val date = dateFormat.format(Date())
        
        val signatureOrigin = "host: iat-api.xfyun.cn\ndate: $date\nGET /v2/iat HTTP/1.1"
        
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(API_SECRET.toByteArray(), "HmacSHA256"))
        val signature = Base64.encodeToString(mac.doFinal(signatureOrigin.toByteArray()), Base64.NO_WRAP)
        
        val authorizationOrigin = "api_key=\"$API_KEY\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"$signature\""
        val authorization = Base64.encodeToString(authorizationOrigin.toByteArray(), Base64.NO_WRAP)
        
        return "$HOST_URL?authorization=${URLEncoder.encode(authorization, "UTF-8")}&date=${URLEncoder.encode(date, "UTF-8")}&host=iat-api.xfyun.cn"
    }
}

