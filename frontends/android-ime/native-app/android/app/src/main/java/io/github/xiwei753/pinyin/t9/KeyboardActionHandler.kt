package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.imecore.ImeInputAction
import io.github.xiwei753.pinyin.imecore.ImeSideEffect
import io.github.xiwei753.pinyin.imecore.ImeStateMachine
import io.github.xiwei753.pinyin.imecore.ImeUiState
import io.github.xiwei753.pinyin.imecore.InputMode
import io.github.xiwei753.pinyin.imecore.CandidateSnapshotItem
import io.github.xiwei753.pinyin.t9.core.T9Engine

class KeyboardActionHandler(
    val actionSink: ImeActionSink,
    private val candidateLimitProvider: () -> Int = { 30 },
) {
    private val stateMachine = ImeStateMachine(candidateLimitProvider)
    private val englishTimeoutRunnable = Runnable { commitEnglishChar() }
    private var engineAdapter: T9EngineAdapter? = null

    val engine: T9Engine? get() = engineAdapter?.rawEngine

    var keyboardMode: KeyboardMode
        get() = stateMachine.mode.toKeyboardMode()
        private set(value) { switchMode(value) }

    var lastTextMode: KeyboardMode
        get() = stateMachine.lastTextMode.toKeyboardMode()
        set(value) {
            handle(ImeInputAction.KeyboardModeSelected(value.toInputMode()))
        }

    val currentCandidates: List<CandidateSnapshotItem> get() = stateMachine.currentCandidates
    val preedit: String get() = stateMachine.preedit
    val rawBuffer: String get() = stateMachine.rawBuffer
    val readings: List<String> get() = stateMachine.readings
    var englishPending: Boolean
        get() = stateMachine.englishPending
        private set(_) {}

    var activeReading: String?
        get() = stateMachine.activeReading
        set(value) {
            if (value != null) setActiveReading(value)
        }

    fun attachEngine(newEngine: T9Engine) {
        engineAdapter = T9EngineAdapter(newEngine)
        stateMachine.attachEngine(engineAdapter!!)
    }

    fun uiState(isDictionaryPreparing: Boolean = false): ImeUiState = stateMachine.uiState(isDictionaryPreparing)

    fun handle(action: ImeInputAction) {
        when (action) {
            ImeInputAction.EnterShortPressed -> onEnterShortPress()
            else -> execute(stateMachine.dispatch(action))
        }
    }

    private fun switchMode(targetMode: KeyboardMode) {
        handle(ImeInputAction.KeyboardModeSelected(targetMode.toInputMode()))
    }

    @Deprecated("Use handle(ImeInputAction.ToggleSymbol); this compatibility wrapper must stay logic-free.")
    fun toggleSymbolKey() = handle(ImeInputAction.ToggleSymbol)
    @Deprecated("Use handle(ImeInputAction.ToggleNumber); this compatibility wrapper must stay logic-free.")
    fun toggleNumberKey() = handle(ImeInputAction.ToggleNumber)
    @Deprecated("Use handle(ImeInputAction.DigitPressed); this compatibility wrapper must stay logic-free.")
    fun onDigitPressed(digit: String) = handle(ImeInputAction.DigitPressed(digit))
    @Deprecated("Use handle(ImeInputAction.SeparatorPressed); this compatibility wrapper must stay logic-free.")
    fun onSeparator() = handle(ImeInputAction.SeparatorPressed)
    @Deprecated("Use handle(ImeInputAction.ZeroPressed); this compatibility wrapper must stay logic-free.")
    fun onZero() = handle(ImeInputAction.ZeroPressed)
    @Deprecated("Use handle(ImeInputAction.DeletePressed); this compatibility wrapper must stay logic-free.")
    fun onDelete() = handle(ImeInputAction.DeletePressed)
    fun onClearComposingForRetype() = handle(ImeInputAction.ClearComposing)
    @Deprecated("Use handle(ImeInputAction.SpacePressed); this compatibility wrapper must stay logic-free.")
    fun onSpace() = handle(ImeInputAction.SpacePressed)
    @Deprecated("Use handle(ImeInputAction.CandidateSelected); this compatibility wrapper must stay logic-free.")
    fun onCandidateClick(index: Int) = handle(ImeInputAction.CandidateSelected(index))
    fun updateCandidateLimit(limit: Int) = handle(ImeInputAction.CandidateLimitChanged(limit))

    fun setActiveReading(reading: String): Boolean {
        val index = readings.indexOf(reading)
        if (index < 0) return false
        handle(ImeInputAction.ReadingSelected(index))
        return true
    }

    fun commitEnglishChar() {
        val effects = mutableListOf<ImeSideEffect>()
        stateMachine.commitEnglishChar(effects)
        execute(effects)
    }

    @Deprecated("Use handle(ImeInputAction.PunctuationCommitted); this compatibility wrapper must stay logic-free.")
    fun onPunctCommit(text: String) {
        handle(ImeInputAction.PunctuationCommitted(text))
    }

    fun onEnter() = onEnterShortPress()

    fun onEnterShortPress() {
        // Android adapter boundary: core EnterShortPressed describes default composing/newline behavior.
        // This path additionally applies EditorInfo action policy and must not move into imecore.
        val hasComposing = when (keyboardMode) {
            KeyboardMode.ChineseT9 -> rawBuffer.isNotEmpty()
            KeyboardMode.EnglishT9 -> englishPending
            else -> false
        }

        if (hasComposing) {
            if (keyboardMode == KeyboardMode.ChineseT9) handle(ImeInputAction.SpacePressed)
            else commitEnglishChar()
        }

        val editorInfo = actionSink.getCurrentEditorInfo()
        if (EnterActionPolicy.shouldSend(editorInfo)) {
            actionSink.performEditorAction(EnterActionPolicy.getAction(editorInfo))
            actionSink.refreshUi()
            return
        }
        if (hasComposing) {
            actionSink.commitNewline()
            actionSink.refreshUi()
            return
        }
        if (EnterActionPolicy.shouldRunExplicitAction(editorInfo)) {
            actionSink.performEditorAction(EnterActionPolicy.getAction(editorInfo))
            actionSink.refreshUi()
            return
        }
        actionSink.commitNewline()
        actionSink.refreshUi()
    }

    fun onEnterLongPress() = handle(ImeInputAction.EnterLongPressed)

    @Deprecated("Candidate snapshot is owned by ImeStateMachine; use CandidateLimitChanged during input handling and uiState() during render.")
    fun refreshCandidates(limit: Int): List<CandidateSnapshotItem> {
        updateCandidateLimit(limit)
        return currentCandidates
    }

    @Deprecated("Use handle(ImeInputAction.ToggleSymbol/ToggleNumber/...) or lifecycle helpers; this compatibility wrapper must stay logic-free.")
    fun switchKeyboardMode(targetMode: KeyboardMode) = handle(ImeInputAction.KeyboardModeSelected(targetMode.toInputMode()))

    fun discardCompositionForLifecycle() = handle(ImeInputAction.LifecycleStartInput)

    fun resetToChineseModeForLifecycle() = switchMode(KeyboardMode.ChineseT9)

    fun onHideKey() {
        if (keyboardMode == KeyboardMode.ChineseT9 && rawBuffer.isNotEmpty()) {
            handle(ImeInputAction.SpacePressed)
        } else if (keyboardMode == KeyboardMode.EnglishT9 && englishPending) {
            commitEnglishChar()
        }
        handle(ImeInputAction.LifecycleFinishInput)
    }

    private fun execute(effects: List<ImeSideEffect>) {
        for (effect in effects) {
            when (effect) {
                is ImeSideEffect.CommitText -> actionSink.commitText(effect.text)
                is ImeSideEffect.CommitCandidate -> actionSink.commitText(effect.text)
                ImeSideEffect.SendDelete -> actionSink.sendDelete()
                ImeSideEffect.CommitNewline -> actionSink.commitNewline()
                ImeSideEffect.FinishComposingText -> actionSink.finishComposingText()
                is ImeSideEffect.PerformEditorAction -> actionSink.performEditorAction(effect.action)
                ImeSideEffect.RefreshUi -> actionSink.refreshUi()
                ImeSideEffect.CancelEnglishTimeout -> actionSink.cancelEnglishTimeout()
                is ImeSideEffect.ScheduleEnglishTimeout -> actionSink.scheduleEnglishTimeout(englishTimeoutRunnable, effect.delayMs)
            }
        }
    }
}

fun KeyboardMode.toInputMode(): InputMode = when (this) {
    KeyboardMode.ChineseT9 -> InputMode.ChineseT9
    KeyboardMode.EnglishT9 -> InputMode.EnglishT9
    KeyboardMode.Number -> InputMode.Number
    KeyboardMode.Symbol -> InputMode.Symbol
}

fun InputMode.toKeyboardMode(): KeyboardMode = when (this) {
    InputMode.ChineseT9 -> KeyboardMode.ChineseT9
    InputMode.EnglishT9 -> KeyboardMode.EnglishT9
    InputMode.Number -> KeyboardMode.Number
    InputMode.Symbol -> KeyboardMode.Symbol
}
