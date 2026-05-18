package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9Engine

class T9ImeController(var engine: T9Engine?) {

    private var _currentCandidates: List<Candidate> = emptyList()
    private var fallbackBuffer: String = ""

    val currentCandidates: List<Candidate> get() = _currentCandidates
    val preedit: String get() = engine?.getPreedit() ?: fallbackBuffer
    val rawBuffer: String get() = engine?.buffer ?: fallbackBuffer

    sealed class ActionResult {
        data class CommitText(val text: String) : ActionResult()
        object SendDelete : ActionResult()
        object Refresh : ActionResult()
        object NoAction : ActionResult()
    }

    fun attachEngine(newEngine: T9Engine) {
        this.engine = newEngine
        for (char in fallbackBuffer) {
            newEngine.inputDigit(char.toString())
        }
        fallbackBuffer = ""
    }

    fun inputDigit(digit: String) {
        val eng = engine
        if (eng != null) {
            eng.inputDigit(digit)
        } else {
            if (digit.matches(Regex("^[1-9]$"))) {
                fallbackBuffer += digit
            }
        }
    }

    fun onSeparator(): ActionResult {
        val eng = engine
        if (eng != null) {
            return if (eng.buffer.isNotEmpty()) {
                eng.inputDigit("1")
                ActionResult.Refresh
            } else {
                ActionResult.NoAction
            }
        } else {
            return if (fallbackBuffer.isNotEmpty()) {
                fallbackBuffer += "1"
                ActionResult.Refresh
            } else {
                ActionResult.NoAction
            }
        }
    }

    fun onZero(): ActionResult {
        val eng = engine
        if (eng != null) {
            if (eng.buffer.isEmpty()) {
                return ActionResult.CommitText(" ")
            }
            if (_currentCandidates.isNotEmpty()) {
                val candidate = _currentCandidates[0]
                eng.commitCandidate(candidate)
                _currentCandidates = emptyList()
                return ActionResult.CommitText(candidate.text)
            }
            // If buffer is not empty but dictionary hasn't loaded properly to give candidates, we don't drop the buffer.
            return ActionResult.NoAction
        } else {
            if (fallbackBuffer.isEmpty()) {
                return ActionResult.CommitText(" ")
            }
            return ActionResult.NoAction
        }
    }

    fun onDelete(): ActionResult {
        val eng = engine
        if (eng != null) {
            if (eng.buffer.isNotEmpty()) {
                eng.backspace()
                return ActionResult.Refresh
            }
        } else {
            if (fallbackBuffer.isNotEmpty()) {
                fallbackBuffer = fallbackBuffer.substring(0, fallbackBuffer.length - 1)
                return ActionResult.Refresh
            }
        }
        return ActionResult.SendDelete
    }

    fun onCandidateClick(index: Int): ActionResult {
        if (index < 0 || index >= _currentCandidates.size) return ActionResult.NoAction
        val candidate = _currentCandidates[index]
        engine?.commitCandidate(candidate)
        _currentCandidates = emptyList()
        fallbackBuffer = ""
        return ActionResult.CommitText(candidate.text)
    }

    fun refreshCandidates(limit: Int): List<Candidate> {
        val eng = engine
        _currentCandidates = if (eng != null) {
            eng.getVisibleCandidates(limit)
        } else {
            emptyList()
        }
        return _currentCandidates
    }

    fun reset() {
        engine?.clear()
        fallbackBuffer = ""
        _currentCandidates = emptyList()
    }
}
