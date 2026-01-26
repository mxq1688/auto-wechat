# WeChat Assistant ProGuard Rules

# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View

# Keep Accessibility Service
-keep class com.wechatassistant.accessibility.** { *; }
-keepclassmembers class com.wechatassistant.accessibility.** { *; }

# Keep WebRTC classes
-keep class org.webrtc.** { *; }
-keep class com.wechatassistant.webrtc.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.wechatassistant.firebase.** { *; }

# Keep data classes
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}

# Keep UI classes
-keep class com.wechatassistant.ui.** { *; }

# Keep R class
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlinx.coroutines.** { *; }

# Keep attributes
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions

# General Android
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}