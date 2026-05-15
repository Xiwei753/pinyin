package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("xiwei_t9_prefs", Context.MODE_PRIVATE)

    fun isHapticFeedbackEnabled(): Boolean {
        return prefs.getBoolean("haptic_feedback_enabled", true)
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
    }

    fun getCandidateCount(): Int {
        return prefs.getInt("candidate_count", 30)
    }

    fun setCandidateCount(count: Int) {
        prefs.edit().putInt("candidate_count", count).apply()
    }

    fun getTheme(): String {
        return prefs.getString("theme", "system") ?: "system"
    }

    fun setTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
    }
}
