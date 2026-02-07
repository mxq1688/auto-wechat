package com.wechatassistant.modules;

import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class WeChatAccessibilityModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext;

    public WeChatAccessibilityModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
    }

    @Override
    public String getName() {
        return "WeChatAccessibilityModule";
    }

    @ReactMethod
    public void isAccessibilityEnabled(Promise promise) {
        try {
            String service = Settings.Secure.getString(
                reactContext.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            boolean enabled = !TextUtils.isEmpty(service) && 
                service.contains("com.wechatassistant/.WeChatAccessibilityService");
            promise.resolve(enabled);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        reactContext.startActivity(intent);
    }

    @ReactMethod
    public void startMonitoring(Promise promise) {
        try {
            Intent intent = new Intent(reactContext, WeChatAccessibilityService.class);
            intent.setAction("START_MONITORING");
            reactContext.startService(intent);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void stopMonitoring(Promise promise) {
        try {
            Intent intent = new Intent(reactContext, WeChatAccessibilityService.class);
            intent.setAction("STOP_MONITORING");
            reactContext.startService(intent);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void sendAutoReply(String contactId, String message, Promise promise) {
        try {
            Intent intent = new Intent(reactContext, WeChatAccessibilityService.class);
            intent.setAction("SEND_MESSAGE");
            intent.putExtra("contact_id", contactId);
            intent.putExtra("message", message);
            reactContext.startService(intent);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }