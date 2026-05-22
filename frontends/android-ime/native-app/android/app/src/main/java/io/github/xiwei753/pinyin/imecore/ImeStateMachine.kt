package io.github.xiwei753.pinyin.imecore

import io.github.xiwei753.pinyin.t9.core.Candidate

class ImeStateMachine(
    private val candidateLimitProvider: () -> Int = { 9 },
) {
    var engine: T9InputEngine? = null
        private set

    var mode: InputMode = InputMode.ChineseT9
        private set

    var lastTextMode: InputMode = InputMode.ChineseT9
        private set

    var currentSymbolCategory: String = "punct"
        private set

    private var candidatesSnapshot: List<Candidate> = emptyList()
    private var fallbackBuffer: String = ""

    private val englishMultiTapLetters = mapOf(
        '2' to "abc", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mno", '7' to "pqrs", '8' to "tuv", '9' to "wxyz"
    )
    private var englishDigit = ' '
    private var englishIndex = 0
    var englishPending = false
        private set

    val currentCandidates: List<Candidate> get() = candidatesSnapshot
    val rawBuffer: String get() = engine?.buffer ?: fallbackBuffer
    val readings: List<String> get() = engine?.readings ?: emptyList()
    val activeReading: String? get() = engine?.activeReading
    val preedit: String
        get() {
            if (mode == InputMode.EnglishT9 && englishPending) {
                val letters = englishMultiTapLetters[englishDigit]
                if (letters != null && englishIndex < letters.length) return letters[englishIndex].toString()
            }
            return engine?.getPreedit() ?: fallbackBuffer
        }

    fun attachEngine(newEngine: T9InputEngine) {
        engine = newEngine
        for (char in fallbackBuffer) newEngine.inputDigit(char.toString())
        fallbackBuffer = ""
        refreshCandidates()
    }

    fun dispatch(action: ImeInputAction): List<ImeSideEffect> {
        val effects = mutableListOf<ImeSideEffect>()
        when (action) {
            is ImeInputAction.DigitPressed -> onDigitPressed(action.digit, effects)
            ImeInputAction.SeparatorPressed -> onSeparator(effects)
            ImeInputAction.ZeroPressed -> onZero(effects)
            ImeInputAction.DeletePressed -> onDelete(effects)
            ImeInputAction.EnterShortPressed -> onEnterShort(effects)
            ImeInputAction.EnterLongPressed -> onEnterLong(effects)
            ImeInputAction.SpacePressed -> onSpace(effects)
            ImeInputAction.ToggleSymbol -> onToggleSymbol(effects)
            ImeInputAction.ToggleNumber -> onToggleNumber(effects)
            ImeInputAction.ToggleChineseEnglish -> onToggleChineseEnglish(effects)
            is ImeInputAction.SymbolCommitted -> {
                effects.add(ImeSideEffect.CommitText(action.text))
                effects.add(ImeSideEffect.RefreshUi)
            }
            is ImeInputAction.SymbolCategorySelected -> {
                currentSymbolCategory = action.category
                effects.add(ImeSideEffect.RefreshUi)
            }
            is ImeInputAction.CandidateSelected -> onCandidateSelected(action.index, effects)
            is ImeInputAction.ReadingSelected -> onReadingSelected(action.index, effects)
            ImeInputAction.ClearComposing -> clearComposing(effects, finishComposing = false)
            ImeInputAction.LifecycleStartInput -> clearComposing(effects, finishComposing = true)
            ImeInputAction.LifecycleFinishInput -> {
                clearComposing(effects, finishComposing = true)
                mode = InputMode.ChineseT9
                effects.add(ImeSideEffect.RefreshUi)
            }
        }
        return effects
    }

    fun refreshCandidates(limit: Int = candidateLimitProvider()): List<Candidate> {
        candidatesSnapshot = if (mode == InputMode.ChineseT9) {
            engine?.getVisibleCandidates(limit) ?: emptyList()
        } else {
            emptyList()
        }
        return candidatesSnapshot
    }

    fun uiState(isDictionaryPreparing: Boolean = false): ImeUiState {
        val composing = rawBuffer.isNotEmpty()
        val isTextMode = mode == InputMode.ChineseT9 || mode == InputMode.EnglishT9
        val candidateVisible = candidatesSnapshot.isNotEmpty() || isDictionaryPreparing
        val rail = when (mode) {
            InputMode.Symbol -> RailState(RailKind.SymbolCategories, listOf("标点", "数学", "括号", "其他"))
            InputMode.Number -> RailState(RailKind.NumberAux, listOf(".", "0"))
            else -> if (composing) RailState(RailKind.Readings, readings) else RailState(RailKind.Punctuation)
        }
        val symbolPanel = SymbolPanelState(currentSymbolCategory)
        val surface = KeyboardSurfaceState(
            mode = mode,
            lastTextMode = lastTextMode,
            railState = rail,
            symbolPanelState = symbolPanel,
            bottomRowKeys = bottomRowKeys(mode),
        )
        return ImeUiState(
            mode = mode,
            lastTextMode = lastTextMode,
            composition = CompositionState(rawBuffer, preedit, readings, activeReading),
            candidateStrip = CandidateStripState(candidateVisible, candidatesSnapshot, isDictionaryPreparing),
            preeditState = PreeditState(isTextMode && composing && preedit.isNotEmpty(), preedit),
            keyboardSurface = surface,
            symbolPanel = symbolPanel,
            isDictionaryPreparing = isDictionaryPreparing,
        )
    }

    private fun bottomRowKeys(currentMode: InputMode): List<String> = when (currentMode) {
        InputMode.Symbol -> listOf("123", "space", "disabled")
        InputMode.Number -> listOf(if (lastTextMode == InputMode.EnglishT9) "英" else "中", "space", "中/英")
        else -> listOf("符", "space", "中/英")
    }

    private fun onDigitPressed(digit: String, effects: MutableList<ImeSideEffect>) {
        when (mode) {
            InputMode.ChineseT9 -> {
                val eng = engine
                if (eng != null) eng.inputDigit(digit) else if (digit.matches(Regex("^[1-9]$"))) fallbackBuffer += digit
                refreshCandidates()
                effects.add(ImeSideEffect.RefreshUi)
            }
            InputMode.EnglishT9 -> onEnglishDigit(digit, effects)
            InputMode.Number, InputMode.Symbol -> {
                effects.add(ImeSideEffect.CommitText(digit))
                effects.add(ImeSideEffect.RefreshUi)
            }
        }
    }

    private fun onEnglishDigit(digit: String, effects: MutableList<ImeSideEffect>) {
        if (digit.length != 1) return
        val d = digit[0]
        val letters = englishMultiTapLetters[d]
        if (letters == null) {
            commitEnglishChar(effects)
            return
        }
        if (englishPending && d == englishDigit) {
            englishIndex = (englishIndex + 1) % letters.length
        } else {
            commitEnglishChar(effects)
            englishDigit = d
            englishIndex = 0
        }
        englishPending = true
        effects.add(ImeSideEffect.CancelEnglishTimeout)
        effects.add(ImeSideEffect.ScheduleEnglishTimeout(600L))
        effects.add(ImeSideEffect.RefreshUi)
    }

    fun commitEnglishChar(effects: MutableList<ImeSideEffect>) {
        if (!englishPending) return
        englishPending = false
        effects.add(ImeSideEffect.CancelEnglishTimeout)
        val letters = englishMultiTapLetters[englishDigit] ?: run { englishDigit = ' '; return }
        if (englishIndex < letters.length) effects.add(ImeSideEffect.CommitText(letters[englishIndex].toString()))
        englishDigit = ' '
        englishIndex = 0
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun onSeparator(effects: MutableList<ImeSideEffect>) {
        if (mode != InputMode.ChineseT9 || rawBuffer.isEmpty()) return
        engine?.inputDigit("1") ?: run { fallbackBuffer += "1" }
        refreshCandidates()
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun onZero(effects: MutableList<ImeSideEffect>) {
        when (mode) {
            InputMode.ChineseT9 -> {
                if (rawBuffer.isEmpty()) {
                    effects.add(ImeSideEffect.CommitText(" "))
                } else if (candidatesSnapshot.isNotEmpty()) {
                    commitCandidate(candidatesSnapshot[0], effects)
                }
            }
            InputMode.EnglishT9 -> {
                commitEnglishChar(effects)
                effects.add(ImeSideEffect.CommitText(" "))
            }
            InputMode.Number -> effects.add(ImeSideEffect.CommitText("0"))
            InputMode.Symbol -> effects.add(ImeSideEffect.CommitText("0"))
        }
    }

    private fun onDelete(effects: MutableList<ImeSideEffect>) {
        if (mode == InputMode.EnglishT9 && englishPending) {
            if (englishIndex > 0) {
                englishIndex--
                effects.add(ImeSideEffect.RefreshUi)
            } else {
                commitEnglishChar(effects)
                effects.add(ImeSideEffect.SendDelete)
                effects.add(ImeSideEffect.RefreshUi)
            }
            return
        }
        if (mode == InputMode.ChineseT9 && rawBuffer.isNotEmpty()) {
            engine?.backspace() ?: run { fallbackBuffer = fallbackBuffer.dropLast(1) }
            refreshCandidates()
            effects.add(ImeSideEffect.RefreshUi)
            return
        }
        effects.add(ImeSideEffect.SendDelete)
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun onSpace(effects: MutableList<ImeSideEffect>) {
        when (mode) {
            InputMode.ChineseT9 -> {
                if (rawBuffer.isEmpty()) effects.add(ImeSideEffect.CommitText(" "))
                else if (candidatesSnapshot.isNotEmpty()) commitCandidate(candidatesSnapshot[0], effects)
                else if (preedit.isNotEmpty()) commitPreedit(effects)
            }
            InputMode.EnglishT9 -> {
                commitEnglishChar(effects)
                effects.add(ImeSideEffect.CommitText(" "))
            }
            InputMode.Symbol, InputMode.Number -> effects.add(ImeSideEffect.CommitText(" "))
        }
    }

    private fun onToggleSymbol(effects: MutableList<ImeSideEffect>) {
        when (mode) {
            InputMode.Symbol -> switchMode(lastTextMode, effects)
            InputMode.Number -> switchMode(InputMode.Symbol, effects)
            else -> switchMode(InputMode.Symbol, effects)
        }
    }

    private fun onToggleNumber(effects: MutableList<ImeSideEffect>) {
        when (mode) {
            InputMode.Number -> switchMode(lastTextMode, effects)
            InputMode.Symbol -> switchMode(InputMode.Number, effects)
            else -> switchMode(InputMode.Number, effects)
        }
    }

    private fun onToggleChineseEnglish(effects: MutableList<ImeSideEffect>) {
        when (mode) {
            InputMode.EnglishT9 -> switchMode(InputMode.ChineseT9, effects)
            InputMode.ChineseT9 -> switchMode(InputMode.EnglishT9, effects)
            else -> switchMode(InputMode.ChineseT9, effects)
        }
    }

    fun switchMode(targetMode: InputMode, effects: MutableList<ImeSideEffect>) {
        if (mode == targetMode) return
        val oldMode = mode
        leavingCurrentMode(effects)
        if ((oldMode == InputMode.ChineseT9 || oldMode == InputMode.EnglishT9) &&
            (targetMode == InputMode.Symbol || targetMode == InputMode.Number)) {
            lastTextMode = oldMode
        }
        mode = targetMode
        refreshCandidates()
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun leavingCurrentMode(effects: MutableList<ImeSideEffect>) {
        when (mode) {
            InputMode.ChineseT9 -> if (rawBuffer.isNotEmpty()) commitFirstCandidateOrPreedit(effects)
            InputMode.EnglishT9 -> if (englishPending) commitEnglishChar(effects)
            else -> {}
        }
        effects.add(ImeSideEffect.FinishComposingText)
    }

    private fun onEnterShort(effects: MutableList<ImeSideEffect>) {
        if (mode == InputMode.ChineseT9 && rawBuffer.isNotEmpty()) commitFirstCandidateOrPreedit(effects)
        if (mode == InputMode.EnglishT9 && englishPending) commitEnglishChar(effects)
        effects.add(ImeSideEffect.CommitNewline)
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun onEnterLong(effects: MutableList<ImeSideEffect>) {
        if (mode == InputMode.ChineseT9 && rawBuffer.isNotEmpty()) commitFirstCandidateOrPreedit(effects)
        if (mode == InputMode.EnglishT9 && englishPending) commitEnglishChar(effects)
        effects.add(ImeSideEffect.CommitNewline)
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun onCandidateSelected(index: Int, effects: MutableList<ImeSideEffect>) {
        if (mode != InputMode.ChineseT9 || index !in candidatesSnapshot.indices) return
        commitCandidate(candidatesSnapshot[index], effects)
    }

    private fun onReadingSelected(index: Int, effects: MutableList<ImeSideEffect>) {
        val reading = readings.getOrNull(index) ?: return
        if (engine?.setActiveReading(reading) == true) {
            refreshCandidates()
            effects.add(ImeSideEffect.RefreshUi)
        }
    }

    private fun commitFirstCandidateOrPreedit(effects: MutableList<ImeSideEffect>) {
        if (candidatesSnapshot.isNotEmpty()) commitCandidate(candidatesSnapshot[0], effects)
        else if (preedit.isNotEmpty()) commitPreedit(effects)
    }

    private fun commitCandidate(candidate: Candidate, effects: MutableList<ImeSideEffect>) {
        engine?.commitCandidate(candidate)
        candidatesSnapshot = emptyList()
        fallbackBuffer = ""
        effects.add(ImeSideEffect.RecordUserSelection(candidate))
        effects.add(ImeSideEffect.CommitCandidate(candidate.text))
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun commitPreedit(effects: MutableList<ImeSideEffect>) {
        effects.add(ImeSideEffect.CommitText(preedit))
        engine?.clear()
        fallbackBuffer = ""
        candidatesSnapshot = emptyList()
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun clearComposing(effects: MutableList<ImeSideEffect>, finishComposing: Boolean) {
        engine?.clear()
        fallbackBuffer = ""
        candidatesSnapshot = emptyList()
        if (englishPending) {
            englishPending = false
            effects.add(ImeSideEffect.CancelEnglishTimeout)
            englishDigit = ' '
            englishIndex = 0
        }
        if (finishComposing) effects.add(ImeSideEffect.FinishComposingText)
        effects.add(ImeSideEffect.RefreshUi)
    }
}
