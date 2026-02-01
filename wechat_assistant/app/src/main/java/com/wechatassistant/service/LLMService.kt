package com.wechatassistant.service

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * LLM 大模型服务 - 用于智能分析语音命令
 */
class LLMService {
    
    companion object {
        private const val TAG = "LLMService"
        
        // DeepSeek API（可替换为其他 API）
        private const val API_URL = "https://api.deepseek.com/chat/completions"
        private const val API_KEY = ""  // 需要用户配置
        
        // 备选：通义千问
        // private const val API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        // private const val API_KEY = ""
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private var apiKey: String = API_KEY
    private var apiUrl: String = API_URL
    
    /**
     * 设置 API 配置
     */
    fun setConfig(url: String, key: String) {
        apiUrl = url
        apiKey = key
    }
    
    /**
     * 分析语音命令，返回结构化结果
     */
    suspend fun analyzeCommand(
        voiceText: String,
        contactList: List<String>
    ): CommandResult = withContext(Dispatchers.IO) {
        
        if (apiKey.isEmpty()) {
            Log.w(TAG, "API key not configured, falling back to keyword matching")
            return@withContext CommandResult(success = false, error = "API未配置")
        }
        
        try {
            val prompt = buildPrompt(voiceText, contactList)
            val response = callLLM(prompt)
            parseResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "LLM analysis failed", e)
            CommandResult(success = false, error = e.message ?: "分析失败")
        }
    }
    
    private fun buildPrompt(voiceText: String, contactList: List<String>): String {
        val contacts = if (contactList.isNotEmpty()) {
            contactList.joinToString("、")
        } else {
            "（未配置联系人）"
        }
        
        return """
你是一个语音命令分析助手。用户想通过语音给微信联系人打电话。

用户说的话："$voiceText"

已配置的联系人列表：$contacts

请分析用户意图，返回 JSON 格式：
{
  "is_call_command": true/false,  // 是否是打电话命令
  "call_type": "video/voice/unknown",  // video=视频通话, voice=语音通话
  "contact_name": "联系人名字",  // 从联系人列表中匹配，如果没找到返回null
  "confidence": 0.0-1.0,  // 置信度
  "reason": "分析原因"
}

注意：
1. 要从联系人列表中匹配最接近的名字（考虑同音字、简称等）
2. 如果用户没明确说视频还是语音，默认为视频通话
3. 只返回 JSON，不要其他内容
""".trimIndent()
    }
    
    private fun callLLM(prompt: String): String {
        val jsonBody = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.1)
            put("max_tokens", 200)
        }
        
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("API error: ${response.code} ${response.message}")
        }
        
        val responseBody = response.body?.string() ?: throw IOException("Empty response")
        Log.d(TAG, "LLM response: $responseBody")
        
        val json = JSONObject(responseBody)
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
    
    private fun parseResponse(response: String): CommandResult {
        return try {
            // 提取 JSON（LLM 可能返回带有其他文字的响应）
            val jsonStr = extractJson(response)
            val json = JSONObject(jsonStr)
            
            val isCallCommand = json.optBoolean("is_call_command", false)
            if (!isCallCommand) {
                return CommandResult(success = true, isCallCommand = false)
            }
            
            val callType = json.optString("call_type", "video")
            val contactName = json.optString("contact_name", null)
            val confidence = json.optDouble("confidence", 0.0)
            val reason = json.optString("reason", "")
            
            CommandResult(
                success = true,
                isCallCommand = true,
                isVideo = callType != "voice",
                contactName = if (contactName == "null") null else contactName,
                confidence = confidence,
                reason = reason
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse LLM response: $response", e)
            CommandResult(success = false, error = "解析失败")
        }
    }
    
    private fun extractJson(text: String): String {
        // 尝试提取 JSON 对象
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            text.substring(start, end + 1)
        } else {
            text
        }
    }
    
    /**
     * 命令分析结果
     */
    data class CommandResult(
        val success: Boolean,
        val isCallCommand: Boolean = false,
        val isVideo: Boolean = true,
        val contactName: String? = null,
        val confidence: Double = 0.0,
        val reason: String = "",
        val error: String? = null
    )
}

