package io.github.xiwei753.pinyin.t9

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryManager
import io.github.xiwei753.pinyin.t9.data.DictionaryState
import io.github.xiwei753.pinyin.t9.data.DictionaryStateListener
import io.github.xiwei753.pinyin.t9.data.UserDictionary

open class XiweiT9ImeService : InputMethodService(), DictionaryStateListener {

    private lateinit var bufferText: TextView
    private lateinit var candidateContainer: LinearLayout
    private lateinit var panelT9: View
    private lateinit var panelSymbol: View
    private lateinit var panelNumber: View

    private lateinit var controller: T9ImeController
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private var userDictionary: UserDictionary? = null
    private var currentEditorInfo: EditorInfo? = null

    internal var debugLogger: T9DebugLogger = AndroidDebugLogger()
    private var isDictPreparing = false
    private var keyboardMode = KeyboardMode.ChineseT9

    private val englishMultiTapLetters = mapOf(
        '2' to "abc", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mno", '7' to "pqrs", '8' to "tuv", '9' to "wxyz"
    )
    private var englishDigit = ' '
    private var englishIndex = 0
    private val englishTimer by lazy { Handler(Looper.getMainLooper()) }
    private val englishTimeoutMs = 600L
    private var englishPending = false
    private val englishTimeoutRunnable = Runnable { commitEnglishChar() }

    private val deleteHandler by lazy { Handler(Looper.getMainLooper()) }
    private var deleteLongPressRunning = false
    private val deleteRepeatDelay = 80L

