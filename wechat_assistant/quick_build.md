# 快速构建APK指南

## 方案1：使用GitHub Actions自动构建（最简单）

1. Fork或上传项目到GitHub
2. 创建 `.github/workflows/build.yml` 文件
3. Push代码后自动构建APK
4. 从Actions页面下载APK

## 方案2：使用Apktool反编译工具

```bash
# 1. 安装必要工具
sudo apt-get install openjdk-11-jdk
wget https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/linux/apktool
chmod +x apktool

# 2. 下载Android SDK命令行工具
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip

# 3. 设置环境变量
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 4. 安装构建工具
sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-33" "build-tools;33.0.0"

# 5. 构建APK
cd WeChatAssistant
./gradlew assembleDebug
```

## 方案3：使用Docker容器构建

```bash
# 使用预配置的Android构建镜像
docker run -v $(pwd):/project mingc/android-build-box bash -c "cd /project && ./gradlew assembleDebug"
```

## 方案4：在线IDE构建

1. 访问 [Gitpod