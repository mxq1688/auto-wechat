package com.wechatassistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.wechatassistant.manager.SettingsManager
import com.wechatassistant.service.VoiceListeningForegroundService

/**
 * 开机自启动广播接收器
 * 开机后自动恢复语音监听前台服务
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != "com.htc.intent.action.QUICKBOOT_POWERON") {
            return
        }

        Log.d(TAG, "Boot completed, checking voice control state...")

        val settings = SettingsManager.getInstance(context)
        val voiceEnabled = settings.voiceRecognitionEnabled

        if (voiceEnabled) {
            Log.d(TAG, "Voice control was enabled, starting foreground service...")
            try {
                val serviceIntent = Intent(context, VoiceListeningForegroundService::class.java).apply {
                    action = "START"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "Foreground service started successfully on boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service on boot: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "Voice control was disabled, skipping auto-start")
        }
    }
}
