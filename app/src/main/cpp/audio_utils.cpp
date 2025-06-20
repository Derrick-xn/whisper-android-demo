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

void spectralSubtractionDenoise(std::vector<float>& audio, int sample_rate) {
    if (audio.empty()) return;
    
    float rms = 0.0f;
    for (float sample : audio) {
        rms += sample * sample;
    }
    rms = std::sqrt(rms / audio.size());
    
    float noise_threshold = rms * 0.1f;
    
    for (float& sample : audio) {
        if (std::abs(sample) < noise_threshold) {
            sample *= 0.1f;
        }
    }
    
    LOGI("Applied spectral subtraction denoising, RMS: %f, threshold: %f", rms, noise_threshold);
}

void normalizeAudio(std::vector<float>& audio) {
    if (audio.empty()) return;
    
    float max_val = 0.0f;
    for (float sample : audio) {
        max_val = std::max(max_val, std::abs(sample));
    }
    
    if (max_val > 0.0f) {
        float scale = 0.8f / max_val;
        for (float& sample : audio) {
            sample *= scale;
        }
        LOGI("Normalized audio, max value was: %f, scale: %f", max_val, scale);
    }
}

void voiceEnhancementFilter(std::vector<float>& audio, int sample_rate) {
    if (audio.empty()) return;
    
    highPassFilter(audio, 300.0f, sample_rate);
    
    for (float& sample : audio) {
        sample *= 1.2f;
        sample = std::max(-0.95f, std::min(0.95f, sample));
    }
    
    LOGI("Applied voice enhancement filter");
}

bool detectVoiceActivity(const std::vector<float>& audio, int sample_rate) {
    if (audio.empty()) return false;
    
    float energy = 0.0f;
    for (float sample : audio) {
        energy += sample * sample;
    }
    energy /= audio.size();
    
    int zero_crossings = 0;
    for (size_t i = 1; i < audio.size(); ++i) {
        if ((audio[i-1] >= 0) != (audio[i] >= 0)) {
            zero_crossings++;
        }
    }
    float zcr = static_cast<float>(zero_crossings) / audio.size();
    
    const float energy_threshold = 0.001f;
    const float zcr_min = 0.01f;
    const float zcr_max = 0.3f;
    
    bool is_voice = (energy > energy_threshold) && (zcr > zcr_min) && (zcr < zcr_max);
    
    LOGI("VAD: energy=%f, zcr=%f, is_voice=%s", energy, zcr, is_voice ? "true" : "false");
    
    return is_voice;
}

void adaptiveNoiseGate(std::vector<float>& audio, float threshold_ratio) {
    if (audio.empty()) return;
    
    float max_val = 0.0f;
    for (float sample : audio) {
        max_val = std::max(max_val, std::abs(sample));
    }
    
    float gate_threshold = max_val * threshold_ratio;
    
    for (float& sample : audio) {
        if (std::abs(sample) < gate_threshold) {
            sample *= 0.01f;
        }
    }
    
    LOGI("Applied adaptive noise gate, threshold: %f", gate_threshold);
} 