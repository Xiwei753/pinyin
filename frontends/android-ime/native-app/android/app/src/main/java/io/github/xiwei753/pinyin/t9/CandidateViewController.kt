package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

enum class CandidateItemType {
    PREEDIT,
    READING,
    CANDIDATE,
    PREPARING
}

data class CandidateItem(
    val type: CandidateItemType,
    val text: String,
    val isActive: Boolean = false,
    val payload: Any? = null
)

class CandidateViewController(
    private val context: Context,
    private val v: KeyboardViews,
    private val themeController: KeyboardThemeController,
    private val settingsRepository: SettingsRepository,
) {
    private var isDictPreparing = false
    private var palette: ThemePalette = ThemePalette(
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

    fun updateThemePalette(p: ThemePalette) {
        palette = p
    }

    fun setDictPreparing(preparing: Boolean) {
        isDictPreparing = preparing
    }

    fun refreshUi(handler: KeyboardActionHandler) {
        val limit = settingsRepository.getCandidateCount()
        val candidates = handler.refreshCandidates(limit)
        val isComposing = handler.rawBuffer.isNotEmpty()
        val state = KeyboardUiState(
            keyboardMode = handler.keyboardMode,
            lastTextMode = handler.lastTextMode,
            rawBuffer = handler.rawBuffer,
            preedit = handler.preedit,
            readings = handler.readings,
            activeReading = handler.activeReading,
            candidatesSnapshot = candidates,
            currentSymCategory = "punct",
            isComposing = isComposing,
            themePalette = palette
        )
        refreshFromState(state, handler)
    }

    fun refreshFromState(state: KeyboardUiState, handler: KeyboardActionHandler) {
        val preedit = state.preedit
        val hasInput = state.rawBuffer.isNotEmpty()
        val isT9Mode = state.keyboardMode == KeyboardMode.ChineseT9 || state.keyboardMode == KeyboardMode.EnglishT9

        if (isT9Mode && hasInput && preedit.isNotEmpty()) {
            v.pinyinFloatingBar.visibility = View.VISIBLE
            v.pinyinFloatingText.text = preedit
        } else {
            v.pinyinFloatingBar.visibility = View.GONE
        }

        v.candidateContainer.removeAllViews()

        if (isDictPreparing) {
            val text = "\u8BCD\u5E93\u51C6\u5907\u4E2D..."
            val btn = createTextView(text, CandidateItemType.PREPARING, false, null, handler)
            v.candidateContainer.addView(btn)
            v.candidateContainer.visibility = View.VISIBLE
            return
        }

        val items = mutableListOf<CandidateItem>()
        if (hasInput) {
            if (preedit.isNotEmpty()) {
                items.add(CandidateItem(CandidateItemType.PREEDIT, preedit))
            }
            for (reading in state.readings) {
                val isActive = (reading == state.activeReading)
                items.add(CandidateItem(CandidateItemType.READING, reading, isActive = isActive, payload = reading))
            }
        }

        for ((index, candidate) in state.candidatesSnapshot.withIndex()) {
            items.add(CandidateItem(CandidateItemType.CANDIDATE, candidate.text, payload = index))
        }

        if (items.isEmpty()) {
            v.candidateContainer.visibility = View.GONE
        } else {
            v.candidateContainer.visibility = View.VISIBLE
            for (item in items) {
                val btn = createTextView(item.text, item.type, item.isActive, item.payload, handler)
                v.candidateContainer.addView(btn)
            }
        }
    }

    private fun createTextView(
        text: String,
        type: CandidateItemType,
        isActive: Boolean,
        payload: Any?,
        handler: KeyboardActionHandler
    ): TextView {
        val tv = TextView(context).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            this.text = text
            isSingleLine = true
        }

        val density = context.resources.displayMetrics.density
        val padH = (16 * density).toInt()
        val padV = (8 * density).toInt()
        tv.setPadding(padH, padV, padH, padV)

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(0, 0, (8 * density).toInt(), 0)
        }
        tv.layoutParams = lp

        when (type) {
            CandidateItemType.PREEDIT -> {
                tv.setTextColor(palette.textColor)
                tv.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 6f * density
                    setColor(palette.preeditBgColor)
                }
                tv.isClickable = false
                tv.isFocusable = false
            }
            CandidateItemType.READING -> {
                if (isActive) {
                    tv.setTextColor(palette.symTabActiveText)
                    tv.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 6f * density
                        setColor(palette.symTabActiveBg)
                    }
                } else {
                    tv.setTextColor(palette.textColor)
                    tv.background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 6f * density
                        setColor(palette.symTabInactiveBg)
                    }
                }
                tv.isClickable = true
                tv.isFocusable = true
                tv.setOnClickListener {
                    val reading = payload as? String
                    if (reading != null) {
                        handler.setActiveReading(reading)
                        handler.actionSink.refreshUi()
                    }
                }
            }
            CandidateItemType.CANDIDATE -> {
                tv.setTextColor(palette.textColor)
                tv.background = getCandidateBg()
                tv.isClickable = true
                tv.isFocusable = true
                tv.setOnClickListener {
                    val index = payload as? Int
                    if (index != null) {
                        handler.onCandidateClick(index)
                    }
                }
            }
            CandidateItemType.PREPARING -> {
                tv.setTextColor(palette.textColor)
                tv.background = getCandidateBg()
                tv.isClickable = false
                tv.isFocusable = false
            }
        }
        return tv
    }

    private fun getCandidateBg(): android.graphics.drawable.Drawable? {
        return try {
            androidx.core.content.ContextCompat.getDrawable(context, R.drawable.candidate_bg)
        } catch (e: Exception) {
            null
        }
    }

    fun resetUi() {
        v.pinyinFloatingBar.visibility = View.GONE
        v.candidateContainer.removeAllViews()
        v.candidateContainer.visibility = View.GONE
    }
}
