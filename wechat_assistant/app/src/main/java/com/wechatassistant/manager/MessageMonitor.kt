package com.wechatassistant.manager

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.text.SimpleDateFormat
import java.util.*

/**
 * 消息监控器
 * 负责从微信界面提取消息信息
 */
class MessageMonitor {
    
    companion object {
        private const val TAG = "MessageMonitor"
        private const val MAX_HISTORY_SIZE = 100
        
        // 微信相关的View ID（可能需要根据微信版本调整）
        const val WECHAT_MESSAGE_LIST_ID = "com.tencent.mm:id/b4a"      // 消息列表
        const val WECHAT_MESSAGE_TEXT_ID = "com.tencent.mm:id/b4c"      // 消息文本
        const val WECHAT_CHAT_NAME_ID = "com.tencent.mm:id/kfs"         // 聊天标题
        const val WECHAT_INPUT_ID = "com.tencent.mm:id/bkk"             // 输入框
        const val WECHAT_SEND_BUTTON_ID = "com.tencent.mm:id/b8k"       // 发送按钮
        const val WECHAT_AVATAR_ID = "com.tencent.mm:id/b47"            // 头像
    }
    
    /**
     * 消息数据类
     */
    data class WeChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val content: String,
        val sender: String?,
        val chatName: String?,           // 聊天窗口名称（私聊为对方名称，群聊为群名）
        val isGroupChat: Boolean,
        val isSelf: Boolean,             // 是否是自己发送的消息
        val timestamp: Long = System.currentTimeMillis(),
        val messageType: MessageType = MessageType.TEXT
    )
    
    enum class MessageType {
        TEXT,           // 文本消息
        IMAGE,          // 图片
        VOICE,          // 语音
        VIDEO,          // 视频
        VIDEO_CALL,     // 视频通话
        VOICE_CALL,     // 语音通话
        RED_PACKET,     // 红包
        TRANSFER,       // 转账
        LOCATION,       // 位置
        FILE,           // 文件
        LINK,           // 链接
        UNKNOWN         // 未知类型
    }
    
    interface MessageListener {
        fun onNewMessage(message: WeChatMessage)
        fun onMessageListUpdated(messages: List<WeChatMessage>)
    }
    
    private val messageHistory = mutableListOf<WeChatMessage>()
    private val processedMessageIds = mutableSetOf<String>()
    private var messageListener: MessageListener? = null
    private var currentChatName: String? = null
    private var isGroupChat: Boolean = false
    
    /**
     * 设置消息监听器
     */
    fun setMessageListener(listener: MessageListener?) {
        messageListener = listener
    }
    
    /**
     * 从无障碍节点提取消息
     */
    fun extractMessages(rootNode: AccessibilityNodeInfo): List<WeChatMessage> {
        val messages = mutableListOf<WeChatMessage>()
        
        // 获取聊天窗口名称
        currentChatName = extractChatName(rootNode)
        isGroupChat = detectGroupChat(rootNode)
        
        // 遍历查找消息节点
        findMessageNodes(rootNode, messages)
        
        // 过滤已处理的消息
        val newMessages = messages.filter { msg ->
            val messageKey = "${msg.content}_${msg.sender}_${msg.chatName}"
            if (processedMessageIds.contains(messageKey)) {
                false
            } else {
                processedMessageIds.add(messageKey)
                // 限制已处理消息ID的数量
                if (processedMessageIds.size > MAX_HISTORY_SIZE * 2) {
                    val toRemove = processedMessageIds.take(MAX_HISTORY_SIZE)
                    processedMessageIds.removeAll(toRemove.toSet())
                }
                true
            }
        }
        
        // 添加到历史记录
        newMessages.forEach { msg ->
            messageHistory.add(msg)
            messageListener?.onNewMessage(msg)
        }
        
        // 限制历史记录大小
        while (messageHistory.size > MAX_HISTORY_SIZE) {
            messageHistory.removeAt(0)
        }
        
        if (newMessages.isNotEmpty()) {
            messageListener?.onMessageListUpdated(messageHistory.toList())
        }
        
        return newMessages
    }
    
    /**
     * 提取聊天窗口名称
     */
    private fun extractChatName(rootNode: AccessibilityNodeInfo): String? {
        // 通过ID查找
        var nodes = rootNode.findAccessibilityNodeInfosByViewId(WECHAT_CHAT_NAME_ID)
        if (nodes.isNotEmpty()) {
            val name = nodes[0].text?.toString()
            nodes.forEach { it.recycle() }
            return name
        }
        
        // 备用方法：查找ActionBar标题
        val titleNode = findNodeByClassName(rootNode, "android.widget.TextView")
        return titleNode?.text?.toString()
    }
    
    /**
     * 检测是否是群聊
     */
    private fun detectGroupChat(rootNode: AccessibilityNodeInfo): Boolean {
        // 群聊通常名称带有括号显示人数，如 "群名(50)"
        val chatName = currentChatName ?: return false
        if (chatName.matches(Regex(".*\\(\\d+\\)$"))) {
            return true
        }
        
        // 检查是否有多个不同的头像（群聊特征）
        val avatarNodes = rootNode.findAccessibilityNodeInfosByViewId(WECHAT_AVATAR_ID)
        val hasMultipleAvatars = avatarNodes.size > 2
        avatarNodes.forEach { it.recycle() }
        
        return hasMultipleAvatars
    }
    
    /**
     * 递归查找消息节点
     */
    private fun findMessageNodes(node: AccessibilityNodeInfo, messages: MutableList<WeChatMessage>) {
        // 检查当前节点是否是消息文本
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && isMessageNode(node)) {
            val message = parseMessage(node, text)
            if (message != null && message.content.isNotBlank()) {
                messages.add(message)
            }
        }
        
        // 递归遍历子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findMessageNodes(child, messages)
            child.recycle()
        }
    }
    
    /**
     * 判断节点是否是消息节点
     */
    private fun isMessageNode(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: return false
        
        // 通常消息是TextView
        if (!className.contains("TextView")) return false
        
        // 过滤系统UI文本
        val text = node.text?.toString() ?: return false
        val filterTexts = listOf("微信", "返回", "更多", "发送", "语音", "表情", "相册")
        if (filterTexts.any { text == it }) return false
        
        // 消息文本通常有一定长度或是常见消息内容
        return text.length >= 1
    }
    
    /**
     * 解析消息内容
     */
    private fun parseMessage(node: AccessibilityNodeInfo, text: String): WeChatMessage? {
        // 判断消息类型
        val messageType = detectMessageType(text, node)
        
        // 判断是否是自己发送的消息（通常自己的消息在右侧）
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        val screenWidth = node.extras.getInt("screenWidth", 1080)
        val isSelf = bounds.centerX() > screenWidth / 2
        
        // 提取发送者（群聊中）
        var sender: String? = null
        if (isGroupChat) {
            sender = extractSender(node)
        }
        
        return WeChatMessage(
            content = text,
            sender = sender,
            chatName = currentChatName,
            isGroupChat = isGroupChat,
            isSelf = isSelf,
            messageType = messageType
        )
    }
    
    /**
     * 提取发送者名称（群聊中）
     */
    private fun extractSender(messageNode: AccessibilityNodeInfo): String? {
        // 在群聊中，发送者名称通常在消息内容的上方或旁边
        val parent = messageNode.parent ?: return null
        
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i) ?: continue
            val siblingText = sibling.text?.toString()
            
            // 发送者名称通常较短且不是消息内容
            if (siblingText != null && 
                siblingText != messageNode.text?.toString() &&
                siblingText.length in 1..20 &&
                !siblingText.contains(":")) {
                sibling.recycle()
                parent.recycle()
                return siblingText
            }
            sibling.recycle()
        }
        parent.recycle()
        return null
    }
    
    /**
     * 检测消息类型
     */
    private fun detectMessageType(text: String, node: AccessibilityNodeInfo): MessageType {
        return when {
            text.contains("[图片]") || text == "图片" -> MessageType.IMAGE
            text.contains("[语音]") || text.matches(Regex("\\d+[\"']?")) -> MessageType.VOICE
            text.contains("[视频]") -> MessageType.VIDEO
            text.contains("视频通话") || text.contains("接听") -> MessageType.VIDEO_CALL
            text.contains("语音通话") -> MessageType.VOICE_CALL
            text.contains("[红包]") || text.contains("微信红包") -> MessageType.RED_PACKET
            text.contains("[转账]") -> MessageType.TRANSFER
            text.contains("[位置]") -> MessageType.LOCATION
            text.contains("[文件]") -> MessageType.FILE
            text.startsWith("http") || text.contains("链接") -> MessageType.LINK
            else -> MessageType.TEXT
        }
    }
    
    /**
     * 按类名查找节点
     */
    private fun findNodeByClassName(node: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (node.className?.toString() == className) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByClassName(child, className)
            if (result != null) return result
            child.recycle()
        }
        return null
    }
    
    /**
     * 获取消息历史
     */
    fun getMessageHistory(): List<WeChatMessage> = messageHistory.toList()
    
    /**
     * 获取最新消息
     */
    fun getLatestMessage(): WeChatMessage? = messageHistory.lastOrNull()
    
    /**
     * 获取最新的非自己发送的消息
     */
    fun getLatestReceivedMessage(): WeChatMessage? {
        return messageHistory.lastOrNull { !it.isSelf }
    }
    
    /**
     * 清空消息历史
     */
    fun clearHistory() {
        messageHistory.clear()
        processedMessageIds.clear()
    }
    
    /**
     * 格式化消息时间
     */
    fun formatMessageTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * 获取当前聊天名称
     */
    fun getCurrentChatName(): String? = currentChatName
    
    /**
     * 是否在群聊中
     */
    fun isInGroupChat(): Boolean = isGroupChat
}

