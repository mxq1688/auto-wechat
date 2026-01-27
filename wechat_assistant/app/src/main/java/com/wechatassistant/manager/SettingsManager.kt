package com.wechatassistant.manager

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置管理器
 * 集中管理应用的所有设置项
 */
class SettingsManager private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "wechat_assistant_settings"
        
        // 自动回复设置
        const val KEY_AUTO_REPLY_ENABLED = "auto_reply_enabled"
        const val KEY_AUTO_REPLY_DELAY = "auto_reply_delay"
        const val KEY_AUTO_REPLY_IN_GROUP = "auto_reply_in_group"
        
        // 视频通话设置
        const val KEY_AUTO_ANSWER_VIDEO = "auto_answer_video"
        const val KEY_AUTO_ANSWER_DELAY = "auto_answer_delay"
        const val KEY_VIDEO_CALL_ENABLED = "video_call_enabled"
        
        // 语音设置
        const val KEY_TTS_ENABLED = "tts_enabled"
        const val KEY_VOICE_RECOGNITION_ENABLED = "voice_recognition_enabled"
        const val KEY_TTS_SPEED = "tts_speed"
        const val KEY_TTS_PITCH = "tts_pitch"
        
        // 悬浮球设置
        const val KEY_FLOATING_BALL_ENABLED = "floating_ball_enabled"
        const val KEY_FLOATING_BALL_SIZE = "floating_ball_size"
        const val KEY_FLOATING_BALL_OPACITY = "floating_ball_opacity"
        
        // 通知设置
        const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        
        // 消息监控设置
        const val KEY_MESSAGE_MONITOR_ENABLED = "message_monitor_enabled"
        const val KEY_LOG_MESSAGES = "log_messages"
        
        // 信令服务器设置
        const val KEY_SIGNALING_SERVER_URL = "signaling_server_url"
        const val KEY_USER_ID = "user_id"
        
        // 白名单/黑名单
        const val KEY_WHITELIST = "whitelist"
        const val KEY_BLACKLIST = "blacklist"
        const val KEY_USE_WHITELIST = "use_whitelist"
        
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // ==================== 自动回复设置 ====================
    
    var autoReplyEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_REPLY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_REPLY_ENABLED, value).apply()
    
    var autoReplyDelay: Long
        get() = prefs.getLong(KEY_AUTO_REPLY_DELAY, 1000L)
        set(value) = prefs.edit().putLong(KEY_AUTO_REPLY_DELAY, value).apply()
    
    var autoReplyInGroup: Boolean
        get() = prefs.getBoolean(KEY_AUTO_REPLY_IN_GROUP, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_REPLY_IN_GROUP, value).apply()
    
    // ==================== 视频通话设置 ====================
    
    var autoAnswerVideo: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ANSWER_VIDEO, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_ANSWER_VIDEO, value).apply()
    
    var autoAnswerDelay: Long
        get() = prefs.getLong(KEY_AUTO_ANSWER_DELAY, 2000L)
        set(value) = prefs.edit().putLong(KEY_AUTO_ANSWER_DELAY, value).apply()
    
    var videoCallEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIDEO_CALL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIDEO_CALL_ENABLED, value).apply()
    
    // ==================== 语音设置 ====================
    
    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()
    
    var voiceRecognitionEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_RECOGNITION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_RECOGNITION_ENABLED, value).apply()
    
    var ttsSpeed: Float
        get() = prefs.getFloat(KEY_TTS_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_SPEED, value).apply()
    
    var ttsPitch: Float
        get() = prefs.getFloat(KEY_TTS_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_PITCH, value).apply()
    
    // ==================== 悬浮球设置 ====================
    
    var floatingBallEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_BALL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_BALL_ENABLED, value).apply()
    
    var floatingBallSize: Int
        get() = prefs.getInt(KEY_FLOATING_BALL_SIZE, 60)
        set(value) = prefs.edit().putInt(KEY_FLOATING_BALL_SIZE, value).apply()
    
    var floatingBallOpacity: Float
        get() = prefs.getFloat(KEY_FLOATING_BALL_OPACITY, 0.8f)
        set(value) = prefs.edit().putFloat(KEY_FLOATING_BALL_OPACITY, value).apply()
    
    // ==================== 通知设置 ====================
    
    var notificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, value).apply()
    
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
    
    // ==================== 消息监控设置 ====================
    
    var messageMonitorEnabled: Boolean
        get() = prefs.getBoolean(KEY_MESSAGE_MONITOR_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MESSAGE_MONITOR_ENABLED, value).apply()
    
    var logMessages: Boolean
        get() = prefs.getBoolean(KEY_LOG_MESSAGES, false)
        set(value) = prefs.edit().putBoolean(KEY_LOG_MESSAGES, value).apply()
    
    // ==================== 信令服务器设置 ====================
    
    var signalingServerUrl: String
        get() = prefs.getString(KEY_SIGNALING_SERVER_URL, "ws://localhost:8080") ?: "ws://localhost:8080"
        set(value) = prefs.edit().putString(KEY_SIGNALING_SERVER_URL, value).apply()
    
    var userId: String
        get() = prefs.getString(KEY_USER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()
    
    // ==================== 白名单/黑名单 ====================
    
    var useWhitelist: Boolean
        get() = prefs.getBoolean(KEY_USE_WHITELIST, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_WHITELIST, value).apply()
    
    fun getWhitelist(): Set<String> {
        return prefs.getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()
    }
    
    fun setWhitelist(list: Set<String>) {
        prefs.edit().putStringSet(KEY_WHITELIST, list).apply()
    }
    
    fun addToWhitelist(name: String) {
        val current = getWhitelist().toMutableSet()
        current.add(name)
        setWhitelist(current)
    }
    
    fun removeFromWhitelist(name: String) {
        val current = getWhitelist().toMutableSet()
        current.remove(name)
        setWhitelist(current)
    }
    
    fun getBlacklist(): Set<String> {
        return prefs.getStringSet(KEY_BLACKLIST, emptySet()) ?: emptySet()
    }
    
    fun setBlacklist(list: Set<String>) {
        prefs.edit().putStringSet(KEY_BLACKLIST, list).apply()
    }
    
    fun addToBlacklist(name: String) {
        val current = getBlacklist().toMutableSet()
        current.add(name)
        setBlacklist(current)
    }
    
    fun removeFromBlacklist(name: String) {
        val current = getBlacklist().toMutableSet()
        current.remove(name)
        setBlacklist(current)
    }
    
    /**
     * 检查联系人是否允许自动回复
     */
    fun isContactAllowed(contactName: String): Boolean {
        val blacklist = getBlacklist()
        if (blacklist.contains(contactName)) {
            return false
        }
        
        if (useWhitelist) {
            val whitelist = getWhitelist()
            return whitelist.contains(contactName)
        }
        
        return true
    }
    
    /**
     * 重置所有设置
     */
    fun resetAll() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 导出设置为Map
     */
    fun exportSettings(): Map<String, Any?> {
        return prefs.all.toMap()
    }
    
    /**
     * 从Map导入设置
     */
    fun importSettings(settings: Map<String, Any?>) {
        val editor = prefs.edit()
        settings.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    editor.putStringSet(key, value as Set<String>)
                }
            }
        }
        editor.apply()
    }
}

