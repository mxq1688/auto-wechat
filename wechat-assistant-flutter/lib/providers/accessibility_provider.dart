import 'package:flutter/foundation.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:permission_handler/permission_handler.dart';

class AccessibilityProvider extends ChangeNotifier {
  bool _isEnabled = false;
  bool _autoAnswer = true;
  int _answerDelay = 3; // seconds
  final FlutterTts _tts = FlutterTts();
  
  bool get isEnabled => _isEnabled;
  bool get autoAnswer => _autoAnswer;
  int get answerDelay => _answerDelay;

  AccessibilityProvider() {
    _initTts();
  }

  void _initTts() async {
    await _tts.setLanguage('zh-CN');
    await _tts.setSpeechRate(0.5);
    await _tts.setVolume(1.0);
    await _tts.setPitch(1.0);
  }

  Future<bool> requestAccessibilityPermission() async {
    // Note: Android accessibility service requires manual setup in Settings
    // This is a placeholder for the actual implementation
    final status = await Permission.systemAlertWindow.request();
    return status.isGranted;
  }

  Future<void> toggleAccessibilityService() async {
    if (!_isEnabled) {
      final granted = await requestAccessibilityPermission();
      if (!granted) {
        return;
      }
    }
    
    _isEnabled = !_isEnabled;
    notifyListeners();
    
    if (_isEnabled) {
      await _speak('无障碍服务已启动');
    } else {
      await _speak('无障碍服务已关闭');
    }
  }

  void setAutoAnswer(bool value) {
    _autoAnswer = value;
    notifyListeners();
  }

  void setAnswerDelay(int seconds) {
    _answerDelay = seconds;
    notifyListeners();
  }

  Future<void> announceIncomingCall(String callerName) async {
    await _speak('收到来自 $callerName 的视频通话');
  }

  Future<void> _speak(String text) async {
    await _tts.speak(text);
  }

  // Simulated method to detect WeChat video call
  // In real implementation, this would use AccessibilityService
  Future<void> detectWeChatCall(String callerName) async {
    await announceIncomingCall(callerName);
    
    if (_autoAnswer) {
      await Future.delayed(Duration(seconds: _answerDelay));
      // Auto-answer logic would go here
      await _speak('自动接听');
    }
  }

  @override
  void dispose() {
    _tts.stop();
    super.dispose();
  }
}