    override fun onCreate() {
        super.onCreate()
        try {
            userDictionary = UserDictionary.getInstance(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("XiweiT9ImeService", "Failed to init user dictionary", e)
            userDictionary = null
        }
        ensureCoreInitialized()
    }

    override fun onDestroy() {
        super.onDestroy()
        DictionaryManager.unregisterListener(this)
        englishTimer.removeCallbacks(englishTimeoutRunnable)
        deleteHandler.removeCallbacksAndMessages(null)
    }

    override fun onStateChanged(state: DictionaryState) {
        Handler(Looper.getMainLooper()).post {
            isDictPreparing = state is DictionaryState.Preparing
            when (state) {
                is DictionaryState.Ready -> {
                    if (this::controller.isInitialized) {
                        val engine = T9Engine(state.dictionary, userDictionary)
                        controller.attachEngine(engine)
                        if (this::bufferText.isInitialized) refreshUi()
                    }
                }
                is DictionaryState.Fallback -> {
                    if (this::controller.isInitialized) {
                        Thread {
                            val dict = DictionaryManager.getProviderBlocking(this)
                            Handler(Looper.getMainLooper()).post {
                                val engine = T9Engine(dict, userDictionary)
                                controller.attachEngine(engine)
                                if (this::bufferText.isInitialized) refreshUi()
                            }
                        }.start()
                    }
                }
                else -> {}
            }
        }
    }

    private fun ensureCoreInitialized() {
        if (!this::settingsRepository.isInitialized) {
            settingsRepository = SettingsRepository(this)
        }
        if (!this::hapticFeedbackManager.isInitialized) {
            hapticFeedbackManager = HapticFeedbackManager(this, settingsRepository)
        }
        if (!this::controller.isInitialized) {
            controller = T9ImeController(null)
            T9DebugLogStore.initFileLogging(filesDir)
            DictionaryManager.registerListener(this)
            DictionaryManager.prepareAsync(this)
            val readyDict = DictionaryManager.getReadyProviderOrNull()
            if (readyDict != null) {
                controller.attachEngine(T9Engine(readyDict, userDictionary))
            }
        }
    }

    override fun onCreateInputView(): View {
        ensureCoreInitialized()
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        bufferText = view.findViewById(R.id.buffer_text)
        candidateContainer = view.findViewById(R.id.candidate_container)
        panelT9 = view.findViewById(R.id.panel_t9)
        panelSymbol = view.findViewById(R.id.panel_symbol)
        panelNumber = view.findViewById(R.id.panel_number)
        setupKeys(view)
        applyThemeAndHeight(view)
        updateKeyboardPanel()
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditorInfo = info
        view?.let { applyThemeAndHeight(it) }
        resetCompositionState()
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        currentEditorInfo = info
        resetCompositionState()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        resetCompositionState()
        keyboardMode = KeyboardMode.ChineseT9
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
        if (controller.rawBuffer.isNotEmpty()) {
            currentInputConnection?.finishComposingText()
        }
        if (this::controller.isInitialized) controller.reset()
        resetUiStateIfReady()
    }

    private fun resetUiStateIfReady() {
        if (this::bufferText.isInitialized) bufferText.text = ""
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

        val allT9 = listOf(
            R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5, R.id.key_6,
            R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_toggle_symbol, R.id.key_0, R.id.key_del,
            R.id.key_toggle_english, R.id.key_toggle_number, R.id.key_hide, R.id.key_enter
        )
        val heightSetting = settingsRepository.getKeyboardHeight()
        val density = resources.displayMetrics.density
        val rowHeightPx = when (heightSetting) {
            "low" -> (44 * density).toInt()
            "high" -> (56 * density).toInt()
            else -> (48 * density).toInt()
        }
        for (id in allT9) {
            val keyView = rootView.findViewById<TextView>(id)
            if (keyView != null) {
                keyView.setTextColor(textColor)
                val parent = keyView.parent as? android.widget.FrameLayout
                if (parent != null) {
                    val params = parent.layoutParams
                    params.height = rowHeightPx
                    parent.layoutParams = params
                }
            }
        }

        val allSymbolKeys = listOf(R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5, R.id.sym_6,
            R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_10, R.id.sym_11, R.id.sym_12,
            R.id.sym_13, R.id.sym_14, R.id.sym_15, R.id.sym_16, R.id.sym_17, R.id.sym_18,
            R.id.sym_19, R.id.sym_20, R.id.sym_21, R.id.sym_22, R.id.sym_23, R.id.sym_24,
            R.id.sym_25, R.id.sym_26, R.id.sym_27, R.id.sym_28, R.id.sym_29, R.id.sym_30)
        for (id in allSymbolKeys) {
            rootView.findViewById<TextView>(id)?.setTextColor(textColor)
        }

        for (i in 0 until candidateContainer.childCount) {
            (candidateContainer.getChildAt(i) as? TextView)?.setTextColor(textColor)
        }
    }

    private fun updateKeyboardPanel() {
        panelT9.visibility = if (keyboardMode == KeyboardMode.ChineseT9 || keyboardMode == KeyboardMode.EnglishT9) View.VISIBLE else View.GONE
        panelSymbol.visibility = if (keyboardMode == KeyboardMode.Symbol) View.VISIBLE else View.GONE
        panelNumber.visibility = if (keyboardMode == KeyboardMode.Number) View.VISIBLE else View.GONE

        val toggleEnglish = view?.findViewById<TextView>(R.id.key_toggle_english)
        if (toggleEnglish != null) {
            toggleEnglish.text = if (keyboardMode == KeyboardMode.EnglishT9) "英" else "中"
        }
    }

    private fun setupKeys(view: View) {
        // Chinese T9 digit keys (2-9)
        val numberKeys = mapOf(
            R.id.key_2 to "2", R.id.key_3 to "3", R.id.key_4 to "4",
            R.id.key_5 to "5", R.id.key_6 to "6", R.id.key_7 to "7",
            R.id.key_8 to "8", R.id.key_9 to "9"
        )
        for ((id, digit) in numberKeys) {
            view.findViewById<TextView>(id).setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                if (keyboardMode == KeyboardMode.EnglishT9) {
                    onEnglishDigit(digit)
                } else {
                    onDigitPressed(digit)
                }
            }
        }

        view.findViewById<TextView>(R.id.key_1).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            if (keyboardMode == KeyboardMode.EnglishT9) {
                onEnglishDigit("1")
            } else {
                val result = controller.onSeparator()
                handleResult(result)
                if (result is T9ImeController.ActionResult.Refresh) {
                    logAction("KEY_1", "separation")
                }
            }
        }

