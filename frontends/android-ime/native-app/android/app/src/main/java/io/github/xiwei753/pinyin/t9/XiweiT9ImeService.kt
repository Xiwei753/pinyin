package io.github.xiwei753.pinyin.t9

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
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
    internal lateinit var xiweiKeyboardView: XiweiKeyboardView
    internal lateinit var themeController: KeyboardThemeController
    internal lateinit var heightController: KeyboardHeightController
    internal lateinit var candidateViewController: CandidateViewController
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

    private val layoutBuilder = KeyboardLayoutBuilder()
    private val registry = SymbolKeyRegistry()

    private var currentSymCategory: String = "punct"

    private val categoryToPage = mapOf(
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
        if (this::xiweiKeyboardView.isInitialized) {
            xiweiKeyboardView.destroy()
        }
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
        try {
            Log.i("XiweiT9ImeService", "inflate start")
            val view = layoutInflater.inflate(R.layout.keyboard_view, null)
            Log.i("XiweiT9ImeService", "inflate end")
            
            Log.i("XiweiT9ImeService", "bind required views start")
            keyboardViews = KeyboardViews.bind(view)
            xiweiKeyboardView = keyboardViews.xiweiKeyboardView
            Log.i("XiweiT9ImeService", "bind required views end")
            
            ensureCoreInitialized()
    
            deleteRepeatController = DeleteRepeatController { handler.onDelete() }
    
            themeController = KeyboardThemeController(settingsRepository, resources)
            heightController = KeyboardHeightController(settingsRepository, resources)
    
            candidateViewController = CandidateViewController(
                context = this,
                v = keyboardViews,
                themeController = themeController,
                settingsRepository = settingsRepository,
            )
    
            setupKeyboardViewActions()
            Log.i("XiweiT9ImeService", "xiweiKeyboardView init done")
            Log.i("XiweiT9ImeService", "callbacks setup done")
    
            applyThemeAndHeight()
            Log.i("XiweiT9ImeService", "theme applied")
            
            updateKeyboardPanel()
            Log.i("XiweiT9ImeService", "layout model built")
            
            Log.i("XiweiT9ImeService", "input view returned")
            return view
        } catch (e: Throwable) {
            Log.e("XiweiT9ImeService", "Failed to create input view", e)
            throw e
        }
    }

    private fun setupKeyboardViewActions() {
        xiweiKeyboardView.onHapticTap = { hapticFeedbackManager.performTap(xiweiKeyboardView) }
        xiweiKeyboardView.onHapticSpecial = { hapticFeedbackManager.performSpecialKey(xiweiKeyboardView) }
        xiweiKeyboardView.onHapticLongPress = { hapticFeedbackManager.performLongPress(xiweiKeyboardView) }

        xiweiKeyboardView.onDeleteRepeat = { handler.onDelete() }

        xiweiKeyboardView.onEnterShortPress = {
            handler.onEnterShortPress()
        }
        xiweiKeyboardView.onEnterLongPress = {
            handler.onEnterLongPress()
        }

        xiweiKeyboardView.requestRebuildLayout = {
            rebuildLayoutModel()
        }

        xiweiKeyboardView.onKeyAction = { action ->
            when {
                action == "separator" -> handler.onSeparator()
                action.startsWith("digit:") -> {
                    val digit = action.removePrefix("digit:")
                    handler.onDigitPressed(digit)
                }
                action == "del" -> handler.onDelete()
                action == "retype" -> handler.onClearComposingForRetype()
                action == "space" -> handler.onSpace()
                action.startsWith("toggle:") -> {
                    val toggle = action.removePrefix("toggle:")
                    when (toggle) {
                        "symbol" -> {
                            handler.toggleSymbolKey()
                            updateKeyboardPanel()
                        }
                        "english" -> {
                            if (handler.keyboardMode == KeyboardMode.EnglishT9) {
                                handler.switchKeyboardMode(KeyboardMode.ChineseT9)
                            } else if (handler.keyboardMode == KeyboardMode.ChineseT9) {
                                handler.switchKeyboardMode(KeyboardMode.EnglishT9)
                            } else {
                                handler.switchKeyboardMode(KeyboardMode.ChineseT9)
                            }
                            updateKeyboardPanel()
                        }
                        "number" -> {
                            handler.toggleNumberKey()
                            updateKeyboardPanel()
                        }
                    }
                }
                action.startsWith("punct:") -> {
                    val punct = action.removePrefix("punct:")
                    handler.onPunctCommit(punct)
                }
                action.startsWith("reading:") -> {
                    val indexStr = action.removePrefix("reading:")
                    val index = indexStr.toIntOrNull()
                    if (index != null) {
                        val readings = handler.readings
                        if (index < readings.size) {
                            handler.setActiveReading(readings[index])
                        }
                    }
                }
                action.startsWith("symtab:") -> {
                    val cat = action.removePrefix("symtab:")
                    currentSymCategory = cat
                    updateKeyboardPanel()
                }
                action.startsWith("symbol:commit:") -> {
                    val text = action.removePrefix("symbol:commit:")
                    handler.onDigitPressed(text)
                    handler.switchKeyboardMode(handler.lastTextMode)
                    updateKeyboardPanel()
                }
            }
        }
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

    private fun applyThemeAndHeight() {
        if (!this::keyboardViews.isInitialized || !this::themeController.isInitialized || !this::xiweiKeyboardView.isInitialized) return
        val palette = themeController.getThemePalette()
        if (this::candidateViewController.isInitialized) {
            candidateViewController.updateThemePalette(palette)
        }
        themeController.applyTheme(keyboardViews, palette)
        xiweiKeyboardView.palette = palette

        val metrics = heightController.calculateHeight()
        xiweiKeyboardView.layoutParams?.height = metrics.shellHeight

        xiweiKeyboardView.keyboardMode = handler.keyboardMode
        xiweiKeyboardView.activeSymCategory = currentSymCategory
        xiweiKeyboardView.lastTextMode = handler.lastTextMode

        rebuildLayoutModel()
        heightController.applyHeight(keyboardViews, metrics)
    }

    private fun rebuildLayoutModel() {
        if (!this::xiweiKeyboardView.isInitialized || !this::handler.isInitialized) return

        val metrics = heightController.calculateHeight()
        val density = resources.displayMetrics.density
        val hGap = (4 * density).toInt()
        val vGap = (4 * density).toInt()

        val panelWidth: Int
        val panelHeight: Int
        if (xiweiKeyboardView.width > 0 && xiweiKeyboardView.height > 0) {
            panelWidth = xiweiKeyboardView.width
            panelHeight = xiweiKeyboardView.height
        } else {
            panelWidth = resources.displayMetrics.widthPixels
            panelHeight = metrics.shellHeight
        }

        val model = when (handler.keyboardMode) {
            KeyboardMode.ChineseT9, KeyboardMode.EnglishT9 -> {
                val readings = handler.readings
                layoutBuilder.buildT9(
                    panelWidth = panelWidth,
                    panelHeight = panelHeight,
                    rowHeight = metrics.rowHeightPx,
                    bottomRowHeight = metrics.bottomRowHeightPx,
                    horizontalGap = hGap,
                    verticalGap = vGap,
                    readings = readings,
                    keyboardMode = handler.keyboardMode,
                )
            }
            KeyboardMode.Number -> {
                layoutBuilder.buildNumber(
                    panelWidth = panelWidth,
                    panelHeight = panelHeight,
                    rowHeight = metrics.rowHeightPx,
                    bottomRowHeight = metrics.bottomRowHeightPx,
                    horizontalGap = hGap,
                    verticalGap = vGap,
                    keyboardMode = handler.keyboardMode,
                    lastTextMode = handler.lastTextMode,
                )
            }
            KeyboardMode.Symbol -> {
                val pageName = currentSymCategory
                val catEntries = getEntriesForPage(pageName)
                layoutBuilder.buildSymbol(
                    panelWidth = panelWidth,
                    panelHeight = panelHeight,
                    rowHeight = metrics.rowHeightPx,
                    bottomRowHeight = metrics.bottomRowHeightPx,
                    horizontalGap = hGap,
                    verticalGap = vGap,
                    symbolEntries = catEntries,
                    activeCategory = pageName,
                    lastTextMode = handler.lastTextMode,
                    categoryToPage = categoryToPage,
                    registry = registry,
                    density = density,
                )
            }
        }

        xiweiKeyboardView.layoutModel = model
        xiweiKeyboardView.invalidate()
    }

    private fun getEntriesForPage(pageName: String): List<Pair<Int, String>> {
        val results = mutableListOf<Pair<Int, String>>()
        for (category in registry.getAllCategories()) {
            if (categoryToPage[category] == pageName) {
                results.addAll(registry.getSymbolsByCategory(category))
            }
        }
        if (results.isEmpty()) {
            return registry.getAllSymbolEntries().take(60)
        }
        return results
    }

    internal fun updateKeyboardPanel() {
        if (!this::handler.isInitialized || !this::keyboardViews.isInitialized) return
        
        if (handler.keyboardMode == KeyboardMode.Symbol || handler.keyboardMode == KeyboardMode.Number) {
            keyboardViews.pinyinFloatingBar.visibility = View.GONE
        }
        
        if (this::xiweiKeyboardView.isInitialized) {
            xiweiKeyboardView.keyboardMode = handler.keyboardMode
            xiweiKeyboardView.activeSymCategory = currentSymCategory
            xiweiKeyboardView.lastTextMode = handler.lastTextMode
            rebuildLayoutModel()
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

            if (actionId != 0 && actionLabel != null) {
                val result = currentInputConnection?.performEditorAction(actionId)
                logEnterAction("custom actionId=$actionId actionLabel=$actionLabel result=$result")
                if (result == true) return
            }

            val hasStandardAction = action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED
            if (hasStandardAction) {
                val result = currentInputConnection?.performEditorAction(action)
                logEnterAction("performEditorAction action=$action result=$result")
                if (result == true) return
            }

            val defaultResult = sendDefaultEditorAction(true)
            logEnterAction("sendDefaultEditorAction result=$defaultResult")
            if (defaultResult) return

            logEnterAction("fallback: commitText newline")
            currentInputConnection?.commitText("\n", 1)
            return
        }
        logEnterAction("no editorInfo, fallback: commitText newline")
        currentInputConnection?.commitText("\n", 1)
    }

    override fun performEnterActionIfAvailable(): Boolean {
        val info = currentEditorInfo ?: return false
        val imeOptions = info.imeOptions
        val flags = imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION
        if (flags != 0) {
            logEnterAction("IME_FLAG_NO_ENTER_ACTION set, skip action")
            return false
        }

        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        val actionId = info.actionId
        val actionLabel = info.actionLabel

        if (actionId != 0 && actionLabel != null) {
            val result = currentInputConnection?.performEditorAction(actionId)
            logEnterAction("performEnterActionIfAvailable custom actionId=$actionId result=$result")
            if (result == true) return true
        }

        val hasStandardAction = action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED
        if (hasStandardAction) {
            val result = currentInputConnection?.performEditorAction(action)
            logEnterAction("performEnterActionIfAvailable action=$action result=$result")
            if (result == true) return true
        }

        return false
    }

    override fun isEnterActionAvailable(): Boolean {
        val info = currentEditorInfo ?: return false
        val imeOptions = info.imeOptions
        val flags = imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION
        if (flags != 0) return false
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) return true
        if (info.actionId != 0 && info.actionLabel != null) return true
        return false
    }

    private fun logEnterAction(msg: String) {
        if (!settingsRepository.isDebugLoggingEnabled()) return
        debugLogger.log("XiweiT9Enter", msg)
    }

    override fun finishComposingText() {
        currentInputConnection?.finishComposingText()
    }

    override fun getCurrentEditorInfo(): EditorInfo? = currentEditorInfo

    override fun performEditorAction(action: Int): Boolean {
        return currentInputConnection?.performEditorAction(action) ?: false
    }

    override fun refreshUi() {
        if (!this::keyboardViews.isInitialized || !this::handler.isInitialized || !this::candidateViewController.isInitialized) return
        candidateViewController.refreshUi(handler)
        if (this::xiweiKeyboardView.isInitialized) {
            rebuildLayoutModel()
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
