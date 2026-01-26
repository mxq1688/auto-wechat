# 微信助手 (WeChat Assistant)

## 项目概述

微信助手是一个基于Android无障碍服务的智能助手应用，提供自动回复、视频通话等增强功能。

## 主要功能

### 1. 自动回复
- 基于关键词的智能回复
- 自定义回复规则
- 支持群聊和私聊
- 可配置启用/禁用

### 2. 视频通话
- 基于WebRTC的实时视频通话
- Firebase Firestore信令服务
- P2P连接，低延迟
- 支持前后摄像头切换

### 3. 消息监控
- 实时监控微信消息
- 消息内容提取
- 发送者识别
- 消息类型分类

## 技术架构

### 核心技术栈
- **语言**: Kotlin
- **最低SDK**: Android 7.0 (API 24)
- **目标SDK**: Android 13 (API 33)

### 主要依赖
- **无障碍服务**: Android Accessibility Service
- **视频通话**: WebRTC
- **信令服务**: Firebase Firestore
- **UI框架**: Material Design 3

## 项目结构

```
WeChatAssistant/
├── app/
│   ├── src/main/
│   │   ├── java/com/wechatassistant/
│   │   │   ├── accessibility/
│   │   │   ├── firebase/
│   │   │   ├── webrtc/
│   │   │   └── ui/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
├── build.gradle
└── settings.gradle
```

## 安装与配置

### 1. 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK 33
- Firebase项目

### 2. Firebase配置
1. 创建Firebase项目
2. 添加Android应用（包名：com.wechatassistant）
3. 下载google-services.json到app目录
4. 启用Firestore数据库

### 3. 构建项目
```bash
# 克隆项目
git clone [repository-url