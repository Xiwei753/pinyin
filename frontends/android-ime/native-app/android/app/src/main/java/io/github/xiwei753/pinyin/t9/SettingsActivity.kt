package io.github.xiwei753.pinyin.t9

import android.app.Activity
import android.os.Bundle
import android.widget.Switch

class SettingsActivity : Activity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsRepository = SettingsRepository(this)

        val hapticSwitch = findViewById<Switch>(R.id.switch_haptic)
        hapticSwitch.isChecked = settingsRepository.isHapticFeedbackEnabled()

        hapticSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.setHapticFeedbackEnabled(isChecked)
        }
    }
}
