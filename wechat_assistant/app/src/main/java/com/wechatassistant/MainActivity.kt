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
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.ComponentName
import com.wechatassistant.manager.AutoReplyManager
import com.wechatassistant.manager.SettingsManager
import com.wechatassistant.service.CoordinatePickerService
import com.wechatassistant.service.CallNotificationListenerService
import com.wechatassistant.service.EnhancedWeChatAccessibilityService
import com.wechatassistant.service.FloatingBallService
import com.wechatassistant.service.VoiceRecognitionService
import com.wechatassistant.ui.VideoCallActivity
import com.wechatassistant.voice.VoiceCommandProcessor

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
    private var voiceRecognitionService: VoiceRecognitionService? = null
    
    // Views
    private lateinit var statusText: TextView
    private lateinit var accessibilityStatusText: TextView
    private lateinit var notificationListenerStatusText: TextView
    private lateinit var enableServiceButton: Button
    private lateinit var enableNotificationListenerButton: Button
    private lateinit var startFloatingBallButton: Button
    private lateinit var titleBar: LinearLayout
    private lateinit var tvTitle: TextView
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
    private lateinit var editTargetUserId: EditText
    private lateinit var messageCountText: TextView
    
    // 语音控制相关
    private lateinit var switchVoiceControl: Switch
    private lateinit var voiceStatusText: TextView
    private lateinit var contactListContainer: GridLayout
    private lateinit var tvNoContacts: View
    
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
        // 标题栏
        titleBar = findViewById(R.id.titleBar)
        tvTitle = findViewById(R.id.tvTitle)
        
        // 状态显示（隐藏的）
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
        editTargetUserId = findViewById(R.id.editTargetUserId)
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
        
        // 语音控制相关
        switchVoiceControl = findViewById(R.id.switchVoiceControl)
        voiceStatusText = findViewById(R.id.voiceStatusText)
        contactListContainer = findViewById(R.id.contactListContainer)
        tvNoContacts = findViewById(R.id.tvNoContacts)
        
        // 加载联系人列表
        loadContactList()
        
        // 右上角设置按钮
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }
        
        switchVoiceControl.setOnCheckedChangeListener { _, isChecked ->
            toggleVoiceControl(isChecked)
        }
        
        setupVoiceRecognition()
        
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
        // 刷新联系人列表
        loadContactList()
        // 返回界面时恢复语音识别
        if (isVoiceControlEnabled) {
            restartListeningIfEnabled()
        }
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
        
        // 更新标题栏颜色
        updateTitleBarColor(isEnabled)
    }
    
    private fun updateTitleBarColor(serviceEnabled: Boolean) {
        val bgColor = if (serviceEnabled) 0xFF4CAF50.toInt() else 0xFFE53935.toInt()  // 绿色 / 红色
        titleBar.setBackgroundColor(bgColor)
        
        // 更新状态栏颜色
        window.statusBarColor = bgColor
        
        // 更新标题文字
        tvTitle.text = if (serviceEnabled) "微信助手" else "微信助手 (服务未启用)"
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
        val targetUserId = editTargetUserId.text.toString().trim()
        
        if (userId.isEmpty()) {
            showToast("请先设置我的ID")
            return
        }
        
        if (targetUserId.isEmpty()) {
            showToast("请输入对方ID")
            return
        }
        
        // 保存设置
        saveSettings()
        
        // 发起视频通话
        val intent = Intent(this, VideoCallActivity::class.java).apply {
            putExtra(VideoCallActivity.EXTRA_TARGET_USER_ID, targetUserId)
            putExtra(VideoCallActivity.EXTRA_IS_INCOMING, false)
        }
        startActivity(intent)
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
    
    private var isVoiceControlEnabled = false
    
    private fun setupVoiceRecognition() {
        // 初始化语音识别服务
        voiceRecognitionService = VoiceRecognitionService(this)
        voiceRecognitionService?.requireWakeWord = false  // 不需要唤醒词，直接说命令
        
        voiceRecognitionService?.setCommandListener(object : VoiceRecognitionService.VoiceCommandListener {
            override fun onCommandRecognized(command: String) {
                runOnUiThread {
                    voiceStatusText.text = "🎤 识别中..."
                    voiceStatusText.setTextColor(0xFF2196F3.toInt())
                }
                // 继续监听
                restartListeningIfEnabled()
            }
            
            override fun onCommandExecuted(command: VoiceCommandProcessor.Command) {
                runOnUiThread {
                    val contactName = command.contactName ?: return@runOnUiThread
                    val callType = if (command.type == VoiceCommandProcessor.CommandType.VIDEO_CALL) "视频" else "语音"
                    voiceStatusText.text = "📞 拨打${contactName}..."
                    voiceStatusText.setTextColor(0xFF4CAF50.toInt())
                    
                    // 执行打电话！
                    val isVideo = command.type == VoiceCommandProcessor.CommandType.VIDEO_CALL
                    makeCall(contactName, isVideo)
                }
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    // 忽略"未识别到语音"错误，继续监听
                    if (!error.contains("未识别") && !error.contains("超时")) {
                        voiceStatusText.text = "⚠️ 出错"
                        voiceStatusText.setTextColor(0xFFFF9800.toInt())
                    }
                }
                // 继续监听
                restartListeningIfEnabled()
            }
            
            override fun onWakeWordDetected() {
                runOnUiThread {
                    voiceStatusText.text = "✨ 在听..."
                    voiceStatusText.setTextColor(0xFF4CAF50.toInt())
                }
            }
            
            override fun onWaitingForCommand() {
                runOnUiThread {
                    voiceStatusText.text = "✨ 请说命令..."
                    voiceStatusText.setTextColor(0xFF4CAF50.toInt())
                }
                // 继续监听等待命令
                restartListeningIfEnabled()
            }
            
            override fun onModelDownloadProgress(progress: Int) {
                runOnUiThread {
                    voiceStatusText.text = "📥 下载: $progress%"
                    voiceStatusText.setTextColor(0xFFFF9800.toInt())
                }
            }
            
            override fun onModelReady() {
                runOnUiThread {
                    voiceStatusText.text = "✅ 就绪"
                    voiceStatusText.setTextColor(0xFF4CAF50.toInt())
                }
            }
        })
    }
    
    private fun toggleVoiceControl(enabled: Boolean) {
        android.util.Log.d("MainActivity", "toggleVoiceControl: $enabled")
        isVoiceControlEnabled = enabled
        
        if (enabled) {
            // 检查录音权限
            if (!checkAudioPermission()) {
                android.util.Log.e("MainActivity", "Audio permission not granted!")
                switchVoiceControl.isChecked = false
                return
            }
            
            // 检查无障碍服务
            if (!EnhancedWeChatAccessibilityService.isServiceRunning()) {
                android.util.Log.e("MainActivity", "Accessibility service not running!")
                showToast("需要先启用无障碍服务")
                switchVoiceControl.isChecked = false
                showSettingsDialog()  // 直接打开设置
                return
            }
            
            android.util.Log.d("MainActivity", "Starting voice recognition...")
            voiceStatusText.text = "🎤 说「给XXX打电话」"
            voiceStatusText.setTextColor(0xFF4CAF50.toInt())
            
            if (voiceRecognitionService != null) {
                voiceRecognitionService?.startListening()
                android.util.Log.d("MainActivity", "Voice recognition started")
            } else {
                android.util.Log.e("MainActivity", "voiceRecognitionService is NULL!")
                showToast("语音服务初始化失败")
            }
        } else {
            voiceStatusText.text = "已关闭"
            voiceStatusText.setTextColor(0xFF888888.toInt())
            voiceRecognitionService?.stopListening()
        }
    }
    
    private fun restartListeningIfEnabled() {
        if (isVoiceControlEnabled) {
            // 延迟一小段时间后重新开始监听
            window.decorView.postDelayed({
                if (isVoiceControlEnabled) {
                    runOnUiThread {
                        voiceStatusText.text = "🎤 说「给XXX打电话」"
                        voiceStatusText.setTextColor(0xFF4CAF50.toInt())
                    }
                    voiceRecognitionService?.startListening()
                }
            }, 1000)
        }
    }
    
    private fun checkAudioPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
            showToast("请授予录音权限")
            return false
        }
        return true
    }
    
    /**
     * 加载联系人列表显示在首页
     */
    private fun loadContactList() {
        contactListContainer.removeAllViews()
        
        val contacts = settings.getContacts()
        val contactPhotos = settings.getContactPhotos()
        
        if (contacts.isEmpty()) {
            tvNoContacts.visibility = View.VISIBLE
            return
        }
        
        tvNoContacts.visibility = View.GONE
        
        // 转换 dp 到 px
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        
        // 固定3列，计算每个头像大小
        val columnCount = 3
        val totalPadding = (32 * density).toInt()  // 两边padding
        val totalMargin = ((columnCount + 1) * 8 * density).toInt()  // 间距
        val photoSize = (screenWidth - totalPadding - totalMargin) / columnCount
        val itemMargin = (8 * density).toInt()
        
        contactListContainer.columnCount = columnCount
        
        contacts.forEach { (wechatName, _) ->
            // 联系人容器（垂直：圆形照片 + 名字）
            val contactItem = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                val params = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
                }
                layoutParams = params
                isClickable = true
                isFocusable = true
            }
            
            // 圆形头像容器（带阴影效果）
            val photoContainer = android.widget.FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(photoSize, photoSize)
                // 圆形裁剪
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
                elevation = 4 * density  // 添加阴影
            }
            
            // 圆形头像
            val photoView = ImageView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFFE0E0E0.toInt())
                
                val photoPath = contactPhotos[wechatName]
                if (photoPath != null && java.io.File(photoPath).exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(photoPath)
                    setImageBitmap(bitmap)
                } else {
                    // 默认头像 - 显示首字母
                    setBackgroundColor(0xFF4CAF50.toInt())
                }
            }
            
            photoContainer.addView(photoView)
            
            // 如果没有照片，显示名字首字母
            val photoPath = contactPhotos[wechatName]
            if (photoPath == null || !java.io.File(photoPath).exists()) {
                val initialView = TextView(this).apply {
                    text = wechatName.firstOrNull()?.toString() ?: "?"
                    textSize = 32f
                    setTextColor(0xFFFFFFFF.toInt())
                    gravity = android.view.Gravity.CENTER
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                photoContainer.addView(initialView)
            }
            
            // 名字标签
            val nameView = TextView(this).apply {
                text = wechatName
                textSize = 14f
                setTextColor(0xFF333333.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(0, (6 * density).toInt(), 0, 0)
                maxLines = 1
                maxWidth = photoSize
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            
            contactItem.addView(photoContainer)
            contactItem.addView(nameView)
            
            // 添加按压动画效果（放大）
            contactItem.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        // 按下时放大 + 增加阴影
                        v.animate()
                            .scaleX(1.1f)
                            .scaleY(1.1f)
                            .setDuration(120)
                            .start()
                        photoContainer.animate()
                            .translationZ(12 * density)
                            .setDuration(120)
                            .start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        // 松开时恢复
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start()
                        photoContainer.animate()
                            .translationZ(0f)
                            .setDuration(120)
                            .start()
                    }
                }
                false // 返回false让点击事件继续传递
            }
            
            // 点击拨打视频
            contactItem.setOnClickListener {
                makeCall(wechatName, isVideo = true)
            }
            
            // 长按拨打语音
            contactItem.setOnLongClickListener {
                makeCall(wechatName, isVideo = false)
                showToast("正在拨打语音电话...")
                true
            }
            
            contactListContainer.addView(contactItem)
        }
    }
    
    private fun makeCall(contactName: String, isVideo: Boolean) {
        android.util.Log.d("MainActivity", "makeCall: contact=$contactName, isVideo=$isVideo")
        
        val serviceRunning = EnhancedWeChatAccessibilityService.isServiceRunning()
        val serviceInstance = EnhancedWeChatAccessibilityService.instance
        android.util.Log.d("MainActivity", "Service running: $serviceRunning, instance: $serviceInstance")
        
        if (!serviceRunning) {
            android.util.Log.e("MainActivity", "Accessibility service not running!")
            showToast("请先启用辅助功能服务")
            return
        }
        
        val callType = if (isVideo) "视频" else "语音"
        voiceStatusText.text = "正在给${contactName}拨打${callType}电话..."
        
        android.util.Log.d("MainActivity", "Calling service directly...")
        
        // 直接调用服务方法（更可靠）
        EnhancedWeChatAccessibilityService.instance?.startMakeCall(contactName, isVideo)
    }
    
    override fun onPause() {
        super.onPause()
        // 离开界面时暂停语音识别
        if (isVoiceControlEnabled) {
            voiceRecognitionService?.stopListening()
        }
    }
    
    override fun onDestroy() {
        isVoiceControlEnabled = false
        voiceRecognitionService?.destroy()
        super.onDestroy()
        try {
            unregisterReceiver(serviceReceiver)
        } catch (e: Exception) {}
    }
    
    private fun showSettingsDialog() {
        // 跳转到设置页面
        startActivity(Intent(this, com.wechatassistant.ui.SettingsActivity::class.java))
    }
    
    private fun showKeywordSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_keywords_settings, null)
        
        // 数据列表 - 新格式：Map<微信名, MutableList<简称>>
        val contactsMap = settings.getContacts().mapValues { it.value.toMutableList() }.toMutableMap()
        val wakeWordList = settings.getWakeWords().toMutableList()
        val videoKeywordList = settings.getVideoCallKeywords().toMutableList()
        val voiceKeywordList = settings.getVoiceCallKeywords().toMutableList()
        val generalKeywordList = settings.getGeneralCallKeywords().toMutableList()
        
        // 容器
        val contactListContainer = dialogView.findViewById<LinearLayout>(R.id.contactListContainer)
        val wakeWordListContainer = dialogView.findViewById<LinearLayout>(R.id.wakeWordListContainer)
        val videoKeywordListContainer = dialogView.findViewById<LinearLayout>(R.id.videoKeywordListContainer)
        val voiceKeywordListContainer = dialogView.findViewById<LinearLayout>(R.id.voiceKeywordListContainer)
        val generalKeywordListContainer = dialogView.findViewById<LinearLayout>(R.id.generalKeywordListContainer)
        
        // 输入框
        val editNewWechatName = dialogView.findViewById<EditText>(R.id.editNewWechatName)
        val editNewWakeWord = dialogView.findViewById<EditText>(R.id.editNewWakeWord)
        val editNewVideoKeyword = dialogView.findViewById<EditText>(R.id.editNewVideoKeyword)
        val editNewVoiceKeyword = dialogView.findViewById<EditText>(R.id.editNewVoiceKeyword)
        val editNewGeneralKeyword = dialogView.findViewById<EditText>(R.id.editNewGeneralKeyword)
        
        // 按钮
        val btnAddContact = dialogView.findViewById<Button>(R.id.btnAddContact)
        val btnAddWakeWord = dialogView.findViewById<Button>(R.id.btnAddWakeWord)
        val btnAddVideoKeyword = dialogView.findViewById<Button>(R.id.btnAddVideoKeyword)
        val btnAddVoiceKeyword = dialogView.findViewById<Button>(R.id.btnAddVoiceKeyword)
        val btnAddGeneralKeyword = dialogView.findViewById<Button>(R.id.btnAddGeneralKeyword)
        val btnToggleAdvanced = dialogView.findViewById<Button>(R.id.btnToggleAdvanced)
        val advancedSettingsContainer = dialogView.findViewById<LinearLayout>(R.id.advancedSettingsContainer)
        
        val checkRequireWakeWord = dialogView.findViewById<CheckBox>(R.id.checkRequireWakeWord)
        checkRequireWakeWord.isChecked = settings.requireWakeWord
        
        // 刷新联系人列表显示
        fun refreshContactList() {
            contactListContainer.removeAllViews()
            
            if (contactsMap.isEmpty()) {
                val emptyView = TextView(this).apply {
                    text = "暂无联系人，请在下方添加"
                    textSize = 13f
                    setTextColor(0xFF999999.toInt())
                    setPadding(8, 12, 8, 12)
                }
                contactListContainer.addView(emptyView)
                return
            }
            
            contactsMap.forEach { (wechatName, aliases) ->
                // 联系人卡片
                val cardView = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(0xFFFFFFFF.toInt())
                    setPadding(12, 10, 12, 10)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 8 }
                }
                
                // 头部：微信名 + 删除按钮
                val headerRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                
                val nameView = TextView(this).apply {
                    text = "📱 $wechatName"
                    textSize = 15f
                    setTextColor(0xFF1976D2.toInt())
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                
                val deleteContactBtn = TextView(this).apply {
                    text = "删除"
                    textSize = 12f
                    setTextColor(0xFFE53935.toInt())
                    setPadding(16, 4, 0, 4)
                    setOnClickListener {
                        contactsMap.remove(wechatName)
                        refreshContactList()
                    }
                }
                
                headerRow.addView(nameView)
                headerRow.addView(deleteContactBtn)
                cardView.addView(headerRow)
                
                // 简称标签区域
                val aliasContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 6 }
                }
                
                // 添加简称标签
                aliases.forEach { alias ->
                    val tag = TextView(this).apply {
                        text = "  $alias  ×"
                        textSize = 12f
                        setTextColor(0xFFFFFFFF.toInt())
                        setBackgroundColor(0xFF42A5F5.toInt())
                        setPadding(10, 4, 10, 4)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { marginEnd = 6 }
                        setOnClickListener {
                            aliases.remove(alias)
                            if (aliases.isEmpty()) {
                                contactsMap.remove(wechatName)
                            }
                            refreshContactList()
                        }
                    }
                    aliasContainer.addView(tag)
                }
                
                cardView.addView(aliasContainer)
                
                // 添加简称输入区
                val addAliasRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 6 }
                }
                
                val aliasInput = EditText(this).apply {
                    hint = "添加简称..."
                    textSize = 13f
                    setPadding(8, 6, 8, 6)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setBackgroundColor(0xFFF5F5F5.toInt())
                }
                
                val addAliasBtn = TextView(this).apply {
                    text = " +添加 "
                    textSize = 12f
                    setTextColor(0xFF4CAF50.toInt())
                    setPadding(12, 6, 4, 6)
                    setOnClickListener {
                        val newAlias = aliasInput.text.toString().trim()
                        if (newAlias.isNotEmpty() && !aliases.contains(newAlias)) {
                            aliases.add(newAlias)
                            aliasInput.text.clear()
                            refreshContactList()
                        }
                    }
                }
                
                addAliasRow.addView(aliasInput)
                addAliasRow.addView(addAliasBtn)
                cardView.addView(addAliasRow)
                
                contactListContainer.addView(cardView)
            }
        }
        
        // 创建标签的函数
        fun createTagView(container: LinearLayout, list: MutableList<String>, text: String, bgColor: Int) {
            val tag = TextView(this).apply {
                this.text = "  $text  ×"
                textSize = 12f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(bgColor)
                setPadding(12, 6, 12, 6)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8; bottomMargin = 4 }
                setOnClickListener {
                    list.remove(text)
                    container.removeView(this)
                }
            }
            container.addView(tag)
        }
        
        fun refreshTagList(container: LinearLayout, list: MutableList<String>, bgColor: Int) {
            container.removeAllViews()
            list.forEach { createTagView(container, list, it, bgColor) }
        }
        
        // 初始化列表显示
        refreshContactList()
        refreshTagList(wakeWordListContainer, wakeWordList, 0xFFFF9800.toInt())
        refreshTagList(videoKeywordListContainer, videoKeywordList, 0xFF2196F3.toInt())
        refreshTagList(voiceKeywordListContainer, voiceKeywordList, 0xFF4CAF50.toInt())
        refreshTagList(generalKeywordListContainer, generalKeywordList, 0xFFFF9800.toInt())
        
        // 添加新联系人（微信名）
        btnAddContact.setOnClickListener {
            val wechatName = editNewWechatName.text.toString().trim()
            if (wechatName.isNotEmpty()) {
                if (!contactsMap.containsKey(wechatName)) {
                    // 新联系人，默认添加微信名本身作为第一个简称
                    contactsMap[wechatName] = mutableListOf(wechatName)
                    refreshContactList()
                    editNewWechatName.text.clear()
                } else {
                    showToast("该联系人已存在")
                }
            } else {
                showToast("请输入微信名")
            }
        }
        
        // 添加唤醒词
        btnAddWakeWord.setOnClickListener {
            val word = editNewWakeWord.text.toString().trim()
            if (word.isNotEmpty() && !wakeWordList.contains(word)) {
                wakeWordList.add(word)
                createTagView(wakeWordListContainer, wakeWordList, word, 0xFFFF9800.toInt())
                editNewWakeWord.text.clear()
            }
        }
        
        // 添加视频关键词
        btnAddVideoKeyword.setOnClickListener {
            val word = editNewVideoKeyword.text.toString().trim()
            if (word.isNotEmpty() && !videoKeywordList.contains(word)) {
                videoKeywordList.add(word)
                createTagView(videoKeywordListContainer, videoKeywordList, word, 0xFF2196F3.toInt())
                editNewVideoKeyword.text.clear()
            }
        }
        
        // 添加语音关键词
        btnAddVoiceKeyword.setOnClickListener {
            val word = editNewVoiceKeyword.text.toString().trim()
            if (word.isNotEmpty() && !voiceKeywordList.contains(word)) {
                voiceKeywordList.add(word)
                createTagView(voiceKeywordListContainer, voiceKeywordList, word, 0xFF4CAF50.toInt())
                editNewVoiceKeyword.text.clear()
            }
        }
        
        // 添加通用关键词
        btnAddGeneralKeyword.setOnClickListener {
            val word = editNewGeneralKeyword.text.toString().trim()
            if (word.isNotEmpty() && !generalKeywordList.contains(word)) {
                generalKeywordList.add(word)
                createTagView(generalKeywordListContainer, generalKeywordList, word, 0xFFFF9800.toInt())
                editNewGeneralKeyword.text.clear()
            }
        }
        
        // 展开/折叠高级设置
        btnToggleAdvanced.setOnClickListener {
            if (advancedSettingsContainer.visibility == View.GONE) {
                advancedSettingsContainer.visibility = View.VISIBLE
                btnToggleAdvanced.text = "收起"
            } else {
                advancedSettingsContainer.visibility = View.GONE
                btnToggleAdvanced.text = "展开"
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("语音设置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 保存联系人（新格式）
                settings.setContacts(contactsMap)
                // 保存唤醒词
                settings.setWakeWords(wakeWordList.toSet())
                settings.requireWakeWord = checkRequireWakeWord.isChecked
                // 保存关键词
                settings.setVideoCallKeywords(videoKeywordList.toSet())
                settings.setVoiceCallKeywords(voiceKeywordList.toSet())
                settings.setGeneralCallKeywords(generalKeywordList.toSet())
                
                voiceRecognitionService?.requireWakeWord = checkRequireWakeWord.isChecked
                showToast("设置已保存")
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("恢复默认") { _, _ ->
                settings.setWakeWords(SettingsManager.DEFAULT_WAKE_WORDS)
                settings.requireWakeWord = false
                settings.setVideoCallKeywords(SettingsManager.DEFAULT_VIDEO_KEYWORDS)
                settings.setVoiceCallKeywords(SettingsManager.DEFAULT_VOICE_KEYWORDS)
                settings.setGeneralCallKeywords(SettingsManager.DEFAULT_GENERAL_KEYWORDS)
                settings.setContactAliases("")
                showToast("已恢复默认")
            }
            .show()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
