package io.github.xiwei753.pinyin.t9

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Spinner
import android.widget.Toast
import android.widget.AdapterView
import android.view.View

class SettingsActivity : Activity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsRepository = SettingsRepository(this)

        val hapticSwitch = findViewById<Switch>(R.id.switch_haptic)
        hapticSwitch.isChecked = settingsRepository.isHapticFeedbackEnabled()
        hapticSwitch.isHapticFeedbackEnabled = false

        hapticSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.setHapticFeedbackEnabled(isChecked)
        }

        val debugLoggingSwitch = findViewById<Switch>(R.id.switch_debug_logging)
        debugLoggingSwitch.isChecked = settingsRepository.isDebugLoggingEnabled()

        debugLoggingSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.setDebugLoggingEnabled(isChecked)
        }

        val exportLogButton = findViewById<Button>(R.id.btn_export_log)
        exportLogButton.setOnClickListener { exportDebugLogs() }

        // Candidate count
        val candidateCountSpinner = findViewById<Spinner>(R.id.spinner_candidate_count)
        val candidateCountValues = resources.getStringArray(R.array.candidate_count_values)
        val currentCount = settingsRepository.getCandidateCount().toString()
        val countIndex = candidateCountValues.indexOf(currentCount)
        if (countIndex >= 0) candidateCountSpinner.setSelection(countIndex)

        candidateCountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                settingsRepository.setCandidateCount(candidateCountValues[position].toInt())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Theme
        val themeSpinner = findViewById<Spinner>(R.id.spinner_theme)
        val themeValues = resources.getStringArray(R.array.theme_values)
        val currentTheme = settingsRepository.getTheme()
        val themeIndex = themeValues.indexOf(currentTheme)
        if (themeIndex >= 0) themeSpinner.setSelection(themeIndex)

        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                settingsRepository.setTheme(themeValues[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Keyboard height
        val heightSpinner = findViewById<Spinner>(R.id.spinner_keyboard_height)
        val heightValues = resources.getStringArray(R.array.keyboard_height_values)
        val currentHeight = settingsRepository.getKeyboardHeight()
        val heightIndex = heightValues.indexOf(currentHeight)
        if (heightIndex >= 0) heightSpinner.setSelection(heightIndex)

        heightSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                settingsRepository.setKeyboardHeight(heightValues[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Dictionary Status
        val dictStatusText = findViewById<android.widget.TextView>(R.id.text_dict_status)
        val dictManager = io.github.xiwei753.pinyin.t9.data.DictionaryManager.getInstance(this)

        if (dictManager.isFallback) {
            dictStatusText.text = "词库加载失败，已回退到示例词库，${dictManager.loadedWordCount} 词"
            dictStatusText.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
        } else {
            dictStatusText.text = "内置词库已加载：rime-ice 来源，${dictManager.loadedWordCount} 词"
            dictStatusText.setTextColor(android.graphics.Color.parseColor("#388E3C"))
        }
    }

    private fun exportDebugLogs() {
        val logContent = T9DebugLogStore.dumpFromFile()
        if (logContent.isEmpty()) {
            Toast.makeText(this, "当前没有调试日志，请先打开调试日志开关并使用输入法", Toast.LENGTH_LONG).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, logContent)
        }
        startActivity(Intent.createChooser(shareIntent, "导出调试日志"))
    }
}
