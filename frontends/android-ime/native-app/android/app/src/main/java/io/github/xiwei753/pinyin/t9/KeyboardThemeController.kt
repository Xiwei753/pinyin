package io.github.xiwei753.pinyin.t9

import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.widget.TextView

object ThemeColors {
    // Light theme colors
    const val LIGHT_BG = 0xFFF0F0F0.toInt()
    const val LIGHT_CANDIDATE_BAR = 0xFFFFFFFF.toInt()
    const val LIGHT_TEXT = 0xFF333333.toInt()
    const val LIGHT_SUB = 0xFF999999.toInt()
    const val LIGHT_PREEDIT_BG = 0xFFE8E8E8.toInt()
    const val LIGHT_TAB_ACTIVE_BG = 0xFFFFFFFF.toInt()
    const val LIGHT_TAB_INACTIVE_BG = 0xFFE0E0E0.toInt()
    const val LIGHT_TAB_ACTIVE_TEXT = 0xFF333333.toInt()
    const val LIGHT_TAB_INACTIVE_TEXT = 0xFF555555.toInt()
    const val LIGHT_KEY_BG = 0xFFFFFFFF.toInt()
    const val LIGHT_SPECIAL_KEY_BG = 0xFFEBEBEB.toInt()
    const val LIGHT_KEY_PRESSED = 0xFFE0E0E0.toInt()
    const val LIGHT_SPECIAL_KEY_PRESSED = 0xFFD6D6D6.toInt()

    // Dark theme colors
    const val DARK_BG = 0xFF121212.toInt()
    const val DARK_CANDIDATE_BAR = 0xFF1E1E1E.toInt()
    const val DARK_TEXT = 0xFFE0E0E0.toInt()
    const val DARK_SUB = 0xFF888888.toInt()
    const val DARK_PREEDIT_BG = 0xFF2A2A2A.toInt()
    const val DARK_TAB_ACTIVE_BG = 0xFF333333.toInt()
    const val DARK_TAB_INACTIVE_BG = 0xFF1E1E1E.toInt()
    const val DARK_TAB_ACTIVE_TEXT = 0xFFE0E0E0.toInt()
    const val DARK_TAB_INACTIVE_TEXT = 0xFF888888.toInt()
    const val DARK_KEY_BG = 0xFF333333.toInt()
    const val DARK_SPECIAL_KEY_BG = 0xFF2A2A2A.toInt()
    const val DARK_KEY_PRESSED = 0xFF444444.toInt()
    const val DARK_SPECIAL_KEY_PRESSED = 0xFF3A3A3A.toInt()
}

class KeyboardThemeController(
    private val settingsRepository: SettingsRepository,
    private val resources: Resources,
) {
    fun getThemePalette(): ThemePalette {
        val theme = settingsRepository.getTheme()
        val isDark = when (theme) {
            "dark" -> true
            "light" -> false
            else -> {
                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
        return if (isDark) {
            ThemePalette(
                bgColor = ThemeColors.DARK_BG,
                candidateBarColor = ThemeColors.DARK_CANDIDATE_BAR,
                textColor = ThemeColors.DARK_TEXT,
                subColor = ThemeColors.DARK_SUB,
                preeditBgColor = ThemeColors.DARK_PREEDIT_BG,
                symTabActiveBg = ThemeColors.DARK_TAB_ACTIVE_BG,
                symTabInactiveBg = ThemeColors.DARK_TAB_INACTIVE_BG,
                symTabActiveText = ThemeColors.DARK_TAB_ACTIVE_TEXT,
                symTabInactiveText = ThemeColors.DARK_TAB_INACTIVE_TEXT,
                isDark = true,
                keyBgColor = ThemeColors.DARK_KEY_BG,
                specialKeyBgColor = ThemeColors.DARK_SPECIAL_KEY_BG,
                keyPressedBgColor = ThemeColors.DARK_KEY_PRESSED,
                specialKeyPressedBgColor = ThemeColors.DARK_SPECIAL_KEY_PRESSED,
            )
        } else {
            ThemePalette(
                bgColor = ThemeColors.LIGHT_BG,
                candidateBarColor = ThemeColors.LIGHT_CANDIDATE_BAR,
                textColor = ThemeColors.LIGHT_TEXT,
                subColor = ThemeColors.LIGHT_SUB,
                preeditBgColor = ThemeColors.LIGHT_PREEDIT_BG,
                symTabActiveBg = ThemeColors.LIGHT_TAB_ACTIVE_BG,
                symTabInactiveBg = ThemeColors.LIGHT_TAB_INACTIVE_BG,
                symTabActiveText = ThemeColors.LIGHT_TAB_ACTIVE_TEXT,
                symTabInactiveText = ThemeColors.LIGHT_TAB_INACTIVE_TEXT,
                isDark = false,
                keyBgColor = ThemeColors.LIGHT_KEY_BG,
                specialKeyBgColor = ThemeColors.LIGHT_SPECIAL_KEY_BG,
                keyPressedBgColor = ThemeColors.LIGHT_KEY_PRESSED,
                specialKeyPressedBgColor = ThemeColors.LIGHT_SPECIAL_KEY_PRESSED,
            )
        }
    }

    fun applyTheme(v: KeyboardViews, palette: ThemePalette) {
        v.imeRoot.setBackgroundColor(palette.bgColor)
        v.candidateBar.setBackgroundColor(palette.candidateBarColor)
        v.pinyinFloatingBar.setBackgroundColor(palette.preeditBgColor)
        v.pinyinFloatingText.setTextColor(palette.textColor)

        for (i in 0 until v.candidateContainer.childCount) {
            (v.candidateContainer.getChildAt(i) as? android.widget.TextView)?.setTextColor(palette.textColor)
        }
    }

}
