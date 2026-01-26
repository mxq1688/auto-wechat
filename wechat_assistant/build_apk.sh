#!/bin/bash

echo "============================="
echo "  WeChat Assistant APK Build"
echo "============================="

# Create keystore directory
mkdir -p app/keystore

# Generate keystore if not exists
KEYSTORE="app/keystore/wechat-assistant.jks"
if [ ! -f "$KEYSTORE" ]; then
    echo "Generating keystore..."
    keytool -genkey -v -keystore "$KEYSTORE" \
        -alias wechat-assistant \
        -keyalg RSA -keysize 2048 \
        -validity 10000 \
        -storepass wechat123456 \
        -keypass wechat123456 \
        -dname "CN=WeChat Assistant, O=WeChatAssistant, C=CN" \
        -noprompt
fi

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean 2>/dev/null || gradle clean

# Build Debug APK
echo "Building Debug APK..."
./gradlew assembleDebug || gradle assembleDebug

# Build Release APK
echo "Building Release APK..."
./gradlew assembleRelease || gradle assembleRelease

# Copy APKs to output directory
mkdir -p output
cp app/build/outputs/apk/debug/*.apk output/ 2>/dev/null
cp app/build/outputs/apk/release/*.apk output/ 2>/dev/null

echo ""
echo "Build Complete!"
echo "APK files are in the output/ directory"
ls -la output/*.apk 2>/dev/null