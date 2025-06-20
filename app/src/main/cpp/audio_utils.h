#ifndef AUDIO_UTILS_H
#define AUDIO_UTILS_H

#include <vector>

// Convert 16-bit PCM audio to float32 format
std::vector<float> convertToFloat32(const std::vector<int16_t>& audio16);

// Resample audio to target sample rate
std::vector<float> resampleAudio(const std::vector<float>& audio, int source_rate, int target_rate);

// Apply high-pass filter to remove low frequency noise
void highPassFilter(std::vector<float>& audio, float cutoff_freq, int sample_rate);

#endif // AUDIO_UTILS_H 