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
        
        // 语音命令关键词设置
        const val KEY_VIDEO_CALL_KEYWORDS = "video_call_keywords"
        const val KEY_VOICE_CALL_KEYWORDS = "voice_call_keywords"
        const val KEY_GENERAL_CALL_KEYWORDS = "general_call_keywords"
        const val KEY_WAKE_WORDS = "wake_words"
        const val KEY_REQUIRE_WAKE_WORD = "require_wake_word"
        
        // 联系人别名设置（格式：别名1=微信名1,别名2=微信名2）
        const val KEY_CONTACT_ALIASES = "contact_aliases"
        // 联系人照片（格式：微信名=照片路径）
        const val KEY_CONTACT_PHOTOS = "contact_photos"
        
        // LLM 大模型设置
        const val KEY_LLM_ENABLED = "llm_enabled"
        const val KEY_LLM_API_URL = "llm_api_url"
        const val KEY_LLM_API_KEY = "llm_api_key"
        const val DEFAULT_LLM_API_URL = "https://api.deepseek.com/chat/completions"
        
        // 默认关键词
        val DEFAULT_VIDEO_KEYWORDS = setOf("视频", "视频通话", "视频电话", "打视频")
        val DEFAULT_VOICE_KEYWORDS = setOf("语音", "语音通话")
        val DEFAULT_GENERAL_KEYWORDS = setOf(
            "打电话", "通话", "打个电话", "呼叫", "联系", "打给", "找", "拨打", "聊聊", "聊一下",
            // 凤阳方言 - "给"发音gé，可能识别成：
            "打个", "打格", "打隔", "个电话", "格电话", "隔电话"
        )
        val DEFAULT_WAKE_WORDS = setOf("小智", "小志", "小知")
        
        // 默认联系人（微信名 -> 别名列表）
        val DEFAULT_CONTACTS = mapOf(
            "大强" to listOf("小强", "强", "大孙", "大孙的", "大孙子")
        )
        
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
    
    // ==================== LLM 大模型设置 ====================
    
    var llmEnabled: Boolean
        get() = prefs.getBoolean(KEY_LLM_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LLM_ENABLED, value).apply()
    
    var llmApiUrl: String
        get() = prefs.getString(KEY_LLM_API_URL, DEFAULT_LLM_API_URL) ?: DEFAULT_LLM_API_URL
        set(value) = prefs.edit().putString(KEY_LLM_API_URL, value).apply()
    
    var llmApiKey: String
        get() = prefs.getString(KEY_LLM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LLM_API_KEY, value).apply()
    
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
    
    // ==================== 语音命令关键词设置 ====================
    
    var requireWakeWord: Boolean
        get() = prefs.getBoolean(KEY_REQUIRE_WAKE_WORD, false)
        set(value) = prefs.edit().putBoolean(KEY_REQUIRE_WAKE_WORD, value).apply()
    
    fun getVideoCallKeywords(): Set<String> {
        return prefs.getStringSet(KEY_VIDEO_CALL_KEYWORDS, DEFAULT_VIDEO_KEYWORDS) ?: DEFAULT_VIDEO_KEYWORDS
    }
    
    fun setVideoCallKeywords(keywords: Set<String>) {
        prefs.edit().putStringSet(KEY_VIDEO_CALL_KEYWORDS, keywords).apply()
    }
    
    fun getVoiceCallKeywords(): Set<String> {
        return prefs.getStringSet(KEY_VOICE_CALL_KEYWORDS, DEFAULT_VOICE_KEYWORDS) ?: DEFAULT_VOICE_KEYWORDS
    }
    
    fun setVoiceCallKeywords(keywords: Set<String>) {
        prefs.edit().putStringSet(KEY_VOICE_CALL_KEYWORDS, keywords).apply()
    }
    
    fun getGeneralCallKeywords(): Set<String> {
        return prefs.getStringSet(KEY_GENERAL_CALL_KEYWORDS, DEFAULT_GENERAL_KEYWORDS) ?: DEFAULT_GENERAL_KEYWORDS
    }
    
    fun setGeneralCallKeywords(keywords: Set<String>) {
        prefs.edit().putStringSet(KEY_GENERAL_CALL_KEYWORDS, keywords).apply()
    }
    
    fun getWakeWords(): Set<String> {
        return prefs.getStringSet(KEY_WAKE_WORDS, DEFAULT_WAKE_WORDS) ?: DEFAULT_WAKE_WORDS
    }
    
    fun setWakeWords(words: Set<String>) {
        prefs.edit().putStringSet(KEY_WAKE_WORDS, words).apply()
    }
    
    // ==================== 联系人别名设置 ====================
    
    /**
     * 获取联系人列表
     * 格式：Map<微信名, List<简称>>
     * 存储格式：微信名:简称1|简称2,微信名2:简称3|简称4
     */
    fun getContacts(): Map<String, List<String>> {
        val contactString = prefs.getString(KEY_CONTACT_ALIASES, "") ?: ""
        if (contactString.isEmpty()) {
            // 返回默认联系人
            return DEFAULT_CONTACTS
        }
        
        val result = mutableMapOf<String, List<String>>()
        contactString.split(",").forEach { entry ->
            val parts = entry.trim().split(":")
            if (parts.size == 2) {
                val wechatName = parts[0].trim()
                val aliases = parts[1].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (wechatName.isNotEmpty() && aliases.isNotEmpty()) {
                    result[wechatName] = aliases
                }
            }
        }
        return result
    }
    
    /**
     * 保存联系人列表
     */
    fun setContacts(contacts: Map<String, List<String>>) {
        val contactString = contacts.entries
            .filter { it.key.isNotEmpty() && it.value.isNotEmpty() }
            .joinToString(",") { "${it.key}:${it.value.joinToString("|")}" }
        prefs.edit().putString(KEY_CONTACT_ALIASES, contactString).apply()
    }
    
    /**
     * 获取联系人别名映射（简称 -> 微信名，用于匹配）
     */
    fun getContactAliases(): Map<String, String> {
        val contacts = getContacts()
        val result = mutableMapOf<String, String>()
        contacts.forEach { (wechatName, aliases) ->
            aliases.forEach { alias ->
                result[alias] = wechatName
            }
            // 微信名本身也作为别名
            result[wechatName] = wechatName
        }
        return result
    }
    
    /**
     * 设置联系人别名（旧格式兼容）
     */
    fun setContactAliases(aliasString: String) {
        prefs.edit().putString(KEY_CONTACT_ALIASES, aliasString).apply()
    }
    
    // ==================== 联系人照片设置 ====================
    
    /**
     * 获取所有联系人照片
     * 格式：Map<微信名, 照片路径>
     */
    fun getContactPhotos(): Map<String, String> {
        val photoString = prefs.getString(KEY_CONTACT_PHOTOS, "") ?: ""
        if (photoString.isEmpty()) return emptyMap()
        
        val result = mutableMapOf<String, String>()
        photoString.split(",").forEach { entry ->
            val parts = entry.trim().split("=")
            if (parts.size == 2) {
                val name = parts[0].trim()
                val path = parts[1].trim()
                if (name.isNotEmpty() && path.isNotEmpty()) {
                    result[name] = path
                }
            }
        }
        return result
    }
    
    /**
     * 获取单个联系人的照片路径
     */
    fun getContactPhoto(wechatName: String): String? {
        return getContactPhotos()[wechatName]
    }
    
    /**
     * 设置联系人照片
     */
    fun setContactPhoto(wechatName: String, photoPath: String) {
        val photos = getContactPhotos().toMutableMap()
        photos[wechatName] = photoPath
        saveContactPhotos(photos)
    }
    
    /**
     * 删除联系人照片
     */
    fun removeContactPhoto(wechatName: String) {
        val photos = getContactPhotos().toMutableMap()
        photos.remove(wechatName)
        saveContactPhotos(photos)
    }
    
    /**
     * 保存所有联系人照片
     */
    private fun saveContactPhotos(photos: Map<String, String>) {
        val photoString = photos.entries
            .filter { it.key.isNotEmpty() && it.value.isNotEmpty() }
            .joinToString(",") { "${it.key}=${it.value}" }
        prefs.edit().putString(KEY_CONTACT_PHOTOS, photoString).apply()
    }
    
    /**
     * 设置联系人别名（从旧 Map 格式，兼容）
     */
    fun setContactAliasesFromMap(aliases: Map<String, String>) {
        // 转换为新格式：按微信名分组
        val grouped = aliases.entries.groupBy({ it.value }, { it.key })
        setContacts(grouped)
    }
    
    /**
     * 根据语音输入的名字模糊匹配真实微信名
     */
    fun matchContactName(spokenName: String): String {
        val aliases = getContactAliases()
        
        // 1. 完全匹配别名
        aliases[spokenName]?.let { return it }
        
        // 2. 别名包含在语音中
        for ((alias, wechatName) in aliases) {
            if (spokenName.contains(alias) || alias.contains(spokenName)) {
                return wechatName
            }
        }
        
        // 没有匹配到，返回原名
        return spokenName
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

