# 微信助手 - Flutter版本

一个用于自动接听微信视频电话并支持语音控制的Android应用（Flutter实现）。

## 功能特性

### 1. 微信视频辅助
- ✅ 自动检测微信视频来电
- ✅ 语音播报来电人信息
- ✅ 自动接听（可设置延迟时间）
- ✅ 悬浮球快捷操作

### 2. 独立视频通话
- ✅ P2P视频通话功能
- ✅ WebRTC技术实现
- ✅ 联系人管理
- ✅ 摄像头切换
- ✅ 静音控制

### 3. 语音控制
- ✅ 语音识别命令
- ✅ 支持命令：
  - "接听电话" - 自动接听来电
  - "打电话给[姓名]" - 拨打视频电话
  - "挂断电话" - 结束通话
  - "打开微信" - 启动微信

## 技术栈

- **Flutter** 3.0+
- **Dart** 3.0+
- **Provider** - 状态管理
- **flutter_webrtc** - 视频通话
- **speech_to_text** - 语音识别
- **flutter_tts** - 文字转语音
- **WebSocket** - 信令服务

## 环境要求

- Flutter SDK 3.0 或更高版本
- Android SDK 21+ (Android 5.0+)
- Android Studio 或 VS Code

## 安装步骤

### 1. 安装依赖

```bash
cd wechat-assistant-flutter
flutter pub get
```

### 2. 配置信令服务器

编辑 `lib/services/signaling_service.dart`，修改WebSocket URL：

```dart
const wsUrl = 'wss://your-signaling-server.com';
```

**免费信令服务器选项：**
- 部署Socket.io到Vercel/Railway/Render
- 使用Firebase Realtime Database
- 使用PeerJS Cloud服务

### 3. 运行应用

```bash
flutter run
```

## 权限配置

应用需要以下权限：

### Android权限
- ✅ 摄像头权限 - 视频通话
- ✅ 麦克风权限 - 语音识别和通话
- ✅ 悬浮窗权限 - 快捷操作
- ✅ 无障碍服务 - 检测微信来电
- ✅ 网络权限 - 视频通话

### 设置无障碍服务

1. 打开**系统设置**
2. 进入**无障碍** > **已安装的服务**
3. 找到**微信助手**并启用
4. 授予必要权限

## 使用说明

### 启动服务

1. 打开应用
2. 点击"启动服务"按钮
3. 按提示授予必要权限

### 语音控制

1. 点击主页面的麦克风按钮
2. 说出语音命令
3. 等待系统执行操作

### 视频通话

1. 进入"联系人"标签
2. 选择联系人
3. 点击视频通话图标

## 项目结构

```
lib/
├── main.dart                 # 应用入口
├── models/                   # 数据模型
│   └── contact.dart         # 联系人模型
├── providers/               # 状态管理
│   ├── accessibility_provider.dart  # 无障碍服务
│   ├── call_provider.dart          # 通话管理
│   └── voice_provider.dart         # 语音控制
├── screens/                 # 页面
│   ├── home_screen.dart            # 主页
│   ├── contacts_screen.dart        # 联系人
│   ├── video_call_screen.dart      # 视频通话
│   └── settings_screen.dart        # 设置
└── services/                # 服务
    └── signaling_service.dart      # 信令服务
```

## 技术限制说明

### 微信无障碍控制

⚠️ **重要说明：**

由于Android系统和微信的安全限制，无障碍服务只能实现**有限的自动化功能**：

- ✅ 可以检测微信界面变化
- ✅ 可以播报来电信息
- ✅ 可以模拟点击接听按钮
- ❌ 无法保证100%自动接听成功
- ❌ 微信更新可能导致功能失效

### 信令服务器

本应用的视频通话功能需要信令服务器。代码中使用占位URL，您需要：

1. 部署自己的信令服务器，或
2. 使用Firebase Realtime Database，或
3. 使用免费的PeerJS Cloud服务

## 故障排除

### 无障碍服务无法启动

- 检查是否在系统设置中启用了无障碍服务
- 确认授予了悬浮窗权限
- 尝试重启应用

### 语音识别不工作

- 检查麦克风权限
- 确认设备联网（在线语音识别）
- 检查语音识别语言设置

### 视频通话连接失败

- 检查网络连接
- 确认信令服务器地址正确
- 检查摄像头和麦克风权限

## 注意事项

1. **隐私保护**：应用不会收集或上传任何个人信息
2. **系统兼容性**：建议Android 8.0+以获得最佳体验
3. **电池优化**：建议将应用加入电池优化白名单
4. **微信兼容性**：微信版本更新可能影响无障碍功能

## 开发计划

- [ ] 支持群视频通话
- [ ] 添加通话录制功能
- [ ] 优化语音识别准确率
- [ ] 支持更多语音命令
- [ ] 添加通话记录统计

## 许可证

MIT License

## 联系方式

如有问题或建议，欢迎提Issue。
