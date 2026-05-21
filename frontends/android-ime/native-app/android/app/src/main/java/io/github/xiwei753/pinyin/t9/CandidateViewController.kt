package io.github.xiwei753.pinyin.t9

import android.content.Context
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
        btn.text = "\u8BCD\u5E93\u51C6\u5907\u4E2D..."
        btn.setOnClickListener(null)
    }

    private fun updateReadingRail(handler: KeyboardActionHandler) {
        val readings = handler.readings
        val rawBuffer = handler.rawBuffer
        val hasReadings = readings.isNotEmpty() && rawBuffer.isNotEmpty()
        val railSlotCount = v.readingTextViews.size

        // Show readings or punct
        // Readings first, then punct fill remaining visible slots
        if (hasReadings) {
            // Show readings in reading TextViews
            for (i in 0 until railSlotCount) {
                val readingTv = v.readingTextViews[i]
                if (i < readings.size) {
                    val reading = readings[i]
                    readingTv.text = reading
                    readingTv.visibility = View.VISIBLE
                    if (reading == handler.activeReading) {
                        readingTv.setBackgroundResource(R.drawable.key_bg)
                        readingTv.alpha = 1.0f
                    } else {
                        readingTv.setBackgroundResource(R.drawable.key_bg_special)
                        readingTv.alpha = 0.85f
                    }
                } else {
                    readingTv.visibility = View.GONE
                }
            }
            // Hide all punct
            for (tv in v.punctTextViews) tv.visibility = View.GONE
        } else {
            // Show punct
            for (tv in v.punctTextViews) tv.visibility = View.VISIBLE
            // Hide all readings
            for (i in 0 until railSlotCount) {
                v.readingTextViews[i].visibility = View.GONE
            }
        }
    }

    fun resetUi() {
        v.pinyinFloatingBar.visibility = View.GONE
        v.candidateContainer.removeAllViews()
        v.candidateContainer.visibility = View.GONE
    }
}
