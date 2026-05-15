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
                // Disable multi-word sentence composition for short inputs
                if (code.length < 4 && j > 0) continue

                if (dp[j] == null || dp[j]!!.isEmpty()) continue

                val part = code.substring(j, i)
                val isPrefix = (i == code.length)

                var partCandidates = if (isPrefix) {
                    dictionary.getPrefixCandidates(part)
                } else {
                    dictionary.getExactCandidates(part)
                }

                // Filter candidates based on input length when matching prefix
                if (isPrefix) {
                    partCandidates = when (code.length) {
                        1 -> partCandidates.filter { it.text.length == 1 }.take(6)
                        2 -> partCandidates.filter { it.text.length <= 2 }
                        3 -> partCandidates.filter { it.text.length <= 3 }
                        else -> partCandidates
                    }
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
                            // Heavy penalty to strictly push multi-word combos below basic words
                            baseScore -= 100000 // 100k penalty per additional word cut
                        }

                        // Penalty for words that are significantly longer than the input prefix
                        if (isPrefix) {
                            val extraChars = partCandidate.text.length - part.length
                            if (extraChars > 0) {
                                baseScore -= extraChars * 30000
                            }
                        }

                        // Bonus for word length: encourage complete multi-character words over single characters
                        // partCandidate.text is the word added in this step.
                        // We check the length of the *text* (Chinese characters).
                        if (partCandidate.text.length >= 2) {
                            baseScore += 50000 * partCandidate.text.length
                        } else if (partCandidate.text.length == 1) {
                            // Small penalty for single character words if they are part of a multi-word sequence
                            // (If it's the only word, newSpaceCount == 0 and prevSpaceCount == 0)
                            if (newSpaceCount > 0) {
                                baseScore -= 5000
                            }
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

        // Final sort to guarantee the raw numeric fallback is at the very end
        // while preserving the score-based sorting for all other candidates
        return limitedResult.sortedWith(Comparator { c1, c2 ->
            if (c1.text == code) return@Comparator 1
            if (c2.text == code) return@Comparator -1
            c2.score.compareTo(c1.score)
        })
    }

    fun commitCandidate(candidate: Candidate): Candidate {
        clear()
        // Clean up the spaces added during sentence composition
        return Candidate(candidate.text.replace(" ", ""), candidate.code, candidate.score)
    }
}
