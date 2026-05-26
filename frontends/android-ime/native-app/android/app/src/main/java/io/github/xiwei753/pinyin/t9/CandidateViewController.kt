package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.imecore.ImeInputAction

enum class CandidateItemType {
    CANDIDATE,
    PREPARING,
    FUNCTION
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
    var onInputAction: ((ImeInputAction) -> Unit)? = null
    var onEditorAction: ((Int) -> Unit)? = null
    var onMoveCursor: ((Boolean) -> Unit)? = null

    private var lastState: KeyboardUiState? = null

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

    fun refreshFromState(state: KeyboardUiState) {
        lastState = state
        v.pinyinFloatingBar.visibility = View.GONE

        if (state.candidateStripState.isDictionaryPreparing || isDictPreparing) {
            try {
                v.candidateContainer.removeAllViews()
                val btn = createTextView("词库准备中...", CandidateItemType.PREPARING, null)
                v.candidateContainer.addView(btn)
            } catch (e: Exception) {}
            v.candidateContainer.visibility = View.VISIBLE
            return
        }

        try { v.candidateContainer.removeAllViews() } catch (e: Exception) {}

        val hasPreedit = state.preeditState.visible && state.preeditState.text.isNotEmpty()
        val hasCandidates = state.candidateStripState.candidates.isNotEmpty()

        if (hasPreedit || hasCandidates || state.rawBuffer.isNotEmpty()) {
            if (hasPreedit) {
                try {
                    val preeditChip = createTextView(state.preeditState.text, CandidateItemType.PREPARING, null)
                    preeditChip.isClickable = false
                    preeditChip.isFocusable = false
                    v.candidateContainer.addView(preeditChip)
                } catch (e: Exception) {}
            }

            for ((index, candidate) in state.candidateStripState.candidates.withIndex()) {
                try {
                    val chip = createTextView(candidate.text, CandidateItemType.CANDIDATE, index)
                    v.candidateContainer.addView(chip)
                } catch (e: Exception) {}
            }

            v.candidateContainer.visibility = if (hasPreedit || hasCandidates) {
                View.VISIBLE
            } else {
                View.GONE
            }
        } else {
            val functions = listOf("板" to "📋", "设" to "⚙", "选" to "⌨")
            try {
                val spacer = View(context)
                spacer.layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                v.candidateContainer.addView(spacer)
            } catch (e: Exception) {}
            try {
                val hideChip = createTextView("▽", CandidateItemType.FUNCTION, "⬇")
                v.candidateContainer.addView(hideChip)
            } catch (e: Exception) {}
            for ((label, payload) in functions) {
                try {
                    val chip = createTextView(label, CandidateItemType.FUNCTION, payload)
                    v.candidateContainer.addView(chip)
                } catch (e: Exception) {}
            }
            v.candidateContainer.visibility = View.VISIBLE
        }
    }

    private fun createTextView(
        text: String,
        type: CandidateItemType,
        payload: Any?,
    ): TextView {
        val tv = TextView(context).apply {
            textSize = palette.layoutTokens.preeditTextSize
            gravity = Gravity.CENTER
            this.text = text
            isSingleLine = true
        }

        val density = context.resources.displayMetrics.density
        val padH = (palette.layoutTokens.preeditBubblePadding * 2 * density).toInt()
        val padV = (palette.layoutTokens.preeditBubblePadding * density).toInt()
        tv.setPadding(padH, padV, padH, padV)

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(0, 0, (8 * density).toInt(), 0)
        }
        tv.layoutParams = lp

        when (type) {
            CandidateItemType.CANDIDATE -> {
                tv.setTextColor(palette.textColor)
                tv.background = getCandidateBg()
                tv.isClickable = true
                tv.isFocusable = true
                tv.setOnClickListener {
                    val index = payload as? Int
                    if (index != null) {
                        onInputAction?.invoke(ImeInputAction.CandidateSelected(index))
                    }
                }
            }
            CandidateItemType.PREPARING -> {
                tv.setTextColor(palette.textColor)
                tv.background = getCandidateBg()
                tv.isClickable = false
                tv.isFocusable = false
            }
            CandidateItemType.FUNCTION -> {
                tv.setTextColor(palette.textColor)
                tv.background = getCandidateBg()
                tv.isClickable = true
                tv.isFocusable = true
                tv.setOnClickListener {
                    val label = payload as? String
                    if (label != null) {
                        onFunctionChipClicked(label)
                    }
                }
            }
        }
        return tv
    }

    private fun onFunctionChipClicked(payload: String) {
        when (payload) {
            "⚙" -> {
                try {
                    val intent = android.content.Intent(context, SettingsActivity::class.java).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }
            "📋" -> {
                onInputAction?.invoke(ImeInputAction.KeyboardModeSelected(io.github.xiwei753.pinyin.imecore.InputMode.ClipboardPanel))
            }
            "⌨" -> {
                onInputAction?.invoke(ImeInputAction.KeyboardModeSelected(io.github.xiwei753.pinyin.imecore.InputMode.SelectionPanel))
            }
            "⬇" -> {
                onInputAction?.invoke(ImeInputAction.HideKeyboard)
            }
        }
    }

    private fun getCandidateBg(): Drawable? {
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
