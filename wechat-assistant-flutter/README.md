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

### 3. 返回应用，状态显示「已开启」即可

### 配置自动回复规则
1. 点击「自动回复」进入规则管理
2. 点击右上角「+」添加新规则
3. 设置规则名称、关键词、回复内容
4. 选择匹配模式和作用域
5. 可选：设置黑白名单、生效时间等
6. 保存并启用规则

### 启动消息监控
1. 确保无障碍服务已开启
2. 在设置中开启「消息监控」
3. 可在「消息历史」查看记录

### 使用视频通话
1. 在设置中配置信令服务器地址
2. 设置用户 ID（唯一标识）
3. 可开启自动接听功能

## ⚙️ 配置说明

### 环境变量（.env）
```env
# 信令服务器配置
SIGNALING_SERVER_URL=http://localhost:3000

# 数据库配置
DATABASE_PATH=/data/wechat_assistant.db

# 日志级别
LOG_LEVEL=info
```

### Android 权限配置
在 `android/app/src/main/AndroidManifest.xml` 中已配置：
- 无障碍服务权限
- 悬浮窗权限
- 麦克风权限（语音功能）
- 摄像头权限（视频通话）
- 存储权限（数据备份）

## 🏗️ 项目结构

```
wechat-assistant-flutter/
├── lib/
│   ├── models/          # 数据模型
│   ├── providers/       # 状态管理
│   ├── screens/         # UI 界面
│   ├── services/        # 业务服务
│   └── main.dart       # 应用入口
├── android/            # Android 原生代码
│   └── app/src/main/java/
│       └── WeChatAccessibilityService.java
├── signaling-server/   # Node.js 信令服务器
│   ├── index.js
│   └── package.json
└── pubspec.yaml       # Flutter 依赖配置
```

## ⚠️ 注意事项

1. **隐私安全**：本应用仅在本地处理数据，不上传任何信息到云端
2. **使用限制**：请勿用于商业用途或违法行为
3. **兼容性**：部分功能依赖微信版本，建议使用微信 8.0+
4. **电池优化**：建议将应用加入电池优化白名单
5. **无障碍服务**：某些系统可能会自动关闭无障碍服务，需要重新开启

## 🔧 故障排除

### 无障碍服务不工作
- 检查是否已授予无障碍权限
- 重启手机后需要重新开启服务
- 某些系统需要在开发者选项中关闭「权限监控」

### 悬浮球不显示
- 检查是否已授予悬浮窗权限
- MIUI/EMUI 等系统需要额外授权
- 在应用设置中允许「显示在其他应用上层」

### 自动回复失败
- 确认无障碍服务正常运行
- 检查规则是否正确配置并启用
- 查看规则优先级和匹配条件

