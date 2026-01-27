package com.wechatassistant.webrtc

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.*

/**
 * WebSocket信令客户端
 * 用于WebRTC连接的信令交换（替代Firebase）
 */
class SignalingClient(
    private val serverUrl: String,
    private val userId: String,
    private val listener: SignalingListener
) {
    
    companion object {
        private const val TAG = "SignalingClient"
        private const val RECONNECT_DELAY = 5000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
    
    interface SignalingListener {
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
        fun onIncomingCall(callerId: String, offer: SessionDescription)
        fun onCallAnswered(answer: SessionDescription)
        fun onCallRejected(reason: String)
        fun onIceCandidate(candidate: IceCandidate)
        fun onCallEnded()
        fun onUserOnline(userId: String)
        fun onUserOffline(userId: String)
    }
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    
    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private var reconnectAttempts = 0
    private var currentCallId: String? = null
    
    /**
     * 连接到信令服务器
     */
    fun connect() {
        if (isConnected) {
            Log.d(TAG, "Already connected")
            return
        }
        
        val request = Request.Builder()
            .url("$serverUrl?userId=$userId")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                isConnected = true
                reconnectAttempts = 0
                
                // 发送注册消息
                sendMessage(JSONObject().apply {
                    put("type", "register")
                    put("userId", userId)
                })
                
                handler.post { listener.onConnected() }
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message received: $text")
                handleMessage(text)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                isConnected = false
                handler.post { listener.onDisconnected() }
                
                // 尝试重连
                scheduleReconnect()
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                isConnected = false
                handler.post { listener.onError(t.message ?: "Unknown error") }
                
                // 尝试重连
                scheduleReconnect()
            }
        })
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
        reconnectAttempts = MAX_RECONNECT_ATTEMPTS // 阻止重连
    }
    
    /**
     * 发起通话
     */
    fun initiateCall(targetUserId: String, offer: SessionDescription): String {
        currentCallId = "${userId}_${targetUserId}_${System.currentTimeMillis()}"
        
        sendMessage(JSONObject().apply {
            put("type", "call_offer")
            put("callId", currentCallId)
            put("from", userId)
            put("to", targetUserId)
            put("offer", JSONObject().apply {
                put("type", offer.type.canonicalForm())
                put("sdp", offer.description)
            })
        })
        
        return currentCallId!!
    }
    
    /**
     * 接受通话
     */
    fun acceptCall(callerId: String, answer: SessionDescription) {
        sendMessage(JSONObject().apply {
            put("type", "call_answer")
            put("callId", currentCallId)
            put("from", userId)
            put("to", callerId)
            put("answer", JSONObject().apply {
                put("type", answer.type.canonicalForm())
                put("sdp", answer.description)
            })
        })
    }
    
    /**
     * 拒绝通话
     */
    fun rejectCall(callerId: String, reason: String = "User rejected") {
        sendMessage(JSONObject().apply {
            put("type", "call_reject")
            put("callId", currentCallId)
            put("from", userId)
            put("to", callerId)
            put("reason", reason)
        })
        currentCallId = null
    }
    
    /**
     * 结束通话
     */
    fun endCall(targetUserId: String) {
        sendMessage(JSONObject().apply {
            put("type", "call_end")
            put("callId", currentCallId)
            put("from", userId)
            put("to", targetUserId)
        })
        currentCallId = null
    }
    
    /**
     * 发送ICE候选者
     */
    fun sendIceCandidate(targetUserId: String, candidate: IceCandidate) {
        sendMessage(JSONObject().apply {
            put("type", "ice_candidate")
            put("callId", currentCallId)
            put("from", userId)
            put("to", targetUserId)
            put("candidate", JSONObject().apply {
                put("sdpMid", candidate.sdpMid)
                put("sdpMLineIndex", candidate.sdpMLineIndex)
                put("candidate", candidate.sdp)
            })
        })
    }
    
    /**
     * 获取在线用户列表
     */
    fun getOnlineUsers() {
        sendMessage(JSONObject().apply {
            put("type", "get_online_users")
            put("userId", userId)
        })
    }
    
    /**
     * 处理收到的消息
     */
    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.getString("type")
            
            handler.post {
                when (type) {
                    "call_offer" -> {
                        currentCallId = json.getString("callId")
                        val callerId = json.getString("from")
                        val offerJson = json.getJSONObject("offer")
                        val offer = SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(offerJson.getString("type")),
                            offerJson.getString("sdp")
                        )
                        listener.onIncomingCall(callerId, offer)
                    }
                    
                    "call_answer" -> {
                        val answerJson = json.getJSONObject("answer")
                        val answer = SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(answerJson.getString("type")),
                            answerJson.getString("sdp")
                        )
                        listener.onCallAnswered(answer)
                    }
                    
                    "call_reject" -> {
                        val reason = json.optString("reason", "Call rejected")
                        listener.onCallRejected(reason)
                        currentCallId = null
                    }
                    
                    "call_end" -> {
                        listener.onCallEnded()
                        currentCallId = null
                    }
                    
                    "ice_candidate" -> {
                        val candidateJson = json.getJSONObject("candidate")
                        val candidate = IceCandidate(
                            candidateJson.getString("sdpMid"),
                            candidateJson.getInt("sdpMLineIndex"),
                            candidateJson.getString("candidate")
                        )
                        listener.onIceCandidate(candidate)
                    }
                    
                    "user_online" -> {
                        val onlineUserId = json.getString("userId")
                        listener.onUserOnline(onlineUserId)
                    }
                    
                    "user_offline" -> {
                        val offlineUserId = json.getString("userId")
                        listener.onUserOffline(offlineUserId)
                    }
                    
                    "error" -> {
                        val error = json.getString("message")
                        listener.onError(error)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }
    
    /**
     * 发送消息
     */
    private fun sendMessage(json: JSONObject) {
        if (!isConnected) {
            Log.w(TAG, "Not connected, cannot send message")
            return
        }
        
        webSocket?.send(json.toString())
        Log.d(TAG, "Message sent: $json")
    }
    
    /**
     * 安排重连
     */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            return
        }
        
        reconnectAttempts++
        Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempts")
        
        handler.postDelayed({
            connect()
        }, RECONNECT_DELAY * reconnectAttempts)
    }
    
    /**
     * 是否已连接
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * 获取当前通话ID
     */
    fun getCurrentCallId(): String? = currentCallId
}

