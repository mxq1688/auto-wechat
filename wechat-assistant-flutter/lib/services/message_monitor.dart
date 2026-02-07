import 'dart:async';
import 'package:flutter/services.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart' as path;
import '../models/message.dart';

class MessageMonitor {
  static final MessageMonitor _instance = MessageMonitor._internal();
  factory MessageMonitor() => _instance;
  MessageMonitor._internal();

  static const platform = MethodChannel('com.wechatassistant/accessibility');
  
  Database? _database;
  final _messageController = StreamController<Message>.broadcast();
  final List<Message> _recentMessages = [