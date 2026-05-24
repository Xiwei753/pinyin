package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.imecore.ImeInputAction
import org.json.JSONArray

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

enum class CandidateBarMode {
    EMPTY_STATE,
    CLIPBOARD,
    SELECTION
}

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

    private var currentBarMode = CandidateBarMode.EMPTY_STATE
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
                val btn = createTextView("词库准备中...", CandidateItemType.PREPARING, false, null)
                v.candidateContainer.addView(btn)
            } catch (e: Exception) {}
            v.candidateContainer.visibility = View.VISIBLE
            return
        }

        try { v.candidateContainer.removeAllViews() } catch (e: Exception) {}

        val hasPreedit = state.preeditState.visible && state.preeditState.text.isNotEmpty()
        val hasCandidates = state.candidateStripState.candidates.isNotEmpty()

        if (hasPreedit || hasCandidates || state.rawBuffer.isNotEmpty()) {
            currentBarMode = CandidateBarMode.EMPTY_STATE
            if (hasPreedit) {
                try {
                    val preeditChip = createTextView(state.preeditState.text, CandidateItemType.PREPARING, false, null)
                    preeditChip.isClickable = false
                    preeditChip.isFocusable = false
                    v.candidateContainer.addView(preeditChip)
                } catch (e: Exception) {}
            }

            for ((index, candidate) in state.candidateStripState.candidates.withIndex()) {
                try {
                    val chip = createTextView(candidate.text, CandidateItemType.CANDIDATE, false, index)
                    v.candidateContainer.addView(chip)
                } catch (e: Exception) {}
            }

            v.candidateContainer.visibility = if (hasPreedit || hasCandidates) {
                View.VISIBLE
            } else {
                View.GONE
            }
        } else {
            updateClipboardHistory()
            when (currentBarMode) {
                CandidateBarMode.EMPTY_STATE -> {
                    val functions = listOf("剪贴板", "设置", "选择", "，", "。", "？", "！", "：", "…")
                    for (func in functions) {
                        try {
                            val chip = createTextView(func, CandidateItemType.FUNCTION, false, func)
                            v.candidateContainer.addView(chip)
                        } catch (e: Exception) {}
                    }
                }
                CandidateBarMode.CLIPBOARD -> {
                    try {
                        val closeChip = createTextView("关闭", CandidateItemType.FUNCTION, false, "clipboard_close")
                        v.candidateContainer.addView(closeChip)
                    } catch (e: Exception) {}

                    val history = getClipboardHistory()
                    if (history.isEmpty()) {
                        try {
                            val emptyChip = createTextView("剪贴板为空", CandidateItemType.PREPARING, false, null)
                            v.candidateContainer.addView(emptyChip)
                        } catch (e: Exception) {}
                    } else {
                        for ((idx, text) in history.withIndex()) {
                            try {
                                val chip = createTextView(text, CandidateItemType.FUNCTION, false, "clip_item_$idx")
                                chip.isSingleLine = true
                                chip.ellipsize = android.text.TextUtils.TruncateAt.END
                                v.candidateContainer.addView(chip)
                            } catch (e: Exception) {}
                        }
                    }
                }
                CandidateBarMode.SELECTION -> {
                    val selectionChips = listOf("←", "→", "全选", "复制", "剪切", "粘贴", "关闭")
                    for (label in selectionChips) {
                        try {
                            val chip = createTextView(label, CandidateItemType.FUNCTION, false, "select_$label")
                            v.candidateContainer.addView(chip)
                        } catch (e: Exception) {}
                    }
                }
            }
            v.candidateContainer.visibility = View.VISIBLE
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
        when {
            payload == "设置" -> {
                try {
                    val intent = android.content.Intent(context, SettingsActivity::class.java).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Safe fallback
                }
            }
            payload == "选择" -> {
                currentBarMode = CandidateBarMode.SELECTION
                lastState?.let { refreshFromState(it) }
            }
            payload == "剪贴板" -> {
                currentBarMode = CandidateBarMode.CLIPBOARD
                lastState?.let { refreshFromState(it) }
            }
            payload == "clipboard_close" -> {
                currentBarMode = CandidateBarMode.EMPTY_STATE
                lastState?.let { refreshFromState(it) }
            }
            payload.startsWith("clip_item_") -> {
                val idx = payload.removePrefix("clip_item_").toIntOrNull()
                if (idx != null) {
                    val history = getClipboardHistory()
                    val text = history.getOrNull(idx)
                    if (!text.isNullOrEmpty()) {
                        onInputAction?.invoke(ImeInputAction.SymbolCommitted(text))
                        currentBarMode = CandidateBarMode.EMPTY_STATE
                        lastState?.let { refreshFromState(it) }
                    }
                }
            }
            payload.startsWith("select_") -> {
                val action = payload.removePrefix("select_")
                when (action) {
                    "←" -> onMoveCursor?.invoke(false)
                    "→" -> onMoveCursor?.invoke(true)
                    "全选" -> onEditorAction?.invoke(android.R.id.selectAll)
                    "复制" -> onEditorAction?.invoke(android.R.id.copy)
                    "剪切" -> onEditorAction?.invoke(android.R.id.cut)
                    "粘贴" -> onEditorAction?.invoke(android.R.id.paste)
                    "关闭" -> {
                        currentBarMode = CandidateBarMode.EMPTY_STATE
                        lastState?.let { refreshFromState(it) }
                    }
                }
            }
            payload in listOf("，", "。", "？", "！", "：", "…") -> {
                onInputAction?.invoke(ImeInputAction.SymbolCommitted(payload))
            }
        }
    }

    private fun updateClipboardHistory() {
        try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                val clip = clipboardManager.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text?.toString()
                    if (!text.isNullOrEmpty()) {
                        addTextToClipboardHistory(text)
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun addTextToClipboardHistory(text: String) {
        val list = getClipboardHistory().toMutableList()
        if (list.isNotEmpty() && list[0] == text) {
            return
        }
        list.remove(text)
        list.add(0, text)
        if (list.size > 20) {
            list.removeAt(list.size - 1)
        }
        saveClipboardHistory(list)
    }

    private fun getClipboardHistory(): List<String> {
        val sharedPrefs = context.getSharedPreferences("xiwei_clipboard_history", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("history", null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {}
        return list
    }

    private fun saveClipboardHistory(list: List<String>) {
        val sharedPrefs = context.getSharedPreferences("xiwei_clipboard_history", Context.MODE_PRIVATE)
        val jsonArray = JSONArray(list)
        sharedPrefs.edit().putString("history", jsonArray.toString()).apply()
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
        currentBarMode = CandidateBarMode.EMPTY_STATE
    }
}
