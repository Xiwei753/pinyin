package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9Engine

class T9ImeController(val engine: T9Engine) {

    private var _currentCandidates: List<Candidate> = emptyList()

    val currentCandidates: List<Candidate> get() = _currentCandidates
    val preedit: String get() = engine.getPreedit()

    sealed class ActionResult {
        data class CommitText(val text: String) : ActionResult()
        object SendDelete : ActionResult()
        object Refresh : ActionResult()
        object NoAction : ActionResult()
    }

    fun inputDigit(digit: String) {
        engine.inputDigit(digit)
    }

    fun onSeparator(): ActionResult {
        return if (engine.buffer.isNotEmpty()) {
            engine.inputDigit("1")
            ActionResult.Refresh
        } else {
            ActionResult.NoAction
        }
    }

    fun onZero(): ActionResult {
        if (engine.buffer.isEmpty()) {
            return ActionResult.CommitText(" ")
        }
        if (_currentCandidates.isNotEmpty()) {
            val candidate = _currentCandidates[0]
            engine.commitCandidate(candidate)
            _currentCandidates = emptyList()
            return ActionResult.CommitText(candidate.text)
        }
        engine.clear()
        _currentCandidates = emptyList()
        return ActionResult.Refresh
    }

    fun onDelete(): ActionResult {
        if (engine.buffer.isNotEmpty()) {
            engine.backspace()
            return ActionResult.Refresh
        }
        return ActionResult.SendDelete
    }

    fun onCandidateClick(index: Int): ActionResult {
        if (index < 0 || index >= _currentCandidates.size) return ActionResult.NoAction
        val candidate = _currentCandidates[index]
        engine.commitCandidate(candidate)
        _currentCandidates = emptyList()
        return ActionResult.CommitText(candidate.text)
    }

    fun refreshCandidates(limit: Int): List<Candidate> {
        _currentCandidates = engine.getVisibleCandidates(limit)
        return _currentCandidates
    }

    fun reset() {
        engine.clear()
        _currentCandidates = emptyList()
    }
}
