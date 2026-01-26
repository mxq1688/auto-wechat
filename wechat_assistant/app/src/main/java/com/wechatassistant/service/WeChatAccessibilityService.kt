package com.wechatassistant.service

import android.accessibilityservice.AccessibilityService
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class WeChatAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "WeChatAccessibility"
        private const val WECHAT_PACKAGE = "com.tencent.mm"
    }
    
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.CHINESE)
            }
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName != WECHAT_PACKAGE) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                checkForVideoCall()
            }
        }
    }
    
    private fun checkForVideoCall() {
        val rootNode = rootInActiveWindow ?: return
        
        // Find accept button
        val acceptButton = findNodeByText(rootNode, "接听")
        if (acceptButton != null) {
            // Auto answer after 2 seconds
            handler.postDelayed({
                acceptButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Auto answered video call")
            }, 2000)
        }
        
        rootNode.recycle()
    }
    
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.contains(text) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            if (result != null) return result
            child.recycle()
        }
        return null
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
    }
}