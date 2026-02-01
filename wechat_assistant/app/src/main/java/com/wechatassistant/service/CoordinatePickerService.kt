package com.wechatassistant.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.wechatassistant.R

/**
 * 坐标采集服务 - 悬浮窗显示点击坐标
 */
class CoordinatePickerService : Service() {
    
    companion object {
        private const val TAG = "CoordinatePicker"
        
        // 保存的坐标
        var savedCoordinates = mutableMapOf<String, Pair<Float, Float>>()
        
        // 坐标名称（按顺序）
        const val COORD_SEARCH_BUTTON = "search_button"      // ① 搜索按钮
        const val COORD_SEARCH_INPUT = "search_input"        // ② 搜索输入框
        const val COORD_PASTE_BUTTON = "paste_button"        // ③ 粘贴按钮
        const val COORD_FIRST_RESULT = "first_result"        // ④ 第一个搜索结果
        const val COORD_PLUS_BUTTON = "plus_button"          // ⑤ 加号按钮
        const val COORD_VIDEO_CALL = "video_call"            // ⑥ 视频通话选项
        const val COORD_CONFIRM_VIDEO = "confirm_video"      // ⑦ 确认视频通话
        const val COORD_CONFIRM_VOICE = "confirm_voice"      // ⑧ 确认语音通话
        
        fun isRunning() = instance != null
        var instance: CoordinatePickerService? = null
    }
    
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var infoView: View? = null
    private var isPickMode = false
    private var currentCoordName = ""
    private var lastX = 0f
    private var lastY = 0f
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createInfoWindow()
        Log.d(TAG, "CoordinatePickerService created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        removeOverlay()
        removeInfoWindow()
        Log.d(TAG, "CoordinatePickerService destroyed")
    }
    
    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun createInfoWindow() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 200
        }
        
        infoView = LayoutInflater.from(this).inflate(R.layout.coordinate_picker_panel, null)
        
        val coordText = infoView?.findViewById<TextView>(R.id.coordText)
        val btnPickSearch = infoView?.findViewById<Button>(R.id.btnPickSearch)
        val btnPickInput = infoView?.findViewById<Button>(R.id.btnPickInput)
        val btnPickPaste = infoView?.findViewById<Button>(R.id.btnPickPaste)
        val btnPickResult = infoView?.findViewById<Button>(R.id.btnPickResult)
        val btnPickPlus = infoView?.findViewById<Button>(R.id.btnPickPlus)
        val btnPickVideo = infoView?.findViewById<Button>(R.id.btnPickVideo)
        val btnPickConfirmVideo = infoView?.findViewById<Button>(R.id.btnPickConfirmVideo)
        val btnPickConfirmVoice = infoView?.findViewById<Button>(R.id.btnPickConfirmVoice)
        val btnClose = infoView?.findViewById<Button>(R.id.btnClose)
        val btnSave = infoView?.findViewById<Button>(R.id.btnSave)
        
        // 设置拖动
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        infoView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(infoView, layoutParams)
                    true
                }
                else -> false
            }
        }
        
        // 按顺序绑定按钮
        btnPickSearch?.setOnClickListener { startPicking(COORD_SEARCH_BUTTON, "① 搜索按钮") }
        btnPickInput?.setOnClickListener { startPicking(COORD_SEARCH_INPUT, "② 搜索输入框") }
        btnPickPaste?.setOnClickListener { startPicking(COORD_PASTE_BUTTON, "③ 粘贴按钮") }
        btnPickResult?.setOnClickListener { startPicking(COORD_FIRST_RESULT, "④ 搜索结果") }
        btnPickPlus?.setOnClickListener { startPicking(COORD_PLUS_BUTTON, "⑤ 加号按钮") }
        btnPickVideo?.setOnClickListener { startPicking(COORD_VIDEO_CALL, "⑥ 视频通话") }
        btnPickConfirmVideo?.setOnClickListener { startPicking(COORD_CONFIRM_VIDEO, "⑦ 确认视频") }
        btnPickConfirmVoice?.setOnClickListener { startPicking(COORD_CONFIRM_VOICE, "⑧ 确认语音") }
        
        btnClose?.setOnClickListener { stopSelf() }
        
        btnSave?.setOnClickListener {
            saveCoordinatesToPrefs()
            Toast.makeText(this, "坐标已保存！", Toast.LENGTH_SHORT).show()
        }
        
        windowManager.addView(infoView, layoutParams)
        updateCoordDisplay()
    }
    
    private fun startPicking(coordName: String, displayName: String) {
        currentCoordName = coordName
        isPickMode = true
        Toast.makeText(this, "请点击 $displayName 的位置", Toast.LENGTH_SHORT).show()
        showOverlay()
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (overlayView != null) return
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        
        overlayView = View(this).apply {
            setBackgroundColor(0x40000000) // 半透明黑色
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN && isPickMode) {
                    lastX = event.rawX
                    lastY = event.rawY
                    savedCoordinates[currentCoordName] = Pair(lastX, lastY)
                    Log.d(TAG, "Picked $currentCoordName: ($lastX, $lastY)")
                    Toast.makeText(this@CoordinatePickerService, 
                        "已记录: (${"%.0f".format(lastX)}, ${"%.0f".format(lastY)})", 
                        Toast.LENGTH_SHORT).show()
                    isPickMode = false
                    removeOverlay()
                    updateCoordDisplay()
                    true
                } else {
                    false
                }
            }
        }
        
        windowManager.addView(overlayView, layoutParams)
    }
    
    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        overlayView = null
    }
    
    private fun removeInfoWindow() {
        infoView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        infoView = null
    }
    
    private fun updateCoordDisplay() {
        val coordText = infoView?.findViewById<TextView>(R.id.coordText)
        val sb = StringBuilder()
        
        // 按顺序显示
        val orderedCoords = listOf(
            COORD_SEARCH_BUTTON to "① 搜索按钮",
            COORD_SEARCH_INPUT to "② 输入框",
            COORD_PASTE_BUTTON to "③ 粘贴",
            COORD_FIRST_RESULT to "④ 结果",
            COORD_PLUS_BUTTON to "⑤ 加号",
            COORD_VIDEO_CALL to "⑥ 视频",
            COORD_CONFIRM_VIDEO to "⑦ 确认视频",
            COORD_CONFIRM_VOICE to "⑧ 确认语音"
        )
        
        for ((key, name) in orderedCoords) {
            val coord = savedCoordinates[key]
            val status = if (coord != null) "✓" else "✗"
            sb.append("$status $name ")
        }
        
        coordText?.text = sb.toString().trim()
    }
    
    private fun saveCoordinatesToPrefs() {
        val prefs = getSharedPreferences("coordinates", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        for ((key, coord) in savedCoordinates) {
            editor.putFloat("${key}_x", coord.first)
            editor.putFloat("${key}_y", coord.second)
        }
        
        editor.apply()
        Log.d(TAG, "Coordinates saved to SharedPreferences")
        
        // 通知 AccessibilityService 更新坐标
        EnhancedWeChatAccessibilityService.instance?.loadCoordinates()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 加载已保存的坐标
        loadCoordinatesFromPrefs()
        updateCoordDisplay()
        return START_NOT_STICKY
    }
    
    private fun loadCoordinatesFromPrefs() {
        val prefs = getSharedPreferences("coordinates", Context.MODE_PRIVATE)
        
        val keys = listOf(
            COORD_SEARCH_BUTTON, COORD_SEARCH_INPUT, COORD_PASTE_BUTTON,
            COORD_FIRST_RESULT, COORD_PLUS_BUTTON, COORD_VIDEO_CALL, 
            COORD_CONFIRM_VIDEO, COORD_CONFIRM_VOICE
        )
        
        for (key in keys) {
            val x = prefs.getFloat("${key}_x", -1f)
            val y = prefs.getFloat("${key}_y", -1f)
            if (x >= 0 && y >= 0) {
                savedCoordinates[key] = Pair(x, y)
            }
        }
    }
}
