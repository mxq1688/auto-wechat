import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart' as path;
import '../models/auto_reply_rule.dart';
import '../models/message.dart';

class AutoReplyManager {
  static final AutoReplyManager _instance = AutoReplyManager._internal();
  factory AutoReplyManager() => _instance;
  AutoReplyManager._internal();

  Database? _database;
  List<AutoReplyRule> _rules = [