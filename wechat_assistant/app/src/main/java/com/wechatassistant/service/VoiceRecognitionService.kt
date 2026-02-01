package com.wechatassistant.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.wechatassistant.voice.VoiceCommandProcessor

/**
 * 语音识别服务
 * 优先级：Vosk (离线免费) > 讯飞 (在线) > 系统默认
 */
class VoiceRecognitionService(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceRecognition"
        
        // 语音识别引擎类型
        enum class Engine {
            VOSK,       // 首选：离线免费
            XUNFEI,     // 备选：在线（有免费额度）
            SYSTEM      // 最后：系统默认
        }
    }
    
    interface VoiceCommandListener {
        fun onCommandRecognized(command: String)
        fun onCommandExecuted(command: VoiceCommandProcessor.Command)
        fun onError(error: String)
        fun onWakeWordDetected()
        fun onWaitingForCommand()
        fun onModelDownloadProgress(progress: Int)  // 模型下载进度
        fun onModelReady()  // 模型准备就绪
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var voskService: VoskVoiceService? = null
    private var xunfeiService: XunfeiVoiceService? = null
    private var isListening = false
    private var currentEngine: Engine = Engine.VOSK
    private var commandListener: VoiceCommandListener? = null
    private val commandProcessor = VoiceCommandProcessor(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var ttsManager: TTSManager? = null
    private var ttsEnabled = true
    
    // 模型状态
    private var isVoskModelReady = false
    private var isInitializing = false
    
    init {
        Log.d(TAG, "Initializing VoiceRecognitionService...")
        // 初始化 TTS
        try {
            ttsManager = TTSManager(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init TTS: ${e.message}")
        }
        initializeEngine()
    }
    
    /**
     * 设置 TTS 是否启用
     */
    fun setTTSEnabled(enabled: Boolean) {
        ttsEnabled = enabled
    }
    
    /**
     * 语音提示（带冷却检查，isError=true表示错误提示需要冷却）
     */
    private fun speakHint(text: String, isError: Boolean = false) {
        if (!ttsEnabled || ttsManager == null) return
        
        if (isError) {
            // 错误提示检查冷却时间
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastHintTime < HINT_COOLDOWN_MS) {
                Log.d(TAG, "Hint cooldown, skipping: $text")
                return
            }
            lastHintTime = currentTime
        }
        
        ttsManager?.speak(text)
    }
    
    /**
     * 初始化语音引擎
     */
    private fun initializeEngine() {
        if (isInitializing) return
        isInitializing = true
        
        // 首先尝试 Vosk
        Log.d(TAG, "Trying Vosk (offline) engine...")
        voskService = VoskVoiceService(context)
        
        voskService?.setListener(object : VoskVoiceService.VoskListener {
            override fun onResult(text: String) {
                Log.d(TAG, "Vosk result: $text")
                mainHandler.post {
                    processCommand(text)
                }
            }
            
            override fun onPartialResult(text: String) {
                Log.d(TAG, "Vosk partial: $text")
                mainHandler.post {
                    commandListener?.onCommandRecognized(text)
                }
            }
            
            override fun onError(error: String) {
                Log.e(TAG, "Vosk error: $error")
                mainHandler.post {
                    commandListener?.onError(error)
                }
            }
            
            override fun onModelDownloadProgress(progress: Int) {
                Log.d(TAG, "Model download: $progress%")
                mainHandler.post {
                    commandListener?.onModelDownloadProgress(progress)
                }
            }
            
            override fun onModelReady() {
                Log.d(TAG, "Vosk model ready!")
                isVoskModelReady = true
                currentEngine = Engine.VOSK
                mainHandler.post {
                    commandListener?.onModelReady()
                }
            }
        })
        
        // 检查 Vosk 模型
        if (voskService?.isModelReady() == true) {
            Log.d(TAG, "Vosk model already exists, loading...")
            voskService?.initModel { success ->
                isInitializing = false
                if (success) {
                    isVoskModelReady = true
                    currentEngine = Engine.VOSK
                    Log.d(TAG, "Using Vosk engine (offline)")
                } else {
                    fallbackToXunfei()
                }
            }
        } else {
            // 模型不存在，先用讯飞，后台下载 Vosk 模型
            Log.d(TAG, "Vosk model not found, using Xunfei first, downloading Vosk model in background...")
            fallbackToXunfei()
            
            // 后台下载 Vosk 模型
            voskService?.initModel { success ->
                if (success) {
                    isVoskModelReady = true
                    Log.d(TAG, "Vosk model downloaded and ready! Will use Vosk next time.")
                }
            }
        }
    }
    
    /**
     * 降级到讯飞
     */
    private fun fallbackToXunfei() {
        Log.d(TAG, "Falling back to Xunfei (online) engine...")
        currentEngine = Engine.XUNFEI
        
        xunfeiService = XunfeiVoiceService(context)
        commandListener?.let { xunfeiService?.setCommandListener(it) }
        
        isInitializing = false
        Log.d(TAG, "Using Xunfei engine (online)")
    }
    
    /**
     * 手动切换到 Vosk（如果模型已就绪）
     */
    fun switchToVosk(): Boolean {
        if (isVoskModelReady) {
            currentEngine = Engine.VOSK
            Log.d(TAG, "Switched to Vosk engine")
            return true
        }
        return false
    }
    
    /**
     * 手动切换到讯飞
     */
    fun switchToXunfei() {
        if (xunfeiService == null) {
            xunfeiService = XunfeiVoiceService(context)
            commandListener?.let { xunfeiService?.setCommandListener(it) }
        }
        currentEngine = Engine.XUNFEI
        Log.d(TAG, "Switched to Xunfei engine")
    }
    
    /**
     * 获取当前引擎
     */
    fun getCurrentEngine(): Engine = currentEngine
    
    /**
     * 获取当前引擎名称
     */
    fun getCurrentEngineName(): String {
        return when (currentEngine) {
            Engine.VOSK -> "Vosk (离线)"
            Engine.XUNFEI -> "讯飞 (在线)"
            Engine.SYSTEM -> "系统默认"
        }
    }
    
    /**
     * 检查 Vosk 模型是否就绪
     */
    fun isVoskReady(): Boolean = isVoskModelReady
    
    /**
     * 手动下载 Vosk 模型
     */
    fun downloadVoskModel() {
        if (voskService == null) {
            voskService = VoskVoiceService(context)
        }
        voskService?.initModel { success ->
            if (success) {
                isVoskModelReady = true
                Log.d(TAG, "Vosk model downloaded successfully")
            }
        }
    }
    
    fun startListening() {
        Log.d(TAG, "startListening called, engine=$currentEngine, isListening=$isListening")
        
        if (isListening) {
            Log.d(TAG, "Already listening, ignoring")
            return
        }
        
        when (currentEngine) {
            Engine.VOSK -> {
                if (isVoskModelReady) {
                    if (voskService?.startListening() == true) {
                        isListening = true
                        Log.d(TAG, "Started Vosk listening")
                    } else {
                        // Vosk 启动失败，降级到讯飞
                        Log.w(TAG, "Vosk failed to start, falling back to Xunfei")
                        switchToXunfei()
                        startXunfeiListening()
                    }
                } else {
                    // 模型未就绪，用讯飞
                    Log.d(TAG, "Vosk model not ready, using Xunfei")
                    switchToXunfei()
                    startXunfeiListening()
                }
            }
            Engine.XUNFEI -> {
                startXunfeiListening()
            }
            Engine.SYSTEM -> {
                startSystemListening()
            }
        }
    }
    
    private fun startXunfeiListening() {
        if (xunfeiService == null) {
            xunfeiService = XunfeiVoiceService(context)
            commandListener?.let { xunfeiService?.setCommandListener(it) }
        }
        xunfeiService?.startListening()
        isListening = true
        Log.d(TAG, "Started Xunfei listening")
    }
    
    private fun startSystemListening() {
        if (speechRecognizer == null) {
            speechRecognizer = createSystemSpeechRecognizer()
            if (speechRecognizer != null) {
                setupRecognitionListener()
            }
        }
        
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            try {
                speechRecognizer?.startListening(intent)
                isListening = true
                Log.d(TAG, "Started system listening")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting system listening: ${e.message}")
                commandListener?.onError("启动语音识别失败: ${e.message}")
            }
        } else {
            commandListener?.onError("语音识别不可用")
        }
    }
    
    private fun createSystemSpeechRecognizer(): SpeechRecognizer? {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            return SpeechRecognizer.createSpeechRecognizer(context)
        }
        return null
    }
    
    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { 
                Log.d(TAG, "End of speech")
                isListening = false 
            }
            
            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频错误"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器繁忙"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
                    else -> "未知错误: $error"
                }
                Log.e(TAG, "Recognition error: $errorMsg")
                commandListener?.onError(errorMsg)
                isListening = false
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    processCommand(matches[0])
                }
                isListening = false
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "Partial result: ${matches[0]}")
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    fun stopListening() {
        if (isListening) {
            when (currentEngine) {
                Engine.VOSK -> voskService?.stopListening()
                Engine.XUNFEI -> xunfeiService?.stopListening()
                Engine.SYSTEM -> speechRecognizer?.stopListening()
            }
            isListening = false
            Log.d(TAG, "Stopped listening")
        }
    }
    
    fun isListening(): Boolean = isListening
    
    // 冷却时间：防止重复触发
    private var lastCommandTime = 0L
    private val COMMAND_COOLDOWN_MS = 10000L  // 10秒冷却
    
    // 提示冷却时间：防止重复提示
    private var lastHintTime = 0L
    private val HINT_COOLDOWN_MS = 8000L  // 8秒内不重复提示
    
    // 唤醒词模式：是否需要唤醒词才能执行命令
    var requireWakeWord = false  // 默认关闭唤醒词
    
    private fun processCommand(voiceInput: String) {
        Log.d(TAG, "Processing command: $voiceInput")
        
        // 检查冷却时间
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCommandTime < COMMAND_COOLDOWN_MS) {
            Log.d(TAG, "Command ignored: still in cooldown period")
            return
        }
        
        commandListener?.onCommandRecognized(voiceInput)
        
        // 检查唤醒词
        val (hasWakeWord, actualCommand) = commandProcessor.checkWakeWord(voiceInput)
        
        if (requireWakeWord && !hasWakeWord) {
            Log.d(TAG, "No wake word detected, ignoring: $voiceInput")
            return
        }
        
        if (hasWakeWord) {
            commandListener?.onWakeWordDetected()
            
            if (actualCommand.isBlank()) {
                Log.d(TAG, "Wake word only, waiting for command...")
                commandListener?.onWaitingForCommand()
                return
            }
        }
        
        // 解析命令
        val commandToParse = if (hasWakeWord) actualCommand else voiceInput
        val command = commandProcessor.parseCommand(commandToParse)
        Log.d(TAG, "Parsed command: type=${command.type}, contact=${command.contactName}")
        
        // 执行命令
        executeCommand(command)
    }
    
    /**
     * 检查联系人是否在设置的名单中
     */
    private fun isContactInList(contactName: String): Boolean {
        val settings = com.wechatassistant.manager.SettingsManager.getInstance(context)
        val contacts = settings.getContacts()
        
        // 检查是否是微信名
        if (contacts.containsKey(contactName)) return true
        
        // 检查是否是别名
        val aliases = settings.getContactAliases()
        return aliases.containsKey(contactName)
    }
    
    private fun executeCommand(command: VoiceCommandProcessor.Command) {
        when (command.type) {
            VoiceCommandProcessor.CommandType.VIDEO_CALL -> {
                if (command.contactName != null) {
                    // 检查联系人是否在名单中
                    if (!isContactInList(command.contactName)) {
                        Log.w(TAG, "Contact not in list: ${command.contactName}")
                        val hint = "${command.contactName}不在联系人名单中，请先在语音设置里添加"
                        speakHint(hint, isError = true)
                        commandListener?.onError(hint)
                        return
                    }
                    
                    Log.d(TAG, "Executing video call to: ${command.contactName}")
                    lastCommandTime = System.currentTimeMillis()
                    speakHint("正在给${command.contactName}拨打视频电话")
                    stopListening()
                    
                    val intent = Intent(EnhancedWeChatAccessibilityService.ACTION_MAKE_VIDEO_CALL).apply {
                        putExtra(EnhancedWeChatAccessibilityService.EXTRA_CONTACT_NAME, command.contactName)
                    }
                    context.sendBroadcast(intent)
                    commandListener?.onCommandExecuted(command)
                } else {
                    Log.w(TAG, "Video call command but no contact name")
                    val hint = "没有听清联系人，请说：给某某打视频"
                    speakHint(hint, isError = true)
                    commandListener?.onError(hint)
                }
            }
            VoiceCommandProcessor.CommandType.VOICE_CALL -> {
                if (command.contactName != null) {
                    // 检查联系人是否在名单中
                    if (!isContactInList(command.contactName)) {
                        Log.w(TAG, "Contact not in list: ${command.contactName}")
                        val hint = "${command.contactName}不在联系人名单中，请先在语音设置里添加"
                        speakHint(hint, isError = true)
                        commandListener?.onError(hint)
                        return
                    }
                    
                    Log.d(TAG, "Executing voice call to: ${command.contactName}")
                    lastCommandTime = System.currentTimeMillis()
                    speakHint("正在给${command.contactName}拨打语音电话")
                    stopListening()
                    
                    val intent = Intent(EnhancedWeChatAccessibilityService.ACTION_MAKE_VOICE_CALL).apply {
                        putExtra(EnhancedWeChatAccessibilityService.EXTRA_CONTACT_NAME, command.contactName)
                    }
                    context.sendBroadcast(intent)
                    commandListener?.onCommandExecuted(command)
                } else {
                    Log.w(TAG, "Voice call command but no contact name")
                    val hint = "没有听清联系人，请说：给某某打电话"
                    speakHint(hint, isError = true)
                    commandListener?.onError(hint)
                }
            }
            VoiceCommandProcessor.CommandType.INCOMPLETE_CALL -> {
                // 有打电话关键词但缺少联系人，提示用户（带冷却）
                Log.d(TAG, "Incomplete call command: ${command.rawCommand}")
                val hint = "没听清联系人，请说：给某某打视频，或者给某某打电话"
                speakHint(hint, isError = true)
                commandListener?.onError(hint)
            }
            VoiceCommandProcessor.CommandType.UNKNOWN -> {
                // 完全无关的输入，静默忽略
                Log.d(TAG, "Unknown/irrelevant input, ignoring: ${command.rawCommand}")
            }
            else -> {
                Log.d(TAG, "Unhandled command type: ${command.type}")
            }
        }
    }
    
    fun makeVideoCall(contactName: String) {
        Log.d(TAG, "makeVideoCall: $contactName")
        val intent = Intent(EnhancedWeChatAccessibilityService.ACTION_MAKE_VIDEO_CALL).apply {
            putExtra(EnhancedWeChatAccessibilityService.EXTRA_CONTACT_NAME, contactName)
        }
        context.sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent for video call")
    }
    
    fun makeVoiceCall(contactName: String) {
        Log.d(TAG, "makeVoiceCall: $contactName")
        val intent = Intent(EnhancedWeChatAccessibilityService.ACTION_MAKE_VOICE_CALL).apply {
            putExtra(EnhancedWeChatAccessibilityService.EXTRA_CONTACT_NAME, contactName)
        }
        context.sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent for voice call")
    }
    
    fun setCommandListener(listener: VoiceCommandListener) {
        commandListener = listener
        xunfeiService?.setCommandListener(listener)
    }
    
    fun destroy() {
        voskService?.destroy()
        xunfeiService?.destroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
        ttsManager?.shutdown()
        ttsManager = null
    }
}
