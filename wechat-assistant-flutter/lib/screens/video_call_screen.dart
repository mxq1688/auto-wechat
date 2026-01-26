import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';
import '../models/contact.dart';
import '../providers/call_provider.dart';

class VideoCallScreen extends StatefulWidget {
  final Contact contact;

  const VideoCallScreen({super.key, required this.contact});

  @override
  State<VideoCallScreen> createState() => _VideoCallScreenState();
}

class _VideoCallScreenState extends State<VideoCallScreen> {
  bool _isMuted = false;
  bool _isLocalCameraOff = false;

  @override
  void initState() {
    super.initState();
    _startCall();
  }

  void _startCall() async {
    final callProvider = context.read<CallProvider>();
    await callProvider.startCall(widget.contact);
  }

  @override
  Widget build(BuildContext context) {
    final callProvider = context.watch<CallProvider>();

    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Stack(
          children: [
            // Remote Video (Full Screen)
            if (callProvider.remoteRenderer != null)
              Positioned.fill(
                child: RTCVideoView(
                  callProvider.remoteRenderer!,
                  objectFit: RTCVideoViewObjectFit.RTCVideoViewObjectFitCover,
                ),
              ),

            // Connection Status Overlay
            if (callProvider.callState != CallState.connected)
              Positioned.fill(
                child: Container(
                  color: Colors.black87,
                  child: Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        CircleAvatar(
                          radius: 60,
                          backgroundColor: Colors.primaries[
                              widget.contact.name.hashCode %
                                  Colors.primaries.length
                          ],
                          child: Text(
                            widget.contact.name[0],
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 48,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        const SizedBox(height: 24),
                        Text(
                          widget.contact.name,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 28,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          _getCallStateText(callProvider.callState),
                          style: TextStyle(
                            color: Colors.grey[400],
                            fontSize: 16,
                          ),
                        ),
                        if (callProvider.callState == CallState.connecting)
                          const Padding(
                            padding: EdgeInsets.only(top: 24),
                            child: CircularProgressIndicator(
                              color: Colors.white,
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              ),

            // Local Video (Picture-in-Picture)
            if (callProvider.localRenderer != null && !_isLocalCameraOff)
              Positioned(
                top: 16,
                right: 16,
                child: Container(
                  width: 120,
                  height: 160,
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.white, width: 2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  clipBehavior: Clip.hardEdge,
                  child: RTCVideoView(
                    callProvider.localRenderer!,
                    objectFit: RTCVideoViewObjectFit.RTCVideoViewObjectFitCover,
                    mirror: true,
                  ),
                ),
              ),

            // Top Bar
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.black.withOpacity(0.6),
                      Colors.transparent,
                    ],
                  ),
                ),
                child: Row(
                  children: [
                    IconButton(
                      icon: const Icon(Icons.arrow_back, color: Colors.white),
                      onPressed: () => Navigator.pop(context),
                    ),
                    Expanded(
                      child: Text(
                        widget.contact.name,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 20,
                          fontWeight: FontWeight.w500,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                    const SizedBox(width: 48), // Balance the back button
                  ],
                ),
              ),
            ),

            // Bottom Control Bar
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [
                      Colors.black.withOpacity(0.8),
                      Colors.transparent,
                    ],
                  ),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    _ControlButton(
                      icon: _isMuted ? Icons.mic_off : Icons.mic,
                      label: _isMuted ? '取消静音' : '静音',
                      onPressed: () {
                        setState(() {
                          _isMuted = !_isMuted;
                        });
                        callProvider.toggleMute();
                      },
                    ),
                    _ControlButton(
                      icon: Icons.call_end,
                      label: '挂断',
                      backgroundColor: Colors.red,
                      onPressed: () async {
                        await callProvider.endCall();
                        if (mounted) {
                          Navigator.pop(context);
                        }
                      },
                    ),
                    _ControlButton(
                      icon: Icons.cameraswitch,
                      label: '切换',
                      onPressed: () {
                        callProvider.toggleCamera();
                      },
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _getCallStateText(CallState state) {
    switch (state) {
      case CallState.idle:
        return '准备中...';
      case CallState.connecting:
        return '正在连接...';
      case CallState.ringing:
        return '等待接听...';
      case CallState.connected:
        return '通话中';
      case CallState.ended:
        return '通话已结束';
    }
  }

  @override
  void dispose() {
    final callProvider = context.read<CallProvider>();
    callProvider.endCall();
    super.dispose();
  }
}

class _ControlButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onPressed;
  final Color? backgroundColor;

  const _ControlButton({
    required this.icon,
    required this.label,
    required this.onPressed,
    this.backgroundColor,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 64,
          height: 64,
          decoration: BoxDecoration(
            color: backgroundColor ?? Colors.white.withOpacity(0.2),
            shape: BoxShape.circle,
          ),
          child: IconButton(
            icon: Icon(icon),
            color: Colors.white,
            iconSize: 32,
            onPressed: onPressed,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          label,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 12,
          ),
        ),
      ],
    );
  }
}
