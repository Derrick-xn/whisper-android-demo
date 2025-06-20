#!/bin/bash

echo "🚀 开始构建 Whisper Android Demo"

# 检查必要的工具
if ! command -v gradle &> /dev/null; then
    echo "❌ Gradle 未安装或不在 PATH 中"
    exit 1
fi

# 检查模型文件
if [ ! -f "app/src/main/assets/ggml-base-q8_0.bin" ]; then
    echo "❌ 模型文件不存在于 assets 目录"
    echo "请确保 ggml-base-q8_0.bin 文件位于 app/src/main/assets/ 目录"
    exit 1
fi

echo "✅ 模型文件检查完成"

# 清理项目
echo "🧹 清理项目..."
./gradlew clean

# 构建项目
echo "🔨 构建项目..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ 构建成功！"
    echo "📱 APK 文件位置: app/build/outputs/apk/debug/app-debug.apk"
    
    # 检查是否有连接的设备
    if command -v adb &> /dev/null; then
        devices=$(adb devices | grep -v "List of devices" | wc -l)
        if [ $devices -gt 1 ]; then
            echo "🔌 发现连接的设备，是否要安装 APK？(y/n)"
            read -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                adb install app/build/outputs/apk/debug/app-debug.apk
                echo "📱 APK 安装完成"
            fi
        fi
    fi
else
    echo "❌ 构建失败"
    exit 1
fi 