        val delKey = view.findViewById<TextView>(R.id.key_del)
        delKey.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            onDeletePressed()
        }
        delKey.setOnLongClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            startDeleteLongPress()
            true
        }
        delKey.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                stopDeleteLongPress()
            }
            false
        }

        view.findViewById<TextView>(R.id.key_0).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            if (keyboardMode == KeyboardMode.EnglishT9) {
                currentInputConnection?.commitText(" ", 1)
            } else {
                onZeroPressed()
            }
        }

        view.findViewById<TextView>(R.id.key_toggle_symbol).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            keyboardMode = KeyboardMode.Symbol
            updateKeyboardPanel()
            resetUiStateIfReady()
        }

        view.findViewById<TextView>(R.id.key_toggle_english).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            if (keyboardMode == KeyboardMode.EnglishT9) {
                commitEnglishChar()
                keyboardMode = KeyboardMode.ChineseT9
            } else {
                if (controller.rawBuffer.isNotEmpty()) {
                    commitFirstCandidateOrPreedit()
                }
                keyboardMode = KeyboardMode.EnglishT9
            }
            updateKeyboardPanel()
        }

        view.findViewById<TextView>(R.id.key_toggle_number).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            keyboardMode = KeyboardMode.Number
            updateKeyboardPanel()
            resetUiStateIfReady()
        }

        view.findViewById<TextView>(R.id.key_hide).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            resetCompositionState()
            requestHideSelf(0)
        }

        view.findViewById<TextView>(R.id.key_enter).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            onEnterPressed()
        }

        // Symbol panel keys
        val symbolTexts = listOf(
            R.id.sym_1 to "，", R.id.sym_2 to "。", R.id.sym_3 to "？", R.id.sym_4 to "！",
            R.id.sym_5 to "：", R.id.sym_6 to "；", R.id.sym_7 to "、", R.id.sym_8 to "“",
            R.id.sym_9 to "”", R.id.sym_10 to "‘", R.id.sym_11 to "’", R.id.sym_12 to "（",
            R.id.sym_13 to "）", R.id.sym_14 to "《", R.id.sym_15 to "》", R.id.sym_16 to "—",
            R.id.sym_17 to "…", R.id.sym_18 to "@", R.id.sym_19 to "#", R.id.sym_20 to "￥",
            R.id.sym_21 to "%", R.id.sym_22 to "&", R.id.sym_23 to "*", R.id.sym_24 to "+",
            R.id.sym_25 to "-", R.id.sym_26 to "=", R.id.sym_27 to "/", R.id.sym_28 to "\\",
            R.id.sym_29 to "<", R.id.sym_30 to ">"
        )
        for ((id, text) in symbolTexts) {
            view.findViewById<TextView>(id).setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                currentInputConnection?.commitText(text, 1)
                keyboardMode = KeyboardMode.ChineseT9
                updateKeyboardPanel()
                refreshUi()
            }
        }

        view.findViewById<TextView>(R.id.sym_back).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            keyboardMode = KeyboardMode.ChineseT9
            updateKeyboardPanel()
            refreshUi()
        }
        view.findViewById<TextView>(R.id.sym_number).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            keyboardMode = KeyboardMode.Number
            updateKeyboardPanel()
        }
        view.findViewById<TextView>(R.id.sym_del).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            sendSystemDelete()
        }
        view.findViewById<TextView>(R.id.sym_enter).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            onEnterPressed()
        }
        view.findViewById<TextView>(R.id.sym_hide).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            requestHideSelf(0)
        }
        view.findViewById<TextView>(R.id.sym_more).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            keyboardMode = KeyboardMode.Symbol
            updateKeyboardPanel()
        }

        // Number panel keys
        val numKeys = listOf(
            R.id.num_1 to "1", R.id.num_2 to "2", R.id.num_3 to "3",
            R.id.num_4 to "4", R.id.num_5 to "5", R.id.num_6 to "6",
            R.id.num_7 to "7", R.id.num_8 to "8", R.id.num_9 to "9",
            R.id.num_0 to "0", R.id.num_dot to "."
        )
        for ((id, text) in numKeys) {
            view.findViewById<TextView>(id).setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                currentInputConnection?.commitText(text, 1)
            }
        }

        view.findViewById<TextView>(R.id.num_del).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            sendSystemDelete()
        }
        view.findViewById<TextView>(R.id.num_back).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            keyboardMode = KeyboardMode.ChineseT9
            updateKeyboardPanel()
            refreshUi()
        }
        view.findViewById<TextView>(R.id.num_symbol).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            keyboardMode = KeyboardMode.Symbol
            updateKeyboardPanel()
        }
        view.findViewById<TextView>(R.id.num_hide).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            requestHideSelf(0)
        }
        view.findViewById<TextView>(R.id.num_enter).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            onEnterPressed()
        }
    }

    private fun onEnglishDigit(digit: String) {
        val d = digit[0]
        val letters = englishMultiTapLetters[d]
        if (letters == null) {
            commitEnglishChar()
            return
        }
        if (englishPending && d == englishDigit) {
            englishIndex = (englishIndex + 1) % letters.length
        } else {
            commitEnglishChar()
            englishDigit = d
            englishIndex = 0
        }
        englishPending = true
        englishTimer.removeCallbacks(englishTimeoutRunnable)
        englishTimer.postDelayed(englishTimeoutRunnable, englishTimeoutMs)
        val composing = letters[englishIndex].toString()
        bufferText.text = composing
    }

    private fun commitEnglishChar() {
        if (!englishPending) return
        englishPending = false
        englishTimer.removeCallbacks(englishTimeoutRunnable)
        val letters = englishMultiTapLetters[englishDigit] ?: run { englishDigit = ' '; return }
        if (englishIndex < letters.length) {
            currentInputConnection?.commitText(letters[englishIndex].toString(), 1)
        }
        bufferText.text = ""
        englishDigit = ' '
        englishIndex = 0
    }

    private fun onDigitPressed(digit: String) {
        controller.inputDigit(digit)
        refreshUi()
        logAction("DIGIT", digit)
    }

    private fun onDeletePressed() {
        if (keyboardMode == KeyboardMode.EnglishT9 && englishPending) {
            val letters = englishMultiTapLetters[englishDigit] ?: ""
            if (englishIndex > 0) {
                englishIndex--
                val composing = letters[englishIndex].toString()
                bufferText.text = composing
            } else {
                commitEnglishChar()
                sendSystemDelete()
            }
            return
        }
        val result = controller.onDelete()
        when (result) {
            is T9ImeController.ActionResult.Refresh -> {
                logAction("KEY_DEL", "buffer ${controller.rawBuffer}")
                refreshUi()
            }
            is T9ImeController.ActionResult.SendDelete -> {
                logAction("KEY_DEL", "send DEL")
                sendSystemDelete()
            }
            else -> {}
        }
    }

    private fun startDeleteLongPress() {
        deleteLongPressRunning = true
        deleteHandler.post(object : Runnable {
            override fun run() {
                if (!deleteLongPressRunning) return
                if (keyboardMode == KeyboardMode.EnglishT9 && englishPending) {
                    commitEnglishChar()
                }
                val result = controller.onDelete()
                if (result is T9ImeController.ActionResult.SendDelete) {
                    sendSystemDelete()
                } else {
                    refreshUi()
                }
                deleteHandler.postDelayed(this, deleteRepeatDelay)
            }
        })
    }

    private fun stopDeleteLongPress() {
        deleteLongPressRunning = false
        deleteHandler.removeCallbacksAndMessages(null)
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
                logAction("KEY_0", "no candidates, clearing")
                refreshUi()
            }
            else -> {}
        }
    }

    private fun onCandidateClicked(index: Int) {
        val result = controller.onCandidateClick(index)
        if (result is T9ImeController.ActionResult.CommitText) {
            logAction("CANDIDATE_CLICK", "committing: ${result.text}")
            currentInputConnection?.commitText(result.text, 1)
            refreshUi()
        }
    }

    private fun onEnterPressed() {
        if (controller.rawBuffer.isNotEmpty()) {
            commitFirstCandidateOrPreedit()
            return
        }
        val info = currentEditorInfo
        if (info != null) {
            val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
            if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
                currentInputConnection?.commitText("\n", 1)
                return
            }
            currentInputConnection?.performEditorAction(action)
            return
        }
        currentInputConnection?.commitText("\n", 1)
    }

    private fun commitFirstCandidateOrPreedit() {
        val candidates = controller.currentCandidates
        if (candidates.isNotEmpty()) {
            onCandidateClicked(0)
        } else if (controller.preedit.isNotEmpty()) {
            currentInputConnection?.commitText(controller.preedit, 1)
            controller.reset()
            refreshUi()
        }
    }

    private fun sendSystemDelete() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    private fun handleResult(result: T9ImeController.ActionResult) {
        when (result) {
            is T9ImeController.ActionResult.Refresh -> refreshUi()
            is T9ImeController.ActionResult.CommitText -> {
                currentInputConnection?.commitText(result.text, 1)
                refreshUi()
            }
            is T9ImeController.ActionResult.SendDelete -> sendSystemDelete()
            is T9ImeController.ActionResult.NoAction -> {}
        }
    }

    private fun refreshUi() {
        bufferText.text = controller.preedit
        val limit = settingsRepository.getCandidateCount()
        val candidates = controller.refreshCandidates(limit)
        candidateContainer.visibility = if (candidates.isEmpty() && !isDictPreparing) View.GONE else View.VISIBLE

        if (isDictPreparing) {
            for (i in 0 until candidateContainer.childCount) candidateContainer.getChildAt(i).visibility = View.GONE
            val btn: TextView = if (candidateContainer.childCount > 0) {
                (candidateContainer.getChildAt(0) as TextView).also { it.visibility = View.VISIBLE }
            } else {
                TextView(this).apply {
                    textSize = 18f
                    val isDark = when (settingsRepository.getTheme()) {
                        "dark" -> true; "light" -> false
                        else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    }
                    setTextColor(if (isDark) android.graphics.Color.parseColor("#E0E0E0") else android.graphics.Color.parseColor("#333333"))
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    background = getDrawable(R.drawable.candidate_bg)
                    isClickable = false; isFocusable = false
                }.also {
                    candidateContainer.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT).apply { setMargins(0, 0, 16, 0) })
                }
            }
            btn.text = "词库准备中..."
            btn.setOnClickListener(null)
            return
        }

        for ((index, candidate) in candidates.withIndex()) {
            val btn: TextView = if (index < candidateContainer.childCount) {
                candidateContainer.getChildAt(index) as TextView
            } else {
                TextView(this).apply {
                    textSize = 18f
                    val isDark = when (settingsRepository.getTheme()) {
                        "dark" -> true; "light" -> false
                        else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    }
                    setTextColor(if (isDark) android.graphics.Color.parseColor("#E0E0E0") else android.graphics.Color.parseColor("#333333"))
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    background = getDrawable(R.drawable.candidate_bg)
                    isClickable = true; isFocusable = true
                }.also {
                    candidateContainer.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT).apply { setMargins(0, 0, 16, 0) })
                }
            }
            btn.visibility = View.VISIBLE
            btn.text = candidate.text
            val ci = index
            btn.setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                onCandidateClicked(ci)
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
        if (engine == null) { debugLogger.log(tag, "Engine not initialized yet."); return }
        debugLogger.log(tag, "mode=$keyboardMode raw=${engine.buffer} preedit=${engine.getPreedit()}")
    }

    private fun logAction(action: String, detail: String) {
        if (!settingsRepository.isDebugLoggingEnabled()) return
        debugLogger.log("XiweiT9Action", "$action: $detail")
    }
}
