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
import com.wechatassistant.manager.SettingsManager
import com.wechatassistant.voice.VoiceCommandProcessor
import kotlinx.coroutines.*

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
    
    // LLM 智能分析
    private val llmService = LLMService()
    private val settings = SettingsManager.getInstance(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 模型状态
    private var isVoskModelReady = false
    private var isInitializing = false
    private var pendingStartListening = false
    
    init {
        Log.d(TAG, "Initializing VoiceRecognitionService...")
        // 初始化 TTS
        try {
            ttsManager = TTSManager(context)
            // TTS 播报前暂停录音，播报完恢复录音
            ttsManager?.setOnSpeechStartListener {
                Log.d(TAG, "TTS start → pausing voice recording")
                pauseListeningForTTS()
            }
            ttsManager?.setOnSpeechDoneListener {
                Log.d(TAG, "TTS done → resuming voice recording")
                resumeListeningAfterTTS()
            }
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
        
        // 始终优先加载 Vosk（离线引擎）
        Log.d(TAG, "Loading Vosk (offline) engine...")
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
                    // 有等待中的 startListening 请求，立即用 Vosk 启动
                    if (pendingStartListening) {
                        Log.d(TAG, "Vosk ready, executing pending startListening")
                        pendingStartListening = false
                        startListening()
                    }
                    // 已在用讯飞监听的情况下，自动切换到 Vosk
                    if (isListening && currentEngine == Engine.VOSK) {
                        Log.d(TAG, "Vosk ready, auto-switching from Xunfei to Vosk")
                        xunfeiService?.stopListening()
                        isListening = false
                        startListening()
                    }
                }
            }
        })
        
        // 始终尝试加载 Vosk 模型（不管文件是否已存在）
        voskService?.initModel { success ->
            isInitializing = false
            if (success) {
                isVoskModelReady = true
                currentEngine = Engine.VOSK
                Log.d(TAG, "Vosk engine (offline) ready")
            } else {
                // Vosk 加载失败，降级到讯飞
                Log.w(TAG, "Vosk model failed to load, falling back to Xunfei")
                fallbackToXunfei()
                // 如果有等待中的请求，立即用讯飞执行
                if (pendingStartListening) {
                    pendingStartListening = false
                    mainHandler.post { startListening() }
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
        commandListener?.let { xunfeiService?.setCommandListener(createXunfeiListenerWrapper(it)) }
        
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
        Log.d(TAG, "startListening called, engine=$currentEngine, isListening=$isListening, voskReady=$isVoskModelReady, initializing=$isInitializing")
        
        if (isListening) {
            Log.d(TAG, "Already listening, ignoring")
            return
        }
        
        // Vosk 正在加载中，排队等待
        if (isInitializing && !isVoskModelReady) {
            Log.d(TAG, "Vosk model still loading, queuing startListening request")
            pendingStartListening = true
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
                        fallbackToXunfei()
                        startXunfeiListening()
                    }
                } else {
                    // 模型未就绪，等待加载完成
                    Log.d(TAG, "Vosk model not ready, queuing startListening request")
                    pendingStartListening = true
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
            isPausedForTTS = false
            Log.d(TAG, "Stopped listening")
        }
    }
    
    fun isListening(): Boolean = isListening
    
    // TTS 播报时临时暂停录音
    private var isPausedForTTS = false
    
    private fun pauseListeningForTTS() {
        if (isListening) {
            Log.d(TAG, "Pausing listening for TTS playback")
            when (currentEngine) {
                Engine.VOSK -> voskService?.stopListening()
                Engine.XUNFEI -> xunfeiService?.stopListening()
                Engine.SYSTEM -> speechRecognizer?.stopListening()
            }
            isPausedForTTS = true
            // 不设 isListening=false，保持逻辑状态
        }
    }
    
    private fun resumeListeningAfterTTS() {
        if (isPausedForTTS) {
            Log.d(TAG, "Resuming listening after TTS playback")
            isPausedForTTS = false
            // 延迟 500ms 恢复，确保 TTS 音频完全结束
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isListening) {
                    when (currentEngine) {
                        Engine.VOSK -> voskService?.startListening()
                        Engine.XUNFEI -> xunfeiService?.startListening()
                        Engine.SYSTEM -> speechRecognizer?.startListening(
                            android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        )
                    }
                    Log.d(TAG, "Listening resumed after TTS")
                }
            }, 500)
        }
    }
    
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
        
        // 先用关键词初步判断是否可能是打电话命令
        val commandToParse = if (hasWakeWord) actualCommand else voiceInput
        val preliminaryCommand = commandProcessor.parseCommand(commandToParse)
        
        // 如果启用了 LLM 且检测到可能是打电话相关
        if (settings.llmEnabled && settings.llmApiKey.isNotEmpty() && 
            isPotentialCallCommand(commandToParse)) {
            Log.d(TAG, "Using LLM for smart analysis...")
            analyzeWithLLM(commandToParse)
        } else {
            // 直接用关键词解析结果
            Log.d(TAG, "Parsed command: type=${preliminaryCommand.type}, contact=${preliminaryCommand.contactName}")
            executeCommand(preliminaryCommand)
        }
    }
    
    /**
     * 判断是否可能是打电话命令（用于决定是否调用 LLM）
     */
    private fun isPotentialCallCommand(text: String): Boolean {
        val keywords = listOf("打", "电话", "视频", "语音", "通话", "呼叫", "联系", "找", "叫")
        return keywords.any { text.contains(it) }
    }
    
    /**
     * 使用 LLM 智能分析命令
     */
    private fun analyzeWithLLM(voiceInput: String) {
        // 配置 LLM
        llmService.setConfig(settings.llmApiUrl, settings.llmApiKey)
        
        // 获取联系人列表
        val contacts = settings.getContacts().keys.toList() + 
                       settings.getContactAliases().keys.toList()
        
        coroutineScope.launch {
            try {
                val result = llmService.analyzeCommand(voiceInput, contacts)
                
                mainHandler.post {
                    if (result.success && result.isCallCommand && result.contactName != null) {
                        Log.d(TAG, "LLM analysis: contact=${result.contactName}, isVideo=${result.isVideo}, confidence=${result.confidence}")
                        
                        // 创建命令并直接执行
                        val command = VoiceCommandProcessor.Command(
                            type = if (result.isVideo) VoiceCommandProcessor.CommandType.VIDEO_CALL 
                                   else VoiceCommandProcessor.CommandType.VOICE_CALL,
                            contactName = result.contactName,
                            rawCommand = voiceInput
                        )
                        executeCommand(command)
                    } else if (result.success && !result.isCallCommand) {
                        Log.d(TAG, "LLM: Not a call command")
                        // 不是打电话命令，忽略
                    } else {
                        Log.w(TAG, "LLM analysis failed: ${result.error}, falling back to keyword matching")
                        // LLM 失败，回退到关键词匹配
                        val command = commandProcessor.parseCommand(voiceInput)
                        executeCommand(command)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "LLM error", e)
                // 出错时回退到关键词匹配
                mainHandler.post {
                    val command = commandProcessor.parseCommand(voiceInput)
                    executeCommand(command)
                }
            }
        }
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
                    
                    // 将别名/语音识别名解析为微信名，确保用微信名去搜索
                    val wechatName = settings.matchContactName(command.contactName)
                    Log.d(TAG, "Executing video call to: wechatName=$wechatName (spoken: ${command.contactName})")
                    lastCommandTime = System.currentTimeMillis()
                    
                    // 先暂停录音再播报（TTSManager 的 onSpeechStart 会自动暂停）
                    speakHint("正在给${wechatName}拨打视频电话")
                    
                    // 正式停止监听（执行命令后不再恢复）
                    isListening = false
                    isPausedForTTS = false
                    
                    // 发送广播时用微信名，这样无障碍服务会用微信名在微信中搜索
                    val intent = Intent(EnhancedWeChatAccessibilityService.ACTION_MAKE_VIDEO_CALL).apply {
                        putExtra(EnhancedWeChatAccessibilityService.EXTRA_CONTACT_NAME, wechatName)
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
                    
                    // 将别名/语音识别名解析为微信名，确保用微信名去搜索
                    val wechatName = settings.matchContactName(command.contactName)
                    Log.d(TAG, "Executing voice call to: wechatName=$wechatName (spoken: ${command.contactName})")
                    lastCommandTime = System.currentTimeMillis()
                    
                    // 先暂停录音再播报（TTSManager 的 onSpeechStart 会自动暂停）
                    speakHint("正在给${wechatName}拨打语音电话")
                    
                    // 正式停止监听（执行命令后不再恢复）
                    isListening = false
                    isPausedForTTS = false
                    
                    // 发送广播时用微信名，这样无障碍服务会用微信名在微信中搜索
                    val intent = Intent(EnhancedWeChatAccessibilityService.ACTION_MAKE_VOICE_CALL).apply {
                        putExtra(EnhancedWeChatAccessibilityService.EXTRA_CONTACT_NAME, wechatName)
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
        // 包装 listener，拦截讯飞的致命错误以便自动恢复
        xunfeiService?.setCommandListener(createXunfeiListenerWrapper(listener))
    }
    
    /**
     * 包装讯飞的 listener，拦截连接失败等致命错误
     * 当讯飞断开时自动重置 isListening，让引擎切换逻辑可以接管
     */
    private fun createXunfeiListenerWrapper(delegate: VoiceCommandListener): VoiceCommandListener {
        return object : VoiceCommandListener {
            override fun onCommandRecognized(command: String) = delegate.onCommandRecognized(command)
            override fun onCommandExecuted(command: VoiceCommandProcessor.Command) = delegate.onCommandExecuted(command)
            override fun onWakeWordDetected() = delegate.onWakeWordDetected()
            override fun onWaitingForCommand() = delegate.onWaitingForCommand()
            override fun onModelDownloadProgress(progress: Int) = delegate.onModelDownloadProgress(progress)
            override fun onModelReady() = delegate.onModelReady()
            override fun onError(error: String) {
                delegate.onError(error)
                // 讯飞连接失败或授权失败 → 重置 isListening，尝试切换到 Vosk
                if (error.contains("连接失败") || error.contains("licc failed")) {
                    Log.w(TAG, "Xunfei fatal error detected: $error, attempting recovery...")
                    mainHandler.post {
                        if (isVoskModelReady) {
                            Log.d(TAG, "Switching to Vosk after Xunfei failure")
                            currentEngine = Engine.VOSK
                            isListening = false
                            startListening()
                        } else {
                            // Vosk 也没就绪，重置标志让后续 restartListening 可以重试讯飞
                            Log.d(TAG, "Vosk not ready, resetting isListening for retry")
                            isListening = false
                        }
                    }
                }
            }
        }
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
