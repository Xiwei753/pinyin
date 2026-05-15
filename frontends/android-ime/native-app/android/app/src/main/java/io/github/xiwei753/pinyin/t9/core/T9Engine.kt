package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class T9Engine(private val dictionary: DictionaryProvider) {
    var buffer = ""
        private set

    private val pinyinComposer = T9PinyinComposer()
    private var lastBuffer = ""
    private var lastCandidates = listOf<Candidate>()

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
        getCandidates()
        val bestCandidate = lastCandidates.firstOrNull()
        if (bestCandidate != null && bestCandidate.text != buffer) {
            return bestCandidate.sourcePinyin
        }
        val compositions = pinyinComposer.getCompositions(buffer)
        if (compositions.isEmpty()) return buffer
        return compositions[0].pinyinString
    }

    fun getCandidates(limit: Int = 30): List<Candidate> {
        if (buffer.isEmpty()) return emptyList()
        if (buffer != lastBuffer) {
            computeCandidates(limit)
        }
        return lastCandidates
    }

    private fun computeCandidates(limit: Int) {
        lastBuffer = buffer

        val compositions = pinyinComposer.getCompositions(buffer)
        if (compositions.isEmpty()) {
            lastCandidates = listOf(Candidate(buffer, buffer, -Int.MAX_VALUE, CandidateType.NORMAL, buffer))
            return
        }

        val allCandidates = mutableListOf<Candidate>()

        // Take the top 3 compositions to explore multiple plausible paths
        val topComps = compositions.take(3)

        for ((index, comp) in topComps.withIndex()) {
            val candidates = if (comp.pinyinList.size == 1) {
                getSingleSyllableCandidates(comp.pinyinList[0], comp.isComplete, limit, comp.pinyinString)
            } else {
                getSentenceCandidates(comp, limit, comp.pinyinString)
            }

            // Provide significant scoring bonuses to candidates from higher-ranked compositions
            val adjustedCandidates = candidates.map { c ->
                val bonus = if (index == 0) 100000 else (3 - index) * 10000
                Candidate(c.text, c.code, c.score + bonus, c.type, c.sourcePinyin)
            }
            allCandidates.addAll(adjustedCandidates)
        }

        val distinctSorted = allCandidates.distinctBy { it.text }
            .sortedByDescending { it.score }
            .take(limit - 1)
            .toMutableList()

        // Always ensure the bare numeric fallback is at the very end
        distinctSorted.removeAll { it.text == buffer }
        distinctSorted.add(Candidate(buffer, buffer, -Int.MAX_VALUE, CandidateType.NORMAL, buffer)) // Bare numeric fallback

        lastCandidates = distinctSorted.sortedWith(Comparator { c1, c2 ->
            if (c1.text == buffer) return@Comparator 1
            if (c2.text == buffer) return@Comparator -1
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
                            baseScore -= 200000
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
