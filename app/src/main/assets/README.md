# Assets Directory

## 模型文件说明

这个目录应该包含Whisper模型文件：

### 需要的文件
- `ggml-base-q8_0.bin` - Whisper base模型（量化版本）

### 获取方式

#### 方法1：从GitHub Releases下载
1. 访问项目的[Releases页面](https://github.com/Derrick-xn/whisper-android-demo/releases)
2. 下载 `ggml-base-q8_0.bin` 文件
3. 将文件放置在 `app/src/main/assets/` 目录中

#### 方法2：从官方下载
1. 访问 [Whisper.cpp模型下载页面](https://huggingface.co/ggerganov/whisper.cpp)
 1.1 国内镜像站：https://hf-mirror.com/ggerganov/whisper.cpp 可以直接下载
2. 下载 `ggml-base.bin` 并使用quantize工具转换为q8_0格式
3. 或直接下载已量化的 `ggml-base-q8_0.bin`

### 文件大小
- `ggml-base-q8_0.bin`: 约78MB

### 注意事项
- 模型文件较大，已从Git跟踪中排除
- 确保文件名完全匹配：`ggml-base-q8_0.bin`
- 文件放置后需要重新编译应用 