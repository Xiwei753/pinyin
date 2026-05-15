package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

class HapticFeedbackManager(private val context: Context, private val settingsRepository: SettingsRepository) {

    fun performTap(view: View) {
        if (!settingsRepository.isHapticFeedbackEnabled()) return

        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            HapticFeedbackConstants.KEYBOARD_TAP
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.performHapticFeedback(constant)
    }

    fun performSpecialKey(view: View) {
        if (!settingsRepository.isHapticFeedbackEnabled()) return

        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            HapticFeedbackConstants.KEYBOARD_PRESS
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.performHapticFeedback(constant)
    }

    fun performLongPress(view: View) {
        if (!settingsRepository.isHapticFeedbackEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
