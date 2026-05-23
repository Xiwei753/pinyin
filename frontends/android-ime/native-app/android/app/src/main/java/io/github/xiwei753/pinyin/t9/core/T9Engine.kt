package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class T9Engine(
    private val dictionary: DictionaryProvider,
    private var userDictionary: io.github.xiwei753.pinyin.t9.data.UserDictionaryProvider? = null,
    private val logger: io.github.xiwei753.pinyin.t9.T9DebugLogger? = null
) {
    var dbQueryCount: Int = 0

    private fun <T> trackQuery(block: () -> T): T {
        dbQueryCount++
        return block()
    }
    var buffer = ""
        private set

    val lockedSyllables = mutableListOf<String>()

    val activeReading: String?
        get() = lockedSyllables.lastOrNull()

    fun getValidCompositions(): List<PinyinComposition> {
        val allCompositions = pinyinComposer.getCompositions(buffer)
        if (lockedSyllables.isEmpty()) return allCompositions

        return allCompositions.filter { comp ->
            if (comp.pinyinList.size < lockedSyllables.size) return@filter false
            var match = true
            for (i in lockedSyllables.indices) {
                if (comp.pinyinList[i] != lockedSyllables[i]) {
                    match = false
                    break
                }
            }
            match
        }
    }

    val readings: List<String>
        get() {
            if (buffer.isEmpty()) return emptyList()
            val validComps = getValidCompositions()
            val nextIndex = lockedSyllables.size
            return ReadingRanker.rank(buffer, validComps, nextIndex).map { it.syllable }
        }

    fun getReadingDigitSpan(reading: String): String {
        if (buffer.isEmpty() || reading.isEmpty()) return ""
        val code = T9CodeMapper.toCode(reading)
        if (buffer.startsWith(code)) return code
        val comps = pinyinComposer.getCompositions(buffer)
        for (comp in comps) {
            val idx = comp.pinyinList.indexOf(reading)
            if (idx >= 0 && idx < comp.segmentDigits.size) {
                return comp.segmentDigits[idx]
            }
        }
        return code
    }

    fun setActiveReading(reading: String): Boolean {
        if (buffer.isEmpty()) return false
        var currentReadings = readings
        if (reading in currentReadings) {
            lockedSyllables.add(reading)
            lastVisibleBuffer = ""
            return true
        } else if (lockedSyllables.isNotEmpty()) {
            val previous = lockedSyllables.toList()
            lockedSyllables.removeLast()
            currentReadings = readings
            if (reading in currentReadings) {
                lockedSyllables.add(reading)
                lastVisibleBuffer = ""
                return true
            } else {
                lockedSyllables.clear()
                lockedSyllables.addAll(previous)
            }
        }

        if (lockedSyllables.isNotEmpty()) {
            val previous = lockedSyllables.toList()
            lockedSyllables.clear()
            currentReadings = readings
            if (reading in currentReadings) {
                lockedSyllables.add(reading)
                lastVisibleBuffer = ""
                return true
            } else {
                lockedSyllables.addAll(previous)
            }
        }

        return false
    }

    fun commitReadingAndKeepBuffer(reading: String): String? {
        return null // Removed as we no longer commit directly when selecting reading
    }

    private val pinyinComposer = T9PinyinComposer()
    private var lastBuffer = ""
    private var lastCandidates = listOf<Candidate>()
    private var lastLimit = -1
    private var lastDictVersion = -1

    private var lastVisibleCandidates = listOf<Candidate>()
    private var lastVisibleLimit = -1
    private var lastVisibleDictVersion = -1
    private var lastVisibleLockedSyllables = listOf<String>()
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
            lockedSyllables.clear()
        }
    }

    fun backspace() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.substring(0, buffer.length - 1)
            while (lockedSyllables.isNotEmpty() && getValidCompositions().isEmpty()) {
                lockedSyllables.removeLast()
            }
        }
    }

    fun clear() {
        buffer = ""
        lockedSyllables.clear()
        lastBuffer = ""
        lastCandidates = emptyList()
        lastLimit = -1
        lastVisibleCandidates = emptyList()
        lastVisibleLimit = -1
        lastVisibleBuffer = ""
    }

    fun getPreedit(): String {
        if (buffer.isEmpty()) return ""
        val visible = getVisibleCandidates(2)
        val bestCandidate = visible.firstOrNull()

        if (bestCandidate != null && bestCandidate.text != buffer) {
            return bestCandidate.sourcePinyin
        }
        val compositions = getValidCompositions()
        val first = compositions.firstOrNull()
        if (first == null || first.pinyinString.isEmpty()) return ""
        return first.pinyinString
    }

    fun getVisibleCandidates(limit: Int = 30): List<Candidate> {
        val currentDictVersion = dictionary.dictionaryVersion
        if (buffer.isEmpty()) return emptyList()
        if (buffer == lastVisibleBuffer && limit == lastVisibleLimit && currentDictVersion == lastVisibleDictVersion && lockedSyllables == lastVisibleLockedSyllables && lastVisibleCandidates.isNotEmpty()) {
            return lastVisibleCandidates
        }
        singleSyllableCache.clear()
        prefixCache.clear()
        exactCache.clear()

        val compositions = getValidCompositions()
        val primaryPinyin: String = compositions.firstOrNull { it.isComplete }?.pinyinString
            ?: compositions.firstOrNull()?.pinyinString ?: ""

        // ── Layer 1: exact phrases (from complete multi-syllable compositions) ──
        val exactPhrases = generateExactPhraseCandidates(compositions, limit)

        // ── Layer 2: user candidates ──
        val userCands = generateUserCandidates(primaryPinyin)

        // ── Layer 3: exact single chars (from single-syllable compositions) ──
        val exactSingles = generateExactSingleCandidates(compositions, limit).let { singles ->
            when {
                buffer.length == 1 -> singles.filter { it.text.length <= 1 }
                buffer.length == 2 -> singles.filter { it.text.length <= 2 }
                else -> singles
            }
        }

        val hasExactPhrase = exactPhrases.isNotEmpty()
        val hasUserCand = userCands.isNotEmpty()
        val hasSingle = exactSingles.isNotEmpty()

        // ── Layer 4: dynamic fallback (only when no other candidates exist) ──
        val dynamicFallback = if (!hasExactPhrase && !hasUserCand && !hasSingle && buffer.length >= 5) {
            generateDynamicFallbackCandidates(compositions)
        } else emptyList()

        // ── Build visible list by strict sourcing: order determined by layer, not score ──
        val combined = linkedSetOf<Candidate>()
        combined.addAll(exactPhrases)
        combined.addAll(userCands.filter { uc -> combined.none { c -> c.text == uc.text } })
        combined.addAll(exactSingles.filter { es -> combined.none { c -> c.text == es.text } })
        combined.addAll(dynamicFallback.filter { d -> combined.none { c -> c.text == d.text } })

        val finalCandidates = combined.toList().take(limit)

        lastVisibleCandidates = finalCandidates
        lastVisibleLimit = limit
        lastVisibleBuffer = buffer
        lastVisibleDictVersion = dictionary.dictionaryVersion
        lastVisibleLockedSyllables = lockedSyllables.toList()

        return finalCandidates
    }

    private fun generateExactPhraseCandidates(
        compositions: List<PinyinComposition>,
        limit: Int,
    ): List<Candidate> {
        val results = mutableListOf<Candidate>()
        for (comp in compositions) {
            if (!comp.isComplete || comp.pinyinList.size < 2) continue
            val candidates = getSentenceCandidates(
                comp, limit, comp.pinyinString, allowDynamic = false
            ).filter { c ->
                c.origin == CandidateOrigin.EXACT_PHRASE && c.text.length >= 2 &&
                !c.text.matches(Regex("^[a-zA-Z\\s]+$"))
            }
            results.addAll(candidates)
        }
        return results.sortedByDescending { it.score }.take(limit)
    }

    private fun generateExactSingleCandidates(
        compositions: List<PinyinComposition>,
        limit: Int,
    ): List<Candidate> {
        val results = mutableListOf<Candidate>()
        for (comp in compositions) {
            if (comp.pinyinList.size != 1) continue
            val pinyin = comp.pinyinList[0]
            val candidates = getSingleSyllableCandidates(
                pinyin, comp.isComplete, limit, comp.pinyinString, comp.segmentDigits
            ).filter { c ->
                c.origin == CandidateOrigin.EXACT_SINGLE && c.text.length == 1 &&
                !c.text.matches(Regex("^[a-zA-Z\\s]+$"))
            }
            results.addAll(candidates)
        }
        return results.sortedByDescending { it.score }.distinctBy { it.text }.take(limit)
    }

    private fun generateUserCandidates(primaryPinyin: String): List<Candidate> {
        if (primaryPinyin.isEmpty() || userDictionary == null) return emptyList()
        return userDictionary!!.getUserCandidates(primaryPinyin)
    }

    private fun generateDynamicFallbackCandidates(
        compositions: List<PinyinComposition>,
    ): List<Candidate> {
        val completeComp = compositions.firstOrNull { it.isComplete } ?: return emptyList()
        if (completeComp.pinyinList.size != 2) return emptyList()
        val pinyins = completeComp.pinyinList

        val batchResults = trackQuery { dictionary.getPinyinExactCandidatesMultiple(listOf(pinyins[0], pinyins[1])) }

        val firstCandidates = (batchResults[pinyins[0]] ?: emptyList())
            .filter { it.origin == CandidateOrigin.EXACT_SINGLE && it.text.length == 1 && it.score >= 50000 }
            .take(3)
        val secondCandidates = (batchResults[pinyins[1]] ?: emptyList())
            .filter { it.origin == CandidateOrigin.EXACT_SINGLE && it.text.length == 1 && it.score >= 50000 }
            .take(3)
        val results = mutableListOf<Candidate>()
        for (fc in firstCandidates) {
            for (sc in secondCandidates) {
                val combinedText = fc.text + sc.text
                val combinedScore = fc.score + sc.score
                if (combinedScore > 80000) {
                    results.add(
                        Candidate(combinedText, fc.code + sc.code, combinedScore,
                            CandidateType.NORMAL, completeComp.pinyinString,
                            CandidateOrigin.SAFE_DYNAMIC_COMPOSITION)
                    )
                }
            }
        }
        return results.sortedByDescending { it.score }.take(1)
    }

    fun getCandidates(limit: Int = 30): List<Candidate> {
        val currentDictVersion = dictionary.dictionaryVersion
        if (buffer.isEmpty()) return emptyList()
        if (buffer != lastBuffer || limit != lastLimit || currentDictVersion != lastDictVersion || lockedSyllables != lastVisibleLockedSyllables) {
            singleSyllableCache.clear()
            prefixCache.clear()
            exactCache.clear()
            lastCandidates = generateCandidates(buffer, limit)
            lastBuffer = buffer
            lastLimit = limit
            lastDictVersion = currentDictVersion
        }
        return lastCandidates
    }

    private val singleSyllableCache = mutableMapOf<String, List<Candidate>>()
    private val prefixCache = mutableMapOf<String, List<Candidate>>()
    private val exactCache = mutableMapOf<String, List<Candidate>>()

    private fun generateCandidates(currentBuffer: String, limit: Int, allowDynamic: Boolean = true): List<Candidate> {
        val startTime = System.currentTimeMillis()
        dbQueryCount = 0


        val compositions = getValidCompositions()
        if (compositions.isEmpty()) {
            return listOf(Candidate(currentBuffer, currentBuffer, -Int.MAX_VALUE, CandidateType.NORMAL, currentBuffer))
        }

        val allCandidates = mutableListOf<Candidate>()

        // Take the top 16 compositions to explore multiple plausible paths
        val topComps = compositions.take(8)

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

            val exactPhrases = if (comp.isComplete) exactCache.getOrPut(comp.pinyinString) { trackQuery { dictionary.getPinyinExactCandidates(comp.pinyinString) } } else emptyList()

            val candidates = if (comp.pinyinList.size == 1) {
                val singles = getSingleSyllableCandidates(comp.pinyinList[0], comp.isComplete, limit, comp.pinyinString, comp.segmentDigits)
                val prefixes = prefixCache.getOrPut(comp.pinyinString) { trackQuery { dictionary.getPinyinPrefixCandidates(comp.pinyinString) } }
                (exactPhrases.map { Candidate(it.text, it.code, it.score, it.type, comp.pinyinString, CandidateOrigin.EXACT_PHRASE) } + singles + prefixes).distinctBy { it.text }
            } else {
                val sentence = getSentenceCandidates(comp, limit, comp.pinyinString, allowDynamic)
                (exactPhrases.map { Candidate(it.text, it.code, it.score, it.type, comp.pinyinString, CandidateOrigin.EXACT_PHRASE) } + sentence).distinctBy { it.text }
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
            .toMutableList()

        // Always ensure the bare numeric fallback is at the very end
        distinctSorted.removeAll { it.text == currentBuffer }
        distinctSorted.add(Candidate(currentBuffer, currentBuffer, -Int.MAX_VALUE, CandidateType.NORMAL, currentBuffer, CandidateOrigin.RAW_FALLBACK)) // Bare numeric fallback

        return distinctSorted.sortedWith(Comparator { c1, c2 ->
            if (c1.text == currentBuffer) return@Comparator 1
            if (c2.text == currentBuffer) return@Comparator -1

            // EXACT_PHRASE always beats everything else
            if (c1.origin == CandidateOrigin.EXACT_PHRASE && c2.origin != CandidateOrigin.EXACT_PHRASE) return@Comparator -1
            if (c2.origin == CandidateOrigin.EXACT_PHRASE && c1.origin != CandidateOrigin.EXACT_PHRASE) return@Comparator 1

            // EXACT_SINGLE and SAFE_DYNAMIC_COMPOSITION beat DYNAMIC_COMPOSITION
            val isC1Safe = c1.origin == CandidateOrigin.EXACT_SINGLE || c1.origin == CandidateOrigin.SAFE_DYNAMIC_COMPOSITION
            val isC2Safe = c2.origin == CandidateOrigin.EXACT_SINGLE || c2.origin == CandidateOrigin.SAFE_DYNAMIC_COMPOSITION
            val isC1Dynamic = c1.origin == CandidateOrigin.DYNAMIC_COMPOSITION
            val isC2Dynamic = c2.origin == CandidateOrigin.DYNAMIC_COMPOSITION

            if (isC1Safe && isC2Dynamic) return@Comparator -1
            if (isC2Safe && isC1Dynamic) return@Comparator 1

            // Spaced dynamic composition shouldn't beat anything that isn't a fallback
            val isC1SpacedDynamic = c1.origin == CandidateOrigin.DYNAMIC_COMPOSITION && c1.text.contains(" ")
            val isC2SpacedDynamic = c2.origin == CandidateOrigin.DYNAMIC_COMPOSITION && c2.text.contains(" ")
            if (isC1SpacedDynamic && !isC2SpacedDynamic) return@Comparator 1
            if (isC2SpacedDynamic && !isC1SpacedDynamic) return@Comparator -1

            c2.score.compareTo(c1.score)
        }).take(limit).also {
            val elapsed = System.currentTimeMillis() - startTime
            val preeditCount = topComps.size
            logger?.log("T9Engine", "generateCandidates: buffer=${currentBuffer.length} preedit=$preeditCount queries=$dbQueryCount time=${elapsed}ms")
        }
    }

    private fun getSingleSyllableCandidates(pinyin: String, isComplete: Boolean, limit: Int, sourcePinyin: String, segmentDigits: List<String>): List<Candidate> {
        val candidates = singleSyllableCache.getOrPut(pinyin) {
            trackQuery { dictionary.getSingleSyllableCandidates(pinyin) }
        }

        val typedCodeLength = if (segmentDigits.isNotEmpty()) segmentDigits[0].length else T9CodeMapper.toCode(pinyin).length
        val codeLen = T9CodeMapper.toCode(pinyin).length

        return candidates
            .filter { candidate ->
                if (codeLen == 1) {
                    candidate.text.length == 1
                } else {
                    true
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

        // Pre-fetch all needed exact candidates
        val allPartStrs = mutableSetOf<String>()
        for (i in 1..pinyins.size) {
            for (j in 0 until i) {
                val partStr = pinyins.subList(j, i).joinToString(" ")
                allPartStrs.add(partStr)
            }
        }
        val batchResults = trackQuery { dictionary.getPinyinExactCandidatesMultiple(allPartStrs.toList()) }

        for (i in 1..pinyins.size) {
            val currentCandidates = mutableListOf<Candidate>()

            // Check for exact phrase short-circuit
            val isPrefix = (i == pinyins.size)
            if (isPrefix) {
                val fullPinyin = pinyins.joinToString(" ")
                val exactPhrases = batchResults[fullPinyin] ?: emptyList()
                val topExactPhrases = exactPhrases.filter { it.origin == CandidateOrigin.EXACT_PHRASE && it.text.length >= 2 && !it.text.matches(Regex("^[a-zA-Z\\s]+$")) }
                if (topExactPhrases.isNotEmpty() && topExactPhrases.size >= limit) {
                    dp[i] = topExactPhrases.map { Candidate(it.text, it.code, it.score + 50000, CandidateType.NORMAL, sourcePinyin, it.origin) }.toMutableList()
                    break
                }
            }

            for (j in 0 until i) {
                if (dp[j] == null || dp[j]!!.isEmpty()) continue

                val partPinyins = pinyins.subList(j, i)
                val partStr = partPinyins.joinToString(" ")
                val isPrefix = (i == pinyins.size)

                val partCandidates = batchResults[partStr] ?: emptyList()

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
                            baseScore -= 1000000
                        }

                        if (comp.rawDigits.length <= 9 && prevCandidate.text.isNotEmpty() && partCandidate.origin == CandidateOrigin.PREFIX_COMPLETION && partCandidate.text.length >= 2) {
                            // Penalize long prefix concatenations on short-ish inputs (like bu tai xinggu)
                            baseScore -= 1000000
                        }

                        if (comp.rawDigits.length <= 9 && isDynamic && partCandidate.origin == CandidateOrigin.PREFIX_COMPLETION) {
                            baseScore -= 500000
                        }

                        if (comp.rawDigits.length <= 4 && isDynamic) {
                            baseScore -= 500000
                        }

                        if (isDynamic) {
                            // general dynamic penalty to let exact phrases win
                            baseScore -= 50000
                        }

                        // Overpower any dynamic combination if space count is high on short input
                        if (comp.rawDigits.length <= 9 && newSpaceCount > 0) {
                            baseScore -= 2000000
                        }

                        if (comp.rawDigits.length <= 9 && isDynamic && newText.contains(" ") && partCandidate.text.length == 1) {
                            baseScore -= 2000000
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
                    .take(if (i == pinyins.size) limit else 10)
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
