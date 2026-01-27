# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# 保留 WebRTC 相关类
-keep class org.webrtc.** { *; }
-keep class org.webrtc.voiceengine.** { *; }
-dontwarn org.webrtc.**

# 保留 OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# 保留应用的数据类
-keep class com.wechatassistant.manager.AutoReplyManager$ReplyRule { *; }
-keep class com.wechatassistant.manager.MessageMonitor$WeChatMessage { *; }
-keep class com.wechatassistant.voice.VoiceCommandProcessor$Command { *; }

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Kotlin 相关
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# 保留 JSON 解析相关
-keepclassmembers class * {
    @org.json.* <fields>;
}

# 保留无障碍服务
-keep class * extends android.accessibilityservice.AccessibilityService

# 通用规则
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留 Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# 保留 Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
