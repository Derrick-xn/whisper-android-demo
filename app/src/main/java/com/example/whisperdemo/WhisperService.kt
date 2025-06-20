package com.example.whisperdemo

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class WhisperService {
    
    companion object {
        private const val TAG = "WhisperService"
        
        init {
            try {
                System.loadLibrary("whisperdemo")
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }
    
    private var isModelLoaded = false
    
    // Native methods
    private external fun getVersion(): String
    private external fun loadModel(modelPath: String): Boolean
    private external fun transcribe(audioData: FloatArray, sampleRate: Int): String
    private external fun releaseModel()
    
    /**
     * 加载Whisper模型
     */
    suspend fun loadWhisperModel(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // 复制模型文件到内部存储
            val modelFile = copyModelToInternalStorage(context)
            if (modelFile == null) {
                return@withContext false
            }
            
            Log.i(TAG, "Loading model from: ${modelFile.absolutePath}")
            isModelLoaded = loadModel(modelFile.absolutePath)
            
            if (isModelLoaded) {
                Log.i(TAG, "Model loaded successfully")
            } else {
                Log.e(TAG, "Failed to load model")
            }
            
            return@withContext isModelLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model", e)
            return@withContext false
        }
    }
    
    /**
     * 转录音频数据
     */
    suspend fun transcribeAudio(audioData: FloatArray, sampleRate: Int = 16000): String = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            Log.w(TAG, "Model not loaded")
            return@withContext ""
        }
        
        try {
            val result = transcribe(audioData, sampleRate)
            Log.d(TAG, "Transcription result: $result")
            return@withContext result.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error during transcription", e)
            return@withContext ""
        }
    }
    
    /**
     * 释放模型资源
     */
    fun release() {
        if (isModelLoaded) {
            releaseModel()
            isModelLoaded = false
            Log.i(TAG, "Model released")
        }
    }
    
    /**
     * 复制模型文件到内部存储
     */
    private fun copyModelToInternalStorage(context: Context): File? {
        try {
            val modelFileName = "ggml-base-q8_0.bin"
            val modelFile = File(context.filesDir, modelFileName)
            
            Log.i(TAG, "Checking model file: ${modelFile.absolutePath}")
            
            // 检查assets中的模型文件
            val assetManager = context.assets
            val assetFiles = assetManager.list("")
            Log.i(TAG, "Assets files: ${assetFiles?.joinToString(", ")}")
            
            // 如果文件已存在且大小合理，直接返回
            if (modelFile.exists()) {
                val existingSize = modelFile.length()
                Log.i(TAG, "Existing file size: $existingSize bytes")
                // 检查文件大小是否合理 (应该大于50MB)
                if (existingSize > 50 * 1024 * 1024) {
                    Log.i(TAG, "Model file already exists in internal storage with reasonable size")
                    return modelFile
                } else {
                    Log.w(TAG, "Existing file size too small, re-copying...")
                    modelFile.delete()
                }
            }
            
            Log.i(TAG, "Copying model file from assets to internal storage...")
            Log.i(TAG, "Target path: ${modelFile.absolutePath}")
            
            // 确保父目录存在
            modelFile.parentFile?.mkdirs()
            
            // 直接使用InputStream复制，不使用openFd
            assetManager.open(modelFileName).use { input ->
                FileOutputStream(modelFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalCopied = 0L
                    var bytesRead: Int
                    
                    Log.i(TAG, "Starting file copy...")
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalCopied += bytesRead
                        
                        // 每10MB打印一次进度
                        if (totalCopied % (10 * 1024 * 1024) == 0L || totalCopied % (10 * 1024 * 1024) < 8192) {
                            Log.i(TAG, "Copied: ${totalCopied / (1024 * 1024)}MB")
                        }
                    }
                    
                    Log.i(TAG, "Total copied: ${totalCopied} bytes")
                    output.flush()
                }
            }
            
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not created after copy operation")
                return null
            }
            
            val finalSize = modelFile.length()
            Log.i(TAG, "Model file copied successfully: ${modelFile.absolutePath}")
            Log.i(TAG, "Final file size: $finalSize bytes (${finalSize / (1024 * 1024)}MB)")
            
            // 检查文件大小是否合理
            if (finalSize < 50 * 1024 * 1024) {
                Log.e(TAG, "Copied file size too small! Got: $finalSize bytes")
                return null
            }
            
            // 验证文件权限
            Log.i(TAG, "File permissions - readable: ${modelFile.canRead()}, writable: ${modelFile.canWrite()}")
            
            return modelFile
            
        } catch (e: IOException) {
            Log.e(TAG, "IOException while copying model file", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while copying model file", e)
            return null
        }
    }
    
    fun getVersionInfo(): String {
        return try {
            getVersion()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version", e)
            "Unknown"
        }
    }
} 