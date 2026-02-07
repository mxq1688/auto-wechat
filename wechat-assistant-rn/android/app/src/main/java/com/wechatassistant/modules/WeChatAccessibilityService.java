package com.wechatassistant.modules;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import java.util.List;

public class WeChatAccessibilityService extends AccessibilityService {
    private boolean isMonitoring = false;
    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isMonitoring) return;
        
        if (event.getPackageName() != null && 
            event.getPackageName().toString().equals(WECHAT_PACKAGE)) {
            
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                processMessage(event);
            }
        }
    }
    
    private void processMessage(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        
        List<AccessibilityNodeInfo> messageNodes = rootNode.findAccessibilityNodeInfosByViewId(
            "com.tencent.mm:id/b4a"
        );
        
        for (AccessibilityNodeInfo node : messageNodes) {
            if (node.getText() != null) {
                String message = node.getText().toString();
                sendMessageToReact(message);
            }
        }
    }
    
    private void sendMessageToReact(String message) {
        WritableMap params = Arguments.createMap();
        params.putString("content", message);
        params.putString("sender", "Unknown");
        params.putDouble("timestamp", System.currentTimeMillis());
        
        WeChatAccessibilityModule.sendEvent("onMessageReceived", params);
    }
    
    @Override
    public void onInterrupt() {
        isMonitoring = false;
    }
}