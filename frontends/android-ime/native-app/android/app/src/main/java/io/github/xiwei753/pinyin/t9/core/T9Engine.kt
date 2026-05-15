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

        val candidates = if (buffer.length < 4) {
            getShortInputCandidates(buffer, limit)
        } else {
            getSentenceCandidates(buffer, limit)
        }

        if (candidates.isNotEmpty()) {
            return candidates
        }

        // Fallback to raw buffer
        return listOf(Candidate(buffer, buffer, -Int.MAX_VALUE))
    }

    private fun getShortInputCandidates(code: String, limit: Int): List<Candidate> {
        val candidates = dictionary.getPrefixCandidates(code)
            .filter { candidate ->
                // Length constraints:
                // len 1 -> allow only 1 char
                // len 2 -> allow max 2 chars
                // len 3 -> allow max 3 chars
                candidate.text.length <= code.length
            }
            .map { candidate ->
                // Score adjustment for short input
                var adjustedScore = candidate.score
                // Penalty for candidate length mismatch with input length
                val extraLen = candidate.code.length - code.length
                if (extraLen > 0) {
                    adjustedScore -= extraLen * 50000
                }

                Candidate(candidate.text, candidate.code, adjustedScore)
            }
            .sortedByDescending { it.score }
            .take(limit - 1)
            .toMutableList()

        // Always ensure the bare numeric fallback is at the very end
        candidates.removeAll { it.text == code }
        candidates.add(Candidate(code, code, -Int.MAX_VALUE)) // Bare numeric fallback

        return candidates.sortedWith(Comparator { c1, c2 ->
            if (c1.text == code) return@Comparator 1
            if (c2.text == code) return@Comparator -1
            c2.score.compareTo(c1.score)
        })
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
                        val prevSpaceCount = prevCandidate.text.count { it == ' ' }
                        val newSpaceCount = newText.count { it == ' ' }
                        if (newSpaceCount > prevSpaceCount) {
                            baseScore -= 100000 // Heavily penalize multiple words, we only want it if the score is very good
                        }

                        // Bonus for exact matches and longer continuous words
                        if (partCandidate.code == part) {
                            baseScore += 50000
                        }

                        if (partCandidate.text.length >= 2) {
                            baseScore += 50000 * partCandidate.text.length
                        } else if (partCandidate.text.length == 1) {
                            if (newSpaceCount > 0) {
                                baseScore -= 10000
                            }
                        }

                        // Also penalize very long prefix matches that overshoot the input significantly
                        if (isPrefix) {
                            val overShoot = partCandidate.code.length - part.length
                            if (overShoot > 0) {
                                baseScore -= overShoot * 20000
                            }
                        }

                        currentCandidates.add(Candidate(newText, newCode, baseScore))
                    }
                }
            }

            if (currentCandidates.isNotEmpty()) {
                dp[i] = currentCandidates.distinctBy { it.text }
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

        val limitedResult = result.take(limit - 1).toMutableList()

        limitedResult.removeAll { it.text == code }
        limitedResult.add(Candidate(code, code, -Int.MAX_VALUE))

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
