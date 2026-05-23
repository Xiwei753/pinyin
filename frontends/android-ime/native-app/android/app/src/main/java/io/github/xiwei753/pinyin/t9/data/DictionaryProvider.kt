package io.github.xiwei753.pinyin.t9.data

import io.github.xiwei753.pinyin.t9.core.Candidate

interface DictionaryProvider {
    fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate>
    fun getPinyinExactCandidatesMultiple(pinyinSequences: List<String>): Map<String, List<Candidate>>
    fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate>
    fun getSingleSyllableCandidates(syllable: String): List<Candidate>

    // For legacy fallback
    fun getCandidates(code: String): List<Candidate>
    fun getExactCandidates(code: String): List<Candidate>
    fun getPrefixCandidates(code: String): List<Candidate>
}
