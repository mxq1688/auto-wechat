package com.wechatassistant

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.ComponentName
import com.wechatassistant.manager.AutoReplyManager
import com.wechatassistant.manager.SettingsManager
import com.wechatassistant.service.CallNotificationListenerService
import com.wechatassistant.service.EnhancedWeChatAccessibilityService
import com.wechatassistant.service.FloatingBallService
import com.wechatassistant.ui.VideoCallActivity

/**
 * 主界面
 * 提供应用设置和功能控制
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }
    
    private lateinit var settings: SettingsManager
    
    // Views
    private lateinit var statusText: TextView
    private lateinit var accessibilityStatusText: TextView
    private lateinit var notificationListenerStatusText: TextView
    private lateinit var enableServiceButton: Button
    private lateinit var enableNotificationListenerButton: Button
    private lateinit var startFloatingBallButton: Button
    private lateinit var switchAutoReply: Switch
    private lateinit var switchAutoAnswer: Switch
    private lateinit var switchTTS: Switch
    private lateinit var switchMessageMonitor: Switch
    private lateinit var switchAutoReplyInGroup: Switch
    private lateinit var editServerUrl: EditText
    private lateinit var editUserId: EditText
    private lateinit var btnSaveSettings: Button
    private lateinit var btnManageRules: Button
    private lateinit var btnTestVideoCall: Button
    private lateinit var messageCountText: TextView
    
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                EnhancedWeChatAccessibilityService.ACTION_SERVICE_CONNECTED -> {
                    updateAccessibilityStatus(true)
                }
                EnhancedWeChatAccessibilityService.ACTION_SERVICE_DISCONNECTED -> {
                    updateAccessibilityStatus(false)
                }
                EnhancedWeChatAccessibilityService.ACTION_NEW_MESSAGE -> {
                    updateMessageCount()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        settings = SettingsManager.getInstance(this)
        
        initViews()
        loadSettings()
        checkPermissions()
        registerReceivers()
    }
    
    private fun initViews() {
        // 状态显示
        statusText = findViewById(R.id.statusText)
        accessibilityStatusText = findViewById(R.id.accessibilityStatusText)
        notificationListenerStatusText = findViewById(R.id.notificationListenerStatusText)
        messageCountText = findViewById(R.id.messageCountText)
        
        // 按钮
        enableServiceButton = findViewById(R.id.enableServiceButton)
        enableNotificationListenerButton = findViewById(R.id.enableNotificationListenerButton)
        startFloatingBallButton = findViewById(R.id.startFloatingBallButton)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        btnManageRules = findViewById(R.id.btnManageRules)
        btnTestVideoCall = findViewById(R.id.btnTestVideoCall)
        
        // 开关
        switchAutoReply = findViewById(R.id.switchAutoReply)
        switchAutoAnswer = findViewById(R.id.switchAutoAnswer)
        switchTTS = findViewById(R.id.switchTTS)
        switchMessageMonitor = findViewById(R.id.switchMessageMonitor)
        switchAutoReplyInGroup = findViewById(R.id.switchAutoReplyInGroup)
        
        // 输入框
        editServerUrl = findViewById(R.id.editServerUrl)
        editUserId = findViewById(R.id.editUserId)
        
        // 设置点击事件
        enableServiceButton.setOnClickListener {
            openAccessibilitySettings()
        }
        
        enableNotificationListenerButton.setOnClickListener {
            openNotificationListenerSettings()
        }
        
        startFloatingBallButton.setOnClickListener {
            toggleFloatingBall()
        }
        
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        btnManageRules.setOnClickListener {
            showRulesDialog()
        }
        
        btnTestVideoCall.setOnClickListener {
            testVideoCall()
        }
        
        // 开关监听
        switchAutoReply.setOnCheckedChangeListener { _, isChecked ->
            settings.autoReplyEnabled = isChecked
            EnhancedWeChatAccessibilityService.instance?.getAutoReplyManager()?.isEnabled = isChecked
            switchAutoReplyInGroup.isEnabled = isChecked
        }
        
        switchAutoAnswer.setOnCheckedChangeListener { _, isChecked ->
            settings.autoAnswerVideo = isChecked
        }
        
        switchTTS.setOnCheckedChangeListener { _, isChecked ->
            settings.ttsEnabled = isChecked
        }
        
        switchMessageMonitor.setOnCheckedChangeListener { _, isChecked ->
            settings.messageMonitorEnabled = isChecked
        }
        
        switchAutoReplyInGroup.setOnCheckedChangeListener { _, isChecked ->
            settings.autoReplyInGroup = isChecked
        }
    }
    
    private fun loadSettings() {
        switchAutoReply.isChecked = settings.autoReplyEnabled
        switchAutoAnswer.isChecked = settings.autoAnswerVideo
        switchTTS.isChecked = settings.ttsEnabled
        switchMessageMonitor.isChecked = settings.messageMonitorEnabled
        switchAutoReplyInGroup.isChecked = settings.autoReplyInGroup
        switchAutoReplyInGroup.isEnabled = settings.autoReplyEnabled
        
        editServerUrl.setText(settings.signalingServerUrl)
        editUserId.setText(settings.userId)
    }
    
    private fun saveSettings() {
        settings.signalingServerUrl = editServerUrl.text.toString().trim()
        settings.userId = editUserId.text.toString().trim()
        
        showToast("设置已保存")
    }
    
    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(EnhancedWeChatAccessibilityService.ACTION_SERVICE_CONNECTED)
            addAction(EnhancedWeChatAccessibilityService.ACTION_SERVICE_DISCONNECTED)
            addAction(EnhancedWeChatAccessibilityService.ACTION_NEW_MESSAGE)
        }
        registerReceiver(serviceReceiver, filter, RECEIVER_NOT_EXPORTED)
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    private fun updateStatus() {
        // 更新悬浮窗权限状态
        val overlayEnabled = Settings.canDrawOverlays(this)
        val overlayStatus = if (overlayEnabled) "已授权" else "未授权"
        statusText.text = "悬浮窗权限: $overlayStatus"
        startFloatingBallButton.isEnabled = overlayEnabled
        
        // 更新悬浮球按钮文本
        val isFloatingBallRunning = FloatingBallService.instance != null
        startFloatingBallButton.text = if (isFloatingBallRunning) "关闭悬浮球" else "启动悬浮球"
        
        // 更新无障碍服务状态
        val isAccessibilityEnabled = EnhancedWeChatAccessibilityService.isServiceRunning()
        updateAccessibilityStatus(isAccessibilityEnabled)
        
        // 更新通知监听服务状态
        updateNotificationListenerStatus()
        
        // 更新消息计数
        updateMessageCount()
    }
    
    private fun updateAccessibilityStatus(isEnabled: Boolean) {
        val status = if (isEnabled) "已启用" else "未启用"
        val color = if (isEnabled) "#4CAF50" else "#F44336"
        accessibilityStatusText.text = "无障碍服务: $status"
        accessibilityStatusText.setTextColor(android.graphics.Color.parseColor(color))
    }
    
    private fun updateMessageCount() {
        val monitor = EnhancedWeChatAccessibilityService.instance?.getMessageMonitor()
        val count = monitor?.getMessageHistory()?.size ?: 0
        messageCountText.text = "已监控消息: $count 条"
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        showToast("请在辅助功能中启用「微信助手服务」")
    }
    
    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
        showToast("请启用「微信视频通话自动接听」通知监听权限")
    }
    
    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat.isNullOrEmpty()) return false
        val names = flat.split(":")
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == packageName &&
                cn.className == CallNotificationListenerService::class.java.name) {
                return true
            }
        }
        return false
    }
    
    private fun updateNotificationListenerStatus() {
        val isEnabled = isNotificationListenerEnabled()
        val status = if (isEnabled) "已启用" else "未启用"
        val color = if (isEnabled) "#4CAF50" else "#F44336"
        notificationListenerStatusText.text = "通知监听服务: $status"
        notificationListenerStatusText.setTextColor(android.graphics.Color.parseColor(color))
    }
    
    private fun toggleFloatingBall() {
        if (FloatingBallService.instance != null) {
            // 关闭悬浮球
            stopService(Intent(this, FloatingBallService::class.java))
            startFloatingBallButton.text = "启动悬浮球"
            showToast("悬浮球已关闭")
        } else {
            // 检查悬浮窗权限
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                showToast("请授予悬浮窗权限")
                return
            }
            
            // 启动悬浮球
            val intent = Intent(this, FloatingBallService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            startFloatingBallButton.text = "关闭悬浮球"
            showToast("悬浮球已启动")
        }
    }
    
    private fun showRulesDialog() {
        val autoReplyManager = EnhancedWeChatAccessibilityService.instance?.getAutoReplyManager()
            ?: AutoReplyManager(this)
        
        val rules = autoReplyManager.getAllRules()
        
        val items = rules.map { rule ->
            "关键词: ${rule.keywords.joinToString(", ")}\n回复: ${rule.reply}"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("自动回复规则 (${rules.size}条)")
            .setItems(items) { _, index ->
                showEditRuleDialog(rules[index])
            }
            .setPositiveButton("添加规则") { _, _ ->
                showAddRuleDialog()
            }
            .setNegativeButton("重置默认") { _, _ ->
                autoReplyManager.resetToDefault()
                showToast("已重置为默认规则")
            }
            .setNeutralButton("关闭", null)
            .show()
    }
    
    private fun showAddRuleDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_rule, null)
        val editKeywords = view.findViewById<EditText>(R.id.editKeywords)
        val editReply = view.findViewById<EditText>(R.id.editReply)
        
        AlertDialog.Builder(this)
            .setTitle("添加回复规则")
            .setView(view)
            .setPositiveButton("添加") { _, _ ->
                val keywords = editKeywords.text.toString().split(",").map { it.trim() }
                val reply = editReply.text.toString().trim()
                
                if (keywords.isNotEmpty() && reply.isNotEmpty()) {
                    val autoReplyManager = EnhancedWeChatAccessibilityService.instance?.getAutoReplyManager()
                        ?: AutoReplyManager(this)
                    
                    val rule = AutoReplyManager.ReplyRule(
                        id = "custom_${System.currentTimeMillis()}",
                        keywords = keywords,
                        reply = reply
                    )
                    autoReplyManager.addRule(rule)
                    showToast("规则已添加")
                } else {
                    showToast("请输入完整信息")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showEditRuleDialog(rule: AutoReplyManager.ReplyRule) {
        AlertDialog.Builder(this)
            .setTitle("规则操作")
            .setMessage("关键词: ${rule.keywords.joinToString(", ")}\n回复: ${rule.reply}")
            .setPositiveButton("删除") { _, _ ->
                val autoReplyManager = EnhancedWeChatAccessibilityService.instance?.getAutoReplyManager()
                    ?: AutoReplyManager(this)
                autoReplyManager.removeRule(rule.id)
                showToast("规则已删除")
            }
            .setNegativeButton("关闭", null)
            .show()
    }
    
    private fun testVideoCall() {
        val userId = editUserId.text.toString().trim()
        if (userId.isEmpty()) {
            showToast("请先设置用户ID")
            return
        }
        
        // 显示输入目标用户ID的对话框
        val input = EditText(this)
        input.hint = "输入对方用户ID"
        
        AlertDialog.Builder(this)
            .setTitle("发起视频通话")
            .setView(input)
            .setPositiveButton("呼叫") { _, _ ->
                val targetUserId = input.text.toString().trim()
                if (targetUserId.isNotEmpty()) {
                    val intent = Intent(this, VideoCallActivity::class.java).apply {
                        putExtra(VideoCallActivity.EXTRA_TARGET_USER_ID, targetUserId)
                        putExtra(VideoCallActivity.EXTRA_IS_INCOMING, false)
                    }
                    startActivity(intent)
                } else {
                    showToast("请输入对方用户ID")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun checkPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
        
        // 检查通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE + 1
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val denied = permissions.filterIndexed { index, _ -> 
                grantResults[index] != PackageManager.PERMISSION_GRANTED 
            }
            if (denied.isNotEmpty()) {
                showToast("部分权限未授予，某些功能可能无法正常工作")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(serviceReceiver)
        } catch (e: Exception) {}
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
