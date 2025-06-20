package com.example.whisperdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.annotation.SuppressLint
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.whisperdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), AudioRecorder.AudioCallback {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var whisperService: WhisperService
    private lateinit var audioRecorder: AudioRecorder
    
    private var isModelLoaded = false
    private var isRecording = false
    
    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isPermissionGranted ->
        if (isPermissionGranted) {
            // 权限授予，可以开始录音
            startRecordingInternal()
        } else {
            // 权限被拒绝
            showToast("需要录音权限才能使用语音识别功能")
            updateUI()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeComponents()
        setupClickListeners()
        loadModel()
    }
    
    private fun initializeComponents() {
        whisperService = WhisperService()
        audioRecorder = AudioRecorder(this)
        audioRecorder.setCallback(this)
        
        // 初始化UI状态
        updateUI()
    }
    
    private fun setupClickListeners() {
        binding.recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
        
        binding.clearButton.setOnClickListener {
            clearResults()
        }
    }
    
    private fun loadModel() {
        binding.progressBar.visibility = View.VISIBLE
        binding.statusText.text = getString(R.string.loading_model)
        binding.recordButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val success = whisperService.loadWhisperModel(this@MainActivity)
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    
                    if (success) {
                        isModelLoaded = true
                        binding.statusText.text = getString(R.string.model_loaded)
                        binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.green))
                        showToast("模型加载成功！版本：${whisperService.getVersionInfo()}")
                    } else {
                        binding.statusText.text = getString(R.string.model_load_failed)
                        binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
                        showToast("模型加载失败，请检查模型文件")
                    }
                    
                    updateUI()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model", e)
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = getString(R.string.model_load_failed)
                    binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
                    showToast("模型加载异常：${e.message}")
                    updateUI()
                }
            }
        }
    }
    
    private fun startRecording() {
        if (!isModelLoaded) {
            showToast("模型未加载，无法开始录音")
            return
        }
        
        // 检查录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            // 请求权限
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            // 已有权限，直接开始录音
            startRecordingInternal()
        }
    }
    
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecordingInternal() {
        if (audioRecorder.startRecording()) {
            isRecording = true
            binding.statusText.text = "正在监听，请说话..."
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.blue))
            updateUI()
            Log.i(TAG, "Recording started")
        } else {
            showToast("启动录音失败")
        }
    }
    
    private fun stopRecording() {
        audioRecorder.stopRecording()
        isRecording = false
        binding.statusText.text = getString(R.string.model_loaded)
        binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.green))
        updateUI()
        Log.i(TAG, "Recording stopped")
    }
    
    private fun clearResults() {
        binding.resultText.text = getString(R.string.recognition_result)
    }
    
    private fun updateUI() {
        binding.recordButton.isEnabled = isModelLoaded
        
        if (isRecording) {
            binding.recordButton.text = getString(R.string.stop_recording)
            binding.recordButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        } else {
            binding.recordButton.text = getString(R.string.start_recording)
            binding.recordButton.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("音频处理设置")
        
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)
        
        // 获取当前设置
        val sharedPrefs = getSharedPreferences("whisper_settings", Context.MODE_PRIVATE)
        val enableDenoising = sharedPrefs.getBoolean("enable_denoising", true)
        val enableVoiceEnhancement = sharedPrefs.getBoolean("enable_voice_enhancement", true)
        val silenceThreshold = sharedPrefs.getInt("silence_threshold", 500)
        
        // 设置控件
        val denoisingSwitch = view.findViewById<Switch>(R.id.switch_denoising)
        val enhancementSwitch = view.findViewById<Switch>(R.id.switch_enhancement)
        val thresholdSeeker = view.findViewById<SeekBar>(R.id.seekbar_threshold)
        val thresholdText = view.findViewById<TextView>(R.id.text_threshold_value)
        
        denoisingSwitch.isChecked = enableDenoising
        enhancementSwitch.isChecked = enableVoiceEnhancement
        thresholdSeeker.progress = silenceThreshold / 10
        thresholdText.text = silenceThreshold.toString()
        
        thresholdSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress * 10
                thresholdText.text = value.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        builder.setView(view)
        builder.setPositiveButton("保存") { _, _ ->
            // 保存设置
            val editor = sharedPrefs.edit()
            editor.putBoolean("enable_denoising", denoisingSwitch.isChecked)
            editor.putBoolean("enable_voice_enhancement", enhancementSwitch.isChecked)
            editor.putInt("silence_threshold", thresholdSeeker.progress * 10)
            editor.apply()
            
            showToast("设置已保存")
        }
        builder.setNegativeButton("取消", null)
        builder.show()
    }
    
    // AudioRecorder.AudioCallback 实现
    override fun onAudioData(data: FloatArray) {
        Log.d(TAG, "Received audio data: ${data.size} samples")
        
        // 在后台线程处理转录
        lifecycleScope.launch {
            try {
                binding.statusText.text = getString(R.string.recognizing)
                binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.blue))
                
                val result = whisperService.transcribeAudio(data)
                
                runOnUiThread {
                    if (result.isNotEmpty()) {
                        // 流式显示 - 先显示当前结果，然后累积
                        val currentTime = System.currentTimeMillis()
                        val timestampedResult = "[$currentTime] $result"
                        
                        val currentText = binding.resultText.text.toString()
                        val newText = if (currentText == getString(R.string.recognition_result)) {
                            getString(R.string.recognition_result) + "\n\n" + timestampedResult
                        } else {
                            currentText + "\n\n" + timestampedResult
                        }
                        binding.resultText.text = newText
                        
                        // 滚动到底部
                        binding.scrollView.post {
                            binding.scrollView.fullScroll(View.FOCUS_DOWN)
                        }
                        
                        // 添加动画效果
                        binding.resultText.alpha = 0.7f
                        binding.resultText.animate()
                            .alpha(1.0f)
                            .setDuration(300)
                            .start()
                        
                        Log.i(TAG, "Transcription result: $result")
                    }
                    
                    // 恢复状态文本
                    if (isRecording) {
                        binding.statusText.text = "正在监听，请说话..."
                    } else {
                        binding.statusText.text = getString(R.string.model_loaded)
                        binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.green))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during transcription", e)
                runOnUiThread {
                    showToast("转录过程中出错：${e.message}")
                    binding.statusText.text = getString(R.string.model_loaded)
                    binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.green))
                }
            }
        }
    }
    
    override fun onVoiceDetected() {
        runOnUiThread {
            binding.statusText.text = "检测到语音，正在录制..."
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.green))
        }
        Log.d(TAG, "Voice detected")
    }
    
    override fun onSilenceDetected() {
        runOnUiThread {
            binding.statusText.text = "处理中..."
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.blue))
        }
        Log.d(TAG, "Silence detected")
    }
    
    override fun onError(error: String) {
        runOnUiThread {
            showToast(error)
            binding.statusText.text = getString(R.string.model_loaded)
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.green))
        }
        Log.e(TAG, "Audio error: $error")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.release()
        whisperService.release()
    }
} 