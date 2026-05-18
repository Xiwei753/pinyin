package io.github.xiwei753.pinyin.t9

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.widget.TextView
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary
import io.github.xiwei753.pinyin.t9.data.DictionaryManager

open class XiweiT9ImeService : InputMethodService() {

    private lateinit var bufferText: TextView
    private lateinit var candidateContainer: LinearLayout

    private lateinit var controller: T9ImeController
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var hapticFeedbackManager: HapticFeedbackManager

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
        if (!this::controller.isInitialized) {
            val dictionary = DictionaryManager.getProvider(this)
            val engine = T9Engine(dictionary)
            controller = T9ImeController(engine)
            T9DebugLogStore.initFileLogging(filesDir)
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

        if (controller.engine.buffer.isNotEmpty()) {
            currentInputConnection?.finishComposingText()
        }

        if (this::controller.isInitialized) {
            controller.reset()
        }
        resetUiStateIfReady()
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
                val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }

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
            else -> (56 * density).toInt()
        }

        for (id in allKeys) {
            val keyView = rootView.findViewById<TextView>(id)
            keyView.setTextColor(textColor)
            if (id == R.id.key_1) {
                keyView.text = "1\n分词"
            }
            val parent = keyView.parent as? android.widget.FrameLayout
            if (parent != null) {
                val params = parent.layoutParams
                params.height = rowHeightPx
                parent.layoutParams = params
            }
        }

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
            val result = controller.onSeparator()
            handleResult(result)
            if (result is T9ImeController.ActionResult.Refresh) {
                logAction("KEY_1", "syllable separation: appended separator to buffer '${controller.engine.buffer}'")
            } else {
                logAction("KEY_1", "buffer empty -> no-op")
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
        }
    }

    private fun onDigitPressed(digit: String) {
        controller.inputDigit(digit)
        refreshUi()
    }

    private fun onDeletePressed() {
        val result = controller.onDelete()
        when (result) {
            is T9ImeController.ActionResult.Refresh -> {
                logAction("KEY_DEL", "buffer '${controller.engine.buffer}' -> engine.backspace()")
                refreshUi()
            }
            is T9ImeController.ActionResult.SendDelete -> {
                logAction("KEY_DEL", "buffer empty -> send DEL key event to app")
                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
            }
            else -> {}
        }
    }

    private fun onZeroPressed() {
        val result = controller.onZero()
        when (result) {
            is T9ImeController.ActionResult.CommitText -> {
                logAction("KEY_0", "committing: ${result.text}")
                currentInputConnection?.commitText(result.text, 1)
                refreshUi()
            }
            is T9ImeController.ActionResult.Refresh -> {
                logAction("KEY_0", "no candidates, clearing engine")
                refreshUi()
            }
            else -> {}
        }
    }

    private fun onCandidateClicked(index: Int) {
        val result = controller.onCandidateClick(index)
        when (result) {
            is T9ImeController.ActionResult.CommitText -> {
                logAction("CANDIDATE_CLICK", "committing: ${result.text}")
                currentInputConnection?.commitText(result.text, 1)
                refreshUi()
            }
            else -> {}
        }
    }

    private fun handleResult(result: T9ImeController.ActionResult) {
        when (result) {
            is T9ImeController.ActionResult.Refresh -> refreshUi()
            is T9ImeController.ActionResult.CommitText -> {
                currentInputConnection?.commitText(result.text, 1)
                refreshUi()
            }
            is T9ImeController.ActionResult.SendDelete -> {
                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
            }
            is T9ImeController.ActionResult.NoAction -> {}
        }
    }

    private fun refreshUi() {
        bufferText.text = controller.preedit

        val limit = settingsRepository.getCandidateCount()
        val candidates = controller.refreshCandidates(limit)

        if (candidates.isEmpty()) {
            candidateContainer.visibility = View.GONE
        } else {
            candidateContainer.visibility = View.VISIBLE
        }

        for ((index, candidate) in candidates.withIndex()) {
            val btn: TextView = if (index < candidateContainer.childCount) {
                candidateContainer.getChildAt(index) as TextView
            } else {
                val newBtn = TextView(this).apply {
                    textSize = 18f
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
                    setMargins(0, 0, 16, 0)
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

        for (i in candidates.size until candidateContainer.childCount) {
            candidateContainer.getChildAt(i).visibility = View.GONE
        }

        logDebugInfo()
    }

    private fun logDebugInfo() {
        if (!settingsRepository.isDebugLoggingEnabled()) return

        val tag = "XiweiT9Debug"
        val engine = controller.engine
        debugLogger.log(tag, "================== T9 Debug Info ==================")
        debugLogger.log(tag, "raw buffer: ${engine.buffer}")
        debugLogger.log(tag, "getPreedit(): ${engine.getPreedit()}")
        debugLogger.log(tag, "currentCandidates isEmpty: ${controller.currentCandidates.isEmpty()}")
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

        val visibleCandidates = controller.currentCandidates.take(10)
        debugLogger.log(tag, "--- T9Engine visible candidates top 10 ---")
        visibleCandidates.forEachIndexed { i, cand ->
            debugLogger.log(tag, "  [$i] text: ${cand.text}, sourcePinyin: ${cand.sourcePinyin}, origin: ${cand.origin}, type: ${cand.type}, score: ${cand.score}, code: ${cand.code}")
        }
        debugLogger.log(tag, "===================================================")
    }

    private fun logAction(action: String, detail: String) {
        if (!settingsRepository.isDebugLoggingEnabled()) return
        debugLogger.log("XiweiT9Action", "$action: $detail")
    }
}
