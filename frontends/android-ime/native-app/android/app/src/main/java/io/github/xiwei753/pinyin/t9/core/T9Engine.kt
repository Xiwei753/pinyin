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

    fun getCandidates(limit: Int = 30): List<Candidate> {
        if (buffer.isEmpty()) return emptyList()
        val candidates = dictionary.getCandidates(buffer)
        return candidates.sortedByDescending { it.score }.take(limit)
    }

    fun selectCandidate(index: Int): Candidate? {
        val candidates = getCandidates(Int.MAX_VALUE)
        if (index >= 0 && index < candidates.size) {
            val selected = candidates[index]
            clear()
            return selected
        }
        return null
    }
}
