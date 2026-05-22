package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9Engine

class KeyboardActionHandler(
    private val actionSink: ImeActionSink
) {
    var engine: T9Engine? = null
        private set

    var keyboardMode: KeyboardMode = KeyboardMode.ChineseT9
        private set

    var lastTextMode: KeyboardMode = KeyboardMode.ChineseT9

    private var _currentCandidates: List<Candidate> = emptyList()
    private var fallbackBuffer: String = ""

    val currentCandidates: List<Candidate> get() = _currentCandidates

    val preedit: String get() {
        if (keyboardMode == KeyboardMode.EnglishT9 && englishPending) {
            val letters = englishMultiTapLetters[englishDigit]
            if (letters != null && englishIndex < letters.length) {
                return letters[englishIndex].toString()
            }
        }
        return engine?.getPreedit() ?: fallbackBuffer
    }

    val rawBuffer: String get() = engine?.buffer ?: fallbackBuffer

    val readings: List<String>
        get() = engine?.readings ?: emptyList()

    var activeReading: String?
        get() = engine?.activeReading
        set(value) {
            engine?.let {
                if (value != null) it.setActiveReading(value) else { /* can't set null */ }
            }
        }

    fun setActiveReading(reading: String): Boolean {
        val success = engine?.setActiveReading(reading) ?: false
        if (success) {
            actionSink.refreshUi()
        }
        return success
    }

    private val englishMultiTapLetters = mapOf(
        '2' to "abc", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mno", '7' to "pqrs", '8' to "tuv", '9' to "wxyz"
    )
    private var englishDigit = ' '
    private var englishIndex = 0
    var englishPending = false
        private set
    private val englishTimeoutRunnable = Runnable { commitEnglishChar() }

    fun attachEngine(newEngine: T9Engine) {
        this.engine = newEngine
        for (char in fallbackBuffer) {
            newEngine.inputDigit(char.toString())
        }
        fallbackBuffer = ""
    }

    fun switchKeyboardMode(targetMode: KeyboardMode) {
        if (keyboardMode == targetMode) return
        val oldMode = keyboardMode
        leavingCurrentMode()
        if ((oldMode == KeyboardMode.ChineseT9 || oldMode == KeyboardMode.EnglishT9) &&
            (targetMode == KeyboardMode.Symbol || targetMode == KeyboardMode.Number)) {
            lastTextMode = oldMode
        }
        keyboardMode = targetMode
        actionSink.refreshUi()
    }

    fun toggleSymbolKey() {
        when (keyboardMode) {
            KeyboardMode.Symbol -> switchKeyboardMode(lastTextMode)
            KeyboardMode.Number -> switchKeyboardMode(KeyboardMode.Symbol)
            else -> switchKeyboardMode(KeyboardMode.Symbol)
        }
    }

    fun toggleNumberKey() {
        when (keyboardMode) {
            KeyboardMode.Number -> switchKeyboardMode(lastTextMode)
            KeyboardMode.Symbol -> switchKeyboardMode(KeyboardMode.Number)
            else -> switchKeyboardMode(KeyboardMode.Number)
        }
    }

    private fun leavingCurrentMode() {
        when (keyboardMode) {
            KeyboardMode.ChineseT9 -> {
                if (rawBuffer.isNotEmpty()) {
                    commitFirstCandidateOrPreedit()
                }
            }
            KeyboardMode.EnglishT9 -> {
                if (englishPending) {
                    commitEnglishChar()
                }
            }
            else -> {}
        }
        actionSink.finishComposingText()
    }

    private fun commitFirstCandidateOrPreedit() {
        if (_currentCandidates.isNotEmpty()) {
            onCandidateClick(0)
        } else if (preedit.isNotEmpty()) {
            actionSink.commitText(preedit)
            engine?.clear()
            fallbackBuffer = ""
            _currentCandidates = emptyList()
            actionSink.refreshUi()
        }
    }

    fun onDigitPressed(digit: String) {
        when (keyboardMode) {
            KeyboardMode.EnglishT9 -> onEnglishDigit(digit)
            KeyboardMode.ChineseT9 -> onChineseDigit(digit)
            KeyboardMode.Number, KeyboardMode.Symbol -> {
                actionSink.commitText(digit)
                actionSink.refreshUi()
            }
        }
    }

    private fun onChineseDigit(digit: String) {
        val eng = engine
        if (eng != null) {
            eng.inputDigit(digit)
        } else {
            if (digit.matches(Regex("^[1-9]$"))) {
                fallbackBuffer += digit
            }
        }
        actionSink.refreshUi()
    }

    private fun onEnglishDigit(digit: String) {
        if (digit.length != 1) return
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
        actionSink.cancelEnglishTimeout()
        actionSink.scheduleEnglishTimeout(englishTimeoutRunnable, 600L)
        actionSink.refreshUi()
    }

    fun commitEnglishChar() {
        if (!englishPending) return
        englishPending = false
        actionSink.cancelEnglishTimeout()
        val letters = englishMultiTapLetters[englishDigit] ?: run { englishDigit = ' '; return }
        if (englishIndex < letters.length) {
            actionSink.commitText(letters[englishIndex].toString())
        }
        englishDigit = ' '
        englishIndex = 0
        actionSink.refreshUi()
    }

    fun onSeparator() {
        if (keyboardMode == KeyboardMode.ChineseT9) {
            val eng = engine
            if (eng != null) {
                if (eng.buffer.isNotEmpty()) {
                    eng.inputDigit("1")
                    actionSink.refreshUi()
                }
            } else {
                if (fallbackBuffer.isNotEmpty()) {
                    fallbackBuffer += "1"
                    actionSink.refreshUi()
                }
            }
        }
    }

    fun onZero() {
        if (keyboardMode == KeyboardMode.EnglishT9) {
            commitEnglishChar()
            actionSink.commitText(" ")
            return
        }
        if (keyboardMode == KeyboardMode.ChineseT9) {
            val eng = engine
            if (eng != null) {
                if (eng.buffer.isEmpty()) {
                    actionSink.commitText(" ")
                } else if (_currentCandidates.isNotEmpty()) {
                    val candidate = _currentCandidates[0]
                    eng.commitCandidate(candidate)
                    _currentCandidates = emptyList()
                    actionSink.commitText(candidate.text)
                    actionSink.refreshUi()
                }
            } else {
                if (fallbackBuffer.isEmpty()) {
                    actionSink.commitText(" ")
                }
            }
        } else if (keyboardMode == KeyboardMode.Number) {
            actionSink.commitText("0")
        }
    }

    fun onClearComposingForRetype() {
        engine?.clear()
        fallbackBuffer = ""
        _currentCandidates = emptyList()

        if (englishPending) {
            englishPending = false
            actionSink.cancelEnglishTimeout()
            englishDigit = ' '
            englishIndex = 0
        }

        actionSink.refreshUi()
    }

    fun onSpace() {
        when (keyboardMode) {
            KeyboardMode.ChineseT9 -> {
                val eng = engine
                if (eng != null) {
                    if (eng.buffer.isEmpty()) {
                        actionSink.commitText(" ")
                    } else if (_currentCandidates.isNotEmpty()) {
                        val candidate = _currentCandidates[0]
                        eng.commitCandidate(candidate)
                        _currentCandidates = emptyList()
                        actionSink.commitText(candidate.text)
                        actionSink.refreshUi()
                    } else {
                        val text = eng.getPreedit()
                        if (text.isNotEmpty()) {
                            actionSink.commitText(text)
                            eng.clear()
                            fallbackBuffer = ""
                            _currentCandidates = emptyList()
                            actionSink.refreshUi()
                        } else {
                            actionSink.commitText(" ")
                        }
                    }
                } else {
                    if (fallbackBuffer.isNotEmpty()) {
                        actionSink.commitText(fallbackBuffer)
                        fallbackBuffer = ""
                        _currentCandidates = emptyList()
                        actionSink.refreshUi()
                    } else {
                        actionSink.commitText(" ")
                    }
                }
            }
            KeyboardMode.EnglishT9 -> {
                if (englishPending) {
                    commitEnglishChar()
                }
                actionSink.commitText(" ")
            }
            KeyboardMode.Symbol, KeyboardMode.Number -> {
                actionSink.commitText(" ")
            }
        }
    }

    fun onPunctCommit(text: String) {
        if (keyboardMode == KeyboardMode.ChineseT9 && rawBuffer.isNotEmpty()) {
            commitFirstCandidateOrPreedit()
        } else if (keyboardMode == KeyboardMode.EnglishT9 && englishPending) {
            commitEnglishChar()
        }
        actionSink.commitText(text)
        actionSink.refreshUi()
    }

    fun onDelete() {
        if (keyboardMode == KeyboardMode.EnglishT9 && englishPending) {
            if (englishIndex > 0) {
                englishIndex--
                actionSink.refreshUi()
            } else {
                commitEnglishChar()
                actionSink.sendDelete()
                actionSink.refreshUi()
            }
            return
        }
        if (keyboardMode == KeyboardMode.ChineseT9) {
            val eng = engine
            if (eng != null) {
                if (eng.buffer.isNotEmpty()) {
                    eng.backspace()
                    actionSink.refreshUi()
                    return
                }
            } else {
                if (fallbackBuffer.isNotEmpty()) {
                    fallbackBuffer = fallbackBuffer.substring(0, fallbackBuffer.length - 1)
                    actionSink.refreshUi()
                    return
                }
            }
        }
        actionSink.sendDelete()
        actionSink.refreshUi()
    }

    fun onEnter() {
        onEnterShortPress()
    }

    fun onEnterShortPress() {
        val hasComposing = when (keyboardMode) {
            KeyboardMode.ChineseT9 -> rawBuffer.isNotEmpty()
            KeyboardMode.EnglishT9 -> englishPending
            else -> false
        }

        if (hasComposing) {
            commitCurrentComposing()
        }

        val editorInfo = actionSink.getCurrentEditorInfo()

        // SEND: perform action after committing composing
        if (EnterActionPolicy.shouldSend(editorInfo)) {
            val action = EnterActionPolicy.getAction(editorInfo)
            actionSink.performEditorAction(action)
            actionSink.refreshUi()
            return
        }

        // Had composing and not SEND: insert newline
        if (hasComposing) {
            actionSink.commitNewline()
            actionSink.refreshUi()
            return
        }

        // SEARCH/GO/NEXT: still run the explicit action
        if (EnterActionPolicy.shouldRunExplicitAction(editorInfo)) {
            val action = EnterActionPolicy.getAction(editorInfo)
            actionSink.performEditorAction(action)
            actionSink.refreshUi()
            return
        }

        // Default (NONE/UNSPECIFIED/DONE): insert newline, do NOT close keyboard
        actionSink.commitNewline()
        actionSink.refreshUi()
    }

    fun onEnterLongPress() {
        when (keyboardMode) {
            KeyboardMode.ChineseT9 -> {
                if (rawBuffer.isNotEmpty()) {
                    commitFirstCandidateOrPreedit()
                }
            }
            KeyboardMode.EnglishT9 -> {
                if (englishPending) {
                    commitEnglishChar()
                }
            }
            else -> {}
        }
        actionSink.commitNewline()
        actionSink.refreshUi()
    }

    private fun commitCurrentComposing() {
        when (keyboardMode) {
            KeyboardMode.ChineseT9 -> {
                if (rawBuffer.isNotEmpty()) {
                    commitFirstCandidateOrPreedit()
                }
            }
            KeyboardMode.EnglishT9 -> {
                if (englishPending) {
                    commitEnglishChar()
                }
            }
            else -> {}
        }
    }

    fun onCandidateClick(index: Int) {
        if (keyboardMode != KeyboardMode.ChineseT9) return
        if (index < 0 || index >= _currentCandidates.size) return
        val candidate = _currentCandidates[index]
        engine?.commitCandidate(candidate)
        _currentCandidates = emptyList()
        fallbackBuffer = ""
        actionSink.commitText(candidate.text)
        actionSink.refreshUi()
    }

    fun refreshCandidates(limit: Int): List<Candidate> {
        val eng = engine
        _currentCandidates = if (eng != null && keyboardMode == KeyboardMode.ChineseT9) {
            eng.getVisibleCandidates(limit)
        } else {
            emptyList()
        }
        return _currentCandidates
    }

    fun discardCompositionForLifecycle() {
        engine?.clear()
        fallbackBuffer = ""
        _currentCandidates = emptyList()

        if (englishPending) {
            englishPending = false
            actionSink.cancelEnglishTimeout()
            englishDigit = ' '
            englishIndex = 0
        }

        actionSink.finishComposingText()
        actionSink.refreshUi()
    }

    fun onHideKey() {
        if (keyboardMode == KeyboardMode.ChineseT9 && rawBuffer.isNotEmpty()) {
            commitFirstCandidateOrPreedit()
        } else if (keyboardMode == KeyboardMode.EnglishT9 && englishPending) {
            commitEnglishChar()
        }
        discardCompositionForLifecycle()
        keyboardMode = KeyboardMode.ChineseT9
        actionSink.refreshUi()
    }
}
