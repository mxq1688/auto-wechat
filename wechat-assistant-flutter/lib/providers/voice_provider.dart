import 'package:flutter/foundation.dart';
import 'package:speech_to_text/speech_to_text.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:permission_handler/permission_handler.dart';

class VoiceProvider extends ChangeNotifier {
  final SpeechToText _speech = SpeechToText();
  final FlutterTts _tts = FlutterTts();
  
  bool _isListening = false;
  bool _isInitialized = false;
  String _lastWords = '';
  double _confidence = 0.0;
  
  bool get isListening => _isListening;
  bool get isInitialized => _isInitialized;
  String get lastWords => _lastWords;
  double get confidence => _confidence;

  VoiceProvider() {
    _initialize();
  }

  Future<void> _initialize() async {
    try {
      _isInitialized = await _speech.initialize(
        onStatus: (status) {
          debugPrint('Speech status: $status');
          if (status == 'done' || status == 'notListening') {
            _isListening = false;
            notifyListeners();
          }
        },
        onError: (error) {
          debugPrint('Speech error: $error');
          _isListening = false;
          notifyListeners();
        },
      );

      // Initialize TTS
      await _tts.setLanguage('zh-CN');
      await _tts.setSpeechRate(0.5);
      await _tts.setVolume(1.0);
      await _tts.setPitch(1.0);
      
      notifyListeners();
    } catch (e) {
      debugPrint('Error initializing speech: $e');
    }
  }

  Future<bool> requestMicrophonePermission() async {
    final status = await Permission.microphone.request();
    return status.isGranted;
  }

  Future<void> startListening() async {
    if (!_isInitialized) {
      await _initialize();
    }

    if (!_isInitialized) {
      await speak('语音识别初始化失败');
      return;
    }

    final hasPermission = await requestMicrophonePermission();
    if (!hasPermission) {
      await speak('需要麦克风权限');
      return;
    }

    _isListening = true;
    _lastWords = '';
    notifyListeners();

    await _speech.listen(
      onResult: (result) {
        _lastWords = result.recognizedWords;
        _confidence = result.confidence;
        notifyListeners();
        
        if (result.finalResult) {
          _processVoiceCommand(_lastWords);
        }
      },
      localeId: 'zh_CN',
      listenMode: ListenMode.confirmation,
    );
  }

  Future<void> stopListening() async {
    await _speech.stop();
    _isListening = false;
    notifyListeners();
  }

  void _processVoiceCommand(String command) {
    debugPrint('Processing command: $command');
    
    // Remove spaces and convert to lowercase for matching
    final normalized = command.replaceAll(' ', '').toLowerCase();
    
    if (normalized.contains('接听') || normalized.contains('接电话')) {
      _onAnswerCallCommand();
    } else if (normalized.contains('打电话') || normalized.contains('视频通话')) {
      final name = _extractContactName(command);
      if (name != null) {
        _onMakeCallCommand(name);
      } else {
        speak('请说出联系人姓名');
      }
    } else if (normalized.contains('挂断') || normalized.contains('结束通话')) {
      _onEndCallCommand();
    } else if (normalized.contains('打开微信')) {
      _onOpenWeChatCommand();
    } else {
      speak('未识别的命令');
    }
  }

  String? _extractContactName(String command) {
    // Extract contact name from commands like "打电话给张三" or "给李四打视频"
    final patterns = [
      RegExp(r'给(.+?)打'),
      RegExp(r'打电话给(.+)'),
      RegExp(r'视频通话(.+)'),
    ];
    
    for (final pattern in patterns) {
      final match = pattern.firstMatch(command);
      if (match != null && match.groupCount > 0) {
        return match.group(1)?.trim();
      }
    }
    
    return null;
  }

  void _onAnswerCallCommand() {
    debugPrint('Voice command: Answer call');
    speak('正在接听');
    // This would trigger the actual answer call action
    // Implementation would depend on the accessibility provider
  }

  void _onMakeCallCommand(String contactName) {
    debugPrint('Voice command: Call $contactName');
    speak('正在呼叫$contactName');
    // This would trigger the make call action
    // Implementation would search contacts and initiate call
  }

  void _onEndCallCommand() {
    debugPrint('Voice command: End call');
    speak('正在挂断');
    // This would trigger the end call action
  }

  void _onOpenWeChatCommand() {
    debugPrint('Voice command: Open WeChat');
    speak('正在打开微信');
    // This would trigger opening WeChat
  }

  Future<void> speak(String text) async {
    await _tts.speak(text);
  }

  @override
  void dispose() {
    _speech.stop();
    _tts.stop();
    super.dispose();
  }
}
