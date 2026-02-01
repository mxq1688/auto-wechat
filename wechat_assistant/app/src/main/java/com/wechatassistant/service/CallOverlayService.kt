package com.wechatassistant.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 拨打电话时的 Loading 提示服务
 */
class CallOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var loadingView: LinearLayout? = null
    private var statusText: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // 超时自动关闭（20秒）
    private val timeoutRunnable = Runnable {
        hide()
    }
    
    companion object {
        private var instance: CallOverlayService? = null
        private const val TIMEOUT_MS = 20000L
        
        fun show(context: Context, contactName: String, isVideo: Boolean) {
            val intent = Intent(context, CallOverlayService::class.java).apply {
                putExtra("action", "show")
                putExtra("contact", contactName)
                putExtra("isVideo", isVideo)
            }
            context.startService(intent)
        }
        
        fun updateStatus(status: String) {
            instance?.updateStatusText(status)
        }
        
        fun hide() {
            instance?.hideLoading()
        }
        
        fun isShowing(): Boolean = instance?.loadingView != null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action") ?: return START_NOT_STICKY
        
        when (action) {
            "show" -> {
                val contact = intent.getStringExtra("contact") ?: ""
                val isVideo = intent.getBooleanExtra("isVideo", true)
                showLoading(contact, isVideo)
            }
            "hide" -> hideLoading()
        }
        
        return START_NOT_STICKY
    }

    private fun showLoading(contactName: String, isVideo: Boolean) {
        if (loadingView != null) {
            // 已经显示，只更新文字
            val callType = if (isVideo) "视频" else "语音"
            statusText?.text = "📞 正在拨打${contactName}..."
            return
        }
        
        val density = resources.displayMetrics.density
        val callType = if (isVideo) "视频" else "语音"
        
        // 创建 Loading 视图
        loadingView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xE6333333.toInt())
            setPadding((16 * density).toInt(), (12 * density).toInt(), (20 * density).toInt(), (12 * density).toInt())
        }
        
        // 图标
        val iconView = TextView(this).apply {
            text = if (isVideo) "📹" else "📞"
            textSize = 20f
        }
        
        // 状态文字
        statusText = TextView(this).apply {
            text = "正在拨打${contactName}..."
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding((12 * density).toInt(), 0, 0, 0)
        }
        
        loadingView?.addView(iconView)
        loadingView?.addView(statusText)
        
        // 窗口参数 - 顶部居中的小提示条
        val params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (80 * density).toInt()  // 距离顶部80dp
        }
        
        try {
            windowManager?.addView(loadingView, params)
            // 设置超时
            handler.postDelayed(timeoutRunnable, TIMEOUT_MS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideLoading() {
        handler.removeCallbacks(timeoutRunnable)
        
        loadingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        loadingView = null
        statusText = null
        
        stopSelf()
    }
    
    private fun updateStatusText(status: String) {
        handler.post {
            statusText?.text = status
        }
    }

    override fun onDestroy() {
        instance = null
        hideLoading()
        super.onDestroy()
    }
}
