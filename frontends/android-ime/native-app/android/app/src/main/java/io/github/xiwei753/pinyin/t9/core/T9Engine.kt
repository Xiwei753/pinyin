package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class T9Engine(private val dictionary: DictionaryProvider) {
    var buffer = ""
        private set

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
        return dictionary.getCandidates(buffer)
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
