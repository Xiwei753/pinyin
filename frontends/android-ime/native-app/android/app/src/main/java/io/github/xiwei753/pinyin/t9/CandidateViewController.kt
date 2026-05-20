package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class CandidateViewController(
    private val context: Context,
    private val v: KeyboardViews,
    private val keyBinder: KeyboardKeyBinder,
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
    )

    fun updateThemePalette(p: ThemePalette) {
        palette = p
    }

    fun setDictPreparing(preparing: Boolean) {
        isDictPreparing = preparing
    }

    fun refreshUi(handler: KeyboardActionHandler) {
        val preedit = handler.preedit
        val hasInput = handler.rawBuffer.isNotEmpty()
        val isT9Mode = handler.keyboardMode == KeyboardMode.ChineseT9 || handler.keyboardMode == KeyboardMode.EnglishT9

        if (isT9Mode && hasInput && preedit.isNotEmpty()) {
            v.pinyinFloatingBar.visibility = View.VISIBLE
            v.pinyinFloatingText.text = preedit
        } else {
            v.pinyinFloatingBar.visibility = View.GONE
        }

        val limit = settingsRepository.getCandidateCount()
        val candidates = handler.refreshCandidates(limit)

        if (candidates.isEmpty() && !isDictPreparing) {
            v.candidateContainer.visibility = View.GONE
        } else {
            v.candidateContainer.visibility = View.VISIBLE
        }

        if (isDictPreparing) {
            showPreparingState()
            return
        }

        for ((index, candidate) in candidates.withIndex()) {
            val btn: TextView = if (index < v.candidateContainer.childCount) {
                v.candidateContainer.getChildAt(index) as TextView
            } else {
                TextView(context).apply {
                    textSize = 18f
                    setTextColor(palette.textColor)
                    gravity = Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    background = context.getDrawable(R.drawable.candidate_bg)
                    isClickable = true
                    isFocusable = true
                }.also {
                    v.candidateContainer.addView(it, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    ).apply { setMargins(0, 0, 16, 0) })
                }
            }
            btn.visibility = View.VISIBLE
            btn.text = candidate.text
            val ci = index
            keyBinder.setupKey(btn, isSpecial = false) { handler.onCandidateClick(ci) }
        }
        for (i in candidates.size until v.candidateContainer.childCount) {
            v.candidateContainer.getChildAt(i).visibility = View.GONE
        }
        updateReadingRail(handler)
    }

    private fun showPreparingState() {
        for (i in 0 until v.candidateContainer.childCount) {
            v.candidateContainer.getChildAt(i).visibility = View.GONE
        }
        val btn: TextView = if (v.candidateContainer.childCount > 0) {
            (v.candidateContainer.getChildAt(0) as TextView).also { it.visibility = View.VISIBLE }
        } else {
            TextView(context).apply {
                textSize = 18f
                setTextColor(palette.textColor)
                gravity = Gravity.CENTER
                setPadding(32, 16, 32, 16)
                background = context.getDrawable(R.drawable.candidate_bg)
                isClickable = false
                isFocusable = false
            }.also {
                v.candidateContainer.addView(it, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { setMargins(0, 0, 16, 0) })
            }
        }
        btn.text = "词库准备中..."
        btn.setOnClickListener(null)
    }

    private fun updateReadingRail(handler: KeyboardActionHandler) {
        val readings = handler.readings
        val rawBuffer = handler.rawBuffer
        if (readings.isNotEmpty() && rawBuffer.isNotEmpty()) {
            for (tv in v.punctTextViews) tv.visibility = View.GONE
            var i = 0
            while (i < v.readingTextViews.size && i < readings.size) {
                val tv = v.readingTextViews[i]
                val reading = readings[i]
                tv.text = reading
                tv.visibility = View.VISIBLE
                if (reading == handler.activeReading) {
                    tv.setBackgroundResource(R.drawable.key_bg)
                    tv.alpha = 1.0f
                } else {
                    tv.setBackgroundResource(R.drawable.key_bg_special)
                    tv.alpha = 0.85f
                }
                i++
            }
            while (i < v.readingTextViews.size) {
                v.readingTextViews[i].visibility = View.GONE
                i++
            }
        } else {
            for (tv in v.punctTextViews) tv.visibility = View.VISIBLE
            for (tv in v.readingTextViews) tv.visibility = View.GONE
        }
    }

    fun resetUi() {
        v.pinyinFloatingBar.visibility = View.GONE
        v.candidateContainer.removeAllViews()
        v.candidateContainer.visibility = View.GONE
    }
}
