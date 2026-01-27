# 微信助手 (WeChat Assistant)

## 项目概述

微信助手是一个基于Android无障碍服务的智能助手应用，提供自动回复、消息监控、视频通话等增强功能。

## 主要功能

### 1. 自动回复 ✅
- 基于关键词的智能回复
- 自定义回复规则（支持添加、删除、编辑）
- 支持精确匹配、包含匹配、正则匹配
- 支持群聊和私聊单独配置
- 白名单/黑名单联系人过滤
- 可配置回复延迟

### 2. 消息监控 ✅
- 实时监控微信消息
- 消息内容提取
- 发送者识别
- 消息类型分类（文本、图片、语音、视频、红包等）
- 消息历史记录

### 3. 视频通话 ✅
- 基于WebRTC的实时视频通话
- WebSocket信令服务（附带Node.js服务器）
- P2P连接，低延迟
- 支持前后摄像头切换
- 音频/视频开关控制
- 自动接听来电（可配置）

### 4. 悬浮球 ✅
- 可拖动悬浮球
- 快捷功能菜单
- 实时状态显示
- 消息计数角标

### 5. 语音功能 ✅
- TTS语音播报（新消息、来电提醒）
- 语音识别（语音指令）
- 语音指令处理

## 技术架构

### 核心技术栈
- **语言**: Kotlin
- **最低SDK**: Android 7.0 (API 24)
- **目标SDK**: Android 14 (API 34)

### 主要依赖
- **无障碍服务**: Android Accessibility Service
- **视频通话**: WebRTC
- **信令服务**: WebSocket (OkHttp)
- **UI框架**: Material Design 3

## 项目结构

```
wechat_assistant/
├── app/
│   ├── src/main/
│   │   ├── java/com/wechatassistant/
│   │   │   ├── MainActivity.kt              # 主界面
│   │   │   ├── manager/
│   │   │   │   ├── AutoReplyManager.kt      # 自动回复管理
│   │   │   │   ├── MessageMonitor.kt        # 消息监控
│   │   │   │   └── SettingsManager.kt       # 设置管理
│   │   │   ├── service/
│   │   │   │   ├── EnhancedWeChatAccessibilityService.kt  # 增强无障碍服务
│   │   │   │   ├── FloatingBallService.kt   # 悬浮球服务
│   │   │   │   ├── TTSManager.kt            # TTS管理
│   │   │   │   └── VoiceRecognitionService.kt  # 语音识别
│   │   │   ├── ui/
│   │   │   │   └── VideoCallActivity.kt     # 视频通话界面
│   │   │   ├── voice/
│   │   │   │   └── VoiceCommandProcessor.kt # 语音指令处理
│   │   │   └── webrtc/
│   │   │       ├── SignalingClient.kt       # 信令客户端
│   │   │       └── WebRTCClient.kt          # WebRTC客户端
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── signaling-server/                         # 信令服务器
│   ├── server.js
│   └── package.json
├── keystore.properties.example               # 签名配置示例
├── build.gradle
└── settings.gradle
```

## 安装与配置

### 1. 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 11 或更高版本
- Android SDK 34
- Node.js 18+ (用于信令服务器)

### 2. 构建项目

```bash
# 克隆项目
git clone [repository-url]

# 进入项目目录
cd wechat_assistant

# 配置签名（可选，仅Release版本需要）
cp keystore.properties.example keystore.properties
# 编辑 keystore.properties 填写你的签名信息

# 构建Debug版本
./gradlew assembleDebug

# APK输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

### 3. 启动信令服务器（视频通话功能需要）

```bash
# 进入信令服务器目录
cd signaling-server

# 安装依赖
npm install

# 启动服务器
npm start

# 服务器默认运行在 ws://localhost:8080
```

### 4. 配置应用

1. 安装APK到Android设备
2. 打开应用，点击「启用辅助功能服务」
3. 在系统设置中找到「微信助手服务」并启用
4. 授予悬浮窗权限
5. 配置信令服务器地址（视频通话功能需要）
6. 根据需要开启各项功能

## 功能使用

### 自动回复
1. 开启「自动回复」开关
2. 点击「管理回复规则」添加或编辑规则
3. 可选择是否在群聊中也自动回复

### 视频通话
1. 配置信令服务器地址和用户ID
2. 确保信令服务器正在运行
3. 点击「测试视频通话」输入对方用户ID发起通话

### 悬浮球
1. 启动悬浮球后会显示在屏幕上
2. 点击悬浮球打开快捷菜单
3. 可以快速切换各项功能的开关

## 权限说明

- **无障碍服务**: 用于读取和操作微信界面
- **悬浮窗**: 用于显示悬浮球
- **摄像头/麦克风**: 用于视频通话
- **网络**: 用于WebRTC连接
- **通知**: 用于前台服务和消息提醒

## 注意事项

1. 无障碍服务需要用户手动在系统设置中启用
2. 微信版本更新可能导致部分功能失效（需要更新View ID）
3. 视频通话需要双方都连接到同一个信令服务器
4. 请勿将此应用用于骚扰他人或其他非法用途

## 开发说明

### 更新微信View ID
如果微信更新导致功能失效，需要更新 `MessageMonitor.kt` 中的View ID常量：

```kotlin
const val WECHAT_MESSAGE_LIST_ID = "com.tencent.mm:id/xxx"
const val WECHAT_MESSAGE_TEXT_ID = "com.tencent.mm:id/xxx"
// ...
```

可以使用 Android SDK 的 `uiautomatorviewer` 工具或无障碍服务日志来获取新的View ID。

## 许可证

MIT License
