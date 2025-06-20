#include "audio_utils.h"
#include <cmath>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "AudioUtils"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

std::vector<float> convertToFloat32(const std::vector<int16_t>& audio16) {
    std::vector<float> audio_f32;
    audio_f32.reserve(audio16.size());
    
    for (int16_t sample : audio16) {
        audio_f32.push_back(static_cast<float>(sample) / 32768.0f);
    }
    
    return audio_f32;
}

std::vector<float> resampleAudio(const std::vector<float>& audio, int source_rate, int target_rate) {
    if (source_rate == target_rate) {
        return audio;
    }
    
    const float ratio = static_cast<float>(source_rate) / target_rate;
    const size_t new_size = static_cast<size_t>(audio.size() / ratio);
    std::vector<float> resampled;
    resampled.reserve(new_size);
    
    for (size_t i = 0; i < new_size; ++i) {
        const float index = i * ratio;
        const size_t index1 = static_cast<size_t>(index);
        const size_t index2 = std::min(index1 + 1, audio.size() - 1);
        const float frac = index - index1;
        
        const float sample = audio[index1] * (1.0f - frac) + audio[index2] * frac;
        resampled.push_back(sample);
    }
    
    LOGI("Resampled audio from %d Hz to %d Hz, size: %zu -> %zu", 
         source_rate, target_rate, audio.size(), resampled.size());
    
    return resampled;
}

void highPassFilter(std::vector<float>& audio, float cutoff_freq, int sample_rate) {
    const float rc = 1.0f / (cutoff_freq * 2.0f * M_PI);
    const float dt = 1.0f / sample_rate;
    const float alpha = rc / (rc + dt);
    
    if (audio.empty()) return;
    
    float prev_input = audio[0];
    float prev_output = audio[0];
    
    for (size_t i = 1; i < audio.size(); ++i) {
        const float output = alpha * (prev_output + audio[i] - prev_input);
        prev_input = audio[i];
        prev_output = output;
        audio[i] = output;
    }
} 