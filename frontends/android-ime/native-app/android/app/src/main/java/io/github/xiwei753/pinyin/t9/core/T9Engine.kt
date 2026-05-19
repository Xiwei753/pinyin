package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class T9Engine(
    private val dictionary: DictionaryProvider,
    private var userDictionary: io.github.xiwei753.pinyin.t9.data.UserDictionaryProvider? = null
) {
    var buffer = ""
        private set

    var activeReading: String? = null
        private set

    val readings: List<String>
        get() {
            if (buffer.isEmpty()) return emptyList()
            val comps = pinyinComposer.getCompositions(buffer)
            val result = linkedSetOf<String>()

            // 1. Readings from compositions ordered by beam score
            for (comp in comps) {
                if (comp.pinyinList.isEmpty()) continue
                if (comp.pinyinList.size == 1) {
                    if (comp.pinyinString.isNotEmpty()) result.add(comp.pinyinString)
                } else if (comp.isComplete) {
                    val lastLen = comp.pinyinList.last().length
                    if (lastLen >= 2) {
                        if (comp.pinyinString.isNotEmpty()) result.add(comp.pinyinString)
                    } else {
                        // Multi-syllable where last syllable is single letter (e.g. "men i")
                        // Show prefix without the trailing single-letter syllable
                        val prefix = comp.pinyinList.dropLast(1).joinToString(" ")
                        if (prefix.isNotEmpty()) result.add(prefix)
                    }
                } else {
                    // Incomplete composition: show prefix that is complete
                    val prefix = comp.pinyinList.dropLast(1).joinToString(" ")
                    if (prefix.isNotEmpty()) result.add(prefix)
                }
            }

            // 2. Prefix-only exact syllable decodings (longer prefixes first)
            // This adds readings like "men" for buffer prefix "636"
            for (end in buffer.length downTo 1) {
                val prefix = buffer.substring(0, end)
                for (s in PinyinSyllableDecoder.getExactSyllables(prefix)) {
                    if (s.isNotEmpty() && s !in result) result.add(s)
                }
            }

            return result.toList()
        }

    fun setActiveReading(reading: String): Boolean {
        if (buffer.isEmpty()) return false
        // Check against all readings, including prefix readings
        val allReadings = readings
        if (reading in allReadings) {
            activeReading = reading
            lastVisibleBuffer = ""
            return true
        }
        return false
    }

    private val pinyinComposer = T9PinyinComposer()
    private var lastBuffer = ""
    private var lastCandidates = listOf<Candidate>()
    private var lastLimit = -1

    private var lastVisibleCandidates = listOf<Candidate>()
    private var lastVisibleLimit = -1
    private var lastVisibleBuffer = ""
    private var lastInternalCandidates = listOf<Candidate>()

    fun getCompositions(): List<PinyinComposition> {
        return pinyinComposer.getCompositions(buffer)
    }

    fun getInternalCandidates(): List<Candidate> {
        return lastInternalCandidates
    }

    fun inputDigit(digit: String) {
        if (digit.matches(Regex("^[1-9]$"))) {
            buffer += digit
            activeReading = null
        }
    }

    fun backspace() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.substring(0, buffer.length - 1)
            activeReading = null
        }
    }

    fun clear() {
        buffer = ""
        activeReading = null
        lastBuffer = ""
        lastCandidates = emptyList()
        lastLimit = -1
        lastVisibleCandidates = emptyList()
        lastVisibleLimit = -1
        lastVisibleBuffer = ""
    }

    fun getPreedit(): String {
        if (buffer.isEmpty()) return ""
        if (activeReading != null) return activeReading!!
        val visible = getVisibleCandidates(2)
        val bestCandidate = visible.firstOrNull()

        if (bestCandidate != null && bestCandidate.text != buffer) {
            return bestCandidate.sourcePinyin
        }
        val compositions = pinyinComposer.getCompositions(buffer)
        val first = compositions.firstOrNull()
        if (first == null || first.pinyinString.isEmpty()) return ""
        return first.pinyinString
    }

    fun getVisibleCandidates(limit: Int = 30): List<Candidate> {
        if (buffer.isEmpty()) return emptyList()
        if (buffer == lastVisibleBuffer && limit == lastVisibleLimit && lastVisibleCandidates.isNotEmpty()) {
            return lastVisibleCandidates
        }

        val compositions = pinyinComposer.getCompositions(buffer)
        val active = activeReading

        val hasExactCompMatch = active != null && compositions.any { it.pinyinString == active }

        val activeComp = if (active != null && hasExactCompMatch) {
            compositions.firstOrNull { it.pinyinString == active }
        } else {
            if (active != null) {
                compositions.firstOrNull {
                    it.pinyinList.isNotEmpty() && it.pinyinList[0] == active
                }
            } else null
        }

        val primaryPinyin: String = if (hasExactCompMatch && active != null) {
            active
        } else if (active != null) {
            active
        } else {
            compositions.firstOrNull { it.isComplete }?.pinyinString ?: ""
        }

        val userCandidates = if (primaryPinyin.isNotEmpty() && userDictionary != null) {
            userDictionary!!.getUserCandidates(primaryPinyin).map { c ->
                val boost = userDictionary!!.getUserBoost(c.sourcePinyin, c.text)
                c.copy(score = c.score + boost)
            }
        } else {
            emptyList()
        }

        val exactCandidatesRaw = if (active != null && !hasExactCompMatch) {
            val code = T9CodeMapper.toCode(active)
            val singleCandidates = dictionary.getSingleSyllableCandidates(active)
            singleCandidates.map { c ->
                var adjustedScore = c.score
                val extraLen = c.code.length - code.length
                if (extraLen > 0) adjustedScore -= extraLen * 50000
                Candidate(c.text, c.code, adjustedScore, c.type, active, c.origin)
            }.filter { c ->
                c.origin == CandidateOrigin.EXACT_SINGLE || c.origin == CandidateOrigin.EXACT_PHRASE
            }
        } else {
            generateCandidates(buffer, limit + 10, allowDynamic = false,
                preferredPinyin = if (hasExactCompMatch && active != null) active else null)
        }

        val exactCandidates = exactCandidatesRaw.filter { c ->
            if (c.origin != CandidateOrigin.EXACT_SINGLE && c.origin != CandidateOrigin.EXACT_PHRASE) return@filter false
            if (c.text == buffer) return@filter false
            if (c.text.matches(Regex("^[a-zA-Z\\s]+$"))) return@filter false

            if (buffer.length == 1) {
                if (c.text.length > 1) return@filter false
                if (c.code.length > 1) return@filter false
            }
            if (buffer.length == 2) {
                if (c.text.length > 2) return@filter false
            }

            true
        }

        val hasExactPhrase = exactCandidates.any { it.origin == CandidateOrigin.EXACT_PHRASE && it.text.length >= 2 }

        val safeDynamicCandidates = if (!hasExactPhrase && buffer.length >= 5 && hasExactCompMatch) {
            generateSafeDynamicCandidates(compositions, limit, preferredPinyin = activeReading).map { c ->
                c.copy(origin = CandidateOrigin.SAFE_DYNAMIC_COMPOSITION)
            }
        } else {
            emptyList()
        }

        val combined = mutableListOf<Candidate>()
        combined.addAll(exactCandidates.filter { it.origin == CandidateOrigin.EXACT_PHRASE })
        combined.addAll(userCandidates.filter { uc -> combined.none { it.text == uc.text } })
        combined.addAll(exactCandidates.filter { it.origin == CandidateOrigin.EXACT_SINGLE })
        combined.addAll(safeDynamicCandidates.take(3))

        val finalCandidates = combined.distinctBy { it.text }.take(limit)

        lastVisibleCandidates = finalCandidates
        lastVisibleLimit = limit
        lastVisibleBuffer = buffer

        return finalCandidates
    }

    private fun generateSafeDynamicCandidates(
        compositions: List<PinyinComposition>,
        limit: Int,
        preferredPinyin: String? = null
    ): List<Candidate> {
        val comps = if (preferredPinyin != null) {
            compositions.filter { it.pinyinString == preferredPinyin }
        } else {
            compositions
        }
        val completeComp = comps.firstOrNull { it.isComplete } ?: return emptyList()
        if (completeComp.pinyinList.size < 2) return emptyList()

        val pinyins = completeComp.pinyinList
        val results = mutableListOf<Candidate>()

        for (i in 1 until pinyins.size) {
            val firstPart = pinyins.subList(0, i).joinToString(" ")
            val secondPart = pinyins.subList(i, pinyins.size).joinToString(" ")

            val firstCandidates = dictionary.getPinyinExactCandidates(firstPart)
                .filter { it.type != CandidateType.LONG_OR_LOW_FREQ && it.score > 30000 }
                .take(5)
            val secondCandidates = dictionary.getPinyinExactCandidates(secondPart)
                .filter { it.type != CandidateType.LONG_OR_LOW_FREQ && it.score > 30000 }
                .take(5)

            for (fc in firstCandidates) {
                for (sc in secondCandidates) {
                    if (fc.text.length == 1 && sc.text.length == 1) {
                        val combinedText = fc.text + sc.text
                        val combinedScore = fc.score + sc.score - 100000

                        if (combinedScore > 50000) {
                            val combinedCode = fc.code + sc.code
                            results.add(
                                Candidate(
                                    combinedText,
                                    combinedCode,
                                    combinedScore,
                                    CandidateType.NORMAL,
                                    completeComp.pinyinString,
                                    CandidateOrigin.SAFE_DYNAMIC_COMPOSITION
                                )
                            )
                        }
                    }
                }
            }
        }

        return results.sortedByDescending { it.score }.take(limit)
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

    private fun generateCandidates(currentBuffer: String, limit: Int, allowDynamic: Boolean = true,
                                   preferredPinyin: String? = null): List<Candidate> {
        val allCompositions = pinyinComposer.getCompositions(currentBuffer)
        if (allCompositions.isEmpty()) {
            return listOf(Candidate(currentBuffer, currentBuffer, -Int.MAX_VALUE, CandidateType.NORMAL, currentBuffer))
        }

        val compositions = if (preferredPinyin != null) {
            allCompositions.filter { it.pinyinString == preferredPinyin }
        } else {
            allCompositions
        }
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
                getSentenceCandidates(comp, limit, comp.pinyinString, allowDynamic)
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

        val sortedAll = allCandidates.sortedByDescending { it.score }
            .distinctBy { it.text }

        lastInternalCandidates = sortedAll.take(10)

        val distinctSorted = sortedAll
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
        val candidates = dictionary.getSingleSyllableCandidates(pinyin)

        val typedCodeLength = if (segmentDigits.isNotEmpty()) segmentDigits[0].length else T9CodeMapper.toCode(pinyin).length
        val codeLen = T9CodeMapper.toCode(pinyin).length

        return candidates
            .filter { candidate ->
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
                Candidate(candidate.text, candidate.code, adjustedScore, candidate.type, sourcePinyin, candidate.origin)
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun getSentenceCandidates(comp: PinyinComposition, limit: Int, sourcePinyin: String, allowDynamic: Boolean = true): List<Candidate> {
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

                val partCandidates = dictionary.getPinyinExactCandidates(partStr)

                val partCodeLength = if (comp.segmentDigits.isNotEmpty()) {
                    comp.segmentDigits.subList(j, i).sumOf { it.length }
                } else {
                    partPinyins.joinToString("") { T9CodeMapper.toCode(it) }.length
                }
                val partCode = partPinyins.joinToString("") { T9CodeMapper.toCode(it) }

                for (prevCandidate in dp[j]!!) {
                    for (partCandidate in partCandidates) {
                        val isDynamic = prevCandidate.text.isNotEmpty()
                        if (!allowDynamic && isDynamic) continue

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
                        }

                        if (comp.rawDigits.length <= 4 && prevCandidate.text.isNotEmpty()) {
                            // Heavily penalize multiple word combinations for short inputs
                            baseScore -= 500000
                        }

                        // For short inputs, heavily penalize or exclude LONG_OR_LOW_FREQ candidates that are part of dynamic combinations
                        if (comp.rawDigits.length <= 4 && partCandidate.type == CandidateType.LONG_OR_LOW_FREQ) {
                            baseScore -= 500000
                        }

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
        if (candidate.text != buffer && candidate.sourcePinyin.isNotEmpty()) {
            userDictionary?.recordSelection(candidate.text, candidate.sourcePinyin)
        }
        clear()
        return candidate
    }
}
