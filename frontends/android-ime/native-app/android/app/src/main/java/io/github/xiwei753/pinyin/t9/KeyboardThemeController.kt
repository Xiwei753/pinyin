package io.github.xiwei753.pinyin.t9

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
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
            )
        }
    }

    fun applyTheme(v: KeyboardViews, palette: ThemePalette) {
        v.imeRoot.setBackgroundColor(palette.bgColor)
        v.candidateBar.setBackgroundColor(palette.candidateBarColor)
        v.pinyinFloatingBar.setBackgroundColor(palette.preeditBgColor)
        v.pinyinFloatingText.setTextColor(palette.textColor)

        setTextColorOnAllKeys(v, palette.textColor, palette.subColor)
    }

    fun setTextColorOnAllKeys(v: KeyboardViews, textColor: Int, subColor: Int) {
        val allKeyTextIds = listOf(
            v.key1Text,
            v.key2Number, v.key2Letters,
            v.key3Number, v.key3Letters,
            v.key4Number, v.key4Letters,
            v.key5Number, v.key5Letters,
            v.key6Number, v.key6Letters,
            v.key7Number, v.key7Letters,
            v.key8Number, v.key8Letters,
            v.key9Number, v.key9Letters,
            v.punctTextViews[0], v.punctTextViews[1], v.punctTextViews[2], v.punctTextViews[3],
            v.readingTextViews[0], v.readingTextViews[1], v.readingTextViews[2], v.readingTextViews[3],
        )
        val subKeyIds = listOf(
            v.key2Number, v.key3Number, v.key4Number, v.key5Number,
            v.key6Number, v.key7Number, v.key8Number, v.key9Number,
        )
        for (tv in allKeyTextIds) {
            if (tv in subKeyIds) {
                tv.setTextColor(subColor)
            } else {
                tv.setTextColor(textColor)
            }
        }

        val extraTextViews = listOf(
            v.keyDel as? TextView, v.keyRetype as? TextView,
            v.keyToggleSymbol as? TextView, v.keyToggleNumber as? TextView,
            v.keySpace as? TextView, v.keyToggleEnglish,
            v.keyEnter as? TextView,
        )
        for (tv in extraTextViews) {
            tv?.setTextColor(textColor)
        }

        for ((_, symTv) in v.symTextViews) {
            symTv.setTextColor(textColor)
        }

        val symBottomTextViews = listOf(v.symBack as? TextView, v.symNumber as? TextView, v.symEnter as? TextView, v.symHide as? TextView)
        for (tv in symBottomTextViews) {
            tv?.setTextColor(textColor)
        }
        (v.symDel as? TextView)?.setTextColor(textColor)

        val numTextViews = listOf(v.num0, v.num1, v.num2, v.num3, v.num4, v.num5, v.num6, v.num7, v.num8, v.num9, v.numDot)
        for (tv in numTextViews) {
            tv.setTextColor(textColor)
        }
        val numBottomTextViews = listOf(v.numBack as? TextView, v.numSymbol as? TextView, v.numHide as? TextView, v.numEnter as? TextView)
        for (tv in numBottomTextViews) {
            tv?.setTextColor(textColor)
        }
        (v.numDel as? TextView)?.setTextColor(textColor)

        for (i in 0 until v.candidateContainer.childCount) {
            (v.candidateContainer.getChildAt(i) as? TextView)?.setTextColor(textColor)
        }
    }

    fun applySymbolTabColors(v: KeyboardViews, palette: ThemePalette, activeCategory: String) {
        val tabs = listOf(
            v.symTabPunct to "punct",
            v.symTabMath to "math",
            v.symTabBracket to "bracket",
            v.symTabOther to "other",
        )
        for ((tab, category) in tabs) {
            if (category == activeCategory) {
                tab.setBackgroundColor(palette.symTabActiveBg)
                tab.setTextColor(palette.symTabActiveText)
            } else {
                tab.setBackgroundColor(palette.symTabInactiveBg)
                tab.setTextColor(palette.symTabInactiveText)
            }
        }
    }
}
