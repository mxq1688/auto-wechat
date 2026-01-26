import 'package:flutter/foundation.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';
import 'dart:async';
import '../models/contact.dart';
import '../services/signaling_service.dart';

enum CallState {
  idle,
  connecting,
  ringing,
  connected,
  ended,
}

class CallProvider extends ChangeNotifier {
  CallState _callState = CallState.idle;
  Contact? _currentContact;
  RTCVideoRenderer? _localRenderer;
  RTCVideoRenderer? _remoteRenderer;
  RTCPeerConnection? _peerConnection;
  MediaStream? _localStream;
  final SignalingService _signalingService = SignalingService();
  
  CallState get callState => _callState;
  Contact? get currentContact => _currentContact;
  RTCVideoRenderer? get localRenderer => _localRenderer;
  RTCVideoRenderer? get remoteRenderer => _remoteRenderer;

  Future<void> initializeRenderers() async {
    _localRenderer = RTCVideoRenderer();
    _remoteRenderer = RTCVideoRenderer();
    await _localRenderer!.initialize();
    await _remoteRenderer!.initialize();
  }

  Future<void> startCall(Contact contact) async {
    _currentContact = contact;
    _callState = CallState.connecting;
    notifyListeners();

    try {
      // Get user media
      _localStream = await navigator.mediaDevices.getUserMedia({
        'audio': true,
        'video': {
          'facingMode': 'user',
        }
      });

      if (_localRenderer != null) {
        _localRenderer!.srcObject = _localStream;
      }

      // Create peer connection
      await _createPeerConnection();

      // Add local stream to peer connection
      _localStream!.getTracks().forEach((track) {
        _peerConnection!.addTrack(track, _localStream!);
      });

      // Create and send offer
      RTCSessionDescription offer = await _peerConnection!.createOffer();
      await _peerConnection!.setLocalDescription(offer);
      
      // Send offer through signaling service
      await _signalingService.sendOffer(contact.id, offer.toMap());

      _callState = CallState.ringing;
      notifyListeners();
    } catch (e) {
      debugPrint('Error starting call: $e');
      await endCall();
    }
  }

  Future<void> answerCall(Contact contact, Map<String, dynamic> offer) async {
    _currentContact = contact;
    _callState = CallState.connecting;
    notifyListeners();

    try {
      // Get user media
      _localStream = await navigator.mediaDevices.getUserMedia({
        'audio': true,
        'video': {
          'facingMode': 'user',
        }
      });

      if (_localRenderer != null) {
        _localRenderer!.srcObject = _localStream;
      }

      // Create peer connection
      await _createPeerConnection();

      // Add local stream
      _localStream!.getTracks().forEach((track) {
        _peerConnection!.addTrack(track, _localStream!);
      });

      // Set remote description
      await _peerConnection!.setRemoteDescription(
        RTCSessionDescription(offer['sdp'], offer['type'])
      );

      // Create and send answer
      RTCSessionDescription answer = await _peerConnection!.createAnswer();
      await _peerConnection!.setLocalDescription(answer);
      
      await _signalingService.sendAnswer(contact.id, answer.toMap());

      _callState = CallState.connected;
      notifyListeners();
    } catch (e) {
      debugPrint('Error answering call: $e');
      await endCall();
    }
  }

  Future<void> _createPeerConnection() async {
    Map<String, dynamic> configuration = {
      'iceServers': [
        {'urls': 'stun:stun.l.google.com:19302'},
        {'urls': 'stun:stun1.l.google.com:19302'},
      ]
    };

    _peerConnection = await createPeerConnection(configuration);

    _peerConnection!.onIceCandidate = (RTCIceCandidate candidate) {
      if (_currentContact != null) {
        _signalingService.sendIceCandidate(_currentContact!.id, candidate.toMap());
      }
    };

    _peerConnection!.onTrack = (RTCTrackEvent event) {
      if (event.streams.isNotEmpty && _remoteRenderer != null) {
        _remoteRenderer!.srcObject = event.streams[0];
        _callState = CallState.connected;
        notifyListeners();
      }
    };

    _peerConnection!.onConnectionState = (RTCPeerConnectionState state) {
      debugPrint('Connection state: $state');
      if (state == RTCPeerConnectionState.RTCPeerConnectionStateDisconnected ||
          state == RTCPeerConnectionState.RTCPeerConnectionStateFailed) {
        endCall();
      }
    };
  }

  Future<void> endCall() async {
    await _localStream?.dispose();
    await _peerConnection?.close();
    
    _localStream = null;
    _peerConnection = null;
    _currentContact = null;
    _callState = CallState.ended;
    
    notifyListeners();
    
    await Future.delayed(const Duration(seconds: 1));
    _callState = CallState.idle;
    notifyListeners();
  }

  Future<void> toggleCamera() async {
    if (_localStream != null) {
      final videoTrack = _localStream!.getVideoTracks().first;
      await Helper.switchCamera(videoTrack);
      notifyListeners();
    }
  }

  Future<void> toggleMute() async {
    if (_localStream != null) {
      final audioTrack = _localStream!.getAudioTracks().first;
      audioTrack.enabled = !audioTrack.enabled;
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _localRenderer?.dispose();
    _remoteRenderer?.dispose();
    _localStream?.dispose();
    _peerConnection?.close();
    super.dispose();
  }
}
