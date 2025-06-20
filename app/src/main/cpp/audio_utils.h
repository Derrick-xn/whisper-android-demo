#ifndef AUDIO_UTILS_H
#define AUDIO_UTILS_H

#include <vector>

// Convert 16-bit PCM audio to float32 format
std::vector<float> convertToFloat32(const std::vector<int16_t>& audio16);

// Resample audio to target sample rate
std::vector<float> resampleAudio(const std::vector<float>& audio, int source_rate, int target_rate);

// Apply high-pass filter to remove low frequency noise
void highPassFilter(std::vector<float>& audio, float cutoff_freq, int sample_rate);

// 新增音频预处理函数
// Apply noise reduction using spectral subtraction
void spectralSubtractionDenoise(std::vector<float>& audio, int sample_rate);

// Normalize audio volume to optimal range
void normalizeAudio(std::vector<float>& audio);

// Apply voice enhancement filter
void voiceEnhancementFilter(std::vector<float>& audio, int sample_rate);

// Detect voice activity using advanced algorithm
bool detectVoiceActivity(const std::vector<float>& audio, int sample_rate);

// Apply adaptive noise gate
void adaptiveNoiseGate(std::vector<float>& audio, float threshold_ratio = 0.1f);

#endif // AUDIO_UTILS_H 