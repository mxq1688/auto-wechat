import 'package:json_annotation/json_annotation.dart';

part 'message.g.dart';

enum MessageType {
  text,
  image,
  voice,
  video,
  redPacket,
  transfer,
  location,
  link,
  file,
  system,
  unknown
}

enum ChatType {
  private,
  group
}

@JsonSerializable()
class Message {
  final String id;
  final String content;
  final String sender;
  final String? senderNickname;
  final String? receiver;
  final MessageType type;
  final ChatType chatType;
  final DateTime timestamp;
  final bool isOutgoing;
  final String? groupId;
  final String? groupName;
  final Map<String, dynamic>? extra;
  final bool isRead;
  final String? replyTo;

  Message({
    required this.id,
    required this.content,
    required this.sender,
    this.senderNickname,
    this.receiver,
    required this.type,
    required this.chatType,
    required this.timestamp,
    this.isOutgoing = false,
    this.groupId,
    this.groupName,
    this.extra,
    this.isRead = false,
    this.replyTo,
  });

  factory Message.fromJson(Map<String, dynamic> json) => _$MessageFromJson(json);
  Map<String, dynamic> toJson() => _$MessageToJson(this);

  Message copyWith({
    String? id,
    String? content,
    String? sender,
    String? senderNickname,
    String? receiver,
    MessageType? type,
    ChatType? chatType,
    DateTime? timestamp,
    bool? isOutgoing,
    String? groupId,
    String? groupName,
    Map<String, dynamic>? extra,
    bool? isRead,
    String? replyTo,
  }) {
    return Message(
      id: id ?? this.id,
      content: content ?? this.content,
      sender: sender ?? this.sender,
      senderNickname: senderNickname ?? this.senderNickname,
      receiver: receiver ?? this.receiver,
      type: type ?? this.type,
      chatType: chatType ?? this.chatType,
      timestamp: timestamp ?? this.timestamp,
      isOutgoing: isOutgoing ?? this.isOutgoing,
      groupId: groupId ?? this.groupId,
      groupName: groupName ?? this.groupName,
      extra: extra ?? this.extra,
      isRead: isRead ?? this.isRead,
      replyTo: replyTo ?? this.replyTo,
    );
  }

  @override
  String toString() {
    return 'Message(id: $id, sender: $sender, type: $type, chatType: $chatType)';
  }
}