### 视频通话连接失败
- 确保信令服务器正常运行
- 检查网络连接和防火墙设置
- 双方需要在同一网络或公网可访问

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境设置
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Flutter 官方代码规范
- 使用 `flutter analyze` 检查代码
- 运行 `flutter test` 确保测试通过

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **Your Name** - *初始开发* - [GitHub](https://github.com/yourusername)

## 🙏 致谢

- Flutter 团队提供的优秀框架
- 所有贡献者和用户的支持
- 开源社区的宝贵资源

## 📞 联系方式

- Issue: [GitHub Issues](https://github.com/yourusername/wechat-assistant-flutter/issues)
- Email: your.email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！语音控制
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

### 3. 返回应用，状态显示「已开启」即可

### 配置自动回复规则
1. 点击「自动回复」进入规则管理
2. 点击右上角「+」添加新规则
3. 设置规则名称、关键词、回复内容
4. 选择匹配模式和作用域
5. 可选：设置黑白名单、生效时间等
6. 保存并启用规则

### 启动消息监控
1. 确保无障碍服务已开启
2. 在设置中开启「消息监控」
3. 可在「消息历史」查看记录

### 使用视频通话
1. 在设置中配置信令服务器地址
2. 设置用户 ID（唯一标识）
3. 可开启自动接听功能

## ⚙️ 配置说明

### 环境变量（.env）
```env
# 信令服务器配置
SIGNALING_SERVER_URL=http://localhost:3000

# 数据库配置
DATABASE_PATH=/data/wechat_assistant.db

# 日志级别
LOG_LEVEL=info
```

### Android 权限配置
在 `android/app/src/main/AndroidManifest.xml` 中已配置：
- 无障碍服务权限
- 悬浮窗权限
- 麦克风权限（语音功能）
- 摄像头权限（视频通话）
- 存储权限（数据备份）

## 🏗️ 项目结构

```
wechat-assistant-flutter/
├── lib/
│   ├── models/          # 数据模型
│   ├── providers/       # 状态管理
│   ├── screens/         # UI 界面
│   ├── services/        # 业务服务
│   └── main.dart       # 应用入口
├── android/            # Android 原生代码
│   └── app/src/main/java/
│       └── WeChatAccessibilityService.java
├── signaling-server/   # Node.js 信令服务器
│   ├── index.js
│   └── package.json
└── pubspec.yaml       # Flutter 依赖配置
```

## ⚠️ 注意事项

1. **隐私安全**：本应用仅在本地处理数据，不上传任何信息到云端
2. **使用限制**：请勿用于商业用途或违法行为
3. **兼容性**：部分功能依赖微信版本，建议使用微信 8.0+
4. **电池优化**：建议将应用加入电池优化白名单
5. **无障碍服务**：某些系统可能会自动关闭无障碍服务，需要重新开启

## 🔧 故障排除

### 无障碍服务不工作
- 检查是否已授予无障碍权限
- 重启手机后需要重新开启服务
- 某些系统需要在开发者选项中关闭「权限监控」

### 悬浮球不显示
- 检查是否已授予悬浮窗权限
- MIUI/EMUI 等系统需要额外授权
- 在应用设置中允许「显示在其他应用上层」

### 自动回复失败
- 确认无障碍服务正常运行
- 检查规则是否正确配置并启用
- 查看规则优先级和匹配条件

### 视频通话连接失败
- 确保信令服务器正常运行
- 检查网络连接和防火墙设置
- 双方需要在同一网络或公网可访问

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境设置
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Flutter 官方代码规范
- 使用 `flutter analyze` 检查代码
- 运行 `flutter test` 确保测试通过

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **Your Name** - *初始开发* - [GitHub](https://github.com/yourusername)

## 🙏 致谢

- Flutter 团队提供的优秀框架
- 所有贡献者和用户的支持
- 开源社区的宝贵资源

## 📞 联系方式

- Issue: [GitHub Issues](https://github.com/yourusername/wechat-assistant-flutter/issues)
- Email: your.email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！运行应用

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
3. 返回应用，状态显示「已开启」即可

### 配置自动回复规则
1. 点击「自动回复」进入规则管理
2. 点击右上角「+」添加新规则
3. 设置规则名称、关键词、回复内容
4. 选择匹配模式和作用域
5. 可选：设置黑白名单、生效时间等
6. 保存并启用规则

### 启动消息监控
1. 确保无障碍服务已开启
2. 在设置中开启「消息监控」
3. 可在「消息历史」查看记录

### 使用视频通话
1. 在设置中配置信令服务器地址
2. 设置用户 ID（唯一标识）
3. 可开启自动接听功能

## ⚙️ 配置说明

### 环境变量（.env）
```env
# 信令服务器配置
SIGNALING_SERVER_URL=http://localhost:3000

# 数据库配置
DATABASE_PATH=/data/wechat_assistant.db

# 日志级别
LOG_LEVEL=info
```

### Android 权限配置
在 `android/app/src/main/AndroidManifest.xml` 中已配置：
- 无障碍服务权限
- 悬浮窗权限
- 麦克风权限（语音功能）
- 摄像头权限（视频通话）
- 存储权限（数据备份）

## 🏗️ 项目结构

```
wechat-assistant-flutter/
├── lib/
│   ├── models/          # 数据模型
│   ├── providers/       # 状态管理
│   ├── screens/         # UI 界面
│   ├── services/        # 业务服务
│   └── main.dart       # 应用入口
├── android/            # Android 原生代码
│   └── app/src/main/java/
│       └── WeChatAccessibilityService.java
├── signaling-server/   # Node.js 信令服务器
│   ├── index.js
│   └── package.json
└── pubspec.yaml       # Flutter 依赖配置
```

## ⚠️ 注意事项

1. **隐私安全**：本应用仅在本地处理数据，不上传任何信息到云端
2. **使用限制**：请勿用于商业用途或违法行为
3. **兼容性**：部分功能依赖微信版本，建议使用微信 8.0+
4. **电池优化**：建议将应用加入电池优化白名单
5. **无障碍服务**：某些系统可能会自动关闭无障碍服务，需要重新开启

## 🔧 故障排除

### 无障碍服务不工作
- 检查是否已授予无障碍权限
- 重启手机后需要重新开启服务
- 某些系统需要在开发者选项中关闭「权限监控」

### 悬浮球不显示
- 检查是否已授予悬浮窗权限
- MIUI/EMUI 等系统需要额外授权
- 在应用设置中允许「显示在其他应用上层」

### 自动回复失败
- 确认无障碍服务正常运行
- 检查规则是否正确配置并启用
- 查看规则优先级和匹配条件

### 视频通话连接失败
- 确保信令服务器正常运行
- 检查网络连接和防火墙设置
- 双方需要在同一网络或公网可访问

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境设置
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Flutter 官方代码规范
- 使用 `flutter analyze` 检查代码
- 运行 `flutter test` 确保测试通过

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **Your Name** - *初始开发* - [GitHub](https://github.com/yourusername)

## 🙏 致谢

- Flutter 团队提供的优秀框架
- 所有贡献者和用户的支持
- 开源社区的宝贵资源

## 📞 联系方式

- Issue: [GitHub Issues](https://github.com/yourusername/wechat-assistant-flutter/issues)
- Email: your.email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！找到**微信助手**并启用
4. 授予必要权限

## 使用说明

### 启动服务

1. 打开应用
2. 点击"启动服务"按钮
3. 返回应用，状态显示「已开启」即可

### 配置自动回复规则
1. 点击「自动回复」进入规则管理
2. 点击右上角「+」添加新规则
3. 设置规则名称、关键词、回复内容
4. 选择匹配模式和作用域
5. 可选：设置黑白名单、生效时间等
6. 保存并启用规则

### 启动消息监控
1. 确保无障碍服务已开启
2. 在设置中开启「消息监控」
3. 可在「消息历史」查看记录

### 使用视频通话
1. 在设置中配置信令服务器地址
2. 设置用户 ID（唯一标识）
3. 可开启自动接听功能

## ⚙️ 配置说明

### 环境变量（.env）
```env
# 信令服务器配置
SIGNALING_SERVER_URL=http://localhost:3000

# 数据库配置
DATABASE_PATH=/data/wechat_assistant.db

# 日志级别
LOG_LEVEL=info
```

### Android 权限配置
在 `android/app/src/main/AndroidManifest.xml` 中已配置：
- 无障碍服务权限
- 悬浮窗权限
- 麦克风权限（语音功能）
- 摄像头权限（视频通话）
- 存储权限（数据备份）

## 🏗️ 项目结构

```
wechat-assistant-flutter/
├── lib/
│   ├── models/          # 数据模型
│   ├── providers/       # 状态管理
│   ├── screens/         # UI 界面
│   ├── services/        # 业务服务
│   └── main.dart       # 应用入口
├── android/            # Android 原生代码
│   └── app/src/main/java/
│       └── WeChatAccessibilityService.java
├── signaling-server/   # Node.js 信令服务器
│   ├── index.js
│   └── package.json
└── pubspec.yaml       # Flutter 依赖配置
```

## ⚠️ 注意事项

1. **隐私安全**：本应用仅在本地处理数据，不上传任何信息到云端
2. **使用限制**：请勿用于商业用途或违法行为
3. **兼容性**：部分功能依赖微信版本，建议使用微信 8.0+
4. **电池优化**：建议将应用加入电池优化白名单
5. **无障碍服务**：某些系统可能会自动关闭无障碍服务，需要重新开启

## 🔧 故障排除

### 无障碍服务不工作
- 检查是否已授予无障碍权限
- 重启手机后需要重新开启服务
- 某些系统需要在开发者选项中关闭「权限监控」

### 悬浮球不显示
- 检查是否已授予悬浮窗权限
- MIUI/EMUI 等系统需要额外授权
- 在应用设置中允许「显示在其他应用上层」

### 自动回复失败
- 确认无障碍服务正常运行
- 检查规则是否正确配置并启用
- 查看规则优先级和匹配条件

### 视频通话连接失败
- 确保信令服务器正常运行
- 检查网络连接和防火墙设置
- 双方需要在同一网络或公网可访问

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境设置
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Flutter 官方代码规范
- 使用 `flutter analyze` 检查代码
- 运行 `flutter test` 确保测试通过

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **Your Name** - *初始开发* - [GitHub](https://github.com/yourusername)

## 🙏 致谢

- Flutter 团队提供的优秀框架
- 所有贡献者和用户的支持
- 开源社区的宝贵资源

## 📞 联系方式

- Issue: [GitHub Issues](https://github.com/yourusername/wechat-assistant-flutter/issues)
- Email: your.email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！按提示授予必要权限

### 语音控制

1. 点击主页面的麦克风按钮
2. 说出语音命令
3. 返回应用，状态显示「已开启」即可

### 配置自动回复规则
1. 点击「自动回复」进入规则管理
2. 点击右上角「+」添加新规则
3. 设置规则名称、关键词、回复内容
4. 选择匹配模式和作用域
5. 可选：设置黑白名单、生效时间等
6. 保存并启用规则

### 启动消息监控
1. 确保无障碍服务已开启
2. 在设置中开启「消息监控」
3. 可在「消息历史」查看记录

### 使用视频通话
1. 在设置中配置信令服务器地址
2. 设置用户 ID（唯一标识）
3. 可开启自动接听功能

## ⚙️ 配置说明

### 环境变量（.env）
```env
# 信令服务器配置
SIGNALING_SERVER_URL=http://localhost:3000

# 数据库配置
DATABASE_PATH=/data/wechat_assistant.db

# 日志级别
LOG_LEVEL=info
```

### Android 权限配置
在 `android/app/src/main/AndroidManifest.xml` 中已配置：
- 无障碍服务权限
- 悬浮窗权限
- 麦克风权限（语音功能）
- 摄像头权限（视频通话）
- 存储权限（数据备份）

## 🏗️ 项目结构

```
wechat-assistant-flutter/
├── lib/
│   ├── models/          # 数据模型
│   ├── providers/       # 状态管理
│   ├── screens/         # UI 界面
│   ├── services/        # 业务服务
│   └── main.dart       # 应用入口
├── android/            # Android 原生代码
│   └── app/src/main/java/
│       └── WeChatAccessibilityService.java
├── signaling-server/   # Node.js 信令服务器
│   ├── index.js
│   └── package.json
└── pubspec.yaml       # Flutter 依赖配置
```

## ⚠️ 注意事项

1. **隐私安全**：本应用仅在本地处理数据，不上传任何信息到云端
2. **使用限制**：请勿用于商业用途或违法行为
3. **兼容性**：部分功能依赖微信版本，建议使用微信 8.0+
4. **电池优化**：建议将应用加入电池优化白名单
5. **无障碍服务**：某些系统可能会自动关闭无障碍服务，需要重新开启

## 🔧 故障排除

### 无障碍服务不工作
- 检查是否已授予无障碍权限
- 重启手机后需要重新开启服务
- 某些系统需要在开发者选项中关闭「权限监控」

### 悬浮球不显示
- 检查是否已授予悬浮窗权限
- MIUI/EMUI 等系统需要额外授权
- 在应用设置中允许「显示在其他应用上层」

### 自动回复失败
- 确认无障碍服务正常运行
- 检查规则是否正确配置并启用
- 查看规则优先级和匹配条件

### 视频通话连接失败
- 确保信令服务器正常运行
- 检查网络连接和防火墙设置
- 双方需要在同一网络或公网可访问

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境设置
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Flutter 官方代码规范
- 使用 `flutter analyze` 检查代码
- 运行 `flutter test` 确保测试通过

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **Your Name** - *初始开发* - [GitHub](https://github.com/yourusername)

## 🙏 致谢

- Flutter 团队提供的优秀框架
- 所有贡献者和用户的支持
- 开源社区的宝贵资源

## 📞 联系方式

- Issue: [GitHub Issues](https://github.com/yourusername/wechat-assistant-flutter/issues)
- Email: your.email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！等待系统执行操作

### 视频通话

1. 进入"联系人"标签
2. 选择联系人
3. 返回应用，状态显示「已开启」即可

### 配置自动回复规则
1. 点击「自动回复」进入规则管理
2. 点击右上角「+」添加新规则
3. 设置规则名称、关键词、回复内容
4. 选择匹配模式和作用域
5. 可选：设置黑白名单、生效时间等
6. 保存并启用规则

### 启动消息监控
1. 确保无障碍服务已开启
2. 在设置中开启「消息监控」
3. 可在「消息历史」查看记录

### 使用视频通话
1. 在设置中配置信令服务器地址
2. 设置用户 ID（唯一标识）
3. 可开启自动接听功能

## ⚙️ 配置说明

### 环境变量（.env）
```env
# 信令服务器配置
SIGNALING_SERVER_URL=http://localhost:3000

# 数据库配置
DATABASE_PATH=/data/wechat_assistant.db

# 日志级别
LOG_LEVEL=info
```

### Android 权限配置
在 `android/app/src/main/AndroidManifest.xml` 中已配置：
- 无障碍服务权限
- 悬浮窗权限
- 麦克风权限（语音功能）
- 摄像头权限（视频通话）
- 存储权限（数据备份）

## 🏗️ 项目结构

```
wechat-assistant-flutter/
├── lib/
│   ├── models/          # 数据模型
│   ├── providers/       # 状态管理
│   ├── screens/         # UI 界面
│   ├── services/        # 业务服务
│   └── main.dart       # 应用入口
├── android/            # Android 原生代码
│   └── app/src/main/java/
│       └── WeChatAccessibilityService.java
├── signaling-server/   # Node.js 信令服务器
│   ├── index.js
│   └── package.json
└── pubspec.yaml       # Flutter 依赖配置
```

## ⚠️ 注意事项

1. **隐私安全**：本应用仅在本地处理数据，不上传任何信息到云端
2. **使用限制**：请勿用于商业用途或违法行为
3. **兼容性**：部分功能依赖微信版本，建议使用微信 8.0+
4. **电池优化**：建议将应用加入电池优化白名单
5. **无障碍服务**：某些系统可能会自动关闭无障碍服务，需要重新开启

## 🔧 故障排除

### 无障碍服务不工作
- 检查是否已授予无障碍权限
- 重启手机后需要重新开启服务
- 某些系统需要在开发者选项中关闭「权限监控」

### 悬浮球不显示
- 检查是否已授予悬浮窗权限
- MIUI/EMUI 等系统需要额外授权
- 在应用设置中允许「显示在其他应用上层」

### 自动回复失败
- 确认无障碍服务正常运行
- 检查规则是否正确配置并启用
- 查看规则优先级和匹配条件

### 视频通话连接失败
- 确保信令服务器正常运行
- 检查网络连接和防火墙设置
- 双方需要在同一网络或公网可访问

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境设置
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Flutter 官方代码规范
- 使用 `flutter analyze` 检查代码
- 运行 `flutter test` 确保测试通过

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **Your Name** - *初始开发* - [GitHub](https://github.com/yourusername)

## 🙏 致谢

- Flutter 团队提供的优秀框架
- 所有贡献者和用户的支持
- 开源社区的宝贵资源

## 📞 联系方式

- Issue: [GitHub Issues](https://github.com/yourusername/wechat-assistant-flutter/issues)
- Email: your.email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！点击视频通话图标

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
3. 返回应用，状态显示「已开启」即可

### 配置自动回复规则
1. 点击「自动回复」进入规则管理
2. 点击右上角「+」添加新规则
3. 设置规则名称、关键词、回复内容
4. 选择匹配模式和作用域
5. 可选：设置黑白名单、生效时间等
6. 保存并启用规则

### 启动消息监控
1. 确保无障碍服务已开启
2. 在设置中开启「消息监控」
3. 可在「消息历史」查看记录

### 使用视频通话
1. 在设置中配置信令服务器地址
2. 设置用户 ID（唯一标识）
3. 可开启自动接听功能

## ⚙️ 配置说明

### 环境变量（.env）
```env
# 信令服务器配置
SIGNALING_SERVER_URL=http://localhost:3000

# 数据库配置
DATABASE_PATH=/data/wechat_assistant.db

# 日志级别
LOG_LEVEL=info
```

### Android 权限配置
在 `android/app/src/main/AndroidManifest.xml` 中已配置：
- 无障碍服务权限
- 悬浮窗权限
- 麦克风权限（语音功能）
- 摄像头权限（视频通话）
- 存储权限（数据备份）

## 🏗️ 项目结构

```
wechat-assistant-flutter/
├── lib/
│   ├── models/          # 数据模型
│   ├── providers/       # 状态管理
│   ├── screens/         # UI 界面
│   ├── services/        # 业务服务
│   └── main.dart       # 应用入口
├── android/            # Android 原生代码
│   └── app/src/main/java/
│       └── WeChatAccessibilityService.java
├── signaling-server/   # Node.js 信令服务器
│   ├── index.js
│   └── package.json
└── pubspec.yaml       # Flutter 依赖配置
```

## ⚠️ 注意事项

1. **隐私安全**：本应用仅在本地处理数据，不上传任何信息到云端
2. **使用限制**：请勿用于商业用途或违法行为
3. **兼容性**：部分功能依赖微信版本，建议使用微信 8.0+
4. **电池优化**：建议将应用加入电池优化白名单
5. **无障碍服务**：某些系统可能会自动关闭无障碍服务，需要重新开启

## 🔧 故障排除

### 无障碍服务不工作
- 检查是否已授予无障碍权限
- 重启手机后需要重新开启服务
- 某些系统需要在开发者选项中关闭「权限监控」

### 悬浮球不显示
- 检查是否已授予悬浮窗权限
- MIUI/EMUI 等系统需要额外授权
- 在应用设置中允许「显示在其他应用上层」

### 自动回复失败
- 确认无障碍服务正常运行
- 检查规则是否正确配置并启用
- 查看规则优先级和匹配条件

### 视频通话连接失败
- 确保信令服务器正常运行
- 检查网络连接和防火墙设置
- 双方需要在同一网络或公网可访问

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境设置
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Flutter 官方代码规范
- 使用 `flutter analyze` 检查代码
- 运行 `flutter test` 确保测试通过

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **Your Name** - *初始开发* - [GitHub](https://github.com/yourusername)

## 🙏 致谢

- Flutter 团队提供的优秀框架
- 所有贡献者和用户的支持
- 开源社区的宝贵资源

## 📞 联系方式

- Issue: [GitHub Issues](https://github.com/yourusername/wechat-assistant-flutter/issues)
- Email: your.email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！使用免费的PeerJS Cloud服务

## 故障排除

### 无障碍服务不工作
- 检查是否已授予无障碍权限
- 重启手机后需要重新开启服务
- 某些系统需要在开发者选项中关闭「权限监控」

### 悬浮球不显示
- 检查是否已授予悬浮窗权限
- MIUI/EMUI 等系统需要额外授权
- 在应用设置中允许「显示在其他应用上层」

### 自动回复失败
- 确认无障碍服务正常运行
- 检查规则是否正确配置并启用
- 查看规则优先级和匹配条件

### 视频通话连接失败
- 确保信令服务器正常运行
- 检查网络连接和防火墙设置
- 双方需要在同一网络或公网可访问

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境设置
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Flutter 官方代码规范
- 使用 `flutter analyze` 检查代码
- 运行 `flutter test` 确保测试通过

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **Your Name** - *初始开发* - [GitHub](https://github.com/yourusername)

## 🙏 致谢

- Flutter 团队提供的优秀框架
- 所有贡献者和用户的支持
- 开源社区的宝贵资源

## 📞 联系方式

- Issue: [GitHub Issues](https://github.com/yourusername/wechat-assistant-flutter/issues)
- Email: your.email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！无法启动

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
3. 返回应用，状态显示「已开启」即可

### 配置自动回复规则
1. 点击「自动回复」进入规则管理
2. 点击右上角「+」添加新规则
3. 设置规则名称、关键词、回复内容
4. 选择匹配模式和作用域
5. 可选：设置黑白名单、生效时间等
6. 保存并启用规则

### 启动消息监控
1. 确保无障碍服务已开启
2. 在设置中开启「消息监控」
3. 可在「消息历史」查看记录

### 使用视频通话
1. 在设置中配置信令服务器地址
2. 设置用户 ID（唯一标识）
3. 可开启自动接听功能

## ⚙️ 配置说明

### 环境变量（.env）
```env
# 信令服务器配置
SIGNALING_SERVER_URL=http://localhost:3000

# 数据库配置
DATABASE_PATH=/data/wechat_assistant.db

# 日志级别
LOG_LEVEL=info
```

### Android 权限配置
在 `android/app/src/main/AndroidManifest.xml` 中已配置：
- 无障碍服务权限
- 悬浮窗权限
- 麦克风权限（语音功能）
- 摄像头权限（视频通话）
- 存储权限（数据备份）

## 🏗️ 项目结构

```
wechat-assistant-flutter/
├── lib/
│   ├── models/          # 数据模型
│   ├── providers/       # 状态管理
│   ├── screens/         # UI 界面
│   ├── services/        # 业务服务
│   └── main.dart       # 应用入口
├── android/            # Android 原生代码
│   └── app/src/main/java/
│       └── WeChatAccessibilityService.java
├── signaling-server/   # Node.js 信令服务器
│   ├── index.js
│   └── package.json
└── pubspec.yaml       # Flutter 依赖配置
```

## ⚠️ 注意事项

1. **隐私安全**：本应用仅在本地处理数据，不上传任何信息到云端
2. **使用限制**：请勿用于商业用途或违法行为
3. **兼容性**：部分功能依赖微信版本，建议使用微信 8.0+
4. **电池优化**：建议将应用加入电池优化白名单
5. **无障碍服务**：某些系统可能会自动关闭无障碍服务，需要重新开启

## 🔧 故障排除

### 无障碍服务不工作
- 检查是否已授予无障碍权限
- 重启手机后需要重新开启服务
- 某些系统需要在开发者选项中关闭「权限监控」

### 悬浮球不显示
- 检查是否已授予悬浮窗权限
- MIUI/EMUI 等系统需要额外授权
- 在应用设置中允许「显示在其他应用上层」

### 自动回复失败
- 确认无障碍服务正常运行
- 检查规则是否正确配置并启用
- 查看规则优先级和匹配条件

### 视频通话连接失败
- 确保信令服务器正常运行
- 检查网络连接和防火墙设置
- 双方需要在同一网络或公网可访问

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发环境设置
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Flutter 官方代码规范
- 使用 `flutter analyze` 检查代码
- 运行 `flutter test` 确保测试通过

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 作者

- **Your Name** - *初始开发* - [GitHub](https://github.com/yourusername)

## 🙏 致谢

- Flutter 团队提供的优秀框架
- 所有贡献者和用户的支持
- 开源社区的宝贵资源

## 📞 联系方式

- Issue: [GitHub Issues](https://github.com/yourusername/wechat-assistant-flutter/issues)
- Email: your.email@example.com

---

⭐ 如果这个项目对你有帮助，请给个星标支持一下！**电池优化**：建议将应用加入电池优化白名单
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
