package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

class HapticFeedbackManager(private val context: Context, private val settingsRepository: SettingsRepository) {

    private fun safePerformHaptic(view: View, constant: Int) {
        if (!settingsRepository.isHapticFeedbackEnabled()) return
        try {
            view.performHapticFeedback(constant)
        } catch (e: SecurityException) {
            // Ignore SecurityException for missing/denied vibration permissions on some ROMs
        } catch (e: RuntimeException) {
            // Ignore other unexpected runtime exceptions from system haptic service
        }
    }

    fun performTap(view: View) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            HapticFeedbackConstants.KEYBOARD_TAP
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        safePerformHaptic(view, constant)
    }

    fun performSpecialKey(view: View) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            HapticFeedbackConstants.KEYBOARD_PRESS
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        safePerformHaptic(view, constant)
    }

    fun performLongPress(view: View) {
        safePerformHaptic(view, HapticFeedbackConstants.LONG_PRESS)
    }
}
