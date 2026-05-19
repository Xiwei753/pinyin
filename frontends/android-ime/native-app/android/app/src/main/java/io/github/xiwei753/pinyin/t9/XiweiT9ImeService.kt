package io.github.xiwei753.pinyin.t9

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryManager
import io.github.xiwei753.pinyin.t9.data.DictionaryState
import io.github.xiwei753.pinyin.t9.data.DictionaryStateListener
import io.github.xiwei753.pinyin.t9.data.UserDictionary

open class XiweiT9ImeService : InputMethodService(), DictionaryStateListener, ImeActionSink {

    @android.annotation.SuppressLint("SoonBlockedPrivateApi")
    internal var testInputConnection: InputConnection? = null

    override fun getCurrentInputConnection(): InputConnection? {
        return testInputConnection ?: super.getCurrentInputConnection()
    }

    private lateinit var bufferText: TextView
    private lateinit var candidateContainer: LinearLayout
    private lateinit var panelT9: View
    private lateinit var panelSymbol: View
    private lateinit var panelNumber: View

    private lateinit var handler: KeyboardActionHandler
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private var userDictionary: UserDictionary? = null
    private var currentEditorInfo: EditorInfo? = null

    internal var debugLogger: T9DebugLogger = AndroidDebugLogger()
    private var isDictPreparing = false
    private var keyboardMode = KeyboardMode.ChineseT9

