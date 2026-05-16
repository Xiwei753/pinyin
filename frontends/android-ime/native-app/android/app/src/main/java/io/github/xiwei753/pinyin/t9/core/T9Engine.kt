package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class T9Engine(private val dictionary: DictionaryProvider) {
    var buffer = ""
        private set

    private val pinyinComposer = T9PinyinComposer()
    private var lastBuffer = ""
    private var lastCandidates = listOf<Candidate>()
    private var lastLimit = -1

    fun inputDigit(digit: String) {
        if (digit.matches(Regex("^[1-9]$"))) {
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

    fun getPreedit(): String {
        if (buffer.isEmpty()) return ""
        val bestCandidate = if (buffer == lastBuffer && lastCandidates.isNotEmpty()) {
            lastCandidates.first()
        } else {
            generateCandidates(buffer, 2).firstOrNull()
        }

        if (bestCandidate != null && bestCandidate.text != buffer) {
            return bestCandidate.sourcePinyin
        }
        val compositions = pinyinComposer.getCompositions(buffer)
        if (compositions.isEmpty()) return buffer
        return compositions[0].pinyinString
    }

    fun getCandidates(limit: Int = 30): List<Candidate> {
        if (buffer.isEmpty()) return emptyList()
        if (buffer != lastBuffer || limit != lastLimit) {
            lastCandidates = generateCandidates(buffer, limit)
            lastBuffer = buffer
            lastLimit = limit
        }
        return lastCandidates
    }

    private fun generateCandidates(currentBuffer: String, limit: Int): List<Candidate> {
        val compositions = pinyinComposer.getCompositions(currentBuffer)
        if (compositions.isEmpty()) {
            return listOf(Candidate(currentBuffer, currentBuffer, -Int.MAX_VALUE, CandidateType.NORMAL, currentBuffer))
        }

        val allCandidates = mutableListOf<Candidate>()

        // Take the top 3 compositions to explore multiple plausible paths
        val topComps = compositions.take(6)

        for ((index, comp) in topComps.withIndex()) {
            val candidates = if (comp.pinyinList.size == 1) {
                getSingleSyllableCandidates(comp.pinyinList[0], comp.isComplete, limit, comp.pinyinString)
            } else {
                getSentenceCandidates(comp, limit, comp.pinyinString)
            }

            // Provide significant scoring bonuses to candidates from higher-ranked compositions
            val adjustedCandidates = candidates.map { c ->
                var bonus = 0
                // Use composition score to boost its candidate scores so the better path surfaces natural multi-word cand
                bonus += comp.score * 50
                // Small positional bonus, so we don't overshadow dict scores
                bonus += (6 - index) * 1000
                // Massive bonus for multi-word full matches from dict (no space)
                if (!c.text.contains(" ") && c.text.length > 1) {
                    bonus += 500000 * c.text.length
                }
                // Penalize ad-hoc combinations (with spaces)
                if (c.text.contains(" ")) {
                    bonus -= 500000
                }

                Candidate(c.text, c.code, c.score + bonus, c.type, c.sourcePinyin)

            }
            allCandidates.addAll(adjustedCandidates)
        }

        val distinctSorted = allCandidates.sortedByDescending { it.score }
            .distinctBy { it.text.replace(" ", "") }
            .take(limit - 1)
            .map { c ->


                Candidate(c.text.replace(" ", ""), c.code, c.score, c.type, c.sourcePinyin)

            }
            .toMutableList()

        // Always ensure the bare numeric fallback is at the very end
        distinctSorted.removeAll { it.text == currentBuffer }
        distinctSorted.add(Candidate(currentBuffer, currentBuffer, -Int.MAX_VALUE, CandidateType.NORMAL, currentBuffer)) // Bare numeric fallback

        return distinctSorted.sortedWith(Comparator { c1, c2 ->
            if (c1.text == currentBuffer) return@Comparator 1
            if (c2.text == currentBuffer) return@Comparator -1
            c2.score.compareTo(c1.score)
        })
    }

    private fun getSingleSyllableCandidates(pinyin: String, isComplete: Boolean, limit: Int, sourcePinyin: String): List<Candidate> {
        val candidates = if (isComplete) {
            dictionary.getSingleSyllableCandidates(pinyin)
        } else {
            dictionary.getPinyinPrefixCandidates(pinyin)
        }

        val code = T9CodeMapper.toCode(pinyin)

        return candidates
            .filter { candidate ->
                if (!isComplete && candidate.type == CandidateType.LONG_OR_LOW_FREQ) return@filter false
                if (code.length == 1) {
                    candidate.type == CandidateType.SINGLE_CHAR || (candidate.type == CandidateType.COMMON_SHORT && candidate.text.length == 1)
                } else if (code.length == 2) {
                    candidate.text.length <= 2
                } else if (code.length == 3) {
                    candidate.text.length <= 3 && candidate.type != CandidateType.LONG_OR_LOW_FREQ
                } else {
                    candidate.text.length <= code.length
                }
            }
            .map { candidate ->
                var adjustedScore = candidate.score
                val extraLen = candidate.code.length - code.length
                if (extraLen > 0) {
                    adjustedScore -= extraLen * 50000
                }
                Candidate(candidate.text, candidate.code, adjustedScore, candidate.type, sourcePinyin)
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun getSentenceCandidates(comp: PinyinComposition, limit: Int, sourcePinyin: String): List<Candidate> {
        val pinyins = comp.pinyinList
        val dp = Array<MutableList<Candidate>?>(pinyins.size + 1) { null }
        dp[0] = mutableListOf(Candidate("", "", 0))

        for (i in 1..pinyins.size) {
            val currentCandidates = mutableListOf<Candidate>()

            for (j in 0 until i) {
                if (dp[j] == null || dp[j]!!.isEmpty()) continue

                val partPinyins = pinyins.subList(j, i)
                val partStr = partPinyins.joinToString("")
                val isPrefix = (i == pinyins.size)

                val partCandidates = if (isPrefix && !comp.isComplete) {
                    dictionary.getPinyinPrefixCandidates(partStr)
                } else {
                    dictionary.getPinyinExactCandidates(partStr)
                }

                val partCode = partPinyins.joinToString("") { T9CodeMapper.toCode(it) }

                for (prevCandidate in dp[j]!!) {
                    for (partCandidate in partCandidates) {
                        val newText = if (prevCandidate.text.isEmpty()) partCandidate.text else prevCandidate.text + " " + partCandidate.text
                        val newCode = prevCandidate.code + partCandidate.code

                        var baseScore = prevCandidate.score + partCandidate.score

                        val prevSpaceCount = prevCandidate.text.count { it == ' ' }
                        val newSpaceCount = newText.count { it == ' ' }
                        if (newSpaceCount > prevSpaceCount) {
                            baseScore -= 100000
                        }

                        if (partCandidate.code == partCode) {
                            baseScore += 50000
                        }

                        if (partCandidate.text.length >= 2) {
                            baseScore += 50000 * partCandidate.text.length
                        } else if (partCandidate.text.length == 1) {
                            if (newSpaceCount > 0) {
                                baseScore -= 10000
                            }
                        }

                        if (isPrefix) {
                            val overShoot = partCandidate.code.length - partCode.length
                            if (overShoot > 0) {
                                baseScore -= overShoot * 20000
                            }
                        }

                        if (comp.rawDigits.length <= 4 && prevCandidate.text.isNotEmpty()) {
                            // Heavily penalize multiple word combinations for short inputs
                            baseScore -= 500000
                        }

                        // For short inputs, heavily penalize or exclude LONG_OR_LOW_FREQ candidates that are part of dynamic combinations
                        if (comp.rawDigits.length <= 4 && partCandidate.type == CandidateType.LONG_OR_LOW_FREQ) {
                            baseScore -= 500000
                        }

                        currentCandidates.add(Candidate(newText, newCode, baseScore, CandidateType.NORMAL, sourcePinyin))
                    }
                }
            }

            if (currentCandidates.isNotEmpty()) {
                dp[i] = currentCandidates.distinctBy { it.text }
                    .map {
                        if (i == pinyins.size) {
                            Candidate(it.text, it.code, it.score + 50000, CandidateType.NORMAL, sourcePinyin)
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

        return dp[pinyins.size]?.take(limit) ?: emptyList()
    }

    fun commitCandidate(candidate: Candidate): Candidate {
        clear()
        return Candidate(candidate.text.replace(" ", ""), candidate.code, candidate.score)
    }
}
