import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class FloatingBallService {
  static final FloatingBallService _instance = FloatingBallService._internal();
  factory FloatingBallService() => _instance;
  FloatingBallService._internal();

  static const platform = MethodChannel('com.wechatassistant/floating_ball');
  bool _isShowing = false;
  int _messageCount = 0;

  bool get isShowing => _isShowing;
  int get messageCount => _messageCount;

  Future<void> initialize() async {
    try {
      await platform.invokeMethod('initialize');
    } catch (e) {
      debugPrint('Failed to initialize floating ball: $e');
    }
  }

  Future<void> show() async {
    try {
      await platform.invokeMethod('show');
      _isShowing = true;
    } catch (e) {
      debugPrint('Failed to show floating ball: $e');
    }
  }

  Future<void> hide() async {
    try {
      await platform.invokeMethod('hide');
      _isShowing = false;
    } catch (e) {
      debugPrint('Failed to hide floating ball: $e');
    }
  }

  void updateMessageCount(int count) {
    _messageCount = count;
    platform.invokeMethod('updateCount', {'count': count});
  }

  void resetMessageCount() {
    updateMessageCount(0);
  }
}