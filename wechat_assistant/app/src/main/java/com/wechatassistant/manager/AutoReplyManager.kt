package com.wechatassistant.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * 自动回复管理器
 * 支持关键词匹配和自定义回复规则
 */
class AutoReplyManager(context: Context) {
    
    companion object {
        private const val TAG = "AutoReplyManager"
        private const val PREFS_NAME = "auto_reply_prefs"
        private const val KEY_RULES = "reply_rules"
        private const val KEY_ENABLED = "auto_reply_enabled"
        private const val KEY_DELAY = "reply_delay"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val rules = mutableListOf<ReplyRule>()
    
    // 是否启用自动回复
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    
    // 回复延迟（毫秒）
    var replyDelay: Long
        get() = prefs.getLong(KEY_DELAY, 1000L)
        set(value) = prefs.edit().putLong(KEY_DELAY, value).apply()
    
    init {
        loadRules()
        // 如果没有规则，添加默认规则
        if (rules.isEmpty()) {
            addDefaultRules()
        }
    }
    
    /**
     * 回复规则数据类
     */
    data class ReplyRule(
        val id: String,
        val keywords: List<String>,      // 触发关键词
        val reply: String,               // 回复内容
        val matchType: MatchType = MatchType.CONTAINS,  // 匹配类型
        val isEnabled: Boolean = true,   // 是否启用
        val priority: Int = 0,           // 优先级，数字越大优先级越高
        val scope: ReplyScope = ReplyScope.ALL  // 适用范围
    )
    
    enum class MatchType {
        EXACT,      // 精确匹配
        CONTAINS,   // 包含匹配
        REGEX       // 正则匹配
    }
    
    enum class ReplyScope {
        ALL,        // 所有聊天
        PRIVATE,    // 仅私聊
        GROUP       // 仅群聊
    }
    
    /**
     * 添加默认规则
     */
    private fun addDefaultRules() {
        val defaultRules = listOf(
            ReplyRule(
                id = "default_1",
                keywords = listOf("你好", "您好", "hi", "hello", "嗨"),
                reply = "你好！有什么可以帮助你的吗？",
                matchType = MatchType.CONTAINS,
                priority = 1
            ),
            ReplyRule(
                id = "default_2",
                keywords = listOf("在吗", "在不在", "有人吗"),
                reply = "在的，请问有什么事？",
                matchType = MatchType.CONTAINS,
                priority = 2
            ),
            ReplyRule(
                id = "default_3",
                keywords = listOf("谢谢", "感谢", "多谢", "thanks"),
                reply = "不客气！",
                matchType = MatchType.CONTAINS,
                priority = 1
            ),
            ReplyRule(
                id = "default_4",
                keywords = listOf("再见", "拜拜", "bye", "晚安"),
                reply = "再见，祝你生活愉快！",
                matchType = MatchType.CONTAINS,
                priority = 1
            ),
            ReplyRule(
                id = "default_5",
                keywords = listOf("忙吗", "方便吗", "有空吗"),
                reply = "我现在有空，请说。",
                matchType = MatchType.CONTAINS,
                priority = 2
            )
        )
        
        rules.addAll(defaultRules)
        saveRules()
    }
    
    /**
     * 根据消息内容获取自动回复
     * @param message 收到的消息
     * @param isGroupChat 是否是群聊
     * @return 匹配的回复内容，如果没有匹配则返回null
     */
    fun getReply(message: String, isGroupChat: Boolean = false): String? {
        if (!isEnabled) return null
        
        val trimmedMessage = message.trim().lowercase()
        
        // 按优先级排序规则
        val sortedRules = rules
            .filter { it.isEnabled }
            .filter { rule ->
                when (rule.scope) {
                    ReplyScope.ALL -> true
                    ReplyScope.PRIVATE -> !isGroupChat
                    ReplyScope.GROUP -> isGroupChat
                }
            }
            .sortedByDescending { it.priority }
        
        for (rule in sortedRules) {
            if (matchRule(trimmedMessage, rule)) {
                Log.d(TAG, "Matched rule: ${rule.id}, reply: ${rule.reply}")
                return rule.reply
            }
        }
        
        return null
    }
    
    /**
     * 检查消息是否匹配规则
     */
    private fun matchRule(message: String, rule: ReplyRule): Boolean {
        return when (rule.matchType) {
            MatchType.EXACT -> {
                rule.keywords.any { it.lowercase() == message }
            }
            MatchType.CONTAINS -> {
                rule.keywords.any { message.contains(it.lowercase()) }
            }
            MatchType.REGEX -> {
                rule.keywords.any { 
                    try {
                        Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(message)
                    } catch (e: Exception) {
                        Log.e(TAG, "Invalid regex: $it", e)
                        false
                    }
                }
            }
        }
    }
    
    /**
     * 添加自定义规则
     */
    fun addRule(rule: ReplyRule) {
        // 移除相同ID的旧规则
        rules.removeAll { it.id == rule.id }
        rules.add(rule)
        saveRules()
        Log.d(TAG, "Added rule: ${rule.id}")
    }
    
    /**
     * 移除规则
     */
    fun removeRule(ruleId: String) {
        rules.removeAll { it.id == ruleId }
        saveRules()
        Log.d(TAG, "Removed rule: $ruleId")
    }
    
    /**
     * 获取所有规则
     */
    fun getAllRules(): List<ReplyRule> = rules.toList()
    
    /**
     * 清空所有规则
     */
    fun clearRules() {
        rules.clear()
        saveRules()
    }
    
    /**
     * 重置为默认规则
     */
    fun resetToDefault() {
        rules.clear()
        addDefaultRules()
    }
    
    /**
     * 保存规则到SharedPreferences
     */
    private fun saveRules() {
        val jsonArray = JSONArray()
        rules.forEach { rule ->
            val jsonObj = JSONObject().apply {
                put("id", rule.id)
                put("keywords", JSONArray(rule.keywords))
                put("reply", rule.reply)
                put("matchType", rule.matchType.name)
                put("isEnabled", rule.isEnabled)
                put("priority", rule.priority)
                put("scope", rule.scope.name)
            }
            jsonArray.put(jsonObj)
        }
        prefs.edit().putString(KEY_RULES, jsonArray.toString()).apply()
    }
    
    /**
     * 从SharedPreferences加载规则
     */
    private fun loadRules() {
        rules.clear()
        val jsonStr = prefs.getString(KEY_RULES, null) ?: return
        
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                val keywordsArray = jsonObj.getJSONArray("keywords")
                val keywords = mutableListOf<String>()
                for (j in 0 until keywordsArray.length()) {
                    keywords.add(keywordsArray.getString(j))
                }
                
                val rule = ReplyRule(
                    id = jsonObj.getString("id"),
                    keywords = keywords,
                    reply = jsonObj.getString("reply"),
                    matchType = try {
                        MatchType.valueOf(jsonObj.optString("matchType", "CONTAINS"))
                    } catch (e: Exception) {
                        MatchType.CONTAINS
                    },
                    isEnabled = jsonObj.optBoolean("isEnabled", true),
                    priority = jsonObj.optInt("priority", 0),
                    scope = try {
                        ReplyScope.valueOf(jsonObj.optString("scope", "ALL"))
                    } catch (e: Exception) {
                        ReplyScope.ALL
                    }
                )
                rules.add(rule)
            }
            Log.d(TAG, "Loaded ${rules.size} rules")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading rules", e)
        }
    }
}

