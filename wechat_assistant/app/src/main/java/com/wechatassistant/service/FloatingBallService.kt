package com.wechatassistant.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.wechatassistant.MainActivity
import com.wechatassistant.R
import com.wechatassistant.manager.SettingsManager

/**
 * 增强版悬浮球服务
 * 提供快捷操作菜单和状态显示
 */
class FloatingBallService : Service() {
    
    companion object {
        private const val TAG = "FloatingBallService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_ball_channel"
        
        const val ACTION_SHOW_MENU = "com.wechatassistant.SHOW_MENU"
        const val ACTION_HIDE_MENU = "com.wechatassistant.HIDE_MENU"
        const val ACTION_UPDATE_STATUS = "com.wechatassistant.UPDATE_STATUS"
        
        @Volatile
        var instance: FloatingBallService? = null
            private set
    }
    
    private lateinit var windowManager: WindowManager
    private lateinit var settings: SettingsManager
    
    private var floatingBallView: View? = null
    private var menuView: View? = null
    private var floatingBallParams: WindowManager.LayoutParams? = null
    private var menuParams: WindowManager.LayoutParams? = null
    
    private var isMenuVisible = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    
    // 状态
    private var autoReplyEnabled = false
    private var messageCount = 0
    
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                EnhancedWeChatAccessibilityService.ACTION_NEW_MESSAGE -> {
                    messageCount++
                    updateBadge()
                }
                EnhancedWeChatAccessibilityService.ACTION_SERVICE_CONNECTED -> {
                    showToast("无障碍服务已连接")
                    updateStatus()
                }
                EnhancedWeChatAccessibilityService.ACTION_SERVICE_DISCONNECTED -> {
                    showToast("无障碍服务已断开")
                    updateStatus()
                }
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settings = SettingsManager.getInstance(this)
        autoReplyEnabled = settings.autoReplyEnabled
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        createFloatingBall()
        createMenu()
        
        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction(EnhancedWeChatAccessibilityService.ACTION_NEW_MESSAGE)
            addAction(EnhancedWeChatAccessibilityService.ACTION_SERVICE_CONNECTED)
            addAction(EnhancedWeChatAccessibilityService.ACTION_SERVICE_DISCONNECTED)
        }
        registerReceiver(serviceReceiver, filter, RECEIVER_NOT_EXPORTED)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮球服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "微信助手悬浮球服务通知"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("微信助手运行中")
            .setContentText("点击打开设置")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingBall() {
        // 创建悬浮球视图
        floatingBallView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            
            // 设置圆形背景
            val size = settings.floatingBallSize
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4CAF50"))
                setStroke(2, Color.parseColor("#388E3C"))
            }
            background = drawable
            alpha = settings.floatingBallOpacity
            
            // 添加图标文字
            addView(TextView(this@FloatingBallService).apply {
            text = "W"
                textSize = (size / 3).toFloat()
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            })
            
            // 添加消息数量角标
            addView(TextView(this@FloatingBallService).apply {
                tag = "badge"
                textSize = 10f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                visibility = View.GONE
            })
        }
        
        // 设置布局参数
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val size = settings.floatingBallSize
        floatingBallParams = WindowManager.LayoutParams(
            dpToPx(size),
            dpToPx(size),
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }
        
        // 添加触摸监听
        floatingBallView?.setOnTouchListener { _, event ->
            handleFloatingBallTouch(event)
        }
        
        windowManager.addView(floatingBallView, floatingBallParams)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun createMenu() {
        // 创建菜单视图
        menuView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            
            // 设置圆角背景
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#FFFFFF"))
                setStroke(1, Color.parseColor("#E0E0E0"))
            }
            background = drawable
            elevation = dpToPx(8).toFloat()
            
            // 添加菜单项
            addView(createMenuItem("自动回复", autoReplyEnabled) {
                toggleAutoReply()
            })
            
            addView(createDivider())
            
            addView(createMenuItem("消息监控", settings.messageMonitorEnabled) {
                toggleMessageMonitor()
            })
            
            addView(createDivider())
            
            addView(createMenuItem("视频自动接听", settings.autoAnswerVideo) {
                toggleAutoAnswer()
            })
            
            addView(createDivider())
            
            addView(createMenuItem("语音播报", settings.ttsEnabled) {
                toggleTTS()
            })
            
            addView(createDivider())
            
            addView(createMenuButton("打开设置") {
                openSettings()
            })
            
            addView(createMenuButton("清空消息计数") {
                clearMessageCount()
            })
            
            addView(createMenuButton("关闭悬浮球") {
                stopSelf()
            })
        }
        
        // 设置菜单布局参数
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 400
        }
        
        // 点击菜单外部关闭菜单
        menuView?.setOnTouchListener { _, _ ->
            hideMenu()
            true
        }
    }
    
    private fun createMenuItem(title: String, isEnabled: Boolean, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12))
            
            addView(TextView(this@FloatingBallService).apply {
                text = title
                textSize = 14f
                setTextColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            
            addView(TextView(this@FloatingBallService).apply {
                tag = "status_$title"
                text = if (isEnabled) "开" else "关"
                textSize = 12f
                setTextColor(if (isEnabled) Color.parseColor("#4CAF50") else Color.parseColor("#999999"))
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(4).toFloat()
                    setColor(if (isEnabled) Color.parseColor("#E8F5E9") else Color.parseColor("#F5F5F5"))
                }
                background = bg
            })
            
            setOnClickListener { onClick() }
        }
    }
    
    private fun createMenuButton(title: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#2196F3"))
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12))
            setOnClickListener { onClick() }
        }
    }
    
    private fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
    }
    
    private fun handleFloatingBallTouch(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                initialX = floatingBallParams?.x ?: 0
                initialY = floatingBallParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                isDragging = false
                return true
                }
                MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                
                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                    isDragging = true
                }
                
                floatingBallParams?.x = initialX + dx.toInt()
                floatingBallParams?.y = initialY + dy.toInt()
                windowManager.updateViewLayout(floatingBallView, floatingBallParams)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // 点击事件
                    onFloatingBallClick()
                }
                return true
            }
        }
        return false
    }
    
    private fun onFloatingBallClick() {
        // 震动反馈
        vibrate()
        
        // 动画效果
        val scaleAnimation = ScaleAnimation(
            1f, 0.8f, 1f, 0.8f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 100
            repeatCount = 1
            repeatMode = Animation.REVERSE
        }
        floatingBallView?.startAnimation(scaleAnimation)
        
        // 切换菜单显示
        if (isMenuVisible) {
            hideMenu()
        } else {
            showMenu()
        }
    }
    
    private fun showMenu() {
        if (isMenuVisible) return
        
        // 更新菜单位置（在悬浮球旁边）
        menuParams?.x = (floatingBallParams?.x ?: 0) + dpToPx(70)
        menuParams?.y = floatingBallParams?.y ?: 0
        
        try {
            windowManager.addView(menuView, menuParams)
            isMenuVisible = true
        } catch (e: Exception) {
            // 视图可能已添加
        }
    }
    
    private fun hideMenu() {
        if (!isMenuVisible) return
        
        try {
            windowManager.removeView(menuView)
            isMenuVisible = false
        } catch (e: Exception) {
            // 视图可能已移除
        }
    }
    
    private fun toggleAutoReply() {
        autoReplyEnabled = !autoReplyEnabled
        settings.autoReplyEnabled = autoReplyEnabled
        
        EnhancedWeChatAccessibilityService.instance?.getAutoReplyManager()?.isEnabled = autoReplyEnabled
        
        updateMenuStatus("自动回复", autoReplyEnabled)
        updateFloatingBallColor()
        showToast(if (autoReplyEnabled) "自动回复已开启" else "自动回复已关闭")
    }
    
    private fun toggleMessageMonitor() {
        settings.messageMonitorEnabled = !settings.messageMonitorEnabled
        updateMenuStatus("消息监控", settings.messageMonitorEnabled)
        showToast(if (settings.messageMonitorEnabled) "消息监控已开启" else "消息监控已关闭")
    }
    
    private fun toggleAutoAnswer() {
        settings.autoAnswerVideo = !settings.autoAnswerVideo
        updateMenuStatus("视频自动接听", settings.autoAnswerVideo)
        showToast(if (settings.autoAnswerVideo) "视频自动接听已开启" else "视频自动接听已关闭")
    }
    
    private fun toggleTTS() {
        settings.ttsEnabled = !settings.ttsEnabled
        updateMenuStatus("语音播报", settings.ttsEnabled)
        showToast(if (settings.ttsEnabled) "语音播报已开启" else "语音播报已关闭")
    }
    
    private fun updateMenuStatus(title: String, isEnabled: Boolean) {
        menuView?.findViewWithTag<TextView>("status_$title")?.apply {
            text = if (isEnabled) "开" else "关"
            setTextColor(if (isEnabled) Color.parseColor("#4CAF50") else Color.parseColor("#999999"))
            
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(4).toFloat()
                setColor(if (isEnabled) Color.parseColor("#E8F5E9") else Color.parseColor("#F5F5F5"))
            }
            background = bg
        }
    }
    
    private fun updateFloatingBallColor() {
        val color = if (autoReplyEnabled) "#4CAF50" else "#9E9E9E"
        (floatingBallView?.background as? GradientDrawable)?.setColor(Color.parseColor(color))
    }
    
    private fun updateBadge() {
        floatingBallView?.findViewWithTag<TextView>("badge")?.apply {
            if (messageCount > 0) {
                text = if (messageCount > 99) "99+" else messageCount.toString()
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }
    
    private fun clearMessageCount() {
        messageCount = 0
        updateBadge()
        showToast("消息计数已清空")
    }
    
    private fun updateStatus() {
        val isServiceRunning = EnhancedWeChatAccessibilityService.isServiceRunning()
        updateFloatingBallColor()
    }
    
    private fun openSettings() {
        hideMenu()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
    
    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        
        try {
            unregisterReceiver(serviceReceiver)
        } catch (e: Exception) {}
        
        try {
            floatingBallView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {}
        
        try {
            if (isMenuVisible) {
                menuView?.let { windowManager.removeView(it) }
            }
        } catch (e: Exception) {}
        }
    }
