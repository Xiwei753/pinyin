package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary

class T9Engine {
    var buffer = ""
        private set

    private val builtinDictionary = BuiltinDictionary()

    fun inputDigit(digit: String) {
        if (digit.matches(Regex("^[2-9]$"))) {
            buffer += digit
        }
    }

    fun backspace() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.substring(0, buffer.length - 1)
        }
    }

    fun clear() {
        buffer = ""
    }

    fun getCandidates(): List<Candidate> {
        return builtinDictionary.getCandidates(buffer)
    }

    fun selectCandidate(index: Int): Candidate? {
        val candidates = getCandidates()
        if (index >= 0 && index < candidates.size) {
            val selected = candidates[index]
            clear()
            return selected
        }
        return null
    }
}
