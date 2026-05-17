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

    // Made internal and mutable for unit testing injection
    internal var debugLogger: T9DebugLogger = AndroidDebugLogger()

    override fun onCreate() {
        super.onCreate()
        ensureCoreInitialized()
    }

    private fun ensureCoreInitialized() {
        if (!this::settingsRepository.isInitialized) {
            settingsRepository = SettingsRepository(this)
        }
        if (!this::hapticFeedbackManager.isInitialized) {
            hapticFeedbackManager = HapticFeedbackManager(this, settingsRepository)
        }
        if (!this::dictionary.isInitialized) {
            dictionary = DictionaryManager.getInstance(this)
        }
        if (!this::engine.isInitialized) {
            engine = T9Engine(dictionary)
        }
    }

    override fun onCreateInputView(): View {
        ensureCoreInitialized()

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
        resetCompositionState()
    }

    override fun onStartInput(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        resetCompositionState()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        resetCompositionState()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        resetCompositionState()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        resetCompositionState()
    }

    private fun resetCompositionState() {
        ensureCoreInitialized()

        // Finish any composing text logic
        if (engine.buffer.isNotEmpty()) {
            // According to Android docs, when we reset state, we shouldn't commit partial buffer
            // to the app unless they explicitly picked it, but we can call finishComposingText()
            // to clear any pre-edit styling if we used InputConnection.setComposingText.
            // Since we use our own custom view for pre-edit currently, just clearing our own buffer is fine.
            // But if we ever rely on setComposingText, this clears it from the app's side safely.
            currentInputConnection?.finishComposingText()
        }

        resetEngineState()
        resetUiStateIfReady()
    }

    private fun resetEngineState() {
        if (this::engine.isInitialized) {
            engine.clear()
        }
        currentCandidates = emptyList()
    }

    private fun resetUiStateIfReady() {
        if (this::bufferText.isInitialized) {
            bufferText.text = ""
        }
        if (this::candidateContainer.isInitialized) {
            candidateContainer.removeAllViews()
            candidateContainer.visibility = View.GONE
        }
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
        currentCandidates = engine.getVisibleCandidates(limit)

        if (currentCandidates.isEmpty()) {
            candidateContainer.visibility = View.GONE
        } else {
            candidateContainer.visibility = View.VISIBLE
        }

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

        logDebugInfo()
    }

    private fun logDebugInfo() {
        if (!settingsRepository.isDebugLoggingEnabled()) return

        val tag = "XiweiT9Debug"
        debugLogger.log(tag, "================== T9 Debug Info ==================")
        debugLogger.log(tag, "raw buffer: ${engine.buffer}")
        debugLogger.log(tag, "getPreedit(): ${engine.getPreedit()}")
        debugLogger.log(tag, "currentCandidates isEmpty: ${currentCandidates.isEmpty()}")
        debugLogger.log(tag, "candidateContainer visibility: ${if (candidateContainer.visibility == View.VISIBLE) "VISIBLE" else "GONE"}")

        val compositions = engine.getCompositions().take(10)
        debugLogger.log(tag, "--- T9PinyinComposer top 10 compositions ---")
        compositions.forEachIndexed { i, comp ->
            debugLogger.log(tag, "  [$i] pinyinString: ${comp.pinyinString}, score: ${comp.score}, isComplete: ${comp.isComplete}, segmentDigits: ${comp.segmentDigits}")
        }

        val internalCandidates = engine.getInternalCandidates().take(10)
        debugLogger.log(tag, "--- T9Engine internal candidates top 10 ---")
        internalCandidates.forEachIndexed { i, cand ->
            debugLogger.log(tag, "  [$i] text: ${cand.text}, sourcePinyin: ${cand.sourcePinyin}, origin: ${cand.origin}, type: ${cand.type}, score: ${cand.score}, code: ${cand.code}")
        }

        val visibleCandidates = currentCandidates.take(10)
        debugLogger.log(tag, "--- T9Engine visible candidates top 10 ---")
        visibleCandidates.forEachIndexed { i, cand ->
            debugLogger.log(tag, "  [$i] text: ${cand.text}, sourcePinyin: ${cand.sourcePinyin}, origin: ${cand.origin}, type: ${cand.type}, score: ${cand.score}, code: ${cand.code}")
        }
        debugLogger.log(tag, "===================================================")
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
