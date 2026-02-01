package com.wechatassistant.voice

import android.content.Context
import android.util.Log
import com.wechatassistant.manager.SettingsManager

class VoiceCommandProcessor(context: Context? = null) {
    
    companion object {
        private const val TAG = "VoiceCommandProcessor"
        
        // 最小有效命令长度（太短的忽略）
        private const val MIN_COMMAND_LENGTH = 3
        
        // 默认关键词（无 context 时使用）
        private val DEFAULT_VIDEO_KEYWORDS = listOf("视频", "视频通话", "视频电话", "打视频")
        private val DEFAULT_VOICE_KEYWORDS = listOf("语音", "语音通话")
        private val DEFAULT_GENERAL_KEYWORDS = listOf("打电话", "通话", "打个电话", "呼叫", "联系")
        private val DEFAULT_WAKE_WORDS = listOf("小智", "小志", "小知", "xiaozhi")
    }
    
    private val settings: SettingsManager? = context?.let { SettingsManager.getInstance(it) }
    
    // 从设置获取关键词，或使用默认值
    private val videoCallKeywords: List<String>
        get() = settings?.getVideoCallKeywords()?.toList() ?: DEFAULT_VIDEO_KEYWORDS
    
    private val voiceCallKeywords: List<String>
        get() = settings?.getVoiceCallKeywords()?.toList() ?: DEFAULT_VOICE_KEYWORDS
    
    private val generalCallKeywords: List<String>
        get() = settings?.getGeneralCallKeywords()?.toList() ?: DEFAULT_GENERAL_KEYWORDS
    
    private val wakeWords: List<String>
        get() = settings?.getWakeWords()?.toList() ?: DEFAULT_WAKE_WORDS
    
    // 所有打电话相关的关键词
    private val allCallKeywords: List<String>
        get() = videoCallKeywords + voiceCallKeywords + generalCallKeywords
    
    enum class CommandType {
        VIDEO_CALL, VOICE_CALL, SEND_MESSAGE, OPEN_CHAT, 
        WAKE_WORD_ONLY,  // 只说了唤醒词，没有命令
        NO_WAKE_WORD,    // 没有唤醒词
        INCOMPLETE_CALL, // 有打电话关键词但缺少联系人（需要提示）
        UNKNOWN          // 完全无关的输入（静默忽略）
    }
    
    data class Command(
        val type: CommandType,
        val contactName: String? = null,
        val message: String? = null,
        val rawCommand: String,
        val hasWakeWord: Boolean = false
    )
    
    /**
     * 检查是否包含唤醒词，返回去掉唤醒词后的命令
     */
    fun checkWakeWord(voiceInput: String): Pair<Boolean, String> {
        val input = voiceInput.trim()
        
        for (wakeWord in wakeWords) {
            if (input.startsWith(wakeWord, ignoreCase = true)) {
                // 去掉唤醒词和可能的标点/空格
                var command = input.substring(wakeWord.length).trim()
                // 去掉开头的逗号、句号等
                command = command.trimStart(',', '，', '。', ' ', '、')
                Log.d(TAG, "Wake word '$wakeWord' detected, command: '$command'")
                return Pair(true, command)
            }
            // 也检查中间包含唤醒词的情况（如"嗨小智给妈妈打视频"）
            if (input.contains(wakeWord, ignoreCase = true)) {
                val index = input.indexOf(wakeWord, ignoreCase = true)
                var command = input.substring(index + wakeWord.length).trim()
                command = command.trimStart(',', '，', '。', ' ', '、')
                Log.d(TAG, "Wake word '$wakeWord' found in middle, command: '$command'")
                return Pair(true, command)
            }
        }
        
        return Pair(false, input)
    }
    
    fun parseCommand(voiceInput: String): Command {
        val input = voiceInput.trim()
        val inputLower = input.lowercase()
        Log.d(TAG, "Parsing: $input")
        
        // 1. 检查命令长度，太短的忽略
        if (input.length < MIN_COMMAND_LENGTH) {
            Log.d(TAG, "Command too short, ignoring: $input")
            return Command(type = CommandType.UNKNOWN, rawCommand = voiceInput)
        }
        
        // 2. 检查是否有打电话相关的关键词（必须有关键词才处理，防止误触发）
        val hasCallKeyword = allCallKeywords.any { inputLower.contains(it) }
        if (!hasCallKeyword) {
            Log.d(TAG, "No call keyword found, ignoring: $input")
            return Command(type = CommandType.UNKNOWN, rawCommand = voiceInput)
        }
        
        // 3. 提取联系人名称
        val contactName = extractContactName(inputLower)
        
        // 4. 有打电话关键词但没联系人 -> 需要提示用户
        if (contactName == null || contactName.isEmpty()) {
            Log.d(TAG, "Has call keyword but no contact: $input")
            return Command(type = CommandType.INCOMPLETE_CALL, rawCommand = voiceInput)
        }
        
        // 5. 判断命令类型
        val commandType = when {
            voiceCallKeywords.any { inputLower.contains(it) } -> CommandType.VOICE_CALL
            else -> CommandType.VIDEO_CALL  // 默认视频
        }
        
        Log.d(TAG, "Parsed: type=$commandType, contact=$contactName")
        
        return Command(
            type = commandType,
            contactName = contactName,
            rawCommand = voiceInput
        )
    }
    
    private fun extractContactName(input: String): String? {
        // 模式匹配提取联系人（必须有明确的命令格式）
        val patterns = listOf(
            "给(.+?)打",       // 给妈妈打电话/打视频
            "和(.+?)视频",     // 和妈妈视频
            "跟(.+?)视频",     // 跟妈妈视频
            "和(.+?)通话",     // 和妈妈通话
            "跟(.+?)通话",     // 跟妈妈通话
            "打视频给(.+)",    // 打视频给妈妈
            "打电话给(.+)",    // 打电话给妈妈
            "给(.+?)视频",     // 给妈妈视频
            "给(.+?)语音",     // 给妈妈语音
            "呼叫(.+)",        // 呼叫妈妈
            "联系(.+)",        // 联系妈妈
            "打给(.+)"         // 打给妈妈
        )
        
        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(input)
            if (match != null && match.groupValues.size > 1) {
                var name = match.groupValues[1].trim()
                // 清理名字中的标点符号
                name = name.replace(Regex("[，。！？、的吧呀啊哦呢]"), "").trim()
                if (name.isNotEmpty() && name.length <= 10) {
                    // 使用 SettingsManager 进行模糊匹配
                    val matchedName = settings?.matchContactName(name) ?: name
                    Log.d(TAG, "Extracted name: '$name' -> matched: '$matchedName'")
                    return matchedName
                }
            }
        }
        
        // 如果模式匹配失败，尝试在输入中查找已知联系人（但仍需要有关键词）
        val knownContacts = settings?.getContactAliases() ?: emptyMap()
        for ((alias, wechatName) in knownContacts) {
            if (input.contains(alias)) {
                Log.d(TAG, "Found known contact: '$alias' -> '$wechatName'")
                return wechatName
            }
        }
        
        return null
    }
    
    fun isVideoCallCommand(command: Command): Boolean {
        return command.type == CommandType.VIDEO_CALL && command.contactName != null
    }
}
