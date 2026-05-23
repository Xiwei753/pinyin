package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.imecore.ImeInputAction

enum class CandidateItemType {
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
    var onInputAction: ((ImeInputAction) -> Unit)? = null
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
        if (state.preeditState.visible) {
            v.pinyinFloatingBar.visibility = View.VISIBLE
            v.pinyinFloatingText.text = state.preeditState.text
        } else {
            v.pinyinFloatingBar.visibility = View.GONE
        }

        if (state.candidateStripState.isDictionaryPreparing || isDictPreparing) {
            val text = "词库准备中..."
            val btn = createTextView(text, CandidateItemType.PREPARING, false, null)
            v.candidateContainer.removeAllViews()
            v.candidateContainer.addView(btn)
            v.candidateContainer.visibility = View.VISIBLE
            return
        }

        val items = state.candidateStripState.candidates.mapIndexed { index, candidate ->
            CandidateItem(CandidateItemType.CANDIDATE, candidate.text, payload = index)
        }

        if (items.isEmpty()) {
            v.candidateContainer.visibility = View.GONE
        } else {
            v.candidateContainer.visibility = View.VISIBLE
            val childCount = v.candidateContainer.childCount

            for (i in items.indices) {
                val item = items[i]
                if (i < childCount) {
                    val tv = v.candidateContainer.getChildAt(i) as? TextView
                    if (tv != null) {
                        tv.text = item.text
                        tv.setOnClickListener {
                            val index = item.payload as? Int
                            if (index != null) {
                                onInputAction?.invoke(ImeInputAction.CandidateSelected(index))
                            }
                        }
                        tv.visibility = View.VISIBLE
                    }
                } else {
                    val btn = createTextView(item.text, item.type, item.isActive, item.payload)
                    v.candidateContainer.addView(btn)
                }
            }

            for (i in items.size until childCount) {
                val tv = v.candidateContainer.getChildAt(i)
                tv.visibility = View.GONE
            }
        }
    }

    private fun createTextView(
        text: String,
        type: CandidateItemType,
        isActive: Boolean,
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
