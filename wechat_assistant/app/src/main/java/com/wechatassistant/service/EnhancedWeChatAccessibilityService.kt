package com.wechatassistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")
        instance = this
        
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
    
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        instance = null
        ttsManager?.shutdown()
        
        // 发送服务断开广播
        sendBroadcast(Intent(ACTION_SERVICE_DISCONNECTED))
    }
}

