package com.wechatassistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.wechatassistant.manager.AutoReplyManager
import com.wechatassistant.manager.MessageMonitor
import com.wechatassistant.manager.SettingsManager

/**
 * 增强版微信无障碍服务
 * 提供消息监控、自动回复、视频通话自动接听等功能
 */
class EnhancedWeChatAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "EnhancedWeChatService"
        const val WECHAT_PACKAGE = "com.tencent.mm"
        
        // 广播Action
        const val ACTION_SERVICE_CONNECTED = "com.wechatassistant.SERVICE_CONNECTED"
        const val ACTION_SERVICE_DISCONNECTED = "com.wechatassistant.SERVICE_DISCONNECTED"
        const val ACTION_NEW_MESSAGE = "com.wechatassistant.NEW_MESSAGE"
        const val ACTION_AUTO_REPLY_SENT = "com.wechatassistant.AUTO_REPLY_SENT"
        const val ACTION_GESTURE_CLICK_ACCEPT = "com.wechatassistant.GESTURE_CLICK_ACCEPT"
        const val ACTION_MAKE_VIDEO_CALL = "com.wechatassistant.MAKE_VIDEO_CALL"
        const val ACTION_MAKE_VOICE_CALL = "com.wechatassistant.MAKE_VOICE_CALL"
        const val EXTRA_CONTACT_NAME = "contact_name"
        
        // 微信控件ID（参考 OneTap 项目，可能随微信版本变化）
        const val WECHAT_ID_TABS = "com.tencent.mm:id/icon_tv"      // 底部导航栏
        const val WECHAT_ID_SEARCH = "com.tencent.mm:id/jha"        // 搜索按钮
        const val WECHAT_ID_INPUT = "com.tencent.mm:id/d98"         // 搜索输入框
        const val WECHAT_ID_LIST = "com.tencent.mm:id/odf"          // 搜索结果列表
        const val WECHAT_ID_MORE = "com.tencent.mm:id/bjz"          // 聊天界面+号
        const val WECHAT_ID_DIALOG = "com.tencent.mm:id/obc"        // 弹窗
        const val WECHAT_ID_CHAT_MENU = "com.tencent.mm:id/a1u"     // 聊天菜单
        
        // 微信 Activity 类名
        const val WECHAT_LAUNCHER = "com.tencent.mm.ui.LauncherUI"
        const val WECHAT_CHAT = "com.tencent.mm.ui.chatting.ChattingUI"
        const val WECHAT_SEARCH = "com.tencent.mm.plugin.fts.ui.FTSMainUI"
        
        // 服务实例（用于外部调用）
        @Volatile
        var instance: EnhancedWeChatAccessibilityService? = null
            private set
        
        fun isServiceRunning(): Boolean = instance != null
    }
    
    private lateinit var settings: SettingsManager
    private lateinit var autoReplyManager: AutoReplyManager
    private lateinit var messageMonitor: MessageMonitor
    private var ttsManager: TTSManager? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var lastProcessedMessage: String? = null
    private var lastReplyTime: Long = 0
    private val MIN_REPLY_INTERVAL = 3000L  // 最小回复间隔，防止频繁回复
    
    // 当前状态
    private var isInWeChatChat = false
    private var currentChatName: String? = null
    private var isVideoCallAnswering = false  // 防止重复接听
    private var lastVideoCallCheckTime = 0L   // 上次检查视频通话的时间
    private var isOnVideoCallScreen = false   // 是否在视频通话界面
    
    // 语音控制打电话状态
    private var isExecutingCallCommand = false
    private var isOperationCancelled = false  // 是否已取消操作
    private var hasEnteredWeChat = false      // 是否已经进入微信（用于离开检测）
    private var targetContactName: String? = null
    private var isVideoCall = true  // true=视频通话, false=语音通话
    private var callCommandStep = 0  // 当前执行步骤
    
    // 用户自定义坐标（通过坐标采集工具设置）
    private var coordSearchButton: Pair<Float, Float>? = null
    private var coordSearchInput: Pair<Float, Float>? = null
    private var coordFirstResult: Pair<Float, Float>? = null
    private var coordPlusButton: Pair<Float, Float>? = null
    private var coordVideoCall: Pair<Float, Float>? = null
    private var coordConfirmVideo: Pair<Float, Float>? = null   // 视频通话确认
    private var coordConfirmVoice: Pair<Float, Float>? = null   // 语音通话确认
    private var coordPasteButton: Pair<Float, Float>? = null
    
    // 微信视频/语音通话相关的界面类名
    private val VIDEO_CALL_ACTIVITIES = listOf(
        "com.tencent.mm.plugin.voip.ui.VideoActivity",
        "com.tencent.mm.plugin.voip.ui.VoipInputActivity", 
        "com.tencent.mm.plugin.voip.ui.VoipUIActivity",
        "com.tencent.mm.plugin.voip.ui.VoiceInputActivity",
        "VoipInputActivity",
        "VideoActivity",
        "VoipUIActivity"
    )
    
    // 接听按钮可能的文本
    private val ACCEPT_BUTTON_TEXTS = listOf(
        "接听", "接受", "视频接听", "语音接听", "接听视频", "接听语音"
    )
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        settings = SettingsManager.getInstance(this)
        autoReplyManager = AutoReplyManager(this)
        messageMonitor = MessageMonitor()
        
        if (settings.ttsEnabled) {
            ttsManager = TTSManager(this)
        }
        
        // 设置消息监听
        messageMonitor.setMessageListener(object : MessageMonitor.MessageListener {
            override fun onNewMessage(message: MessageMonitor.WeChatMessage) {
                handleNewMessage(message)
            }
            
            override fun onMessageListUpdated(messages: List<MessageMonitor.WeChatMessage>) {
                // 可以在这里更新UI或发送广播
            }
        })
    }
    
    // 广播接收器 - 接收各种命令
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, ">>> commandReceiver.onReceive: action=${intent?.action}")
            when (intent?.action) {
                ACTION_GESTURE_CLICK_ACCEPT -> {
                    Log.d(TAG, "Received gesture click broadcast!")
                    performAcceptButtonGestureClick()
                }
                ACTION_MAKE_VIDEO_CALL -> {
                    val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
                    Log.d(TAG, "Received make video call command for: $contactName")
                    if (!contactName.isNullOrEmpty()) {
                        startMakeCall(contactName, isVideo = true)
                    }
                }
                ACTION_MAKE_VOICE_CALL -> {
                    val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
                    Log.d(TAG, "Received make voice call command for: $contactName")
                    if (!contactName.isNullOrEmpty()) {
                        startMakeCall(contactName, isVideo = false)
                    }
                }
            }
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")
        instance = this
        
        // 加载用户自定义坐标
        loadCoordinates()
        
        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction(ACTION_GESTURE_CLICK_ACCEPT)
            addAction(ACTION_MAKE_VIDEO_CALL)
            addAction(ACTION_MAKE_VOICE_CALL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }
        Log.d(TAG, "Broadcast receiver registered for call commands")
        
        // 发送服务连接广播
        sendBroadcast(Intent(ACTION_SERVICE_CONNECTED))
        
        // 语音播报
        ttsManager?.speak("微信助手服务已启动")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""
        
        // 如果不是微信的事件，检查是否需要处理视频通话（视频通话可能有特殊的包名）
        if (packageName != WECHAT_PACKAGE) {
            // 但如果我们在视频通话界面，继续检查
            if (isOnVideoCallScreen && settings.autoAnswerVideo) {
                Log.d(TAG, "Non-WeChat event but on video call screen: $packageName")
                // 继续处理
            } else {
                if (isInWeChatChat) {
                    isInWeChatChat = false
                    currentChatName = null
                }
                return
            }
        }
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // 输入框文本变化
            }
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                handleNotification(event)
            }
        }
    }
    
    /**
     * 处理窗口状态变化
     */
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: return
        Log.d(TAG, "Window state changed: $className")
        
        // 检测是否在聊天界面
        isInWeChatChat = className.contains("ChattingUI") || 
                         className.contains("LauncherUI")
        
        // 检测是否是视频/语音通话界面
        val isVideoCallActivity = VIDEO_CALL_ACTIVITIES.any { 
            className.contains(it, ignoreCase = true) 
        }
        
        Log.d(TAG, "autoAnswerVideo=${settings.autoAnswerVideo}, isVideoCallActivity=$isVideoCallActivity, containsVoip=${className.contains("voip", ignoreCase = true)}")
        
        // 更新视频通话界面状态
        isOnVideoCallScreen = isVideoCallActivity || className.contains("voip", ignoreCase = true)
        
        // 检查视频通话（如果是通话界面或者开启了自动接听）
        if (settings.autoAnswerVideo && isOnVideoCallScreen) {
            Log.d(TAG, "Detected potential video call activity: $className")
            checkAndAutoAnswerVideoCall()
        }
        
        // 在聊天界面时监控消息
        if (isInWeChatChat && settings.messageMonitorEnabled) {
            val rootNode = rootInActiveWindow ?: return
            try {
                currentChatName = extractChatName(rootNode)
                messageMonitor.extractMessages(rootNode)
            } finally {
                rootNode.recycle()
            }
        }
    }
    
    /**
     * 处理窗口内容变化
     */
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        // 检查视频通话（窗口内容变化时也检查，确保不遗漏）
        // 如果在视频通话界面，更频繁检查
        if (settings.autoAnswerVideo && !isVideoCallAnswering) {
            val now = System.currentTimeMillis()
            val checkInterval = if (isOnVideoCallScreen) 300L else 1000L
            
            if (now - lastVideoCallCheckTime > checkInterval) {
                lastVideoCallCheckTime = now
                if (isOnVideoCallScreen) {
                    Log.d(TAG, "Content changed on video call screen, checking...")
                }
                checkAndAutoAnswerVideoCall()
            }
        }
        
        if (!isInWeChatChat) return
        
        val rootNode = rootInActiveWindow ?: return
        try {
            // 监控消息变化
            if (settings.messageMonitorEnabled) {
                messageMonitor.extractMessages(rootNode)
            }
        } finally {
            rootNode.recycle()
        }
    }
    
    /**
     * 处理通知
     */
    private fun handleNotification(event: AccessibilityEvent) {
        val text = event.text.joinToString("")
        Log.d(TAG, "Notification: $text")
        
        // 语音播报新消息
        if (settings.ttsEnabled && text.isNotBlank()) {
            ttsManager?.speak("收到新消息: $text")
        }
    }
    
    /**
     * 处理新消息
     */
    private fun handleNewMessage(message: MessageMonitor.WeChatMessage) {
        Log.d(TAG, "New message: ${message.content} from ${message.sender ?: message.chatName}")
        
        // 发送新消息广播
        sendBroadcast(Intent(ACTION_NEW_MESSAGE).apply {
            putExtra("content", message.content)
            putExtra("sender", message.sender)
            putExtra("chatName", message.chatName)
            putExtra("isGroupChat", message.isGroupChat)
        })
        
        // 如果是自己发的消息，不处理
        if (message.isSelf) return
        
        // 检查是否需要自动回复
        if (settings.autoReplyEnabled) {
            // 检查是否在群聊且不允许群聊回复
            if (message.isGroupChat && !settings.autoReplyInGroup) {
                return
            }
            
            // 检查联系人是否在允许列表中
            val contactName = message.sender ?: message.chatName ?: return
            if (!settings.isContactAllowed(contactName)) {
                Log.d(TAG, "Contact $contactName not allowed for auto reply")
                return
            }
            
            // 防止频繁回复
            val now = System.currentTimeMillis()
            if (now - lastReplyTime < MIN_REPLY_INTERVAL) {
                Log.d(TAG, "Reply too frequent, skipping")
                return
            }
            
            // 防止重复回复同一条消息
            val messageKey = "${message.content}_${message.chatName}"
            if (messageKey == lastProcessedMessage) {
                return
            }
            
            // 获取自动回复内容
            val reply = autoReplyManager.getReply(message.content, message.isGroupChat)
            if (reply != null) {
                lastProcessedMessage = messageKey
                // 延迟回复，更自然
                handler.postDelayed({
                    sendAutoReply(reply)
                }, settings.autoReplyDelay)
            }
        }
    }
    
    /**
     * 发送自动回复
     */
    private fun sendAutoReply(replyText: String) {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // 查找输入框
            val inputNode = findInputBox(rootNode)
            if (inputNode == null) {
                Log.e(TAG, "Input box not found")
                return
            }
            
            // 设置输入框文本
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replyText)
            inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            inputNode.recycle()
            
            // 延迟后点击发送按钮
            handler.postDelayed({
                clickSendButton()
            }, 500)
            
            lastReplyTime = System.currentTimeMillis()
            Log.d(TAG, "Auto reply sent: $replyText")
            
            // 发送广播
            sendBroadcast(Intent(ACTION_AUTO_REPLY_SENT).apply {
                putExtra("reply", replyText)
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending auto reply", e)
        } finally {
            rootNode.recycle()
        }
    }
    
    /**
     * 查找输入框
     */
    private fun findInputBox(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 通过ID查找
        var nodes = rootNode.findAccessibilityNodeInfosByViewId(MessageMonitor.WECHAT_INPUT_ID)
        if (nodes.isNotEmpty()) {
            return nodes[0]
        }
        
        // 备用方法：查找EditText
        return findNodeByClassName(rootNode, "android.widget.EditText")
    }
    
    /**
     * 点击发送按钮
     */
    private fun clickSendButton() {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // 通过ID查找发送按钮
            var nodes = rootNode.findAccessibilityNodeInfosByViewId(MessageMonitor.WECHAT_SEND_BUTTON_ID)
            if (nodes.isNotEmpty()) {
                nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                nodes.forEach { it.recycle() }
                return
            }
            
            // 备用方法：通过文本查找
            val sendButton = findNodeByText(rootNode, "发送")
            sendButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            sendButton?.recycle()
            
        } finally {
            rootNode.recycle()
        }
    }
    
    /**
     * 检查并自动接听视频通话
     */
    private fun checkAndAutoAnswerVideoCall() {
        Log.d(TAG, "checkAndAutoAnswerVideoCall() called")
        
        if (isVideoCallAnswering) {
            Log.d(TAG, "Already answering a call, skipping")
            return
        }
        
        // 尝试获取活动窗口
        var rootNode = rootInActiveWindow
        
        // 如果活动窗口为空或没有子节点，尝试遍历所有窗口
        if (rootNode == null || rootNode.childCount == 0) {
            Log.d(TAG, "rootInActiveWindow is empty, trying getWindows()")
            rootNode?.recycle()
            rootNode = null
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val allWindows = windows
                Log.d(TAG, "Found ${allWindows.size} windows total")
                
                // 先打印所有窗口信息
                for ((index, window) in allWindows.withIndex()) {
                    val windowRoot = window.root
                    val childCount = windowRoot?.childCount ?: -1
                    Log.d(TAG, "Window[$index]: title='${window.title}', type=${window.type}, layer=${window.layer}, childCount=$childCount")
                    windowRoot?.recycle()
                }
                
                // 优先查找微信窗口（type=1 是应用窗口）
                for (window in allWindows) {
                    val windowRoot = window.root
                    if (windowRoot != null) {
                        val title = window.title?.toString() ?: ""
                        // type=1 是应用窗口，优先使用
                        // 或者标题包含微信相关内容
                        if (window.type == 1 || title.contains("微信") || title.contains("WeChat")) {
                            if (windowRoot.childCount > 0) {
                                Log.d(TAG, "Selected WeChat window: title='$title', type=${window.type}, childCount=${windowRoot.childCount}")
                                rootNode = windowRoot
                                break
                            }
                        }
                        if (rootNode == null) {
                            windowRoot.recycle()
                        }
                    }
                }
                
                // 如果没找到微信窗口，使用第一个有内容且不是状态栏(type=3)的窗口
                if (rootNode == null) {
                    for (window in allWindows) {
                        if (window.type == 3) continue  // 跳过状态栏
                        val windowRoot = window.root
                        if (windowRoot != null && windowRoot.childCount > 0) {
                            Log.d(TAG, "Fallback to window: title='${window.title}', type=${window.type}, childCount=${windowRoot.childCount}")
                            rootNode = windowRoot
                            break
                        }
                        windowRoot?.recycle()
                    }
                }
            }
        }
        
        if (rootNode == null) {
            Log.d(TAG, "No valid root node found!")
            return
        }
        
        Log.d(TAG, "Got root node, child count: ${rootNode.childCount}")
        
        try {
            // 打印节点树用于调试
            Log.d(TAG, "=== START NODE TREE ===")
            printNodeTree(rootNode, 0)
            Log.d(TAG, "=== END NODE TREE ===")
            
            // 尝试多种方式查找接听按钮
            var acceptButton: AccessibilityNodeInfo? = null
            var buttonRect: android.graphics.Rect? = null
            
            // 方法1: 直接查找可点击的接听按钮
            for (text in ACCEPT_BUTTON_TEXTS) {
                acceptButton = findClickableNodeByText(rootNode, text)
                if (acceptButton != null) {
                    Log.d(TAG, "Found clickable button with text: $text, clickable: ${acceptButton.isClickable}")
                    break
                }
            }
            
            // 方法2: 查找文本节点然后找其可点击的父节点
            if (acceptButton == null) {
                for (text in ACCEPT_BUTTON_TEXTS) {
                    val textNode = findNodeByText(rootNode, text)
                    if (textNode != null) {
                        Log.d(TAG, "Found text node: $text, finding clickable parent...")
                        // 保存文本节点的位置
                        buttonRect = android.graphics.Rect()
                        textNode.getBoundsInScreen(buttonRect)
                        
                        acceptButton = findClickableParent(textNode)
                        textNode.recycle()
                        
                        if (acceptButton != null) {
                            Log.d(TAG, "Found clickable parent for text: $text")
                            break
                        }
                    }
                }
            }
            
            // 方法3: 通过 content-description 查找
            if (acceptButton == null) {
                acceptButton = findClickableNodeByContentDescription(rootNode, "接听")
                    ?: findClickableNodeByContentDescription(rootNode, "接受")
                    ?: findClickableNodeByContentDescription(rootNode, "视频接听")
                if (acceptButton != null) {
                    Log.d(TAG, "Found button by content description")
                }
            }
            
            // 获取按钮位置
            if (acceptButton != null && buttonRect == null) {
                buttonRect = android.graphics.Rect()
                acceptButton.getBoundsInScreen(buttonRect)
            }
            
            if (acceptButton == null) {
                Log.d(TAG, "NO ACCEPT BUTTON FOUND! WeChat's video call UI is not accessible.")
                Log.d(TAG, "Please enable CallNotificationListenerService to auto-answer via notification!")
                
                // 发送广播请求通知监听服务帮助接听
                if (CallNotificationListenerService.isServiceRunning()) {
                    Log.d(TAG, "Notification listener service is running, it will handle auto-answer")
                } else {
                    Log.w(TAG, "Notification listener service is NOT running! Auto-answer will not work!")
                }
            }
            
            if (acceptButton != null && buttonRect != null) {
                isVideoCallAnswering = true
                
                val centerX = buttonRect.centerX().toFloat()
                val centerY = buttonRect.centerY().toFloat()
                
                Log.d(TAG, "Video call detected, button at ($centerX, $centerY), auto answering in ${settings.autoAnswerDelay}ms")
                
                // 语音播报
                ttsManager?.announceIncomingCall(currentChatName ?: "未知联系人")
                
                // 延迟自动接听
                handler.postDelayed({
                    try {
                        var clickSuccess = false
                        
                        // 尝试1: 重新查找并点击
                        val newRoot = rootInActiveWindow
                        if (newRoot != null) {
                            for (text in ACCEPT_BUTTON_TEXTS) {
                                val newButton = findClickableNodeByText(newRoot, text)
                                    ?: findNodeByText(newRoot, text)?.let { textNode ->
                                        val parent = findClickableParent(textNode)
                                        textNode.recycle()
                                        parent
                                    }
                                
                                if (newButton != null) {
                                    clickSuccess = newButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    Log.d(TAG, "Click on node result: $clickSuccess")
                                    newButton.recycle()
                                    if (clickSuccess) break
                                }
                            }
                            newRoot.recycle()
                        }
                        
                        // 尝试2: 使用手势点击
                        if (!clickSuccess) {
                            Log.d(TAG, "Node click failed, trying gesture click at ($centerX, $centerY)")
                            performClickGesture(centerX, centerY)
                        }
                        
                        ttsManager?.announceCallAction("已自动接听")
                        Log.d(TAG, "Video call auto answer attempt completed")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error auto answering", e)
                    } finally {
                        // 延迟重置状态
                        handler.postDelayed({
                            isVideoCallAnswering = false
                        }, 5000)
                    }
                }, settings.autoAnswerDelay)
                
                acceptButton.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking video call", e)
        } finally {
            rootNode.recycle()
        }
    }
    
    /**
     * 执行点击手势（更可靠的版本）
     */
    private fun performClickGesture(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(x, y)
            path.lineTo(x, y)
            
            val strokeDesc = GestureDescription.StrokeDescription(path, 0, 50)
            val gesture = GestureDescription.Builder()
                .addStroke(strokeDesc)
                .build()
            
            val result = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Gesture click completed at ($x, $y)")
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Gesture click cancelled at ($x, $y)")
                }
            }, null)
            
            Log.d(TAG, "Dispatch gesture result: $result")
        }
    }
    
    /**
     * 在微信视频通话界面的接听按钮位置执行手势点击
     * 微信接听按钮通常在屏幕底部右侧（绿色按钮）
     * 尝试多个位置以覆盖不同版本的微信布局
     */
    private fun performAcceptButtonGestureClick() {
        Log.d(TAG, "performAcceptButtonGestureClick() called")
        
        // 获取屏幕尺寸
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        Log.d(TAG, "Screen size: ${screenWidth}x${screenHeight}")
        
        // 微信视频通话来电界面的接听按钮可能在不同位置
        // 尝试多个位置覆盖不同版本和屏幕布局
        // 重点覆盖屏幕最底部区域
        val clickPositions = listOf(
            // 最底部区域 (按钮可能非常靠下)
            Pair(screenWidth * 0.75f, screenHeight * 0.92f),  // 右下角最底
            Pair(screenWidth * 0.75f, screenHeight * 0.95f),  // 更底
            Pair(screenWidth * 0.80f, screenHeight * 0.90f),  // 右下
            Pair(screenWidth * 0.70f, screenHeight * 0.92f),  // 稍左下
            // 中下部区域
            Pair(screenWidth * 0.75f, screenHeight * 0.85f),
            Pair(screenWidth * 0.75f, screenHeight * 0.80f),
            Pair(screenWidth * 0.80f, screenHeight * 0.88f),
            // 如果是横向排列的两个按钮，绿色接听在右边
            Pair(screenWidth * 0.72f, screenHeight * 0.90f)
        )
        
        // 快速依次尝试每个位置
        clickPositions.forEachIndexed { index, (x, y) ->
            handler.postDelayed({
                Log.d(TAG, "Click attempt ${index + 1} at ($x, $y)")
                performClickGesture(x, y)
            }, (index * 200).toLong())  // 每200ms尝试一个位置，更快
        }
        
        // 语音播报
        handler.postDelayed({
            ttsManager?.announceCallAction("已尝试自动接听")
        }, (clickPositions.size * 200 + 500).toLong())
        
        // 重置状态
        handler.postDelayed({
            isVideoCallAnswering = false
        }, 5000)
    }
    
    // ==================== 语音控制打电话功能 ====================
    
    /**
     * 开始执行打电话命令
     * @param contactName 联系人名称
     * @param isVideo true=视频通话, false=语音通话
     */
    fun startMakeCall(contactName: String, isVideo: Boolean) {
        if (isExecutingCallCommand) {
            Log.d(TAG, "Already executing a call command, ignoring")
            ttsManager?.speak("正在执行中，请稍候")
            return
        }
        
        Log.d(TAG, "Starting make call: contact=$contactName, isVideo=$isVideo")
        isExecutingCallCommand = true
        isOperationCancelled = false
        targetContactName = contactName
        isVideoCall = isVideo
        callCommandStep = 0
        
        val callType = if (isVideo) "视频" else "语音"
        ttsManager?.speak("正在给${contactName}拨打${callType}电话")
        
        // 显示全屏遮罩，防止用户误操作
        CallOverlayService.show(this, contactName, isVideo)
        
        // 步骤1: 打开微信
        handler.postDelayed({
            if (!isOperationCancelled) {
                openWeChat()
            }
        }, 1000)
    }
    
    /**
     * 取消当前操作
     */
    fun cancelCurrentOperation() {
        Log.d(TAG, "Cancelling current operation")
        isOperationCancelled = true
        ttsManager?.speak("已取消")
        resetCallCommand()
    }
    
    /**
     * 步骤1: 打开微信
     */
    private fun openWeChat() {
        if (isOperationCancelled) return
        
        Log.d(TAG, "Step 1: Opening WeChat")
        callCommandStep = 1
        CallOverlayService.updateStatus("正在打开微信...")
        
        try {
            // 方法1: 尝试获取启动Intent
            var launchIntent = packageManager.getLaunchIntentForPackage(WECHAT_PACKAGE)
            
            // 方法2: 如果方法1失败，尝试直接构造Intent
            if (launchIntent == null) {
                Log.d(TAG, "getLaunchIntentForPackage returned null, trying explicit intent")
                launchIntent = Intent().apply {
                    setClassName(WECHAT_PACKAGE, "com.tencent.mm.ui.LauncherUI")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            } else {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            startActivity(launchIntent)
            Log.d(TAG, "WeChat launch intent sent")
            
            // 等待微信打开后，点击搜索
            handler.postDelayed({
                if (!isOperationCancelled) {
                    clickSearchButton()
                }
            }, 2000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WeChat", e)
            ttsManager?.speak("打开微信失败，请确认微信已安装")
            CallOverlayService.updateStatus("打开微信失败")
            handler.postDelayed({ resetCallCommand() }, 2000)
        }
    }
    
    /**
     * 步骤2: 点击搜索按钮
     * 直接使用坐标点击（更可靠）
     */
    private fun clickSearchButton() {
        if (isOperationCancelled) return
        
        Log.d(TAG, "========== Step 2: Click search button ==========")
        callCommandStep = 2
        CallOverlayService.updateStatus("正在点击搜索...")
        
        // 优先使用用户自定义坐标
        val coord = coordSearchButton
        val (searchX, searchY) = if (coord != null) {
            Log.d(TAG, "Using user-defined coordinates for search button")
            coord
        } else {
            val displayMetrics = DisplayMetrics()
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()
            
            // 搜索图标在右上角，根据截图位置计算
            val x = screenWidth * 0.82f
            val y = screenHeight * 0.045f + 50f
            Pair(x, y)
        }
        
        Log.d(TAG, "Clicking search at ($searchX, $searchY)")
        performClickGesture(searchX, searchY)
        
        handler.postDelayed({
            if (!isOperationCancelled) {
                inputContactName()
            }
        }, 1500)
    }
    
    /**
     * 点击节点（递归查找可点击父节点）
     */
    private fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        node ?: return false
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            node.parent?.let { clickNode(it) } ?: false
        }
    }
    
    /**
     * 步骤3: 在搜索框输入联系人名称
     * 方案：使用剪贴板粘贴（因为无法直接访问微信UI节点）
     */
    private var inputRetryCount = 0
    
    private fun inputContactName() {
        if (isOperationCancelled) return
        
        Log.d(TAG, "========== Step 3: Input contact name: $targetContactName ==========")
        callCommandStep = 3
        CallOverlayService.updateStatus("正在输入联系人名称...")
        
        val contactName = targetContactName ?: ""
        if (contactName.isEmpty()) {
            Log.e(TAG, "Contact name is empty!")
            CallOverlayService.updateStatus("联系人名称为空")
            handler.postDelayed({ resetCallCommand() }, 2000)
            return
        }
        
        // 直接使用多窗口搜索方式输入文本（更可靠）
        Log.d(TAG, "Will try to input text: $contactName")
        
        handler.postDelayed({
            if (!isOperationCancelled) {
                trySetTextToSearchInput(contactName)
            }
        }, 800)
    }
    
    /**
     * 尝试设置文本到搜索输入框
     */
    private fun trySetTextToSearchInput(text: String) {
        Log.d(TAG, "===== trySetTextToSearchInput: $text =====")
        
        // 打印所有窗口信息
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "Total windows: ${windows.size}")
            
            for ((index, window) in windows.withIndex()) {
                val root = window.root
                Log.d(TAG, "Window[$index]: title=${window.title}, root=${root != null}")
                
                if (root != null) {
                    // 打印根节点下的所有节点
                    printNodeTree(root, 0)
                    
                    // 查找 EditText 节点
                    val editNode = findEditableNode(root)
                    if (editNode != null) {
                        Log.d(TAG, ">>> Found editable node: ${editNode.className}, isEditable=${editNode.isEditable}")
                        
                        // 先点击获取焦点
                        val focusResult = editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        val clickResult = editNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d(TAG, "Focus: $focusResult, Click: $clickResult")
                        
                        // 设置文本
                        val args = Bundle()
                        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                        val result = editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        Log.d(TAG, ">>> ACTION_SET_TEXT result: $result")
                        
                        editNode.recycle()
                        root.recycle()
                        
                        if (result) {
                            handler.postDelayed({
                                clickFirstSearchResult()
                            }, 1500)
                            return
                        }
                    }
                    root.recycle()
                }
            }
        }
        
        // 如果找不到编辑框，尝试粘贴方法
        Log.w(TAG, "===== No editable node found, trying paste =====")
        
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("contact", text)
        clipboard.setPrimaryClip(clip)
        Log.d(TAG, "Copied '$text' to clipboard")
        
        performLongPressAndPaste()
    }
    
    /**
     * 递归查找可编辑节点
     */
    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        val className = node.className?.toString() ?: ""
        if (className.contains("EditText")) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableNode(child)
            child.recycle()
            if (result != null) {
                return result
            }
        }
        return null
    }
    
    /**
     * 长按输入框触发粘贴菜单
     */
    private fun performLongPressAndPaste() {
        Log.d(TAG, "===== Performing long press to paste =====")
        
        // 优先使用用户自定义坐标
        val coord = coordSearchInput
        val (inputX, inputY) = if (coord != null) {
            Log.d(TAG, "Using user-defined coordinates for search input")
            coord
        } else {
            // 默认：搜索框位置
            Pair(300f, 70f)
        }
        
        Log.d(TAG, "Long pressing at ($inputX, $inputY)")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(inputX, inputY)
            
            val strokeDesc = GestureDescription.StrokeDescription(path, 0, 800)
            val gesture = GestureDescription.Builder()
                .addStroke(strokeDesc)
                .build()
            
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Long press completed, waiting for paste menu...")
                    
                    // 延迟等待粘贴菜单出现
                    handler.postDelayed({
                        clickPasteButton()
                    }, 800)
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.e(TAG, "Long press cancelled")
                    inputRetryCount++
                    if (inputRetryCount < 3) {
                        handler.postDelayed({ performLongPressAndPaste() }, 500)
                    } else {
                        ttsManager?.speak("粘贴失败")
                        resetCallCommand()
                    }
                }
            }, null)
        }
    }
    
    /**
     * 查找焦点输入节点
     */
    private fun findFocusedInputNode(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        rootNode.recycle()
        return focusedNode
    }
    
    /**
     * 点击粘贴按钮
     */
    private fun clickPasteButton() {
        Log.d(TAG, "===== Looking for paste button =====")
        
        // 打印所有窗口找粘贴按钮
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "Total windows: ${windows.size}")
            
            for ((index, window) in windows.withIndex()) {
                val root = window.root ?: continue
                Log.d(TAG, "Window[$index]: title=${window.title}")
                
                // 打印节点树找粘贴
                printNodeTree(root, 0)
                
                val pasteNode = findNodeByTextOrDesc(root, "粘贴")
                if (pasteNode != null) {
                    Log.d(TAG, ">>> Found paste node! <<<")
                    
                    // 获取节点边界并点击中心
                    val rect = Rect()
                    pasteNode.getBoundsInScreen(rect)
                    val centerX = rect.centerX().toFloat()
                    val centerY = rect.centerY().toFloat()
                    Log.d(TAG, ">>> Paste bounds: $rect, clicking at ($centerX, $centerY)")
                    
                    performClickGesture(centerX, centerY)
                    pasteNode.recycle()
                    root.recycle()
                    
                    handler.postDelayed({
                        clickFirstSearchResult()
                    }, 2000)
                    return
                }
                root.recycle()
            }
        }
        
        // 备用：优先使用用户自定义坐标，否则使用默认坐标
        val coord = coordPasteButton
        val (pasteX, pasteY) = if (coord != null) {
            Log.w(TAG, ">>> Paste node NOT found, using user-defined coordinates")
            coord
        } else {
            Log.w(TAG, ">>> Paste node NOT found, using default coordinates (130, 175)")
            Pair(130f, 175f)
        }
        performClickGesture(pasteX, pasteY)
        
        handler.postDelayed({
            clickFirstSearchResult()
        }, 2000)
    }
    
    /**
     * 通过文本或描述查找节点
     */
    private fun findNodeByTextOrDesc(node: AccessibilityNodeInfo, target: String): AccessibilityNodeInfo? {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        
        if (text.contains(target) || desc.contains(target)) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByTextOrDesc(child, target)
            child.recycle()
            if (result != null) {
                return result
            }
        }
        return null
    }
    
    /**
     * 输入文本到节点（递归查找可编辑节点）
     */
    private fun inputText(node: AccessibilityNodeInfo?, text: String): Boolean {
        node ?: return false
        
        Log.d(TAG, "inputText: text='$text', node.className=${node.className}, isEditable=${node.isEditable}, isFocusable=${node.isFocusable}")
        
        if (node.isEditable) {
            // 先尝试聚焦节点
            if (node.isFocusable) {
                val focusResult = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                Log.d(TAG, "Focus action result: $focusResult")
            }
            
            // 尝试点击节点
            if (node.isClickable) {
                val clickResult = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Click action result: $clickResult")
            }
            
            // 设置文本
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "ACTION_SET_TEXT result: $result")
            
            if (!result) {
                // 如果 SET_TEXT 失败，尝试使用剪贴板
                Log.d(TAG, "SET_TEXT failed, trying clipboard")
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("text", text)
                clipboard.setPrimaryClip(clip)
                
                // 尝试粘贴
                val pasteResult = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                Log.d(TAG, "ACTION_PASTE result: $pasteResult")
                return pasteResult
            }
            
            return result
        } else {
            Log.d(TAG, "Node not editable, trying parent")
            return node.parent?.let { 
                val parentResult = inputText(it, text)
                it.recycle()
                parentResult
            } ?: false
        }
    }
    
    /**
     * 查找 EditText 节点
     */
    private fun findEditText(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className?.toString()?.contains("EditText") == true) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditText(child)
            child.recycle()
            if (result != null) {
                return result
            }
        }
        return null
    }
    
    /**
     * 步骤4: 点击搜索结果中的第一个联系人
     */
    private fun clickFirstSearchResult() {
        if (isOperationCancelled) return
        
        Log.d(TAG, "========== Step 4: Click first search result ==========")
        callCommandStep = 4
        CallOverlayService.updateStatus("正在选择联系人...")
        
        // 优先使用用户自定义坐标
        val coord = coordFirstResult
        val (resultX, resultY) = if (coord != null) {
            Log.d(TAG, "Using user-defined coordinates for first result")
            coord
        } else {
            val displayMetrics = DisplayMetrics()
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()
            
            // 搜索结果第一项大约在屏幕高度 18-20% 处（搜索框下方）
            Pair(screenWidth * 0.5f, screenHeight * 0.18f)
        }
        
        Log.d(TAG, "Clicking first search result at ($resultX, $resultY)")
        performClickGesture(resultX, resultY)
        
        handler.postDelayed({
            if (!isOperationCancelled) {
                clickPlusButton()
            }
        }, 2000)
    }
    
    // 保留原方法但不再使用
    private fun findContactInChatList() {
        // 改用搜索方式
        clickSearchButton()
    }
    
    /**
     * 打印所有可见的文本内容
     */
    private fun printAllVisibleText(node: AccessibilityNodeInfo, depth: Int = 0) {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        
        if (text.isNotEmpty() || desc.isNotEmpty()) {
            val indent = "  ".repeat(depth)
            Log.d(TAG, "${indent}TEXT: '$text' DESC: '$desc' BOUNDS: $rect CLASS: ${node.className}")
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            printAllVisibleText(child, depth + 1)
            child.recycle()
        }
    }
    
    /**
     * 递归查找包含指定文本的节点
     */
    private fun findNodeWithTextRecursive(node: AccessibilityNodeInfo, targetText: String): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString() ?: ""
        val nodeDesc = node.contentDescription?.toString() ?: ""
        
        // 检查当前节点是否包含目标文本
        if (nodeText.contains(targetText) || nodeDesc.contains(targetText)) {
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            // 确保节点有有效的位置和大小，且在聊天列表区域（排除顶部标题栏）
            if (rect.width() > 0 && rect.height() > 0 && rect.top > 150) {
                Log.d(TAG, "Match found: text='$nodeText', desc='$nodeDesc', bounds=$rect")
                return AccessibilityNodeInfo.obtain(node)
            }
        }
        
        // 递归搜索子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeWithTextRecursive(child, targetText)
            child.recycle()
            if (result != null) {
                return result
            }
        }
        
        return null
    }
    
    /**
     * 打印所有文本节点（用于调试）
     */
    private fun printAllTextNodes(node: AccessibilityNodeInfo, depth: Int) {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        
        if (text.isNotEmpty() || desc.isNotEmpty()) {
            val indent = "  ".repeat(depth)
            Log.d(TAG, "${indent}Text: '$text', Desc: '$desc', Class: ${node.className}")
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            printAllTextNodes(child, depth + 1)
            child.recycle()
        }
    }
    
    private var scrollAttempts = 0
    
    /**
     * 滑动聊天列表查找联系人
     */
    private fun scrollAndFindContact() {
        scrollAttempts++
        
        if (scrollAttempts > 3) {
            Log.e(TAG, "Contact not found after scrolling")
            ttsManager?.speak("在聊天列表中未找到${targetContactName}")
            resetCallCommand()
            scrollAttempts = 0
            return
        }
        
        Log.d(TAG, "Scrolling to find contact, attempt $scrollAttempts")
        
        // 向下滑动
        val displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        val startX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.7f
        val endY = displayMetrics.heightPixels * 0.3f
        
        performSwipeGesture(startX, startY, startX, endY)
        
        // 滑动后再次查找
        handler.postDelayed({
            findContactInChatList()
        }, 1000)
    }
    
    /**
     * 执行滑动手势
     */
    private fun performSwipeGesture(startX: Float, startY: Float, endX: Float, endY: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)
            
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            
            dispatchGesture(gestureBuilder.build(), null, null)
        }
    }
    
    /**
     * 步骤5: 点击通话按钮
     */
    /**
     * 步骤5: 点击聊天界面的 + 号按钮
     */
    private fun clickPlusButton() {
        if (isOperationCancelled) return
        
        Log.d(TAG, "========== Step 5: Click plus button ==========")
        callCommandStep = 5
        CallOverlayService.updateStatus("正在打开功能菜单...")
        
        // 优先使用用户自定义坐标
        val coord = coordPlusButton
        val (plusX, plusY) = if (coord != null) {
            Log.d(TAG, "Using user-defined coordinates for plus button")
            coord
        } else {
            val displayMetrics = DisplayMetrics()
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()
            
            // + 号按钮在聊天输入框右侧（右下角）
            Pair(screenWidth * 0.92f, screenHeight * 0.94f)
        }
        
        Log.d(TAG, "Clicking + button at ($plusX, $plusY)")
        performClickGesture(plusX, plusY)
        
        handler.postDelayed({
            if (!isOperationCancelled) {
                clickVideoCallOption()
            }
        }, 1500)
    }
    
    /**
     * 步骤6: 点击视频/语音通话选项
     * + 菜单展开后，视频通话在底部弹出的菜单第一行
     */
    private fun clickVideoCallOption() {
        if (isOperationCancelled) return
        
        Log.d(TAG, "========== Step 6: Click call option, isVideo=$isVideoCall ==========")
        callCommandStep = 6
        val callType = if (isVideoCall) "视频" else "语音"
        CallOverlayService.updateStatus("正在选择${callType}通话...")
        
        // 优先使用用户自定义坐标
        val coord = coordVideoCall
        val (optionX, optionY) = if (coord != null) {
            Log.d(TAG, "Using user-defined coordinates for video call option")
            coord
        } else {
            val displayMetrics = DisplayMetrics()
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()
            
            // + 菜单展开后，是底部弹出的面板
            // 视频通话在第一行第三个位置
            Pair(screenWidth * 0.83f, screenHeight * 0.78f)
        }
        
        Log.d(TAG, "Clicking video call option at ($optionX, $optionY)")
        performClickGesture(optionX, optionY)
        
        handler.postDelayed({
            if (!isOperationCancelled) {
                clickVideoCallConfirm()
            }
        }, 1500)
    }
    
    /**
     * 步骤7: 点击视频/语音通话确认（第二次点击）
     * 弹窗确认选择
     */
    private fun clickVideoCallConfirm() {
        if (isOperationCancelled) return
        
        Log.d(TAG, "Step 7: Click call confirm")
        callCommandStep = 7
        val callType = if (isVideoCall) "视频" else "语音"
        CallOverlayService.updateStatus("正在确认${callType}通话...")
        
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.d(TAG, "Root node is null, call may have started")
            finishCall()
            return
        }
        
        // 再次查找 "视频通话" 或 "语音通话"
        val targetText = if (isVideoCall) "视频通话" else "语音通话"
        val confirmNodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        Log.d(TAG, "Found ${confirmNodes.size} confirm nodes with '$targetText'")
        
        if (confirmNodes.isNotEmpty()) {
            val confirmNode = confirmNodes.first()
            if (clickNode(confirmNode)) {
                Log.d(TAG, "Clicked confirm by text!")
                confirmNode.recycle()
                rootNode.recycle()
                finishCall()
                return
            }
            
            // 尝试坐标点击
            val rect = android.graphics.Rect()
            confirmNode.getBoundsInScreen(rect)
            if (rect.width() > 0) {
                performClickGesture(rect.exactCenterX(), rect.exactCenterY())
                confirmNode.recycle()
                rootNode.recycle()
                finishCall()
                return
            }
            confirmNode.recycle()
        }
        
        rootNode.recycle()
        
        // 使用用户自定义坐标（根据通话类型选择）
        val coord = if (isVideoCall) coordConfirmVideo else coordConfirmVoice
        if (coord != null) {
            Log.d(TAG, "Using user-defined coordinates for ${if (isVideoCall) "video" else "voice"} call confirm")
            performClickGesture(coord.first, coord.second)
        } else {
            Log.d(TAG, "No user-defined coordinates for call confirm")
        }
        
        finishCall()
    }
    
    private fun finishCall() {
        val callType = if (isVideoCall) "视频" else "语音"
        val contactName = targetContactName ?: ""
        
        CallOverlayService.updateStatus("✅ 已发起${callType}通话")
        ttsManager?.speak("正在给${contactName}拨打${callType}电话")
        Log.d(TAG, "Call initiated!")
        
        // 延迟关闭遮罩，让用户看到成功提示
        handler.postDelayed({
            resetCallCommand()
        }, 1500)
    }
    
    // 保留旧函数名以兼容
    private fun clickCallOption() {
        clickVideoCallOption()
    }
    
    private fun clickCallOptionConfirm() {
        clickVideoCallConfirm()
    }
    
    /**
     * 重置打电话命令状态
     */
    private fun resetCallCommand() {
        isExecutingCallCommand = false
        isOperationCancelled = false
        hasEnteredWeChat = false
        targetContactName = null
        callCommandStep = 0
        scrollAttempts = 0
        
        // 隐藏遮罩
        CallOverlayService.hide()
    }
    
    /**
     * 通过类名查找节点
     */
    private fun findNodesByClassName(root: AccessibilityNodeInfo, className: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClassNameRecursive(root, className, result)
        return result
    }
    
    private fun findNodesByClassNameRecursive(node: AccessibilityNodeInfo, className: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node.className?.toString() == className) {
            result.add(AccessibilityNodeInfo.obtain(node))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesByClassNameRecursive(child, className, result)
            child.recycle()
        }
    }
    
    // ==================== 语音控制打电话功能结束 ====================
    
    /**
     * 查找可点击的节点（通过文本）
     */
    private fun findClickableNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString() ?: ""
        val nodeDesc = node.contentDescription?.toString() ?: ""
        
        // 如果节点包含文本且可点击
        if ((nodeText.contains(text, ignoreCase = true) || nodeDesc.contains(text, ignoreCase = true)) 
            && node.isClickable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        // 递归搜索子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findClickableNodeByText(child, text)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        return null
    }
    
    /**
     * 查找可点击的节点（通过 content-description）
     */
    private fun findClickableNodeByContentDescription(node: AccessibilityNodeInfo, description: String): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString()?.contains(description, ignoreCase = true) == true 
            && node.isClickable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findClickableNodeByContentDescription(child, description)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        return null
    }
    
    /**
     * 向上查找可点击的父节点
     */
    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current = node.parent
        var depth = 0
        val maxDepth = 10
        
        while (current != null && depth < maxDepth) {
            if (current.isClickable) {
                Log.d(TAG, "Found clickable parent at depth $depth, class: ${current.className}")
                return current
            }
            
            val parent = current.parent
            current.recycle()
            current = parent
            depth++
        }
        
        current?.recycle()
        return null
    }
    
    /**
     * 打印节点树（用于调试）
     */
    private fun printNodeTree(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > 15) return  // 防止太深
        
        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val clickable = node.isClickable
        val className = node.className?.toString()?.substringAfterLast(".") ?: "?"
        
        // 打印所有节点
        Log.d(TAG, "${indent}[$className] t='$text' d='$desc' click=$clickable")
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            printNodeTree(child, depth + 1)
            child.recycle()
        }
    }
    
    /**
     * 提取聊天名称
     */
    private fun extractChatName(rootNode: AccessibilityNodeInfo): String? {
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(MessageMonitor.WECHAT_CHAT_NAME_ID)
        if (nodes.isNotEmpty()) {
            val name = nodes[0].text?.toString()
            nodes.forEach { it.recycle() }
            return name
        }
        return null
    }
    
    /**
     * 通过文本查找节点
     */
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text) == true) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        return null
    }
    
    /**
     * 通过类名查找节点
     */
    private fun findNodeByClassName(node: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (node.className?.toString() == className) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByClassName(child, className)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        return null
    }
    
    /**
     * 执行手势（用于滑动等操作）
     */
    fun performGesture(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            
            dispatchGesture(gesture, null, null)
        }
    }
    
    /**
     * 模拟点击
     */
    fun performClick(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(x, y)
            
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            
            dispatchGesture(gesture, null, null)
        }
    }
    
    /**
     * 获取自动回复管理器
     */
    fun getAutoReplyManager(): AutoReplyManager = autoReplyManager
    
    /**
     * 获取消息监控器
     */
    fun getMessageMonitor(): MessageMonitor = messageMonitor
    
    /**
     * 获取设置管理器
     */
    fun getSettingsManager(): SettingsManager = settings
    
    /**
     * 加载用户自定义坐标
     */
    fun loadCoordinates() {
        val prefs = getSharedPreferences("coordinates", Context.MODE_PRIVATE)
        
        fun loadCoord(key: String): Pair<Float, Float>? {
            val x = prefs.getFloat("${key}_x", -1f)
            val y = prefs.getFloat("${key}_y", -1f)
            return if (x >= 0 && y >= 0) Pair(x, y) else null
        }
        
        coordSearchButton = loadCoord(CoordinatePickerService.COORD_SEARCH_BUTTON)
        coordSearchInput = loadCoord(CoordinatePickerService.COORD_SEARCH_INPUT)
        coordFirstResult = loadCoord(CoordinatePickerService.COORD_FIRST_RESULT)
        coordPlusButton = loadCoord(CoordinatePickerService.COORD_PLUS_BUTTON)
        coordVideoCall = loadCoord(CoordinatePickerService.COORD_VIDEO_CALL)
        coordConfirmVideo = loadCoord(CoordinatePickerService.COORD_CONFIRM_VIDEO)
        coordConfirmVoice = loadCoord(CoordinatePickerService.COORD_CONFIRM_VOICE)
        coordPasteButton = loadCoord(CoordinatePickerService.COORD_PASTE_BUTTON)
        
        Log.d(TAG, "Loaded coordinates: search=$coordSearchButton, input=$coordSearchInput, paste=$coordPasteButton, result=$coordFirstResult, plus=$coordPlusButton, video=$coordVideoCall, confirmVideo=$coordConfirmVideo, confirmVoice=$coordConfirmVoice")
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        instance = null
        ttsManager?.shutdown()
        
        // 注销广播接收器
        try {
            unregisterReceiver(commandReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        
        // 发送服务断开广播
        sendBroadcast(Intent(ACTION_SERVICE_DISCONNECTED))
    }
}

