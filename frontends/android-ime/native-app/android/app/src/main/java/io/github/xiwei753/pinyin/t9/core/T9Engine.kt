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

        val combinations = getSentenceCandidates(buffer, limit)
        if (combinations.isNotEmpty()) {
            return combinations
        }

        // Fallback to raw buffer
        return listOf(Candidate(buffer, buffer, 0))
    }

    private fun getSentenceCandidates(code: String, limit: Int): List<Candidate> {
        val dp = Array<MutableList<Candidate>?>(code.length + 1) { null }
        dp[0] = mutableListOf(Candidate("", "", 0))

        for (i in 1..code.length) {
            val currentCandidates = mutableListOf<Candidate>()

            for (j in 0 until i) {
                if (dp[j] == null || dp[j]!!.isEmpty()) continue

                val part = code.substring(j, i)
                val isPrefix = (i == code.length)

                val partCandidates = if (isPrefix) {
                    dictionary.getPrefixCandidates(part)
                } else {
                    dictionary.getExactCandidates(part)
                }

                for (prevCandidate in dp[j]!!) {
                    for (partCandidate in partCandidates) {
                        val newText = if (prevCandidate.text.isEmpty()) partCandidate.text else prevCandidate.text + " " + partCandidate.text
                        val newCode = prevCandidate.code + partCandidate.code
                        val newScore = prevCandidate.score + partCandidate.score
                        currentCandidates.add(Candidate(newText, newCode, newScore))
                    }
                }
            }

            if (currentCandidates.isNotEmpty()) {
                dp[i] = currentCandidates.distinctBy { it.text }
                    .sortedByDescending { it.score }
                    .take(limit)
                    .toMutableList()
            } else {
                dp[i] = mutableListOf()
            }
        }

        return dp[code.length] ?: emptyList()
    }

    fun selectCandidate(index: Int): Candidate? {
        val candidates = getCandidates(Int.MAX_VALUE)
        if (index >= 0 && index < candidates.size) {
            val selected = candidates[index]
            clear()

            // Clean up the spaces added during sentence composition
            return Candidate(selected.text.replace(" ", ""), selected.code, selected.score)
        }
        return null
    }
}
