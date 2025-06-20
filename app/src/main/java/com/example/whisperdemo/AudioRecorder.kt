package com.example.whisperdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.math.abs

class AudioRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
        
        // 音频处理参数
        private const val SILENCE_THRESHOLD = 500
        private const val MIN_AUDIO_LENGTH_MS = 1000 // 最小音频长度1秒
        private const val MAX_AUDIO_LENGTH_MS = 30000 // 最大音频长度30秒
    }
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    
    private val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
    private val audioBuffer = mutableListOf<Short>()
    private var silenceCounter = 0
    private var hasDetectedVoice = false
    
    interface AudioCallback {
        fun onAudioData(data: FloatArray)
        fun onVoiceDetected()
        fun onSilenceDetected()
        fun onError(error: String)
    }
    
    private var callback: AudioCallback? = null
    
    fun setCallback(callback: AudioCallback) {
        this.callback = callback
    }
    
    /**
     * 检查录音权限
     */
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 开始录音
     */
    fun startRecording(): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }
        
        if (!hasRecordPermission()) {
            callback?.onError("录音权限未授予")
            return false
        }
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                callback?.onError("音频录制器初始化失败")
                return false
            }
            
            audioRecord?.startRecording()
            isRecording = true
            
            // 重置状态
            audioBuffer.clear()
            silenceCounter = 0
            hasDetectedVoice = false
            
            // 开始录音循环
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                recordingLoop()
            }
            
            Log.i(TAG, "Recording started")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            callback?.onError("启动录音失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 停止录音
     */
    fun stopRecording() {
        if (!isRecording) {
            return
        }
        
        isRecording = false
        recordingJob?.cancel()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            // 处理最后的音频数据 - 使用协程
            if (audioBuffer.isNotEmpty() && hasDetectedVoice) {
                CoroutineScope(Dispatchers.IO).launch {
                    processAudioBuffer()
                }
            }
            
            Log.i(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }
    
    /**
     * 录音循环
     */
    private suspend fun recordingLoop() {
        val buffer = ShortArray(bufferSize / 2)
        
        while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    processAudioChunk(buffer, bytesRead)
                }
                
                delay(10) // 短暂延时避免过度占用CPU
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in recording loop", e)
                withContext(Dispatchers.Main) {
                    callback?.onError("录音过程中出错: ${e.message}")
                }
                break
            }
        }
    }
    
    /**
     * 处理音频数据块
     */
    private suspend fun processAudioChunk(buffer: ShortArray, length: Int) {
        // 转换为float数组用于更精确的分析
        val floatBuffer = FloatArray(length)
        for (i in 0 until length) {
            floatBuffer[i] = buffer[i] / 32768.0f
        }
        
        // 使用改进的VAD算法
        val isVoiceDetected = detectVoiceActivityNative(floatBuffer, SAMPLE_RATE)
        
        // 检测音量（作为备用）
        var sum = 0L
        for (i in 0 until length) {
            sum += abs(buffer[i].toInt())
        }
        val averageAmplitude = sum / length
        
        // 组合判断：使用VAD结果和音量阈值
        val hasVoice = isVoiceDetected || (averageAmplitude > SILENCE_THRESHOLD)
        
        if (hasVoice) {
            // 检测到语音
            silenceCounter = 0
            if (!hasDetectedVoice) {
                hasDetectedVoice = true
                withContext(Dispatchers.Main) {
                    callback?.onVoiceDetected()
                }
            }
            
            // 添加音频数据到缓冲区
            for (i in 0 until length) {
                audioBuffer.add(buffer[i])
            }
            
        } else {
            // 检测到静音
            silenceCounter++
            
            // 如果已经检测到语音且静音时间足够长，处理音频
            if (hasDetectedVoice && silenceCounter > 30) { // 减少到0.3秒的静音
                withContext(Dispatchers.Main) {
                    callback?.onSilenceDetected()
                }
                
                if (audioBuffer.isNotEmpty()) {
                    processAudioBuffer()
                }
                
                // 重置状态
                audioBuffer.clear()
                hasDetectedVoice = false
                silenceCounter = 0
            }
        }
        
        // 检查音频缓冲区大小，避免过长
        val currentLengthMs = audioBuffer.size * 1000 / SAMPLE_RATE
        if (currentLengthMs > MAX_AUDIO_LENGTH_MS) {
            Log.w(TAG, "Audio buffer too long, processing...")
            processAudioBuffer()
            audioBuffer.clear()
            hasDetectedVoice = false
        }
    }
    
    /**
     * 处理音频缓冲区
     */
    private suspend fun processAudioBuffer() {
        if (audioBuffer.isEmpty()) return
        
        val lengthMs = audioBuffer.size * 1000 / SAMPLE_RATE
        if (lengthMs < MIN_AUDIO_LENGTH_MS) {
            Log.d(TAG, "Audio too short (${lengthMs}ms), skipping")
            return
        }
        
        // 转换为Float数组
        val floatArray = FloatArray(audioBuffer.size)
        for (i in audioBuffer.indices) {
            floatArray[i] = audioBuffer[i] / 32768.0f
        }
        
        withContext(Dispatchers.Main) {
            callback?.onAudioData(floatArray)
        }
        
        Log.d(TAG, "Processed audio buffer: ${audioBuffer.size} samples (${lengthMs}ms)")
    }
    
    /**
     * 获取当前录音状态
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * 释放资源
     */
    fun release() {
        stopRecording()
    }
    
    // 添加native方法声明
    private external fun detectVoiceActivityNative(audioData: FloatArray, sampleRate: Int): Boolean
    
    companion object {
        init {
            try {
                System.loadLibrary("whisperdemo")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library for VAD", e)
            }
        }
    }
} 