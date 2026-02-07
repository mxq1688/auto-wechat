import 'package:json_annotation/json_annotation.dart';

part 'auto_reply_rule.g.dart';

enum MatchType {
  exact,     // 精确匹配
  contains,  // 包含匹配
  regex,     // 正则匹配
  startsWith,// 开头匹配
  endsWith   // 结尾匹配
}

enum RuleScope {
  all,       // 所有聊天
  private,   // 仅私聊
  group,     // 仅群聊
  specific   // 特定联系人/群
}

@JsonSerializable()
class AutoReplyRule {
  final String id;
  final String name;
  final String keyword;
  final String reply;
  final MatchType matchType;
  final RuleScope scope;
  final bool enabled;
  final int priority;
  final List<String>? whitelist;
  final List<String>? blacklist;
  final int? delaySeconds;
  final DateTime? startTime;
  final DateTime? endTime;
  final int? maxRepliesPerDay;
  final Map<String, int>? replyCount;
  final DateTime createdAt;
  final DateTime updatedAt;
  final Map<String, dynamic>? extra;

  AutoReplyRule({
    required this.id,
    required this.name,
    required this.keyword,
    required this.reply,
    this.matchType = MatchType.contains,
    this.scope = RuleScope.all,
    this.enabled = true,
    this.priority = 100,
    this.whitelist,
    this.blacklist,
    this.delaySeconds,
    this.startTime,
    this.endTime,
    this.maxRepliesPerDay,
    this.replyCount,
    required this.createdAt,
    required this.updatedAt,
    this.extra,
  });

  factory AutoReplyRule.fromJson(Map<String, dynamic> json) => 
      _$AutoReplyRuleFromJson(json);
  Map<String, dynamic> toJson() => _$AutoReplyRuleToJson(this);

  bool appliesTo(String message, String? senderId, bool isGroup) {
    if (!enabled) return false;

    if (scope == RuleScope.private && isGroup) return false;
    if (scope == RuleScope.group && !isGroup) return false;
    
    if (senderId != null) {
      if (blacklist?.contains(senderId) ?? false) return false;
      if (scope == RuleScope.specific && 
          !(whitelist?.contains(senderId) ?? false)) return false;
    }

    final now = DateTime.now();
    if (startTime != null && now.isBefore(startTime!)) return false;
    if (endTime != null && now.isAfter(endTime!)) return false;

    if (maxRepliesPerDay != null) {
      final todayKey = '${now.year}-${now.month}-${now.day}';
      final todayCount = replyCount?[todayKey