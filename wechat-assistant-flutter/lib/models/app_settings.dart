import 'package:json_annotation/json_annotation.dart';

part 'app_settings.g.dart';

@JsonSerializable()
class AppSettings {
  // 自动回复设置
  final bool autoReplyEnabled;
  final bool replyInGroups;
  final bool replyInPrivate;
  final int defaultReplyDelay; // 默认回复延迟（秒）
  
  // 消息监控设置
  final bool messageMonitorEnabled;
  final bool saveMessageHistory;
  final int maxHistoryDays; // 消息历史保存天数
  final bool monitorGroups;
  final bool monitorPrivate;
  
  // 悬浮球设置
  final bool floatingBallEnabled;
  final double floatingBallOpacity;
  final int floatingBallSize;
  final bool showMessageCount;
  final String floatingBallPosition; // left, right
  
  // 语音设置
  final bool ttsEnabled;
  final bool voiceCommandEnabled;
  final String ttsLanguage;
  final double ttsSpeechRate;
  final double ttsPitch;
  final bool announceIncomingMessages;
  final bool announceVideoCall;
  
  // 视频通话设置
  final bool autoAnswerVideo;
  final int autoAnswerDelay; // 自动接听延迟（秒）
  final String signalingServerUrl;
  final String userId;
  final bool useFrontCamera;
  final bool enableVideoByDefault;
  final bool enableAudioByDefault;
  
  // 通知设置
  final bool notificationsEnabled;
  final bool soundEnabled;
  final bool vibrationEnabled;
  final String notificationSound;
  
  // 隐私设置
  final List<String> globalWhitelist;
  final List<String> globalBlacklist;
  final bool hideMessageContent; // 通知中隐藏消息内容
  
  // 系统设置
  final String theme; // light, dark, system
  final String language; // zh, en
  final bool keepScreenOn;
  final bool runInBackground;
  final bool startOnBoot;
  
  // 高级设置
  final bool debugMode;
  final bool collectCrashReports;
  final String backupPath;
  final DateTime? lastBackupTime;
  final Map<String, dynamic>? extra;

  AppSettings({
    this.autoReplyEnabled = false,
    this.replyInGroups = false,
    this.replyInPrivate = true,
    this.defaultReplyDelay = 1,
    this.messageMonitorEnabled = false,
    this.saveMessageHistory = true,
    this.maxHistoryDays = 30,
    this.monitorGroups = true,
    this.monitorPrivate = true,
    this.floatingBallEnabled = false,
    this.floatingBallOpacity = 0.8,
    this.floatingBallSize = 60,
    this.showMessageCount = true,
    this.floatingBallPosition = 'right',
    this.ttsEnabled = false,
    this.voiceCommandEnabled = false,
    this.ttsLanguage = 'zh-CN',
    this.ttsSpeechRate = 1.0,
    this.ttsPitch = 1.0,
    this.announceIncomingMessages = false