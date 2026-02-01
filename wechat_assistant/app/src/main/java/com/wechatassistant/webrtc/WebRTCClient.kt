package com.wechatassistant.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

/**
 * WebRTC客户端
 * 提供完整的视频通话功能
 */
class WebRTCClient(
    private val context: Context,
    private val eglBase: EglBase,
    private val observer: WebRTCObserver
) {
    
    companion object {
        private const val TAG = "WebRTCClient"
        
        // ICE服务器配置
        private val ICE_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )
    }
    
    interface WebRTCObserver {
        fun onLocalStream(stream: MediaStream)
        fun onRemoteStream(stream: MediaStream)
        fun onLocalIceCandidate(candidate: IceCandidate)  // 本地生成的ICE候选，需要发送给对方
        fun onIceConnectionChange(state: PeerConnection.IceConnectionState)
        fun onConnectionChange(state: PeerConnection.PeerConnectionState)
        fun onWebRTCError(error: String)  // WebRTC错误
    }
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    
    private var localStream: MediaStream? = null
    private var isInitialized = false
    private var isFrontCamera = true
    
    init {
        initPeerConnectionFactory()
    }
    
    /**
     * 初始化PeerConnectionFactory
     */
    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        
        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext, true, true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
        
        isInitialized = true
        Log.d(TAG, "PeerConnectionFactory initialized")
    }
    
    /**
     * 创建本地媒体流
     */
    fun createLocalMediaStream(localView: SurfaceViewRenderer?) {
        if (!isInitialized) {
            Log.e(TAG, "PeerConnectionFactory not initialized")
            return
        }
        
        // 创建音频
        createAudioTrack()
        
        // 创建视频
        createVideoTrack(localView)
        
        // 创建媒体流
        localStream = peerConnectionFactory?.createLocalMediaStream("local_stream")
        localAudioTrack?.let { localStream?.addTrack(it) }
        localVideoTrack?.let { localStream?.addTrack(it) }
        
        localStream?.let { observer.onLocalStream(it) }
        Log.d(TAG, "Local media stream created")
    }
    
    /**
     * 创建音频轨道
     */
    private fun createAudioTrack() {
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        
        localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_track", localAudioSource)
        localAudioTrack?.setEnabled(true)
    }
    
    /**
     * 创建视频轨道
     */
    private fun createVideoTrack(localView: SurfaceViewRenderer?) {
        videoCapturer = createCameraCapturer()
        if (videoCapturer == null) {
            Log.e(TAG, "Failed to create camera capturer")
            observer.onWebRTCError("无法访问摄像头")
            return
        }
        
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        localVideoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)
        
        videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)
        
        localVideoTrack = peerConnectionFactory?.createVideoTrack("video_track", localVideoSource)
        localVideoTrack?.setEnabled(true)
        
        // 添加到本地预览
        localView?.let {
            it.setMirror(isFrontCamera)
            localVideoTrack?.addSink(it)
        }
    }
    
    /**
     * 创建摄像头捕获器
     */
    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        
        // 优先使用前置摄像头
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    isFrontCamera = true
                    return capturer
                }
            }
        }
        
        // 如果没有前置摄像头，使用后置摄像头
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    isFrontCamera = false
                    return capturer
                }
            }
        }
        
        return null
    }
    
    /**
     * 切换摄像头
     */
    fun switchCamera() {
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                isFrontCamera = isFront
                Log.d(TAG, "Camera switched to ${if (isFront) "front" else "back"}")
            }
            
            override fun onCameraSwitchError(error: String?) {
                Log.e(TAG, "Camera switch error: $error")
                observer.onWebRTCError("切换摄像头失败: $error")
            }
        })
    }
    
    /**
     * 创建PeerConnection
     */
    fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(ICE_SERVERS).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                    Log.d(TAG, "Signaling state: $state")
    }
    
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "ICE connection state: $state")
                    state?.let { observer.onIceConnectionChange(it) }
                }
                
                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    Log.d(TAG, "ICE receiving: $receiving")
                }
                
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    Log.d(TAG, "ICE gathering state: $state")
                }
                
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        Log.d(TAG, "ICE candidate: ${it.sdp}")
                        observer.onLocalIceCandidate(it)
                    }
                }
                
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                    Log.d(TAG, "ICE candidates removed")
                }
                
                override fun onAddStream(stream: MediaStream?) {
                    Log.d(TAG, "Remote stream added")
                    stream?.let { observer.onRemoteStream(it) }
                }
                
                override fun onRemoveStream(stream: MediaStream?) {
                    Log.d(TAG, "Remote stream removed")
                }
                
                override fun onDataChannel(channel: DataChannel?) {
                    Log.d(TAG, "Data channel created")
                }
                
                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "Renegotiation needed")
                }
                
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    Log.d(TAG, "Track added")
                    streams?.firstOrNull()?.let { observer.onRemoteStream(it) }
                }
                
                override fun onConnectionChange(state: PeerConnection.PeerConnectionState?) {
                    Log.d(TAG, "Connection state: $state")
                    state?.let { observer.onConnectionChange(it) }
                }
                
                override fun onTrack(transceiver: RtpTransceiver?) {
                    Log.d(TAG, "Track received")
                }
            }
        )
        
        // 添加本地流
        localStream?.audioTracks?.forEach { track ->
            peerConnection?.addTrack(track, listOf("local_stream"))
        }
        localStream?.videoTracks?.forEach { track ->
            peerConnection?.addTrack(track, listOf("local_stream"))
        }
        
        Log.d(TAG, "PeerConnection created")
    }
    
    /**
     * 创建Offer
     */
    fun createOffer(callback: (SessionDescription?) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.d(TAG, "Offer created")
                sdp?.let { setLocalDescription(it) { callback(sdp) } }
            }
            
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Create offer failed: $error")
                observer.onWebRTCError("创建Offer失败: $error")
                callback(null)
            }
            
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    /**
     * 创建Answer
     */
    fun createAnswer(callback: (SessionDescription?) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.d(TAG, "Answer created")
                sdp?.let { setLocalDescription(it) { callback(sdp) } }
            }
            
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Create answer failed: $error")
                observer.onWebRTCError("创建Answer失败: $error")
                callback(null)
            }
            
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    /**
     * 设置本地描述
     */
    private fun setLocalDescription(sdp: SessionDescription, onSuccess: () -> Unit) {
        peerConnection?.setLocalDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
            
            override fun onSetSuccess() {
                Log.d(TAG, "Local description set")
                onSuccess()
            }
            
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Set local description failed: $error")
                observer.onWebRTCError("设置本地描述失败: $error")
            }
        }, sdp)
    }
    
    /**
     * 设置远程描述
     */
    fun setRemoteDescription(sdp: SessionDescription, onSuccess: () -> Unit = {}) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
            
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set")
                onSuccess()
            }
            
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Set remote description failed: $error")
                observer.onWebRTCError("设置远程描述失败: $error")
            }
        }, sdp)
    }
    
    /**
     * 添加ICE候选者
     */
    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
        Log.d(TAG, "ICE candidate added")
    }
    
    /**
     * 设置音频是否静音
     */
    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        Log.d(TAG, "Audio ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 设置视频是否开启
     */
    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        Log.d(TAG, "Video ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 关闭连接
     */
    fun close() {
        Log.d(TAG, "Closing WebRTC client")
        
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capturer", e)
        }
        
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        localVideoSource?.dispose()
        localAudioSource?.dispose()
        surfaceTextureHelper?.dispose()
        
        peerConnection?.close()
        peerConnection?.dispose()
        
        localVideoTrack = null
        localAudioTrack = null
        localVideoSource = null
        localAudioSource = null
        videoCapturer = null
        surfaceTextureHelper = null
        peerConnection = null
        localStream = null
    }
    
    /**
     * 释放资源
     */
    fun release() {
        close()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        isInitialized = false
        Log.d(TAG, "WebRTC client released")
    }
}
