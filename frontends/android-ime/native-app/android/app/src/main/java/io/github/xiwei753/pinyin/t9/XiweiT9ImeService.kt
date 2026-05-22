package io.github.xiwei753.pinyin.t9

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.github.xiwei753.pinyin.imecore.ImeInputAction
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
            debugLogger.log("XiweiT9ImeService", "Failed to init user dictionary: " + e.stackTraceToString())
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
            handler = KeyboardActionHandler(this) { settingsRepository.getCandidateCount() }
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
            debugLogger.log("XiweiT9ImeService", "inflate start")
            val view = layoutInflater.inflate(R.layout.keyboard_view, null)
            debugLogger.log("XiweiT9ImeService", "inflate end")
            
            debugLogger.log("XiweiT9ImeService", "bind required views start")
            keyboardViews = KeyboardViews.bind(view)
            xiweiKeyboardView = keyboardViews.xiweiKeyboardView
            debugLogger.log("XiweiT9ImeService", "bind required views end")
            
            ensureCoreInitialized()
    
            deleteRepeatController = DeleteRepeatController { handleInputAction(ImeInputAction.DeletePressed) }
    
            themeController = KeyboardThemeController(settingsRepository, resources)
            heightController = KeyboardHeightController(settingsRepository, resources)
    
            candidateViewController = CandidateViewController(
                context = this,
                v = keyboardViews,
                themeController = themeController,
                settingsRepository = settingsRepository,
            )
            candidateViewController.onInputAction = { handleInputAction(it) }
    
            setupKeyboardViewActions()
            debugLogger.log("XiweiT9ImeService", "xiweiKeyboardView init done")
            debugLogger.log("XiweiT9ImeService", "callbacks setup done")
    
            applyThemeAndHeight()
            debugLogger.log("XiweiT9ImeService", "theme applied")
            
            updateKeyboardPanel()
            debugLogger.log("XiweiT9ImeService", "layout model built")
            
            debugLogger.log("XiweiT9ImeService", "input view returned")
            return view
        } catch (e: Throwable) {
            debugLogger.log("XiweiT9ImeService", "Failed to create input view: " + e.stackTraceToString())
            throw e
        }
    }

    private fun setupKeyboardViewActions() {
        xiweiKeyboardView.onHapticTap = { hapticFeedbackManager.performTap(xiweiKeyboardView) }
        xiweiKeyboardView.onHapticSpecial = { hapticFeedbackManager.performSpecialKey(xiweiKeyboardView) }
        xiweiKeyboardView.onHapticLongPress = { hapticFeedbackManager.performLongPress(xiweiKeyboardView) }

        xiweiKeyboardView.onDeleteRepeat = { handleInputAction(ImeInputAction.DeletePressed) }

        xiweiKeyboardView.onEnterShortPress = {
            handleInputAction(ImeInputAction.EnterShortPressed)
        }
        xiweiKeyboardView.onEnterLongPress = {
            handleInputAction(ImeInputAction.EnterLongPressed)
        }

        xiweiKeyboardView.requestRebuildLayout = {
            rebuildLayoutModel()
        }

        xiweiKeyboardView.onInputAction = { action -> handleInputAction(action) }
        xiweiKeyboardView.onKeyAction = { action -> handleKeyboardAction(action) }
    }

    internal fun handleInputAction(action: ImeInputAction) {
        if (!this::handler.isInitialized) return
        if (!isActionAllowedByPolicy(action)) return
        val debugLogging = settingsRepository.isDebugLoggingEnabled()
        val beforeMode = if (debugLogging) handler.keyboardMode else null
        val beforeBufferEmpty = if (debugLogging) handler.rawBuffer.isEmpty() else null
        handler.handle(action)
        renderCurrentState()
        if (debugLogging) {
            val afterMode = handler.keyboardMode
            val afterBufferEmpty = handler.rawBuffer.isEmpty()
            val uiState = handler.uiState(isDictPreparing)
            val candidateCount = uiState.candidatesSnapshot.size
            val preeditVisible = uiState.preeditState.visible
            val symbolCategory = uiState.currentSymbolCategory
            debugLogger.log("XiweiT9StateMachine",
                "action=${action::class.simpleName} " +
                "beforeMode=$beforeMode afterMode=$afterMode " +
                "beforeBufferEmpty=$beforeBufferEmpty afterBufferEmpty=$afterBufferEmpty " +
                "candidateCount=$candidateCount " +
                "preeditVisible=$preeditVisible " +
                "currentSymbolCategory=$symbolCategory")
        }
    }

    private fun isActionAllowedByPolicy(action: ImeInputAction): Boolean {
        val policy = EditorInputTypePolicy.resolve(currentEditorInfo)
        if (policy.allowChineseCandidates) return true
        return when (action) {
            ImeInputAction.ToggleChineseEnglish -> false
            is ImeInputAction.KeyboardModeSelected -> action.mode != io.github.xiwei753.pinyin.imecore.InputMode.ChineseT9
            else -> true
        }
    }

    internal fun handleKeyboardAction(action: String) {
        val inputAction = when {
            action == "separator" -> ImeInputAction.SeparatorPressed
            action.startsWith("digit:") -> ImeInputAction.DigitPressed(action.removePrefix("digit:"))
            action == "del" -> ImeInputAction.DeletePressed
            action == "retype" -> ImeInputAction.ClearComposing
            action == "space" -> ImeInputAction.SpacePressed
            action == "toggle:symbol" -> ImeInputAction.ToggleSymbol
            action == "toggle:english" -> ImeInputAction.ToggleChineseEnglish
            action == "toggle:number" -> ImeInputAction.ToggleNumber
            action.startsWith("punct:") -> ImeInputAction.PunctuationCommitted(action.removePrefix("punct:"))
            action.startsWith("reading:") -> action.removePrefix("reading:").toIntOrNull()?.let { ImeInputAction.ReadingSelected(it) }
            action.startsWith("symtab:") -> ImeInputAction.SymbolCategorySelected(action.removePrefix("symtab:"))
            action.startsWith("symbol:commit:") -> ImeInputAction.SymbolCommitted(action.removePrefix("symbol:commit:"))
            else -> null
        }
        if (inputAction != null) handleInputAction(inputAction)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditorInfo = info
        applyThemeAndHeight()
        if (this::handler.isInitialized) applyEditorContext(info, restarting)
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        currentEditorInfo = info
        if (this::handler.isInitialized) applyEditorContext(info, restarting)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (this::handler.isInitialized) handler.handle(ImeInputAction.LifecycleFinishInput)
        updateKeyboardPanel()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        if (this::handler.isInitialized) handler.handle(ImeInputAction.LifecycleFinishInput)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (this::handler.isInitialized) handler.handle(ImeInputAction.LifecycleFinishInput)
    }

    private fun applyEditorContext(info: EditorInfo?, restarting: Boolean = false) {
        val policy = EditorInputTypePolicy.resolve(info)
        if (settingsRepository.isDebugLoggingEnabled()) {
            val inputTypeVal = info?.inputType ?: 0
            val classMask = inputTypeVal and InputType.TYPE_MASK_CLASS
            val variation = inputTypeVal and InputType.TYPE_MASK_VARIATION
            val isPhone = classMask == InputType.TYPE_CLASS_PHONE
            val isNumber = classMask == InputType.TYPE_CLASS_NUMBER
            val isPassword = classMask == InputType.TYPE_CLASS_TEXT && (
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            ) || classMask == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            val isUrl = classMask == InputType.TYPE_CLASS_TEXT && (
                variation == InputType.TYPE_TEXT_VARIATION_URI ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
            )
            val isEmail = classMask == InputType.TYPE_CLASS_TEXT && (
                variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            )
            debugLogger.log("XiweiT9EditorPolicy",
                String.format("inputType=0x%08x imeOptions=0x%08x classMask=0x%08x variation=0x%08x " +
                    "defaultKeyboardMode=%s defaultLastTextMode=%s allowChineseCandidates=%s " +
                    "enterBehavior=%s restarting=%s " +
                    "password=%s number=%s phone=%s url=%s email=%s",
                    inputTypeVal, info?.imeOptions ?: 0, classMask, variation,
                    policy.defaultKeyboardMode, policy.defaultLastTextMode, policy.allowChineseCandidates,
                    policy.enterBehavior, restarting,
                    isPassword, isNumber, isPhone, isUrl, isEmail))
        }
        handler.beginInputContext(policy.defaultKeyboardMode, policy.defaultLastTextMode)
        renderCurrentState()
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
        xiweiKeyboardView.activeSymCategory = handler.uiState(isDictPreparing).currentSymbolCategory
        xiweiKeyboardView.lastTextMode = handler.lastTextMode

        rebuildLayoutModel()
        heightController.applyHeight(keyboardViews, metrics)
    }

    private fun rebuildLayoutModel() {
        if (!this::handler.isInitialized) return
        val state = buildKeyboardUiState()
        renderFromState(state)
    }

    private fun rebuildLayoutModel(state: KeyboardUiState): KeyboardLayoutModel {
        if (!this::xiweiKeyboardView.isInitialized || !this::handler.isInitialized) {
            return KeyboardLayoutModel(emptyList(), emptyList(), null, 0, 0)
        }

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

        return layoutBuilder.build(
            state = state,
            panelWidth = panelWidth,
            panelHeight = panelHeight,
            rowHeight = metrics.rowHeightPx,
            bottomRowHeight = metrics.bottomRowHeightPx,
            horizontalGap = hGap,
            verticalGap = vGap,
            categoryToPage = categoryToPage,
            registry = registry,
            density = density,
            symbolEntries = if (state.keyboardMode == KeyboardMode.Symbol) getEntriesForPage(state.currentSymCategory) else emptyList(),
        )
    }

    private fun renderFromState(state: KeyboardUiState) {
        if (!this::xiweiKeyboardView.isInitialized) return
        xiweiKeyboardView.keyboardMode = state.keyboardMode
        xiweiKeyboardView.activeSymCategory = state.currentSymCategory
        xiweiKeyboardView.lastTextMode = state.lastTextMode
        xiweiKeyboardView.palette = state.themePalette

        val model = rebuildLayoutModel(state)
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
        renderCurrentState()
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

    private fun buildKeyboardUiState(): KeyboardUiState {
        val palette = if (this::themeController.isInitialized) themeController.getThemePalette() else ThemePalette(
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
        val baseState = if (this::handler.isInitialized) {
            handler.uiState(isDictPreparing).toAndroidKeyboardUiState(palette)
        } else {
            KeyboardUiState(
                keyboardMode = KeyboardMode.ChineseT9,
                lastTextMode = KeyboardMode.ChineseT9,
                rawBuffer = "",
                preedit = "",
                readings = emptyList(),
                activeReading = null,
                candidatesSnapshot = emptyList(),
                currentSymCategory = "punct",
                isComposing = false,
                themePalette = palette
            )
        }
        val policy = EditorInputTypePolicy.resolve(currentEditorInfo)
        if (policy.allowChineseCandidates) return baseState
        return baseState.copy(
            candidateStripState = baseState.candidateStripState.copy(visible = false, candidates = emptyList()),
            compositionState = baseState.compositionState.copy(preedit = "", readings = emptyList(), activeReading = null),
            preeditState = baseState.preeditState.copy(visible = false, text = ""),
        )
    }

    override fun refreshUi() {
        if (!this::keyboardViews.isInitialized || !this::handler.isInitialized || !this::candidateViewController.isInitialized) return
        renderCurrentState()
        logDebugInfo()
    }

    private fun renderCurrentState() {
        if (!this::keyboardViews.isInitialized || !this::handler.isInitialized || !this::candidateViewController.isInitialized) return
        val state = buildKeyboardUiState()
        candidateViewController.refreshFromState(state)
        if (this::xiweiKeyboardView.isInitialized) {
            renderFromState(state)
        }
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
