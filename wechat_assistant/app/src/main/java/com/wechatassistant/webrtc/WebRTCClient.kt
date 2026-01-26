package com.wechatassistant.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCClient(private val context: Context, private val observer: PeerConnection.Observer) {
    
    companion object {
        private const val TAG = "WebRTCClient"
    }
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var localEglBase: EglBase? = null
    
    init {
        initPeerConnectionFactory()
    }
    
    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        
        localEglBase = EglBase.create()
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(localEglBase?.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(localEglBase?.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }
    
    fun initPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
    }
    
    fun createOffer(sdpObserver: SdpObserver) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createOffer(sdpObserver, constraints)
    }
    
    fun createAnswer(sdpObserver: SdpObserver) {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(sdpObserver, constraints)
    }
    
    fun setLocalDescription(sdp: SessionDescription, observer: SdpObserver) {
        peerConnection?.setLocalDescription(observer, sdp)
    }
    
    fun setRemoteDescription(sdp: SessionDescription, observer: SdpObserver) {
        peerConnection?.setRemoteDescription(observer, sdp)
    }
    
    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }
    
    fun close() {
        peerConnection?.close()
        peerConnection = null
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        localEglBase?.release()
        localEglBase = null
    }
}