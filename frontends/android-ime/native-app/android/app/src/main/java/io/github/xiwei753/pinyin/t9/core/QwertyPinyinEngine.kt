package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class QwertyPinyinEngine(
    private val dictionary: DictionaryProvider,
    private var userDictionary: io.github.xiwei753.pinyin.t9.data.UserDictionaryProvider? = null,
    private val logger: io.github.xiwei753.pinyin.t9.T9DebugLogger? = null,
) {
    var buffer = ""
        private set

    private val parser = PinyinSyllableParser()

    private var lastBuffer = ""
    private var lastCandidates = listOf<Candidate>()

    fun inputLetter(letter: Char) {
        if (letter in 'a'..'z') {
            buffer += letter
        }
    }

    fun backspace() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.substring(0, buffer.length - 1)
        }
    }

    fun clear() {
        buffer = ""
        lastBuffer = ""
        lastCandidates = emptyList()
    }

    fun getPreedit(): String {
        if (buffer.isEmpty()) return ""
        val comps = parser.getCompositions(buffer)
        return comps.firstOrNull()?.pinyinString ?: buffer
    }

    fun getVisibleCandidates(limit: Int = 30): List<Candidate> {
        if (buffer.isEmpty()) return emptyList()
        if (buffer == lastBuffer && lastCandidates.isNotEmpty()) {
            return lastCandidates
        }

        val comps = parser.getCompositions(buffer)
        if (comps.isEmpty()) {
            lastBuffer = buffer
            lastCandidates = emptyList()
            return emptyList()
        }

        val primaryPinyin = comps.firstOrNull { it.isComplete }?.pinyinString
            ?: comps.firstOrNull()?.pinyinString ?: ""

        val allCandidates = mutableListOf<Candidate>()

        if (primaryPinyin.isNotEmpty()) {
            // Multi-syllable exact phrase lookups
            for (comp in comps) {
                if (!comp.isComplete) continue
                val exactCands = dictionary.getPinyinExactCandidates(comp.pinyinString)
                allCandidates.addAll(exactCands.filter { c ->
                    c.text.length >= 2 && !c.text.matches(Regex("^[a-zA-Z\\s]+$"))
                }.map { c ->
                    Candidate(c.text, c.code, c.score + 10000000, c.type, comp.pinyinString, CandidateOrigin.EXACT_PHRASE)
                })
            }

            // Single syllable candidates
            if (allCandidates.isEmpty()) {
                for (comp in comps) {
                    if (comp.pinyinList.size == 1 && comp.isComplete) {
                        val singles = dictionary.getSingleSyllableCandidates(comp.pinyinList[0])
                        allCandidates.addAll(singles.filter { c ->
                            c.text.length == 1 && !c.text.matches(Regex("^[a-zA-Z\\s]+$"))
                        }.map { c ->
                            Candidate(c.text, c.code, c.score + 500000, c.type, comp.pinyinString, CandidateOrigin.EXACT_SINGLE)
                        })
                    }
                }
            }

            // User dictionary
            val userCands = userDictionary?.getUserCandidates(primaryPinyin) ?: emptyList()
            allCandidates.addAll(userCands.map { c ->
                Candidate(c.text, c.code, c.score + 20000000, c.type, primaryPinyin, CandidateOrigin.USER_HISTORY)
            })
        }

        val result = allCandidates
            .sortedByDescending { it.score }
            .distinctBy { it.text }
            .take(limit)

        lastBuffer = buffer
        lastCandidates = result
        return result
    }

    fun commitCandidate(candidate: Candidate): Candidate {
        if (candidate.text != buffer && candidate.sourcePinyin.isNotEmpty()) {
            userDictionary?.recordSelection(candidate.text, candidate.sourcePinyin)
        }
        clear()
        return candidate
    }
}
