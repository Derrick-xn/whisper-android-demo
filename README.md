# Whisper Android Demo

基于 whisper.cpp 的安卓语音识别实时演示应用。

## 功能特性

- ✨ 实时语音录制和识别
- 🔄 流式语音处理
- 🎯 语音活动检测 (VAD)
- 📱 原生安卓界面
- 🚀 使用 whisper-base-q8 量化模型，快速推理
- 🎤 自动检测语音开始和结束
- 📄 识别结果实时显示

## 系统要求

- Android API 24+ (Android 7.0+)
- ARMv7 或 ARM64 设备
- 至少 2GB RAM
- 录音权限

## 构建说明

### 环境准备

1. 安装 Android Studio (Arctic Fox 或更高版本)
2. 安装 NDK (r21e 或更高版本)
3. 安装 CMake (3.22.1+)

### 编译步骤

1. 克隆或下载项目
```bash
git clone <repository-url>
cd whisper-base-q8
```

2. 确保模型文件存在
项目应包含 `ggml-base-q8_0.bin` 模型文件，这个文件会被自动复制到 `app/src/main/assets/` 目录。

3. 在 Android Studio 中打开项目

4. 同步项目依赖
   - 首次构建时，CMake 会自动下载并编译 whisper.cpp
   - 这个过程可能需要几分钟时间

5. 构建项目
   - 选择目标设备（推荐使用 ARM64 设备以获得最佳性能）
   - 点击 Run 或 Debug

## 使用说明

1. **启动应用**
   - 应用启动时会自动加载 Whisper 模型
   - 等待 "模型加载完成" 提示

2. **开始录音**
   - 点击 "开始录音" 按钮
   - 授予录音权限（如果需要）
   - 应用会开始监听语音

3. **语音识别**
   - 开始说话，应用会显示 "检测到语音，正在录制..."
   - 停止说话后，应用会自动处理音频并显示识别结果
   - 识别结果会实时显示在界面上

4. **停止录音**
   - 点击 "停止录音" 按钮停止监听
   - 或等待自动检测静音后自动停止

## 项目结构

```
app/
├── src/main/
│   ├── cpp/                    # Native C++ 代码
│   │   ├── CMakeLists.txt     # CMake 构建配置
│   │   ├── native-lib.cpp     # JNI 接口实现
│   │   ├── audio_utils.h      # 音频处理工具
│   │   └── audio_utils.cpp    # 音频处理实现
│   ├── java/com/example/whisperdemo/
│   │   ├── MainActivity.kt    # 主界面
│   │   ├── WhisperService.kt  # Whisper 服务封装
│   │   └── AudioRecorder.kt   # 音频录制器
│   ├── res/                   # 资源文件
│   └── assets/
│       └── ggml-base-q8_0.bin # Whisper 模型文件
└── build.gradle.kts           # 构建配置
```

## 技术架构

### 核心组件

1. **WhisperService**: 负责模型加载和语音转录
2. **AudioRecorder**: 处理实时音频录制和 VAD
3. **MainActivity**: 用户界面和交互逻辑

### 音频处理流程

1. **录制**: 使用 `AudioRecord` 以 16kHz 单声道格式录制音频
2. **VAD**: 实时检测语音活动，自动分段处理
3. **预处理**: 音频格式转换和重采样
4. **识别**: 调用 whisper.cpp 进行语音转录
5. **显示**: 实时更新识别结果到界面

## 性能优化

- 使用量化模型 (Q8) 减少内存占用和计算量
- 多线程处理避免 UI 阻塞
- 智能音频分段减少不必要的计算
- 缓存模型文件避免重复加载

## 故障排除

### 常见问题

1. **模型加载失败**
   - 检查 `ggml-base-q8_0.bin` 文件是否存在于 assets 目录
   - 确保文件大小正确（约 78MB）

2. **录音权限问题**
   - 在设备设置中手动授予录音权限
   - 重启应用

3. **识别结果为空**
   - 确保在安静环境中测试
   - 尝试调整 `SILENCE_THRESHOLD` 参数
   - 检查日志输出

4. **编译错误**
   - 确保 NDK 和 CMake 版本正确
   - 清理项目后重新构建
   - 检查网络连接（需要下载 whisper.cpp）

### 日志调试

使用 `adb logcat` 查看详细日志：
```bash
adb logcat -s WhisperDemo:* WhisperService:* AudioRecorder:* AudioUtils:*
```

## 许可证

本项目基于 MIT 许可证开源。

Whisper.cpp 基于 MIT 许可证：https://github.com/ggerganov/whisper.cpp

## 贡献

欢迎提交 Issue 和 Pull Request！

## 相关链接

- [whisper.cpp](https://github.com/ggerganov/whisper.cpp)
- [OpenAI Whisper](https://github.com/openai/whisper) 