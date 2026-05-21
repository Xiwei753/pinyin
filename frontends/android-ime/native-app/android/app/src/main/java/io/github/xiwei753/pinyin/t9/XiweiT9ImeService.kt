package io.github.xiwei753.pinyin.t9

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
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

    internal lateinit var keyboardViews: KeyboardViews
    internal lateinit var themeController: KeyboardThemeController
    internal lateinit var heightController: KeyboardHeightController
    internal lateinit var panelController: KeyboardPanelController
    internal lateinit var candidateViewController: CandidateViewController
    internal lateinit var keyBinder: KeyboardKeyBinder
    internal lateinit var deleteRepeatController: DeleteRepeatController
    internal lateinit var handler: KeyboardActionHandler
    internal lateinit var settingsRepository: SettingsRepository
    internal lateinit var hapticFeedbackManager: HapticFeedbackManager
    private var userDictionary: UserDictionary? = null
    private var currentEditorInfo: EditorInfo? = null

    internal var debugLogger: T9DebugLogger = AndroidDebugLogger()
    private var isDictPreparing = false

    private val englishTimer by lazy { Handler(Looper.getMainLooper()) }
    private var currentEnglishRunnable: Runnable? = null

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
        deleteRepeatController.destroy()
    }

    override fun onStateChanged(state: DictionaryState) {
        Handler(Looper.getMainLooper()).post {
            isDictPreparing = state is DictionaryState.Preparing
            if (this::candidateViewController.isInitialized) {
                candidateViewController.setDictPreparing(isDictPreparing)
            }
            when (state) {
                is DictionaryState.Ready -> {
                    if (this::handler.isInitialized) {
                        val engine = T9Engine(state.dictionary, userDictionary)
                        handler.attachEngine(engine)
                        if (this::keyboardViews.isInitialized) refreshUi()
                    }
                }
                is DictionaryState.Fallback -> {
                    if (this::handler.isInitialized) {
                        Thread {
                            val dict = DictionaryManager.getProviderBlocking(this)
                            Handler(Looper.getMainLooper()).post {
                                val engine = T9Engine(dict, userDictionary)
                                handler.attachEngine(engine)
                                if (this::keyboardViews.isInitialized) refreshUi()
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

        deleteRepeatController = DeleteRepeatController { handler.onDelete() }

        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardViews = KeyboardViews.bind(view)

        themeController = KeyboardThemeController(settingsRepository, resources)
        heightController = KeyboardHeightController(settingsRepository, resources)
        panelController = KeyboardPanelController(keyboardViews)

        keyBinder = KeyboardKeyBinder(
            v = keyboardViews,
            hapticFeedbackManager = hapticFeedbackManager,
            panelController = panelController,
            deleteRepeatController = deleteRepeatController,
            onModeChanged = { updateKeyboardPanel() },
        )
        keyBinder.setOnRefreshUi { refreshUi() }

        candidateViewController = CandidateViewController(
            context = this,
            v = keyboardViews,
            keyBinder = keyBinder,
            themeController = themeController,
            settingsRepository = settingsRepository,
        )

        keyBinder.setupAllKeys(handler)
        setupSymbolCategories()
        populateSymbolPages()
        applyThemeAndHeight()
        updateKeyboardPanel()
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditorInfo = info
        applyThemeAndHeight()
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
        if (this::candidateViewController.isInitialized) {
            candidateViewController.resetUi()
        }
    }

    private fun applyThemeAndHeight() {
        if (!this::keyboardViews.isInitialized || !this::themeController.isInitialized) return
        val palette = themeController.getThemePalette()
        if (this::candidateViewController.isInitialized) {
            candidateViewController.updateThemePalette(palette)
        }
        themeController.applyTheme(keyboardViews, palette)
        val metrics = heightController.calculateHeight()
        heightController.applyHeight(keyboardViews, metrics)
    }

    internal fun updateKeyboardPanel() {
        if (!this::handler.isInitialized || !this::panelController.isInitialized || !this::keyboardViews.isInitialized) return
        panelController.updatePanel(handler.keyboardMode, handler.lastTextMode)
    }

    private fun setupSymbolCategories() {
        val tabs = mapOf(
            keyboardViews.symTabPunct to "punct",
            keyboardViews.symTabMath to "math",
            keyboardViews.symTabBracket to "bracket",
            keyboardViews.symTabOther to "other",
        )
        for ((tabView, category) in tabs) {
            keyBinder.setupKey(tabView, false) {
                val palette = themeController.getThemePalette()
                panelController.setSymbolCategory(category, themeController, palette)
            }
        }
    }

    private fun populateSymbolPages() {
        val registry = SymbolKeyRegistry()
        val density = resources.displayMetrics.density

        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = density,
            symbolPanelWidth = resources.displayMetrics.widthPixels,
            rowHeight = (48 * density).toInt(),
        )

        val pageMap = mapOf<String, android.widget.LinearLayout>(
            "punct" to (keyboardViews.symPagePunct as? android.widget.LinearLayout)!!,
            "math" to (keyboardViews.symPageMath as? android.widget.LinearLayout)!!,
            "bracket" to (keyboardViews.symPageBracket as? android.widget.LinearLayout)!!,
            "other" to (keyboardViews.symPageOther as? android.widget.LinearLayout)!!,
        )

        // Apply content insets to target pages directly.
        // Insets on the temporary buildPage are lost during child transfer,
        // so we must set them on the actual target page containers.
        for ((_, targetPage) in pageMap) {
            targetPage.setPadding(
                metrics.contentInsetLeft,
                metrics.contentInsetTop,
                metrics.contentInsetRight,
                metrics.contentInsetBottom,
            )
        }

        val categoryToPage = mapOf(
            SymbolKeyRegistry.Category.FULLWIDTH_PUNCT to "punct",
            SymbolKeyRegistry.Category.HALFWIDTH_PUNCT to "punct",
            SymbolKeyRegistry.Category.MATH to "math",
            SymbolKeyRegistry.Category.BRACKET to "bracket",
            SymbolKeyRegistry.Category.CURRENCY to "other",
            SymbolKeyRegistry.Category.UNIT to "other",
            SymbolKeyRegistry.Category.NETWORK to "other",
            SymbolKeyRegistry.Category.SEQUENCE to "other",
            SymbolKeyRegistry.Category.ARROW to "other",
            SymbolKeyRegistry.Category.GREEK to "other",
            SymbolKeyRegistry.Category.OTHER to "other",
        )

        // Symbol click handler: commit text and return to last text mode
        val onSymbolClick = { symbol: String ->
            handler.onDigitPressed(symbol)
            handler.switchKeyboardMode(handler.lastTextMode)
            updateKeyboardPanel()
        }
        val onSymbolTouch = { view: android.view.View ->
            hapticFeedbackManager.performTap(view)
        }

        for (category in registry.getAllCategories()) {
            val pageName = categoryToPage[category] ?: "other"
            val page = pageMap[pageName] ?: continue
            val entries = registry.getSymbolsByCategory(category)
            if (entries.isEmpty()) continue

            val symPage = SymbolGridController.buildPage(
                context = this,
                entries = entries,
                rowHeightPx = metrics.cellHeight,
                generatedSymbolViews = keyboardViews.generatedSymbolViews,
                textSize = 20f,
                textColor = 0xFF333333.toInt(),
                metrics = metrics,
                onSymbolClick = onSymbolClick,
                onSymbolTouch = onSymbolTouch,
            )

            // Transfer all children from the grid page to the target page
            while (symPage.childCount > 0) {
                val child = symPage.getChildAt(0)
                symPage.removeViewAt(0)
                page.addView(child)
            }
        }
    }

    // --- ImeActionSink implementation ---
    override fun commitText(text: String) {
        logAction("COMMIT", text)
        currentInputConnection?.commitText(text, 1)
    }

    override fun commitNewline() {
        logAction("NEWLINE", "\\n")
        currentInputConnection?.commitText("\n", 1)
    }

    override fun sendDelete() {
        logAction("KEY_DEL", "send DEL")
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    override fun sendDefaultEditorAction(fromEnterKey: Boolean): Boolean {
        return super.sendDefaultEditorAction(fromEnterKey)
    }

    override fun performEditorActionOrNewline() {
        val info = currentEditorInfo
        if (info != null) {
            val imeOptions = info.imeOptions
            val action = imeOptions and EditorInfo.IME_MASK_ACTION
            val actionId = info.actionId
            val actionLabel = info.actionLabel

            logEnterAction("imeOptions=$imeOptions action=$action actionId=$actionId actionLabel=$actionLabel")

            // Step 1: custom actionId/actionLabel
            if (actionId != 0 && actionLabel != null) {
                val result = currentInputConnection?.performEditorAction(actionId)
                logEnterAction("custom actionId=$actionId actionLabel=$actionLabel result=$result")
                if (result == true) return
            }

            // Step 2: standard IME action (SEND/GO/SEARCH/DONE/NEXT/PREVIOUS)
            val hasStandardAction = action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED
            if (hasStandardAction) {
                val result = currentInputConnection?.performEditorAction(action)
                logEnterAction("performEditorAction action=$action result=$result")
                if (result == true) return
            }

            // Step 3: default editor action via framework
            val defaultResult = sendDefaultEditorAction(true)
            logEnterAction("sendDefaultEditorAction result=$defaultResult")
            if (defaultResult) return

            // Step 4: final fallback - insert newline
            logEnterAction("fallback: commitText newline")
            currentInputConnection?.commitText("\n", 1)
            return
        }
        logEnterAction("no editorInfo, fallback: commitText newline")
        currentInputConnection?.commitText("\n", 1)
    }

    private fun logEnterAction(msg: String) {
        if (!settingsRepository.isDebugLoggingEnabled()) return
        debugLogger.log("XiweiT9Enter", msg)
    }

    override fun finishComposingText() {
        currentInputConnection?.finishComposingText()
    }

    override fun refreshUi() {
        if (!this::keyboardViews.isInitialized || !this::handler.isInitialized || !this::candidateViewController.isInitialized) return
        candidateViewController.refreshUi(handler)
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
