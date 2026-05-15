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
        return listOf(Candidate(buffer, buffer, -Int.MAX_VALUE))
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

                        // Calculate score with penalties and bonuses
                        var baseScore = prevCandidate.score + partCandidate.score

                        // Penalty for word count (avoiding excessive fragmentation)
                        // Count spaces to determine number of extra words added
                        val prevSpaceCount = prevCandidate.text.count { it == ' ' }
                        val newSpaceCount = newText.count { it == ' ' }
                        if (newSpaceCount > prevSpaceCount) {
                            baseScore -= 10000 // 10k penalty per additional word cut
                        }

                        // Bonus if this combination perfectly covers the input up to `i`
                        // (We evaluate this globally later, but adding a small progressive bonus here helps maintain quality candidates in the DP limit)

                        currentCandidates.add(Candidate(newText, newCode, baseScore))
                    }
                }
            }

            if (currentCandidates.isNotEmpty()) {
                dp[i] = currentCandidates.distinctBy { it.text }
                    // Additional bonus for reaching the end
                    .map {
                        if (i == code.length) {
                            Candidate(it.text, it.code, it.score + 50000)
                        } else {
                            it
                        }
                    }
                    .sortedByDescending { it.score }
                    .take(limit)
                    .toMutableList()
            } else {
                dp[i] = mutableListOf()
            }
        }

        val result = dp[code.length]?.toMutableList() ?: mutableListOf()

        // Ensure we take up to limit-1 candidates to leave room for fallback
        val limitedResult = result.take(limit - 1).toMutableList()

        // Always ensure the bare numeric fallback is at the very end
        limitedResult.removeAll { it.text == code }
        limitedResult.add(Candidate(code, code, -Int.MAX_VALUE)) // Bare numeric fallback

        return limitedResult
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
