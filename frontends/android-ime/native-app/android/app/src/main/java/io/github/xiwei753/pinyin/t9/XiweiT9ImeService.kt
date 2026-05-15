package io.github.xiwei753.pinyin.t9

import android.inputmethodservice.InputMethodService
import android.view.View

import android.widget.LinearLayout
import android.view.LayoutInflater
import android.widget.TextView
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary
import io.github.xiwei753.pinyin.t9.data.DictionaryManager

class XiweiT9ImeService : InputMethodService() {

    private lateinit var bufferText: TextView
    private lateinit var candidateContainer: LinearLayout

    private lateinit var dictionary: BuiltinDictionary
    private lateinit var engine: T9Engine
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private var currentCandidates: List<io.github.xiwei753.pinyin.t9.core.Candidate> = emptyList()

    override fun onCreateInputView(): View {
        settingsRepository = SettingsRepository(this)
        hapticFeedbackManager = HapticFeedbackManager(this, settingsRepository)
        dictionary = DictionaryManager.getInstance(this)
        engine = T9Engine(dictionary)

        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        bufferText = view.findViewById(R.id.buffer_text)
        candidateContainer = view.findViewById(R.id.candidate_container)

        setupKeys(view)

        applyThemeAndHeight(view)

        return view
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Re-apply theme and height when view is shown again, in case settings changed
        view?.let { applyThemeAndHeight(it) }
    }

    private var view: View? = null

    private fun applyThemeAndHeight(rootView: View) {
        this.view = rootView
        val theme = settingsRepository.getTheme()
        val isDark = when (theme) {
            "dark" -> true
            "light" -> false
            else -> {
                // system default
                val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }

        // Apply theme colors
        val bgColor = if (isDark) android.graphics.Color.parseColor("#121212") else android.graphics.Color.parseColor("#F0F0F0")
        val candidateBarColor = if (isDark) android.graphics.Color.parseColor("#1E1E1E") else android.graphics.Color.parseColor("#FFFFFF")
        val textColor = if (isDark) android.graphics.Color.parseColor("#E0E0E0") else android.graphics.Color.parseColor("#333333")

        rootView.setBackgroundColor(bgColor)
        rootView.findViewById<LinearLayout>(R.id.candidate_bar).setBackgroundColor(candidateBarColor)
        bufferText.setTextColor(if (isDark) android.graphics.Color.parseColor("#888888") else android.graphics.Color.parseColor("#888888"))

        val allKeys = listOf(
            R.id.key_1, R.id.key_2, R.id.key_3,
            R.id.key_4, R.id.key_5, R.id.key_6,
            R.id.key_7, R.id.key_8, R.id.key_9,
            R.id.key_star, R.id.key_0, R.id.key_del
        )

        val heightSetting = settingsRepository.getKeyboardHeight()
        val density = resources.displayMetrics.density
        val rowHeightPx = when (heightSetting) {
            "low" -> (48 * density).toInt()
            "high" -> (64 * density).toInt()
            else -> (56 * density).toInt() // normal
        }

        for (id in allKeys) {
            val keyView = rootView.findViewById<TextView>(id)
            keyView.setTextColor(textColor)
            // Change text of key 1 to indicate separator
            if (id == R.id.key_1) {
                keyView.text = "1\n分词"
            }
            // Adjust height
            val parent = keyView.parent as? android.widget.FrameLayout
            if (parent != null) {
                val params = parent.layoutParams
                params.height = rowHeightPx
                parent.layoutParams = params
            }
        }

        // Update candidates' text colors if any exist
        for (i in 0 until candidateContainer.childCount) {
            val child = candidateContainer.getChildAt(i) as? TextView
            child?.setTextColor(textColor)
        }
    }

    private fun setupKeys(view: View) {
        val numberKeys = mapOf(
            R.id.key_2 to "2",
            R.id.key_3 to "3",
            R.id.key_4 to "4",
            R.id.key_5 to "5",
            R.id.key_6 to "6",
            R.id.key_7 to "7",
            R.id.key_8 to "8",
            R.id.key_9 to "9"
        )

        for ((id, digit) in numberKeys) {
            view.findViewById<TextView>(id).setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                onDigitPressed(digit)
            }
        }

        view.findViewById<TextView>(R.id.key_1).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            if (engine.buffer.isNotEmpty()) {
                onDigitPressed("1")
            } else {
                // Future: symbol panel or nothing
            }
        }

        view.findViewById<TextView>(R.id.key_del).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            onDeletePressed()
        }

        view.findViewById<TextView>(R.id.key_0).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            onZeroPressed()
        }

        view.findViewById<TextView>(R.id.key_star).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            // Do nothing for now
        }
    }

    private fun onDigitPressed(digit: String) {
        engine.inputDigit(digit)
        updateUi()
    }

    private fun onDeletePressed() {
        if (engine.buffer.isNotEmpty()) {
            engine.backspace()
            updateUi()
        } else {
            // If buffer is empty, send a backspace to the app
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
        }
    }

    private fun onZeroPressed() {
        if (engine.buffer.isEmpty()) {
            currentInputConnection?.commitText(" ", 1)
        } else {
            if (currentCandidates.isNotEmpty()) {
                val candidateToCommit = currentCandidates[0]
                val committed = engine.commitCandidate(candidateToCommit)
                currentInputConnection?.commitText(committed.text, 1)
            } else {
                currentInputConnection?.commitText(engine.buffer, 1)
                engine.clear()
            }
            updateUi()
        }
    }

    private fun updateUi() {
        bufferText.text = engine.getPreedit()

        val limit = settingsRepository.getCandidateCount()
        currentCandidates = engine.getCandidates(limit)

        for ((index, candidate) in currentCandidates.withIndex()) {
            val btn: TextView = if (index < candidateContainer.childCount) {
                candidateContainer.getChildAt(index) as TextView
            } else {
                val newBtn = TextView(this).apply {
                    textSize = 18f
                    // Text color is updated in applyThemeAndHeight, but set a default here
                    val currentTheme = settingsRepository.getTheme()
                    val isDark = currentTheme == "dark" || (currentTheme == "system" && (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES))
                    setTextColor(if (isDark) android.graphics.Color.parseColor("#E0E0E0") else android.graphics.Color.parseColor("#333333"))
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    background = getDrawable(R.drawable.candidate_bg)
                    isClickable = true
                    isFocusable = true
                }
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(0, 0, 16, 0) // Right margin for spacing
                }
                candidateContainer.addView(newBtn, layoutParams)
                newBtn
            }

            btn.visibility = View.VISIBLE
            btn.text = candidate.text
            btn.setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                onCandidateClicked(index)
            }
        }

        // Hide unused views
        for (i in currentCandidates.size until candidateContainer.childCount) {
            candidateContainer.getChildAt(i).visibility = View.GONE
        }
    }

    private fun onCandidateClicked(index: Int) {
        if (index >= 0 && index < currentCandidates.size) {
            val candidateToCommit = currentCandidates[index]
            val committed = engine.commitCandidate(candidateToCommit)
            currentInputConnection?.commitText(committed.text, 1)
            updateUi()
        }
    }
}
