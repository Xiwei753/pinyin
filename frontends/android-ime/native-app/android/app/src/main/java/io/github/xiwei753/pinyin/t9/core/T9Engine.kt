package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class T9Engine(private val dictionary: DictionaryProvider) {
    var buffer = ""
        private set

    private val pinyinComposer = T9PinyinComposer()
    private var lastBuffer = ""
    private var lastCandidates = listOf<Candidate>()
    private var lastLimit = -1

    private var lastVisibleCandidates = listOf<Candidate>()
    private var lastVisibleLimit = -1

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
        val visible = getVisibleCandidates(2)
        val bestCandidate = visible.firstOrNull()

        if (bestCandidate != null && bestCandidate.text != buffer) {
            return bestCandidate.sourcePinyin
        }
        val compositions = pinyinComposer.getCompositions(buffer)
        if (compositions.isEmpty()) return buffer
        return compositions[0].pinyinString
    }

    fun getVisibleCandidates(limit: Int = 30): List<Candidate> {
        if (buffer.isEmpty()) return emptyList()
        if (buffer == lastBuffer && limit == lastVisibleLimit && lastVisibleCandidates.isNotEmpty()) {
            return lastVisibleCandidates
        }

        // We request more internal candidates to ensure we have enough after filtering
        val internalCandidates = getCandidates(limit + 10)

        val visible = internalCandidates.filter { c ->
            // Origin filtering: allow EXACT_SINGLE and EXACT_PHRASE
            if (c.origin != CandidateOrigin.EXACT_SINGLE && c.origin != CandidateOrigin.EXACT_PHRASE) return@filter false

            // Exclude the raw numeric fallback
            if (c.text == buffer) return@filter false

            // Exclude pinyin/english only candidates
            if (c.text.matches(Regex("^[a-zA-Z\\s]+$"))) return@filter false

            // Short input gate: length 1
            if (buffer.length == 1) {
                // Only allow single characters
                if (c.text.length > 1) return@filter false
                // Prevent prefix brain-completion: the pinyin code length should not exceed typed length
                if (c.code.length > 1) return@filter false
            }
            // Short input gate: length 2
            if (buffer.length == 2) {
                if (c.text.length > 2) return@filter false
            }

            true
        }.take(limit)

        lastVisibleCandidates = visible
        lastVisibleLimit = limit

        return visible
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

        // Take the top 16 compositions to explore multiple plausible paths
        val topComps = compositions.take(16)

        // Pre-calculate path qualities for exact single syllable paths
        // This ensures the highest frequency candidates heavily influence the path score,
        // allowing "neng" to beat "meng" natively based on dictionary frequency.
        val exactSingleSyllableMaxScores = mutableMapOf<String, Int>()
        for (comp in topComps) {
            if (comp.isComplete && comp.pinyinList.size == 1) {
                val pinyin = comp.pinyinList[0]
                if (!exactSingleSyllableMaxScores.containsKey(pinyin)) {
                    val cands = dictionary.getSingleSyllableCandidates(pinyin)
                    val maxScore = cands.maxOfOrNull { it.score } ?: 0
                    exactSingleSyllableMaxScores[pinyin] = maxScore
                }
            }
        }

        val hasValidExactSingleSyllable = exactSingleSyllableMaxScores.values.maxOrNull() ?: 0 > 0

        for (comp in topComps) {
            if (hasValidExactSingleSyllable && !comp.isComplete && comp.pinyinList.size > 1) {
                // If we have a valid exact full-span single syllable candidate,
                // do not generate candidates for multi-syllable prefix compositions
                // (e.g. 6364 -> men ge).
                continue
            }

            val candidates = if (comp.pinyinList.size == 1) {
                getSingleSyllableCandidates(comp.pinyinList[0], comp.isComplete, limit, comp.pinyinString, comp.segmentDigits)
            } else {
                getSentenceCandidates(comp, limit, comp.pinyinString)
            }

            val adjustedCandidates = candidates.map { c ->
                var bonus = 0
                // Give a smaller boost from composition score so valid candidates can rise
                bonus += comp.score * 10

                // If this candidate comes from an exact single syllable path,
                // give a bonus proportional to the path's maximum candidate score
                if (comp.isComplete && comp.pinyinList.size == 1) {
                    val pathQuality = exactSingleSyllableMaxScores[comp.pinyinList[0]] ?: 0
                    if (pathQuality > 0) {
                        bonus += pathQuality
                    }
                }

                // Massive bonus for multi-word full matches from dict (no space)
                if (!c.text.contains(" ") && c.text.length > 1) {
                    bonus += 500000 * c.text.length
                }
                // Penalize ad-hoc combinations (with spaces)
                if (c.text.contains(" ")) {
                    bonus -= 500000
                }

                // Heavily penalize prefix brain completions if it's combining into a phrase
                if (!comp.isComplete) {
                    bonus -= 100000
                }

                Candidate(c.text, c.code, c.score + bonus, c.type, c.sourcePinyin, c.origin)
            }
            allCandidates.addAll(adjustedCandidates)
        }

        val distinctSorted = allCandidates.sortedByDescending { it.score }
            .distinctBy { it.text }
            .take(limit - 1)
            .toMutableList()

        // Always ensure the bare numeric fallback is at the very end
        distinctSorted.removeAll { it.text == currentBuffer }
        distinctSorted.add(Candidate(currentBuffer, currentBuffer, -Int.MAX_VALUE, CandidateType.NORMAL, currentBuffer, CandidateOrigin.RAW_FALLBACK)) // Bare numeric fallback

        return distinctSorted.sortedWith(Comparator { c1, c2 ->
            if (c1.text == currentBuffer) return@Comparator 1
            if (c2.text == currentBuffer) return@Comparator -1
            c2.score.compareTo(c1.score)
        })
    }

    private fun getSingleSyllableCandidates(pinyin: String, isComplete: Boolean, limit: Int, sourcePinyin: String, segmentDigits: List<String>): List<Candidate> {
        val candidates = if (isComplete) {
            dictionary.getSingleSyllableCandidates(pinyin)
        } else {
            dictionary.getPinyinPrefixCandidates(pinyin)
        }

        val typedCodeLength = if (segmentDigits.isNotEmpty()) segmentDigits[0].length else T9CodeMapper.toCode(pinyin).length
        val codeLen = T9CodeMapper.toCode(pinyin).length

        return candidates
            .filter { candidate ->
                if (!isComplete && candidate.type == CandidateType.LONG_OR_LOW_FREQ) return@filter false
                if (codeLen == 1) {
                    candidate.type == CandidateType.SINGLE_CHAR || (candidate.type == CandidateType.COMMON_SHORT && candidate.text.length == 1)
                } else if (codeLen == 2) {
                    candidate.text.length <= 2
                } else if (codeLen == 3) {
                    candidate.text.length <= 3 && candidate.type != CandidateType.LONG_OR_LOW_FREQ
                } else {
                    candidate.text.length <= codeLen
                }
            }
            .map { candidate ->
                var adjustedScore = candidate.score
                val extraLen = candidate.code.length - typedCodeLength
                if (extraLen > 0) {
                    adjustedScore -= extraLen * 50000
                }
                if (!isComplete && typedCodeLength <= 2) {
                    adjustedScore -= 2000000 // Huge penalty for brain-completing a 1-2 digit prefix
                }
                Candidate(candidate.text, candidate.code, adjustedScore, candidate.type, sourcePinyin, candidate.origin)
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
                val partStr = partPinyins.joinToString(" ")
                val isPrefix = (i == pinyins.size)

                val partCandidates = if (isPrefix && !comp.isComplete) {
                    dictionary.getPinyinPrefixCandidates(partStr)
                } else {
                    dictionary.getPinyinExactCandidates(partStr)
                }

                val partCodeLength = if (comp.segmentDigits.isNotEmpty()) {
                    comp.segmentDigits.subList(j, i).sumOf { it.length }
                } else {
                    partPinyins.joinToString("") { T9CodeMapper.toCode(it) }.length
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
                            val overShoot = partCandidate.code.length - partCodeLength
                            if (overShoot > 0) {
                                baseScore -= overShoot * 20000
                            }
                            // If the final segment typing is very short (1-2 digits), heavily penalize prefix brain-completion
                            val lastSegTypedLength = if (comp.segmentDigits.isNotEmpty()) comp.segmentDigits[i-1].length else 3
                            if (!comp.isComplete && lastSegTypedLength <= 2) {
                                baseScore -= 2000000
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

                        val isDynamic = prevCandidate.text.isNotEmpty()
                        val origin = if (isDynamic) CandidateOrigin.DYNAMIC_COMPOSITION else partCandidate.origin

                        currentCandidates.add(Candidate(newText, newCode, baseScore, CandidateType.NORMAL, sourcePinyin, origin))
                    }
                }
            }

            if (currentCandidates.isNotEmpty()) {
                dp[i] = currentCandidates.distinctBy { it.text }
                    .map {
                        if (i == pinyins.size) {
                            Candidate(it.text, it.code, it.score + 50000, CandidateType.NORMAL, sourcePinyin, it.origin)
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
        return candidate
    }
}
