package com.wechatassistant.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.wechatassistant.R
import com.wechatassistant.manager.SettingsManager
import com.wechatassistant.service.CallNotificationListenerService
import com.wechatassistant.service.CoordinatePickerService
import com.wechatassistant.service.EnhancedWeChatAccessibilityService
import com.wechatassistant.service.FloatingBallService

class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings = SettingsManager.getInstance(this)

        // 返回按钮
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        setupPermissionButtons()
        setupFeatureSwitches()
        setupToolButtons()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupPermissionButtons() {
        findViewById<Button>(R.id.btnEnableAccessibility).setOnClickListener {
            openAccessibilitySettings()
        }

        findViewById<Button>(R.id.btnEnableNotification).setOnClickListener {
            openNotificationListenerSettings()
        }

        findViewById<Button>(R.id.btnEnableOverlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

    private fun setupFeatureSwitches() {
        val switchAutoAnswer = findViewById<Switch>(R.id.switchAutoAnswer)
        val switchTTS = findViewById<Switch>(R.id.switchTTS)
        val switchMessageMonitor = findViewById<Switch>(R.id.switchMessageMonitor)
        val switchAutoReply = findViewById<Switch>(R.id.switchAutoReply)

        // 加载当前设置
        switchAutoAnswer.isChecked = settings.autoAnswerVideo
        switchTTS.isChecked = settings.ttsEnabled
        switchMessageMonitor.isChecked = settings.messageMonitorEnabled
        switchAutoReply.isChecked = settings.autoReplyEnabled

        // 监听变化
        switchAutoAnswer.setOnCheckedChangeListener { _, isChecked ->
            settings.autoAnswerVideo = isChecked
        }

        switchTTS.setOnCheckedChangeListener { _, isChecked ->
            settings.ttsEnabled = isChecked
        }

        switchMessageMonitor.setOnCheckedChangeListener { _, isChecked ->
            settings.messageMonitorEnabled = isChecked
        }

        switchAutoReply.setOnCheckedChangeListener { _, isChecked ->
            settings.autoReplyEnabled = isChecked
            EnhancedWeChatAccessibilityService.instance?.getAutoReplyManager()?.isEnabled = isChecked
        }
    }

    private fun setupToolButtons() {
        findViewById<Button>(R.id.btnVoiceSettings).setOnClickListener {
            startActivity(Intent(this, VoiceSettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnCoordinatePicker).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (CoordinatePickerService.isRunning()) {
                stopService(Intent(this, CoordinatePickerService::class.java))
                Toast.makeText(this, "坐标采集工具已关闭", Toast.LENGTH_SHORT).show()
            } else {
                startService(Intent(this, CoordinatePickerService::class.java))
                Toast.makeText(this, "坐标采集工具已启动，请切换到微信", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnFloatingBall).setOnClickListener {
            toggleFloatingBall()
        }
    }

    private fun updatePermissionStatus() {
        val tvAccessibilityStatus = findViewById<TextView>(R.id.tvAccessibilityStatus)
        val tvNotificationStatus = findViewById<TextView>(R.id.tvNotificationStatus)
        val tvOverlayStatus = findViewById<TextView>(R.id.tvOverlayStatus)

        val isAccessibilityEnabled = EnhancedWeChatAccessibilityService.isServiceRunning()
        val isNotificationEnabled = isNotificationListenerEnabled()
        val isOverlayEnabled = Settings.canDrawOverlays(this)

        tvAccessibilityStatus.text = if (isAccessibilityEnabled) "✅ 已启用" else "❌ 未启用"
        tvAccessibilityStatus.setTextColor(if (isAccessibilityEnabled) 0xFF4CAF50.toInt() else 0xFFE91E63.toInt())

        tvNotificationStatus.text = if (isNotificationEnabled) "✅ 已启用" else "❌ 未启用 (自动接听需要)"
        tvNotificationStatus.setTextColor(if (isNotificationEnabled) 0xFF4CAF50.toInt() else 0xFFE91E63.toInt())

        tvOverlayStatus.text = if (isOverlayEnabled) "✅ 已启用" else "❌ 未启用"
        tvOverlayStatus.setTextColor(if (isOverlayEnabled) 0xFF4CAF50.toInt() else 0xFFE91E63.toInt())
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, CallNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "请找到并启用「微信助手」服务", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开无障碍设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
            Toast.makeText(this, "请找到并启用「微信助手」", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开通知监听设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFloatingBall() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }

        if (FloatingBallService.instance != null) {
            stopService(Intent(this, FloatingBallService::class.java))
            Toast.makeText(this, "悬浮球已关闭", Toast.LENGTH_SHORT).show()
        } else {
            startService(Intent(this, FloatingBallService::class.java))
            Toast.makeText(this, "悬浮球已启动", Toast.LENGTH_SHORT).show()
        }
    }
}

