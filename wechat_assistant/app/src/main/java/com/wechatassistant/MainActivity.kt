package com.wechatassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var enableServiceButton: Button
    private lateinit var startFloatingBallButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        statusText = findViewById(R.id.statusText)
        enableServiceButton = findViewById(R.id.enableServiceButton)
        startFloatingBallButton = findViewById(R.id.startFloatingBallButton)
        
        // Set button click listeners
        enableServiceButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "请在辅助功能中启用微信助手服务", Toast.LENGTH_LONG).show()
        }
        
        startFloatingBallButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(this, com.wechatassistant.service.FloatingBallService::class.java)
                startService(intent)
                Toast.makeText(this, "悬浮球已启动", Toast.LENGTH_SHORT).show()
                updateStatus()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    private fun updateStatus() {
        val overlayEnabled = Settings.canDrawOverlays(this)
        val status = "悬浮窗权限: " + if (overlayEnabled) "已授权" else "未授权"
        statusText.text = status
        startFloatingBallButton.isEnabled = overlayEnabled
    }
}