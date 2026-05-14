package io.github.xiwei753.pinyin.t9.data

import io.github.xiwei753.pinyin.t9.core.Candidate

interface DictionaryProvider {
    fun getCandidates(code: String): List<Candidate>
}
