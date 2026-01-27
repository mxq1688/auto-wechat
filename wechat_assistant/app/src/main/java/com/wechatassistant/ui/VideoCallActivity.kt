package com.wechatassistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wechatassistant.R
import com.wechatassistant.manager.SettingsManager
import com.wechatassistant.service.TTSManager
import com.wechatassistant.webrtc.SignalingClient
import com.wechatassistant.webrtc.WebRTCClient
import org.webrtc.*

/**
 * 视频通话界面
 */
class VideoCallActivity : AppCompatActivity(), 
    SignalingClient.SignalingListener, 
    WebRTCClient.WebRTCObserver {
    
    companion object {
        private const val TAG = "VideoCallActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
        
        const val EXTRA_TARGET_USER_ID = "target_user_id"
        const val EXTRA_IS_INCOMING = "is_incoming"
        const val EXTRA_CALLER_ID = "caller_id"
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
    
    // Views
    private lateinit var localVideoView: SurfaceViewRenderer
    private lateinit var remoteVideoView: SurfaceViewRenderer
    private lateinit var callStatusText: TextView
    private lateinit var callerNameText: TextView
    private lateinit var btnMute: ImageButton
    private lateinit var btnVideo: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnEndCall: ImageButton
    private lateinit var btnAccept: ImageButton
    private lateinit var btnReject: ImageButton
    private lateinit var incomingCallLayout: View
    private lateinit var callControlsLayout: View
    
    // WebRTC
    private lateinit var eglBase: EglBase
    private var webRTCClient: WebRTCClient? = null
    private var signalingClient: SignalingClient? = null
    
    // State
    private lateinit var settings: SettingsManager
    private var ttsManager: TTSManager? = null
    
    private var targetUserId: String? = null
    private var callerId: String? = null
    private var isIncoming: Boolean = false
    private var isAudioEnabled = true
    private var isVideoEnabled = true
    private var isCallActive = false
    private var pendingOffer: SessionDescription? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)
        
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        settings = SettingsManager.getInstance(this)
        if (settings.ttsEnabled) {
            ttsManager = TTSManager(this)
        }
        
        // 获取Intent参数
        targetUserId = intent.getStringExtra(EXTRA_TARGET_USER_ID)
        isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)
        callerId = intent.getStringExtra(EXTRA_CALLER_ID)
        
        initViews()
        
        if (checkPermissions()) {
            initWebRTC()
        } else {
            requestPermissions()
        }
    }
    
    private fun initViews() {
        localVideoView = findViewById(R.id.localVideoView)
        remoteVideoView = findViewById(R.id.remoteVideoView)
        callStatusText = findViewById(R.id.callStatusText)
        callerNameText = findViewById(R.id.callerNameText)
        btnMute = findViewById(R.id.btnMute)
        btnVideo = findViewById(R.id.btnVideo)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnEndCall = findViewById(R.id.btnEndCall)
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)
        incomingCallLayout = findViewById(R.id.incomingCallLayout)
        callControlsLayout = findViewById(R.id.callControlsLayout)
        
        // 设置按钮点击事件
        btnMute.setOnClickListener { toggleMute() }
        btnVideo.setOnClickListener { toggleVideo() }
        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnEndCall.setOnClickListener { endCall() }
        btnAccept.setOnClickListener { acceptCall() }
        btnReject.setOnClickListener { rejectCall() }
        
        // 根据是否是来电显示不同UI
        if (isIncoming) {
            incomingCallLayout.visibility = View.VISIBLE
            callControlsLayout.visibility = View.GONE
            callerNameText.text = "来电: $callerId"
            callStatusText.text = "正在响铃..."
            ttsManager?.announceIncomingCall(callerId ?: "未知")
        } else {
            incomingCallLayout.visibility = View.GONE
            callControlsLayout.visibility = View.VISIBLE
            callerNameText.text = "呼叫: $targetUserId"
            callStatusText.text = "正在呼叫..."
        }
    }
    
    private fun initWebRTC() {
        eglBase = EglBase.create()
        
        // 初始化视频视图
        localVideoView.init(eglBase.eglBaseContext, null)
        localVideoView.setEnableHardwareScaler(true)
        localVideoView.setMirror(true)
        
        remoteVideoView.init(eglBase.eglBaseContext, null)
        remoteVideoView.setEnableHardwareScaler(true)
        remoteVideoView.setMirror(false)
        
        // 创建WebRTC客户端
        webRTCClient = WebRTCClient(this, eglBase, this)
        webRTCClient?.createLocalMediaStream(localVideoView)
        webRTCClient?.createPeerConnection()
        
        // 连接信令服务器
        val serverUrl = settings.signalingServerUrl
        val userId = settings.userId.ifEmpty { "user_${System.currentTimeMillis()}" }
        
        signalingClient = SignalingClient(serverUrl, userId, this)
        signalingClient?.connect()
    }
    
    private fun startCall() {
        if (targetUserId.isNullOrEmpty()) {
            showToast("目标用户ID无效")
            finish()
            return
        }
        
        webRTCClient?.createOffer { offer ->
            offer?.let {
                signalingClient?.initiateCall(targetUserId!!, it)
                callStatusText.text = "正在等待对方接听..."
            }
        }
    }
    
    private fun acceptCall() {
        incomingCallLayout.visibility = View.GONE
        callControlsLayout.visibility = View.VISIBLE
        callStatusText.text = "正在连接..."
        
        pendingOffer?.let { offer ->
            webRTCClient?.setRemoteDescription(offer) {
                webRTCClient?.createAnswer { answer ->
                    answer?.let {
                        signalingClient?.acceptCall(callerId!!, it)
                    }
                }
            }
        }
    }
    
    private fun rejectCall() {
        signalingClient?.rejectCall(callerId!!, "用户拒绝")
        ttsManager?.announceCallAction("已拒绝通话")
        finish()
    }
    
    private fun endCall() {
        val target = if (isIncoming) callerId else targetUserId
        target?.let { signalingClient?.endCall(it) }
        
        ttsManager?.announceCallAction("通话已结束")
        cleanup()
        finish()
    }
    
    private fun toggleMute() {
        isAudioEnabled = !isAudioEnabled
        webRTCClient?.setAudioEnabled(isAudioEnabled)
        btnMute.setImageResource(
            if (isAudioEnabled) R.drawable.ic_mic_on else R.drawable.ic_mic_off
        )
        showToast(if (isAudioEnabled) "麦克风已开启" else "麦克风已静音")
    }
    
    private fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        webRTCClient?.setVideoEnabled(isVideoEnabled)
        btnVideo.setImageResource(
            if (isVideoEnabled) R.drawable.ic_video_on else R.drawable.ic_video_off
        )
        localVideoView.visibility = if (isVideoEnabled) View.VISIBLE else View.GONE
        showToast(if (isVideoEnabled) "摄像头已开启" else "摄像头已关闭")
    }
    
    private fun switchCamera() {
        webRTCClient?.switchCamera()
    }
    
    // ==================== SignalingClient.SignalingListener ====================
    
    override fun onConnected() {
        runOnUiThread {
            showToast("信令服务器已连接")
            
            // 如果是主动呼叫，开始通话
            if (!isIncoming) {
                startCall()
            }
        }
    }
    
    override fun onDisconnected() {
        runOnUiThread {
            callStatusText.text = "连接已断开"
        }
    }
    
    override fun onError(error: String) {
        runOnUiThread {
            showToast("错误: $error")
            callStatusText.text = "连接错误"
        }
    }
    
    override fun onIncomingCall(callerId: String, offer: SessionDescription) {
        runOnUiThread {
            this.callerId = callerId
            this.pendingOffer = offer
            callerNameText.text = "来电: $callerId"
            incomingCallLayout.visibility = View.VISIBLE
        }
    }
    
    override fun onCallAnswered(answer: SessionDescription) {
        runOnUiThread {
            callStatusText.text = "正在连接..."
            webRTCClient?.setRemoteDescription(answer)
            isCallActive = true
        }
    }
    
    override fun onCallRejected(reason: String) {
        runOnUiThread {
            showToast("通话被拒绝: $reason")
            ttsManager?.announceCallAction("对方拒绝了通话")
            finish()
        }
    }
    
    override fun onIceCandidate(candidate: IceCandidate) {
        webRTCClient?.addIceCandidate(candidate)
    }
    
    override fun onCallEnded() {
        runOnUiThread {
            showToast("通话已结束")
            ttsManager?.announceCallAction("通话已结束")
            cleanup()
            finish()
        }
    }
    
    override fun onUserOnline(userId: String) {
        // 可以更新UI显示用户在线状态
    }
    
    override fun onUserOffline(userId: String) {
        runOnUiThread {
            if (userId == targetUserId || userId == callerId) {
                showToast("对方已离线")
                callStatusText.text = "对方已离线"
            }
        }
    }
    
    // ==================== WebRTCClient.WebRTCObserver ====================
    
    override fun onLocalStream(stream: MediaStream) {
        // 本地流已经在createLocalMediaStream中处理
    }
    
    override fun onRemoteStream(stream: MediaStream) {
        runOnUiThread {
            stream.videoTracks.firstOrNull()?.addSink(remoteVideoView)
            callStatusText.text = "通话中"
            isCallActive = true
        }
    }
    
    override fun onLocalIceCandidate(candidate: IceCandidate) {
        // 本地生成的ICE候选，发送给对方
        val target = if (isIncoming) callerId else targetUserId
        target?.let { signalingClient?.sendIceCandidate(it, candidate) }
    }
    
    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
        runOnUiThread {
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED -> {
                    callStatusText.text = "通话中"
                    isCallActive = true
                }
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    callStatusText.text = "连接断开"
                }
                PeerConnection.IceConnectionState.FAILED -> {
                    callStatusText.text = "连接失败"
                    showToast("连接失败，请重试")
                }
                else -> {}
            }
        }
    }
    
    override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
        runOnUiThread {
            when (state) {
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    callStatusText.text = "通话中"
                }
                PeerConnection.PeerConnectionState.DISCONNECTED,
                PeerConnection.PeerConnectionState.FAILED -> {
                    if (isCallActive) {
                        showToast("通话已断开")
                        finish()
                    }
                }
                else -> {}
            }
        }
    }
    
    override fun onWebRTCError(error: String) {
        runOnUiThread {
            showToast("WebRTC错误: $error")
        }
    }
    
    // ==================== Permissions ====================
    
    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initWebRTC()
            } else {
                showToast("需要相机和麦克风权限才能进行视频通话")
                finish()
            }
        }
    }
    
    // ==================== Lifecycle ====================
    
    private fun cleanup() {
        webRTCClient?.close()
        signalingClient?.disconnect()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        
        localVideoView.release()
        remoteVideoView.release()
        eglBase.release()
        
        webRTCClient?.release()
        ttsManager?.shutdown()
    }
    
    override fun onBackPressed() {
        if (isCallActive) {
            endCall()
        } else {
            super.onBackPressed()
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

