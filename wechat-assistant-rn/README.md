# 微信助手 - React Native版本

一个用于自动接听微信视频电话并支持语音控制的移动应用（React Native + Expo实现）。

## 功能特性

### 1. 微信视频辅助
- ✅ 自动检测微信视频来电
- ✅ 语音播报来电人信息（Expo Speech）
- ✅ 自动接听（可设置延迟时间）
- ✅ 状态实时监控

### 2. 独立视频通话
- ✅ P2P视频通话功能
- ✅ WebRTC技术实现
- ✅ 联系人管理
- ✅ 摄像头切换
- ✅ 静音控制
- ✅ 实时视频预览

### 3. 语音控制
- ✅ 语音识别命令（@react-native-voice/voice）
- ✅ 支持命令：
  - "接听电话" - 自动接听来电
  - "打电话给[姓名]" - 拨打视频电话
  - "挂断电话" - 结束通话
  - "打开微信" - 启动微信

## 技术栈

- **React Native** 0.81+
- **Expo** 54+
- **TypeScript**
- **Zustand** - 状态管理
- **NativeWind** - Tailwind CSS for React Native
- **react-native-webrtc** - 视频通话
- **@react-native-voice/voice** - 语音识别
- **expo-speech** - 文字转语音
- **expo-router** - 文件系统路由

## 环境要求

- Node.js 18+ 或 Bun
- iOS 13+ 或 Android 5.0+
- Expo CLI
- 物理设备（推荐，WebRTC在模拟器上可能不稳定）

## 快速开始

### 1. 安装依赖

```bash
cd wechat-assistant-rn

# 使用 npm
npm install

# 或使用 bun（更快）
bun install
```

### 2. 启动开发服务器

```bash
npm start
# 或
bun start
```

### 3. 在设备上运行

#### iOS（需要Mac）
```bash
npm run ios
```

#### Android
```bash
npm run android
```

#### 使用Expo Go（最简单）
1. 在手机上安装 Expo Go 应用
2. 扫描终端显示的二维码
3. 等待应用加载完成

## 权限配置

### iOS权限（自动配置）
- ✅ 摄像头权限 - 视频通话
- ✅ 麦克风权限 - 语音识别和通话
- ✅ 语音识别权限 - 语音命令

### Android权限（自动配置）
- ✅ 摄像头权限
- ✅ 麦克风权限
- ✅ 网络权限
- ✅ 悬浮窗权限（可选）
- ✅ 前台服务权限（可选）

## 项目结构

```
wechat-assistant-rn/
├── app/                      # 路由页面
│   ├── (tabs)/              # 标签页
│   │   ├── index.tsx        # 主页
│   │   ├── contacts.tsx     # 联系人
│   │   ├── settings.tsx     # 设置
│   │   └── _layout.tsx      # 标签页布局
│   ├── call/                # 通话页面
│   │   └── [id].tsx         # 视频通话
│   └── _layout.tsx          # 根布局
├── types/                   # TypeScript类型
│   └── contact.ts           # 联系人类型
├── utils/                   # 工具函数
│   ├── accessibilityStore.ts  # 无障碍状态
│   ├── voiceStore.ts          # 语音控制状态
│   └── callStore.ts           # 通话状态
├── constants/               # 常量配置
├── assets/                  # 静态资源
└── package.json
```

## 使用说明

### 启动服务

1. 打开应用
2. 在主页点击"启动服务"按钮
3. 按系统提示授予必要权限

### 语音控制

1. 点击主页面的麦克风按钮
2. 说出语音命令（中文）
3. 等待系统识别并执行

### 发起视频通话

1. 切换到"联系人"标签
2. 选择要呼叫的联系人
3. 点击视频通话图标
4. 等待连接建立

## 技术说明

### WebRTC实现

应用使用 `react-native-webrtc` 实现P2P视频通话：

- **STUN服务器**: 使用Google公共STUN服务器
- **信令**: 需要自建信令服务器（代码中为占位实现）
- **媒体流**: 支持音频+视频双向传输

### 语音识别

使用 `@react-native-voice/voice` 实现：

- **语言**: 中文（zh-CN）
- **模式**: 连续识别
- **处理**: 本地命令解析

### 状态管理

使用 Zustand 管理应用状态：

- **accessibilityStore**: 无障碍服务状态
- **voiceStore**: 语音识别状态
- **callStore**: 视频通话状态

## 已知限制

### 微信无障碍控制

⚠️ **重要说明**：

- React Native应用无法直接访问其他应用的无障碍服务
- 微信自动接听功能需要配合原生模块实现
- 当前版本提供独立的视频通话功能

### WebRTC信令

本应用的视频通话需要信令服务器：

- 当前代码使用占位实现
- 生产环境需要部署真实信令服务器
- 可选方案：
  - Socket.io服务器
  - Firebase Realtime Database
  - PeerJS Cloud

## 开发计划

- [ ] 实现完整的信令服务
- [ ] 添加群视频通话
- [ ] 通话录制功能
- [ ] 优化语音识别准确率
- [ ] 添加更多语音命令
- [ ] 通话记录和统计
- [ ] 原生模块开发（真实微信集成）

## 故障排除

### 语音识别不工作

- 检查麦克风权限
- 确认设备联网（需要云端识别）
- 尝试重启应用

### 视频通话连接失败

- 检查网络连接
- 确认信令服务器配置
- 检查摄像头和麦克风权限
- 尝试在物理设备上测试

### 应用崩溃

- 清除缓存：`npm start -- --clear`
- 重新安装依赖：`rm -rf node_modules && npm install`
- 检查系统权限设置

## 构建生产版本

### iOS
```bash
eas build --platform ios
```

### Android
```bash
eas build --platform android
```

需要先配置 EAS Build：
```bash
npm install -g eas-cli
eas login
eas build:configure
```

## 许可证

MIT License

## 技术支持

如有问题或建议，欢迎提Issue。

## 与Flutter版本对比

| 特性 | React Native版本 | Flutter版本 |
|------|------------------|-------------|
| 跨平台 | iOS + Android + Web | iOS + Android |
| 开发效率 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 性能 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 生态系统 | 丰富 | 中等 |
| 学习曲线 | 平缓（JS/TS） | 中等（Dart） |
| 热重载 | ✅ | ✅ |
| WebRTC支持 | ✅ 成熟 | ✅ 成熟 |
| 原生集成 | ✅ 容易 | ✅ 容易 |

两个版本各有优势，选择取决于团队技术栈和具体需求。
