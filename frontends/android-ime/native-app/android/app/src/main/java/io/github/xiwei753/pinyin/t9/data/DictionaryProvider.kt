package io.github.xiwei753.pinyin.t9.data

import io.github.xiwei753.pinyin.t9.core.Candidate

interface DictionaryProvider {
    fun getCandidates(code: String): List<Candidate>
    fun getExactCandidates(code: String): List<Candidate>
    fun getPrefixCandidates(code: String): List<Candidate>
}
