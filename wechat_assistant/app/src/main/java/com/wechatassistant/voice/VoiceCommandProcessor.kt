package com.wechatassistant.voice

import android.util.Log

class VoiceCommandProcessor {
    
    companion object {
        private const val TAG = "VoiceCommandProcessor"
    }
    
    enum class CommandType {
        VIDEO_CALL, VOICE_CALL, SEND_MESSAGE, OPEN_CHAT, UNKNOWN
    }
    
    data class Command(
        val type: CommandType,
        val contactName: String? = null,
        val message: String? = null,
        val rawCommand: String
    )
    
    private val videoCallKeywords = listOf("视频", "视频通话", "视频电话", "打视频")
    private val voiceCallKeywords = listOf("语音", "语音通话", "打电话", "通话")
    
    fun parseCommand(voiceInput: String): Command {
        val input = voiceInput.trim().lowercase()
        Log.d(TAG, "Parsing: $input")
        
        val commandType = when {
            videoCallKeywords.any { input.contains(it) } -> CommandType.VIDEO_CALL
            voiceCallKeywords.any { input.contains(it) } -> CommandType.VOICE_CALL
            else -> CommandType.UNKNOWN
        }
        
        val contactName = extractContactName(input)
        
        return Command(
            type = commandType,
            contactName = contactName,
            rawCommand = voiceInput
        )
    }
    
    private fun extractContactName(input: String): String? {
        // 匹配模式: "给XXX打视频" "和XXX视频" "打视频给XXX"
        val patterns = listOf(
            "给(.+?)打",
            "和(.+?)视频",
            "跟(.+?)视频",
            "打视频给(.+)",
            "打电话给(.+)",
            "给(.+?)视频"
        )
        
        patterns.forEach { pattern ->
            val regex = Regex(pattern)
            val match = regex.find(input)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }
        
        // 尝试简单提取
        val keywords = videoCallKeywords + voiceCallKeywords
        var name = input
        keywords.forEach { name = name.replace(it, "") }
        name = name.replace("给", "").replace("打", "").replace("和", "")
            .replace("跟", "").trim()
        
        return if (name.isNotEmpty()) name else null
    }
    
    fun isVideoCallCommand(command: Command): Boolean {
        return command.type == CommandType.VIDEO_CALL && command.contactName != null
    }
}