import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import '../models/app_settings.dart';

class SettingsProvider extends ChangeNotifier {
  AppSettings _settings = AppSettings();
  late SharedPreferences _prefs;
  bool _isInitialized = false;

  AppSettings get settings => _settings;
  bool get isInitialized => _isInitialized;

  // Auto reply settings
  bool get autoReplyEnabled => _settings.autoReplyEnabled;
  bool get replyInGroups => _settings.replyInGroups;
  bool get replyInPrivate => _settings.replyInPrivate;
  int get defaultReplyDelay => _settings.defaultReplyDelay;

  // Message monitor settings
  bool get messageMonitorEnabled => _settings.messageMonitorEnabled;
  bool get saveMessageHistory => _settings.saveMessageHistory;
  int get maxHistoryDays => _settings.maxHistoryDays;

  // Floating ball settings
  bool get floatingBallEnabled => _settings.floatingBallEnabled;
  double get floatingBallOpacity => _settings.floatingBallOpacity;
  int get floatingBallSize => _settings.floatingBallSize;

  // Voice settings
  bool get ttsEnabled => _settings.ttsEnabled;
  bool get voiceCommandEnabled => _settings.voiceCommandEnabled;
  String get ttsLanguage => _settings.ttsLanguage;

  // Video call settings
  bool get autoAnswerVideo => _settings.autoAnswerVideo;
  int get autoAnswerDelay => _settings.autoAnswerDelay;
  String get signalingServerUrl => _settings.signalingServerUrl;

  // Theme
  String get theme => _settings.theme;
  
  Future<void> initialize() async {
    if (_isInitialized) return;
    
    _prefs = await SharedPreferences.getInstance();
    await loadSettings();
    _isInitialized = true;