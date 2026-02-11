package com.wechatassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wechatassistant.MainActivity
import com.wechatassistant.R
import com.wechatassistant.voice.VoiceCommandProcessor

class VoiceListeningForegroundService : Service() {

    private var voiceRecognitionService: VoiceRecognitionService? = null
    private var isListening = false
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "VoiceFgService"
        private const val NOTIFICATION_CHANNEL_ID = "VoiceListeningChannel"
        private const val NOTIFICATION_ID = 101

        var isServiceRunning = false
            private set

        // 广播 Action，用于向 MainActivity 发送状态更新
        const val ACTION_VOICE_STATUS_UPDATE = "com.wechatassistant.ACTION_VOICE_STATUS_UPDATE"
        const val EXTRA_STATUS_TEXT = "status_text"
        const val EXTRA_STATUS_COLOR = "status_color"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        isServiceRunning = true

        // 获取 WakeLock 防止 CPU 休眠
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoiceAssistant::VoiceListening")
        wakeLock?.acquire()

        // 初始化语音识别（内部会初始化 TTS）
        voiceRecognitionService = VoiceRecognitionService(this)
        voiceRecognitionService?.requireWakeWord = false

        voiceRecognitionService?.setCommandListener(object : VoiceRecognitionService.VoiceCommandListener {
            override fun onCommandRecognized(command: String) {
                Log.d(TAG, "Command recognized: $command")
                sendStatusUpdate("🎤 识别: $command", 0xFF2196F3.toInt())
            }

            override fun onCommandExecuted(command: VoiceCommandProcessor.Command) {
                Log.d(TAG, "Command executed: ${command.contactName}")
                val contactName = command.contactName ?: return
                val callType = if (command.type == VoiceCommandProcessor.CommandType.VIDEO_CALL) "视频" else "语音"
                sendStatusUpdate("📞 拨打${contactName}${callType}...", 0xFF4CAF50.toInt())

                // 注意：广播已由 VoiceRecognitionService.executeCommand() 发送（且用的是微信名），
                // 这里不再重复发送，避免无障碍服务收到两次拨打指令

                // 拨打后延迟继续监听
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isListening) {
                        voiceRecognitionService?.startListening()
                        sendStatusUpdate("🎤 语音监听中，说「给XXX打电话」", 0xFF4CAF50.toInt())
                    }
                }, 15000) // 15秒后恢复监听（给拨打留时间）
            }

            override fun onError(error: String) {
                Log.e(TAG, "Error: $error")
                if (!error.contains("未识别") && !error.contains("超时")) {
                    sendStatusUpdate("⚠️ $error", 0xFFFF9800.toInt())
                }
                // 出错后继续监听
                restartListening()
            }

            override fun onWakeWordDetected() {
                Log.d(TAG, "Wake word detected")
                sendStatusUpdate("✨ 在听...", 0xFF4CAF50.toInt())
            }

            override fun onWaitingForCommand() {
                Log.d(TAG, "Waiting for command")
                sendStatusUpdate("✨ 请说命令...", 0xFF4CAF50.toInt())
                restartListening()
            }

            override fun onModelDownloadProgress(progress: Int) {
                Log.d(TAG, "Model download: $progress%")
                sendStatusUpdate("📥 模型下载: $progress%", 0xFFFF9800.toInt())
            }

            override fun onModelReady() {
                Log.d(TAG, "Model ready")
                sendStatusUpdate("✅ 模型就绪", 0xFF4CAF50.toInt())
            }
        })

        startForeground(NOTIFICATION_ID, createNotification("语音助手正在运行"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            "START" -> {
                if (!isListening) {
                    voiceRecognitionService?.startListening()
                    isListening = true
                    sendStatusUpdate("🎤 语音监听中，说「给XXX打电话」", 0xFF4CAF50.toInt())
                    updateNotification("🎤 语音监听中")
                }
            }
            "STOP" -> {
                voiceRecognitionService?.stopListening()
                isListening = false
                sendStatusUpdate("⏸️ 语音控制已关闭", 0xFF9E9E9E.toInt())
                stopSelf()
            }
        }
        return START_STICKY // 被系统杀后尝试重启
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        voiceRecognitionService?.stopListening()
        voiceRecognitionService?.destroy()
        voiceRecognitionService = null
        isListening = false
        isServiceRunning = false

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        sendStatusUpdate("⏸️ 语音控制已关闭", 0xFF9E9E9E.toInt())
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun restartListening() {
        if (isListening) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isListening && isServiceRunning) {
                    voiceRecognitionService?.startListening()
                    sendStatusUpdate("🎤 语音监听中，说「给XXX打电话」", 0xFF4CAF50.toInt())
                }
            }, 1000)
        }
    }

    private fun createNotification(contentText: String): Notification {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("微信语音助手")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_mic_on)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "语音监听服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "语音助手后台监听"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendStatusUpdate(text: String, color: Int) {
        val intent = Intent(ACTION_VOICE_STATUS_UPDATE).apply {
            putExtra(EXTRA_STATUS_TEXT, text)
            putExtra(EXTRA_STATUS_COLOR, color)
        }
        sendBroadcast(intent)
    }
}