    private val englishTimer by lazy { Handler(Looper.getMainLooper()) }
    private var currentEnglishRunnable: Runnable? = null

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
        cancelEnglishTimeout()
        deleteHandler.removeCallbacksAndMessages(null)
    }

    override fun onStateChanged(state: DictionaryState) {
        Handler(Looper.getMainLooper()).post {
            isDictPreparing = state is DictionaryState.Preparing
            when (state) {
                is DictionaryState.Ready -> {
                    if (this::handler.isInitialized) {
                        val engine = T9Engine(state.dictionary, userDictionary)
                        handler.attachEngine(engine)
                        if (this::bufferText.isInitialized) refreshUi()
                    }
                }
                is DictionaryState.Fallback -> {
                    if (this::handler.isInitialized) {
                        Thread {
                            val dict = DictionaryManager.getProviderBlocking(this)
                            Handler(Looper.getMainLooper()).post {
                                val engine = T9Engine(dict, userDictionary)
                                handler.attachEngine(engine)
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
        if (!this::handler.isInitialized) {
            handler = KeyboardActionHandler(this)
            T9DebugLogStore.initFileLogging(filesDir)
            DictionaryManager.registerListener(this)
            DictionaryManager.prepareAsync(this)
            val readyDict = DictionaryManager.getReadyProviderOrNull()
            if (readyDict != null) {
                handler.attachEngine(T9Engine(readyDict, userDictionary))
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
        if (this::handler.isInitialized) handler.discardCompositionForLifecycle()
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        currentEditorInfo = info
        if (this::handler.isInitialized) handler.discardCompositionForLifecycle()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (this::handler.isInitialized) handler.discardCompositionForLifecycle()
        if (this::handler.isInitialized) handler.switchKeyboardMode(KeyboardMode.ChineseT9)
        updateKeyboardPanel()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        if (this::handler.isInitialized) handler.discardCompositionForLifecycle()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (this::handler.isInitialized) handler.discardCompositionForLifecycle()
    }


    private fun resetUiStateIfReady() {
        if (this::bufferText.isInitialized) bufferText.text = ""
        if (this::candidateContainer.isInitialized) {
            candidateContainer.removeAllViews()
            candidateContainer.visibility = View.GONE
        }
    }

    private var view: View? = null

    private fun setTextColorOnAllKeys(rootView: View, textColor: Int, subColor: Int) {
        val allKeyTextIds = listOf(
            R.id.key_1_text,
            R.id.key_2_number, R.id.key_2_letters,
            R.id.key_3_number, R.id.key_3_letters,
            R.id.key_4_number, R.id.key_4_letters,
            R.id.key_5_number, R.id.key_5_letters,
            R.id.key_6_number, R.id.key_6_letters,
            R.id.key_7_number, R.id.key_7_letters,
            R.id.key_8_number, R.id.key_8_letters,
            R.id.key_9_number, R.id.key_9_letters,
            R.id.key_0_number, R.id.key_0_text,
            R.id.punct_1, R.id.punct_2, R.id.punct_3, R.id.punct_4,
            R.id.key_del, R.id.key_retype,
            R.id.key_toggle_symbol, R.id.key_toggle_number, R.id.key_space,
            R.id.key_toggle_english, R.id.key_enter
        )
        for (id in allKeyTextIds) {
            val tv = rootView.findViewById<TextView>(id)
            if (tv != null) {
                if (id == R.id.key_2_number || id == R.id.key_3_number ||
                    id == R.id.key_4_number || id == R.id.key_5_number ||
                    id == R.id.key_6_number || id == R.id.key_7_number ||
                    id == R.id.key_8_number || id == R.id.key_9_number ||
                    id == R.id.key_0_text) {
                    tv.setTextColor(subColor)
                } else {
                    tv.setTextColor(textColor)
                }
            }
        }
        val allSymbolKeys = listOf(R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5, R.id.sym_6,
            R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_10, R.id.sym_11, R.id.sym_12,
            R.id.sym_13, R.id.sym_14, R.id.sym_15, R.id.sym_16, R.id.sym_17, R.id.sym_18,
            R.id.sym_19, R.id.sym_20, R.id.sym_21, R.id.sym_22, R.id.sym_23, R.id.sym_24,
            R.id.sym_25, R.id.sym_26, R.id.sym_27, R.id.sym_28, R.id.sym_29, R.id.sym_30,
            R.id.sym_back, R.id.sym_number, R.id.sym_del, R.id.sym_enter, R.id.sym_hide,
            R.id.num_0, R.id.num_1, R.id.num_2, R.id.num_3, R.id.num_4, R.id.num_5,
            R.id.num_6, R.id.num_7, R.id.num_8, R.id.num_9, R.id.num_dot,
            R.id.num_del, R.id.num_back, R.id.num_symbol, R.id.num_hide, R.id.num_enter
        )
        for (id in allSymbolKeys) {
            rootView.findViewById<TextView>(id)?.setTextColor(textColor)
        }
        for (i in 0 until candidateContainer.childCount) {
            (candidateContainer.getChildAt(i) as? TextView)?.setTextColor(textColor)
        }
    }

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
        val subColor = if (isDark) android.graphics.Color.parseColor("#888888") else android.graphics.Color.parseColor("#999999")

        rootView.setBackgroundColor(bgColor)
        rootView.findViewById<LinearLayout>(R.id.candidate_bar).setBackgroundColor(candidateBarColor)
        bufferText.setTextColor(if (isDark) android.graphics.Color.parseColor("#888888") else android.graphics.Color.parseColor("#888888"))

        setTextColorOnAllKeys(rootView, textColor, subColor)

        val heightSetting = settingsRepository.getKeyboardHeight()
        val density = resources.displayMetrics.density
        val rowHeightPx = when (heightSetting) {
            "low" -> (44 * density).toInt()
            "high" -> (56 * density).toInt()
            else -> (48 * density).toInt()
        }
        val bottomRowHeightPx = when (heightSetting) {
            "low" -> (40 * density).toInt()
            "high" -> (48 * density).toInt()
            else -> (44 * density).toInt()
        }

        val t9RowIds = listOf(R.id.row_t9_1, R.id.row_t9_2, R.id.row_t9_3, R.id.row_t9_4)
        for (id in t9RowIds) {
            rootView.findViewById<View>(id)?.layoutParams?.height = rowHeightPx
        }
        rootView.findViewById<View>(R.id.row_bottom)?.layoutParams?.height = bottomRowHeightPx

        val symRowIds = listOf(R.id.row_sym_1, R.id.row_sym_2, R.id.row_sym_3, R.id.row_sym_4, R.id.row_sym_5, R.id.row_sym_6)
        for (id in symRowIds) {
            rootView.findViewById<View>(id)?.layoutParams?.height = rowHeightPx
        }
        rootView.findViewById<View>(R.id.row_sym_bottom)?.layoutParams?.height = bottomRowHeightPx

        val numRowIds = listOf(R.id.row_num_1, R.id.row_num_2, R.id.row_num_3, R.id.row_num_4)
        for (id in numRowIds) {
            rootView.findViewById<View>(id)?.layoutParams?.height = rowHeightPx
        }
        rootView.findViewById<View>(R.id.row_num_bottom)?.layoutParams?.height = bottomRowHeightPx
    }



    private fun updateKeyboardPanel() {
        if (!this::handler.isInitialized) return
        val keyboardMode = handler.keyboardMode
        panelT9.visibility = if (keyboardMode == KeyboardMode.ChineseT9 || handler.keyboardMode == KeyboardMode.EnglishT9) View.VISIBLE else View.GONE
        panelSymbol.visibility = if (handler.keyboardMode == KeyboardMode.Symbol) View.VISIBLE else View.GONE
        panelNumber.visibility = if (handler.keyboardMode == KeyboardMode.Number) View.VISIBLE else View.GONE

        val toggleEnglish = view?.findViewById<TextView>(R.id.key_toggle_english)
        if (toggleEnglish != null) {
            toggleEnglish.text = if (handler.keyboardMode == KeyboardMode.EnglishT9) "英/中" else "中/英"
        }
    }

    private fun setupKeys(view: View) {
        // T9 digit keys 2-9
        val numberKeys = mapOf(
            R.id.key_2 to "2", R.id.key_3 to "3", R.id.key_4 to "4",
            R.id.key_5 to "5", R.id.key_6 to "6", R.id.key_7 to "7",
            R.id.key_8 to "8", R.id.key_9 to "9"
        )
        for ((id, digit) in numberKeys) {
            view.findViewById<View>(id)?.setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                handler.onDigitPressed(digit)
                logAction("DIGIT", digit)
            }
        }

        // Key 1 - separator/分词
        view.findViewById<View>(R.id.key_1_text)?.setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            handler.onSeparator()
            logAction("KEY_1", "separation")
        }

        // Delete key
        val delKey = view.findViewById<View>(R.id.key_del)
        delKey?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onDelete()
        }
        delKey?.setOnLongClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            startDeleteLongPress()
            true
        }
        delKey?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                stopDeleteLongPress()
            }
            false
        }

        // Key 0
        view.findViewById<View>(R.id.key_0)?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onZero()
        }

        // Retype key
        view.findViewById<View>(R.id.key_retype)?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onClearComposingForRetype()
            logAction("RETYPE", "clear")
        }

        // Punct keys - commit directly
        val punctKeys = mapOf(
            R.id.punct_1 to "，", R.id.punct_2 to "。",
            R.id.punct_3 to "？", R.id.punct_4 to "！"
        )
        for ((id, text) in punctKeys) {
            view.findViewById<View>(id)?.setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                handler.onPunctCommit(text)
                logAction("PUNCT", text)
            }
        }

        // Toggle symbol panel
        view.findViewById<View>(R.id.key_toggle_symbol)?.setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            handler.switchKeyboardMode(KeyboardMode.Symbol)
            updateKeyboardPanel()
        }

        // Toggle English/Chinese
        view.findViewById<View>(R.id.key_toggle_english)?.setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            if (handler.keyboardMode == KeyboardMode.EnglishT9) {
                handler.switchKeyboardMode(KeyboardMode.ChineseT9)
            } else {
                handler.switchKeyboardMode(KeyboardMode.EnglishT9)
            }
            updateKeyboardPanel()
        }

        // Toggle number panel
        view.findViewById<View>(R.id.key_toggle_number)?.setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            handler.switchKeyboardMode(KeyboardMode.Number)
            updateKeyboardPanel()
        }

        // Space key
        view.findViewById<View>(R.id.key_space)?.setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            handler.onSpace()
            logAction("SPACE", "space")
        }

        // Enter key
        view.findViewById<View>(R.id.key_enter)?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onEnter()
        }

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
            view.findViewById<TextView>(id)?.setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                handler.onDigitPressed(text)
                handler.switchKeyboardMode(KeyboardMode.ChineseT9)
                updateKeyboardPanel()
            }
        }

        view.findViewById<TextView>(R.id.sym_back)?.setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            handler.switchKeyboardMode(KeyboardMode.ChineseT9)
            updateKeyboardPanel()
        }
        view.findViewById<TextView>(R.id.sym_number)?.setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            handler.switchKeyboardMode(KeyboardMode.Number)
            updateKeyboardPanel()
        }
        view.findViewById<TextView>(R.id.sym_del)?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onDelete()
        }
        view.findViewById<TextView>(R.id.sym_enter)?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onEnter()
        }
        view.findViewById<TextView>(R.id.sym_hide)?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onHideKey()
            updateKeyboardPanel()
            requestHideSelf(0)
        }
        val numKeys = listOf(
            R.id.num_1 to "1", R.id.num_2 to "2", R.id.num_3 to "3",
            R.id.num_4 to "4", R.id.num_5 to "5", R.id.num_6 to "6",
            R.id.num_7 to "7", R.id.num_8 to "8", R.id.num_9 to "9",
            R.id.num_0 to "0", R.id.num_dot to "."
        )
        for ((id, text) in numKeys) {
            view.findViewById<TextView>(id)?.setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                handler.onDigitPressed(text)
            }
        }

        view.findViewById<TextView>(R.id.num_del)?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onDelete()
        }
        view.findViewById<TextView>(R.id.num_back)?.setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            handler.switchKeyboardMode(KeyboardMode.ChineseT9)
            updateKeyboardPanel()
        }
        view.findViewById<TextView>(R.id.num_symbol)?.setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            handler.switchKeyboardMode(KeyboardMode.Symbol)
            updateKeyboardPanel()
        }
        view.findViewById<TextView>(R.id.num_hide)?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onHideKey()
            updateKeyboardPanel()
            requestHideSelf(0)
        }
        view.findViewById<TextView>(R.id.num_enter)?.setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            handler.onEnter()
        }
    }





    private fun startDeleteLongPress() {
        deleteLongPressRunning = true
        deleteHandler.post(object : Runnable {
            override fun run() {
                if (!deleteLongPressRunning) return
                handler.onDelete()
                deleteHandler.postDelayed(this, deleteRepeatDelay)
            }
        })
    }

    private fun stopDeleteLongPress() {
        deleteLongPressRunning = false
        deleteHandler.removeCallbacksAndMessages(null)
    }










    // --- ImeActionSink implementation ---
    override fun commitText(text: String) {
        logAction("COMMIT", text)
        currentInputConnection?.commitText(text, 1)
    }
    override fun sendDelete() {
        logAction("KEY_DEL", "send DEL")
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }
    override fun performEditorActionOrNewline() {
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
    override fun finishComposingText() {
        currentInputConnection?.finishComposingText()
    }
    override fun refreshUi() {
        if (!this::bufferText.isInitialized || !this::handler.isInitialized) return
        bufferText.text = handler.preedit
        val limit = settingsRepository.getCandidateCount()
        val candidates = handler.refreshCandidates(limit)
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
                handler.onCandidateClick(ci)
            }
        }
        for (i in candidates.size until candidateContainer.childCount) {
            candidateContainer.getChildAt(i).visibility = View.GONE
        }
        logDebugInfo()
    }
    override fun scheduleEnglishTimeout(runnable: Runnable, delayMs: Long) {
        currentEnglishRunnable = runnable
        englishTimer.postDelayed(runnable, delayMs)
    }
    override fun cancelEnglishTimeout() {
        currentEnglishRunnable?.let { englishTimer.removeCallbacks(it) }
        currentEnglishRunnable = null
    }

    private fun logDebugInfo() {
        if (!settingsRepository.isDebugLoggingEnabled()) return
        val tag = "XiweiT9Debug"
        val engine = handler.engine
        if (engine == null) { debugLogger.log(tag, "Engine not initialized yet."); return }
        debugLogger.log(tag, "mode=${handler.keyboardMode} raw=${engine.buffer} preedit=${engine.getPreedit()}")
    }

    private fun logAction(action: String, detail: String) {
        if (!settingsRepository.isDebugLoggingEnabled()) return
        debugLogger.log("XiweiT9Action", "$action: $detail")
    }
}
