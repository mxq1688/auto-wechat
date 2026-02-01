package com.wechatassistant.service

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.wechatassistant.manager.SettingsManager

/**
 * 通知监听服务
 * 用于监听微信视频/语音通话通知并自动接听
 */
class CallNotificationListenerService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "CallNotificationService"
        const val WECHAT_PACKAGE = "com.tencent.mm"
        
        @Volatile
        var instance: CallNotificationListenerService? = null
            private set
        
        fun isServiceRunning(): Boolean = instance != null
    }
    
    private lateinit var settings: SettingsManager
    private val handler = Handler(Looper.getMainLooper())
    private var isAnswering = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        settings = SettingsManager.getInstance(this)
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected!!!")
        instance = this
        
        // 列出当前所有通知
        try {
            val notifications = activeNotifications
            Log.d(TAG, "Current active notifications: ${notifications.size}")
            for (sbn in notifications) {
                Log.d(TAG, "  - ${sbn.packageName}: ${sbn.notification.extras.getString(android.app.Notification.EXTRA_TITLE)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing notifications", e)
        }
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
        instance = null
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(TAG, "onNotificationPosted: ${sbn.packageName}")
        
        if (sbn.packageName != WECHAT_PACKAGE) {
            Log.d(TAG, "Not WeChat notification, ignoring")
            return
        }
        
        val notification = sbn.notification
        val extras = notification.extras
        
        // 获取通知标题和内容
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        
        Log.d(TAG, "=== WeChat Notification ===")
        Log.d(TAG, "  title='$title'")
        Log.d(TAG, "  text='$text'")
        Log.d(TAG, "  subText='$subText'")
        Log.d(TAG, "  bigText='$bigText'")
        Log.d(TAG, "  category='${notification.category}'")
        Log.d(TAG, "  actions=${notification.actions?.size ?: 0}")
        
        // 打印所有操作按钮
        notification.actions?.forEachIndexed { index, action ->
            Log.d(TAG, "  action[$index]: '${action.title}'")
        }
        
        if (!settings.autoAnswerVideo) {
            Log.d(TAG, "Auto answer is disabled")
            return
        }
        
        // 检测是否是来电通知（支持多种格式）
        val allText = "$title $text $subText $bigText".lowercase()
        val isIncomingCall = allText.contains("通话") || 
                            allText.contains("视频") ||
                            allText.contains("语音") ||
                            allText.contains("邀请") ||
                            allText.contains("来电") ||
                            allText.contains("voip") ||
                            allText.contains("call") ||
                            notification.category == Notification.CATEGORY_CALL
        
        // 检查是否有接听按钮
        val hasAnswerAction = notification.actions?.any { action ->
            val actionText = action.title?.toString()?.lowercase() ?: ""
            actionText.contains("接听") || actionText.contains("接受") || 
            actionText.contains("answer") || actionText.contains("accept")
        } ?: false
        
        Log.d(TAG, "isIncomingCall=$isIncomingCall, hasAnswerAction=$hasAnswerAction, isAnswering=$isAnswering")
        
        if ((isIncomingCall || hasAnswerAction) && !isAnswering) {
            Log.d(TAG, ">>> Detected incoming call notification! Will auto-answer...")
            isAnswering = true
            
            // 延迟后尝试接听
            handler.postDelayed({
                tryAnswerFromNotification(notification, sbn)
            }, settings.autoAnswerDelay)
        }
    }
    
    /**
     * 尝试从通知中接听电话
     */
    private fun tryAnswerFromNotification(notification: Notification, sbn: StatusBarNotification) {
        try {
            // 方法1: 查找通知中的操作按钮
            val actions = notification.actions
            if (actions != null) {
                Log.d(TAG, "Found ${actions.size} notification actions")
                
                for ((index, action) in actions.withIndex()) {
                    val actionTitle = action.title?.toString() ?: ""
                    Log.d(TAG, "Action[$index]: '$actionTitle'")
                    
                    // 查找接听按钮
                    if (actionTitle.contains("接听") || 
                        actionTitle.contains("接受") ||
                        actionTitle.contains("视频接听") ||
                        actionTitle.contains("语音接听") ||
                        actionTitle.contains("Answer")) {
                        
                        Log.d(TAG, "Found answer action: '$actionTitle', executing...")
                        
                        try {
                            action.actionIntent.send()
                            Log.d(TAG, "Answer action sent successfully!")
                            
                            // 发送广播通知
                            sendBroadcast(Intent("com.wechatassistant.CALL_ANSWERED"))
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sending action intent", e)
                        }
                        break
                    }
                }
            } else {
                Log.d(TAG, "No notification actions found, trying alternative methods...")
                
                // 方法2: 先点击通知打开通话界面
                try {
                    notification.contentIntent?.send()
                    Log.d(TAG, "Clicked notification to open call screen")
                    
                    // 方法3: 快速发送广播让无障碍服务执行手势点击
                    handler.postDelayed({
                        Log.d(TAG, "Sending gesture click broadcast to accessibility service")
                        val intent = Intent(EnhancedWeChatAccessibilityService.ACTION_GESTURE_CLICK_ACCEPT)
                        sendBroadcast(intent)
                    }, 300) // 快速响应
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error clicking notification", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error trying to answer from notification", e)
        } finally {
            // 延迟重置状态
            handler.postDelayed({
                isAnswering = false
            }, 5000)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == WECHAT_PACKAGE) {
            Log.d(TAG, "WeChat notification removed")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        instance = null
    }
}

