class Contact {
  final String id;
  final String name;
  final String? avatarUrl;
  final String? phoneNumber;
  final DateTime? lastCallTime;

  Contact({
    required this.id,
    required this.name,
    this.avatarUrl,
    this.phoneNumber,
    this.lastCallTime,
  });

  factory Contact.fromJson(Map<String, dynamic> json) {
    return Contact(
      id: json['id'] as String,
      name: json['name'] as String,
      avatarUrl: json['avatarUrl'] as String?,
      phoneNumber: json['phoneNumber'] as String?,
      lastCallTime: json['lastCallTime'] != null
          ? DateTime.parse(json['lastCallTime'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'avatarUrl': avatarUrl,
      'phoneNumber': phoneNumber,
      'lastCallTime': lastCallTime?.toIso8601String(),
    };
  }

  Contact copyWith({
    String? id,
    String? name,
    String? avatarUrl,
    String? phoneNumber,
    DateTime? lastCallTime,
  }) {
    return Contact(
      id: id ?? this.id,
      name: name ?? this.name,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      phoneNumber: phoneNumber ?? this.phoneNumber,
      lastCallTime: lastCallTime ?? this.lastCallTime,
    );
  }
}
