# 快速入门指南

## 5分钟快速体验 Whisper Android Demo

### 第一步：环境检查

确保您已安装：
- ✅ Android Studio (2021.3.1 或更高版本)
- ✅ Android SDK (API 24+)
- ✅ NDK (r21e 或更高版本)
- ✅ CMake (3.22.1+)

### 第二步：打开项目

1. 启动 Android Studio
2. 选择 "Open an existing project"
3. 选择 `whisper-base-q8` 文件夹
4. 等待项目同步完成

### 第三步：检查模型文件

确认 `app/src/main/assets/ggml-base-q8_0.bin` 文件存在
- 文件大小约 78MB
- 如果不存在，请从项目根目录复制 `ggml-base-q8_0.bin` 到 assets 目录

### 第四步：连接设备

1. 连接您的 Android 设备（API 24+ / Android 7.0+）
2. 启用开发者选项和 USB 调试
3. 在 Android Studio 中选择您的设备

### 第五步：运行应用

1. 点击 Android Studio 中的绿色 "Run" 按钮 ▶️
2. 等待编译完成（首次编译可能需要5-10分钟）
3. 应用会自动安装并启动

### 第六步：体验功能

1. **等待模型加载**
   - 应用启动后会显示 "正在加载Whisper模型..."
   - 等待显示 "模型加载完成"

2. **授予录音权限**
   - 点击 "开始录音" 
   - 允许录音权限

3. **开始语音识别**
   - 对着手机说话
   - 应用会自动检测语音开始和结束
   - 识别结果会实时显示

## 常见问题快速解决

### Q: 编译失败怎么办？
A: 
1. 确保网络连接正常（需要下载 whisper.cpp）
2. 尝试 `Build -> Clean Project` 然后重新构建
3. 检查 NDK 和 CMake 版本

### Q: 模型加载失败？
A:
1. 检查 assets 目录中是否有模型文件
2. 确认文件大小约 78MB
3. 重新安装应用

### Q: 识别结果为空？
A:
1. 确保在安静环境中测试
2. 说话声音清晰，距离手机不要太远
3. 检查录音权限是否授予

### Q: 应用崩溃？
A:
1. 查看 logcat 日志
2. 确保设备有足够内存（建议 2GB+）
3. 尝试在更高端的设备上运行

## 高级使用

### 自定义参数

修改 `AudioRecorder.kt` 中的参数：
```kotlin
private const val SILENCE_THRESHOLD = 500    // 静音检测阈值
private const val MIN_AUDIO_LENGTH_MS = 1000 // 最小音频长度
```

修改 `native-lib.cpp` 中的 whisper 参数：
```cpp
wparams.language = "zh";     // 语言设置
wparams.n_threads = 4;       // 线程数
```

### 调试模式

启用详细日志：
```bash
adb logcat -s WhisperDemo:* WhisperService:* AudioRecorder:*
```

## 性能提示

1. **推荐设备**：ARM64 架构，4GB+ RAM
2. **最佳环境**：安静室内，距离麦克风30cm以内
3. **电池优化**：长时间使用建议连接充电器

## 下一步

体验完成后，您可以：
1. 查看完整的 [README.md](README.md) 了解更多技术细节
2. 修改代码适配您的具体需求
3. 集成到您的正式项目中

🎉 恭喜！您已经成功运行了 Whisper Android Demo！ 