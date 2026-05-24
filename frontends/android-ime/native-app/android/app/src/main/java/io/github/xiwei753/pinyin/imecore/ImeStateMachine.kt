package io.github.xiwei753.pinyin.imecore

class ImeStateMachine(
    private val candidateLimitProvider: () -> Int = { 9 },
    private val deferCandidateComputation: Boolean = false,
) {
    var engine: T9InputEngine? = null
        private set

    var mode: InputMode = InputMode.ChineseT9
        private set

    var lastTextMode: InputMode = InputMode.ChineseT9
        private set

    var currentSymbolCategory: String = "punct"
        private set

    private var candidateSelections: List<CandidateSelection> = emptyList()
    private var candidateLimit: Int = candidateLimitProvider()
    private var fallbackBuffer: String = ""
    private var pendingCandidateRequest: CandidateRequest? = null
    private var nextCandidateRequestId: Long = 0L

    private val englishMultiTapLetters = mapOf(
        '2' to "abc", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mno", '7' to "pqrs", '8' to "tuv", '9' to "wxyz"
    )
    private var englishDigit = ' '
    private var englishIndex = 0
    var englishPending = false
        private set

    val currentCandidates: List<CandidateSnapshotItem> get() = candidateSelections.map { it.snapshot }
    val rawBuffer: String get() = engine?.buffer ?: fallbackBuffer
    val readings: List<String> get() = engine?.readings ?: emptyList()
    val activeReading: String? get() = engine?.activeReading
    val preedit: String
        get() {
            if (mode == InputMode.EnglishT9 && englishPending) {
                val letters = englishMultiTapLetters[englishDigit]
                if (letters != null && englishIndex < letters.length) return letters[englishIndex].toString()
            }
            val currentEngine = engine ?: return fallbackBuffer
            return if (deferCandidateComputation) currentEngine.getPreeditHint() else currentEngine.getPreedit()
        }

    fun attachEngine(newEngine: T9InputEngine) {
        engine = newEngine
        for (char in fallbackBuffer) newEngine.inputDigit(char.toString())
        fallbackBuffer = ""
        refreshCandidates()
    }

    fun drainPendingCandidateRequest(): CandidateRequest? {
        val request = pendingCandidateRequest
        pendingCandidateRequest = null
        return request
    }

    fun applyCandidateResult(result: CandidateResult): Boolean {
        if (!deferCandidateComputation) return false
        if (result.requestId != nextCandidateRequestId) return false
        if (result.buffer != rawBuffer) return false
        if (result.lockedSyllables != (engine?.lockedSyllables ?: emptyList<String>())) return false
        candidateSelections = result.candidates.map { snapshot ->
            CandidateSelection(snapshot) { engine?.commitCandidate(snapshot) }
        }
        return true
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
            is ImeInputAction.KeyboardModeSelected -> switchMode(action.mode, effects)
            is ImeInputAction.CandidateLimitChanged -> {
                candidateLimit = action.limit
                refreshCandidates()
                effects.add(ImeSideEffect.RefreshUi)
            }
            is ImeInputAction.SymbolCommitted -> {
                effects.add(ImeSideEffect.CommitText(action.text))
                effects.add(ImeSideEffect.RefreshUi)
            }
            is ImeInputAction.PunctuationCommitted -> onPunctuationCommitted(action.text, effects)
            is ImeInputAction.SymbolCategorySelected -> {
                currentSymbolCategory = action.category
                effects.add(ImeSideEffect.RefreshUi)
            }
            is ImeInputAction.CandidateSelected -> onCandidateSelected(action.index, effects)
            is ImeInputAction.ReadingSelected -> onReadingSelected(action.index, effects)
            ImeInputAction.ClearComposing -> clearComposing(effects, finishComposing = false)
            is ImeInputAction.LifecycleStartInput -> onLifecycleStart(action, effects)
            ImeInputAction.LifecycleFinishInput -> {
                clearComposing(effects, finishComposing = true)
                mode = InputMode.ChineseT9
                lastTextMode = InputMode.ChineseT9
                currentSymbolCategory = "punct"
                effects.add(ImeSideEffect.RefreshUi)
            }
            
            // Clipboard actions
            is ImeInputAction.ClipboardItemClicked -> {
                effects.add(ImeSideEffect.CommitText(action.text))
                val targetMode = if (lastTextMode == InputMode.ChineseT9 || lastTextMode == InputMode.EnglishT9) lastTextMode else InputMode.ChineseT9
                switchMode(targetMode, effects)
            }
            ImeInputAction.ClipboardPageUp -> {
                effects.add(ImeSideEffect.ClipboardPageUp)
            }
            ImeInputAction.ClipboardPageDown -> {
                effects.add(ImeSideEffect.ClipboardPageDown)
            }
            ImeInputAction.ClosePanel -> {
                val targetMode = if (lastTextMode == InputMode.ChineseT9 || lastTextMode == InputMode.EnglishT9) lastTextMode else InputMode.ChineseT9
                switchMode(targetMode, effects)
            }
            
            // Selection actions
            ImeInputAction.SelectionMoveLeft -> effects.add(ImeSideEffect.SelectionMoveLeft)
            ImeInputAction.SelectionMoveRight -> effects.add(ImeSideEffect.SelectionMoveRight)
            ImeInputAction.SelectionMoveUp -> effects.add(ImeSideEffect.SelectionMoveUp)
            ImeInputAction.SelectionMoveDown -> effects.add(ImeSideEffect.SelectionMoveDown)
            ImeInputAction.SelectionSelectAll -> effects.add(ImeSideEffect.SelectionSelectAll)
            ImeInputAction.SelectionCopy -> effects.add(ImeSideEffect.SelectionCopy)
            ImeInputAction.SelectionCut -> effects.add(ImeSideEffect.SelectionCut)
            ImeInputAction.SelectionPaste -> effects.add(ImeSideEffect.SelectionPaste)
            ImeInputAction.SelectionUndo -> effects.add(ImeSideEffect.SelectionUndo)

        }
        return effects
    }

    private fun onLifecycleStart(action: ImeInputAction.LifecycleStartInput, effects: MutableList<ImeSideEffect>) {
        clearComposing(effects, finishComposing = true)
        mode = action.initialMode
        lastTextMode = action.initialLastTextMode
        currentSymbolCategory = "punct"
        refreshCandidates()
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun refreshCandidates() {
        if (mode != InputMode.ChineseT9) {
            candidateSelections = emptyList()
            pendingCandidateRequest = null
            nextCandidateRequestId++
            return
        }

        if (rawBuffer.isEmpty()) {
            candidateSelections = emptyList()
            pendingCandidateRequest = null
            nextCandidateRequestId++
            return
        }

        val currentEngine = engine
        if (deferCandidateComputation) {
            candidateSelections = emptyList()
            val request = CandidateRequest(
                requestId = ++nextCandidateRequestId,
                buffer = rawBuffer,
                lockedSyllables = currentEngine?.lockedSyllables?.toList() ?: emptyList(),
                limit = candidateLimit,
            )
            pendingCandidateRequest = request
            return
        }

        candidateSelections = currentEngine?.getVisibleCandidates(candidateLimit) ?: emptyList()
    }

    fun uiState(isDictionaryPreparing: Boolean = false): ImeUiState {
        val composing = rawBuffer.isNotEmpty()
        val isTextMode = mode == InputMode.ChineseT9 || mode == InputMode.EnglishT9
        val candidatesSnapshot = currentCandidates
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
            InputMode.ClipboardPanel, InputMode.SelectionPanel -> {}
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
                } else if (candidateSelections.isNotEmpty()) {
                    commitCandidate(candidateSelections[0], effects)
                }
            }
            InputMode.EnglishT9 -> {
                commitEnglishChar(effects)
                effects.add(ImeSideEffect.CommitText(" "))
            }
            InputMode.Number -> effects.add(ImeSideEffect.CommitText("0"))
            InputMode.Symbol -> effects.add(ImeSideEffect.CommitText("0"))
            InputMode.ClipboardPanel, InputMode.SelectionPanel -> {}
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
                else if (candidateSelections.isNotEmpty()) commitCandidate(candidateSelections[0], effects)
                else if (preedit.isNotEmpty()) commitPreedit(effects)
            }
            InputMode.EnglishT9 -> {
                commitEnglishChar(effects)
                effects.add(ImeSideEffect.CommitText(" "))
            }
            InputMode.Symbol, InputMode.Number -> effects.add(ImeSideEffect.CommitText(" "))
            InputMode.ClipboardPanel, InputMode.SelectionPanel -> {}
        }
    }

    private fun onPunctuationCommitted(text: String, effects: MutableList<ImeSideEffect>) {
        when {
            mode == InputMode.ChineseT9 && rawBuffer.isNotEmpty() -> commitFirstCandidateOrPreedit(effects)
            mode == InputMode.EnglishT9 && englishPending -> commitEnglishChar(effects)
        }
        effects.add(ImeSideEffect.CommitText(text))
        effects.add(ImeSideEffect.RefreshUi)
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
            (targetMode == InputMode.Symbol || targetMode == InputMode.Number ||
             targetMode == InputMode.ClipboardPanel || targetMode == InputMode.SelectionPanel)) {
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
        if (mode != InputMode.ChineseT9 || index !in candidateSelections.indices) return
        commitCandidate(candidateSelections[index], effects)
    }

    private fun onReadingSelected(index: Int, effects: MutableList<ImeSideEffect>) {
        val reading = readings.getOrNull(index) ?: return
        if (engine?.setActiveReading(reading) == true) {
            refreshCandidates()
            effects.add(ImeSideEffect.RefreshUi)
        }
    }

    private fun commitFirstCandidateOrPreedit(effects: MutableList<ImeSideEffect>) {
        if (candidateSelections.isNotEmpty()) commitCandidate(candidateSelections[0], effects)
        else if (preedit.isNotEmpty()) commitPreedit(effects)
    }

    private fun commitCandidate(candidate: CandidateSelection, effects: MutableList<ImeSideEffect>) {
        candidate.commit()
        candidateSelections = emptyList()
        fallbackBuffer = ""
        pendingCandidateRequest = null
        nextCandidateRequestId++
        effects.add(ImeSideEffect.CommitCandidate(candidate.snapshot.text))
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun commitPreedit(effects: MutableList<ImeSideEffect>) {
        effects.add(ImeSideEffect.CommitText(preedit))
        engine?.clear()
        fallbackBuffer = ""
        candidateSelections = emptyList()
        pendingCandidateRequest = null
        nextCandidateRequestId++
        effects.add(ImeSideEffect.RefreshUi)
    }

    private fun clearComposing(effects: MutableList<ImeSideEffect>, finishComposing: Boolean) {
        engine?.clear()
        fallbackBuffer = ""
        candidateSelections = emptyList()
        pendingCandidateRequest = null
        nextCandidateRequestId++
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
