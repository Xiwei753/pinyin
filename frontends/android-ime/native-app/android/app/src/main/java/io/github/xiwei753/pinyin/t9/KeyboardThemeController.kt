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

        setTextColorOnAllKeys(v, palette.textColor, palette.subColor)
        applyKeyBackgrounds(v, palette)
    }

    fun setTextColorOnAllKeys(v: KeyboardViews, textColor: Int, subColor: Int) {
        val allKeyTextIds = mutableListOf<TextView>()
        allKeyTextIds.add(v.key1Text)
        allKeyTextIds.addAll(listOf(
            v.key2Number, v.key2Letters,
            v.key3Number, v.key3Letters,
            v.key4Number, v.key4Letters,
            v.key5Number, v.key5Letters,
            v.key6Number, v.key6Letters,
            v.key7Number, v.key7Letters,
            v.key8Number, v.key8Letters,
            v.key9Number, v.key9Letters,
        ))
        allKeyTextIds.addAll(v.punctTextViews)
        allKeyTextIds.addAll(v.readingTextViews)
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

        for (symView in v.generatedSymbolViews) {
            (symView as? android.widget.TextView)?.setTextColor(textColor)
        }

        val numBottomTextViews = emptyList<TextView>()
        for (tv in numBottomTextViews) {
            tv.setTextColor(textColor)
        }


        for (i in 0 until v.candidateContainer.childCount) {
            (v.candidateContainer.getChildAt(i) as? TextView)?.setTextColor(textColor)
        }
    }

    fun applyKeyBackgrounds(v: KeyboardViews, palette: ThemePalette) {
        val radiusPx = (14f * resources.displayMetrics.density).toInt()

        fun createBg(normalColor: Int, pressedColor: Int): android.graphics.drawable.StateListDrawable {
            val normalDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(normalColor)
                cornerRadius = radiusPx.toFloat()
            }
            val pressedDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(pressedColor)
                cornerRadius = radiusPx.toFloat()
            }
            return android.graphics.drawable.StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
                addState(intArrayOf(), normalDrawable)
            }
        }

        val normalBg = createBg(palette.keyBgColor, palette.keyPressedBgColor)
        val specialBg = createBg(palette.specialKeyBgColor, palette.specialKeyPressedBgColor)

        // Normal keys
        v.key1Text.background = normalBg
        v.t9Key2Frame.background = normalBg
        v.t9Key3Frame.background = normalBg
        v.t9Key4Frame.background = normalBg
        v.t9Key5Frame.background = normalBg
        v.t9Key6Frame.background = normalBg
        v.t9Key7Frame.background = normalBg
        v.t9Key8Frame.background = normalBg
        v.t9Key9Frame.background = normalBg

        v.num1.background = normalBg
        v.num2.background = normalBg
        v.num3.background = normalBg
        v.num4.background = normalBg
        v.num5.background = normalBg
        v.num6.background = normalBg
        v.num7.background = normalBg
        v.num8.background = normalBg
        v.num9.background = normalBg

        v.keySpace.background = normalBg

        // Symbol generated views
        for (symView in v.generatedSymbolViews) {
            symView.background = normalBg
        }

        // Special keys
        v.keyDel.background = specialBg
        v.keyRetype.background = specialBg
        v.keyEnter.background = specialBg
        v.keyToggleNumber.background = specialBg
        v.keyToggleEnglish.background = specialBg
        v.keyToggleSymbol.background = specialBg
        v.numDot.background = specialBg
        v.num0.background = specialBg
        v.leftScrollRail.background = specialBg
    }

    fun applySymbolTabColors(v: KeyboardViews, palette: ThemePalette, activeCategory: String) {
        val radiusPx = (14f * resources.displayMetrics.density).toInt()

        fun createTabBg(color: Int): android.graphics.drawable.GradientDrawable {
            return android.graphics.drawable.GradientDrawable().apply {
                setColor(color)
                cornerRadius = radiusPx.toFloat()
            }
        }

        val tabs = listOf(
            v.symTabPunct to "punct",
            v.symTabMath to "math",
            v.symTabBracket to "bracket",
            v.symTabOther to "other",
        )
        for ((tab, category) in tabs) {
            val textColor = if (category == activeCategory) palette.symTabActiveText else palette.symTabInactiveText
            val bgColor = if (category == activeCategory) palette.symTabActiveBg else palette.symTabInactiveBg
            tab.background = createTabBg(bgColor)
            tab.setTextColor(textColor)
        }
    }
}
