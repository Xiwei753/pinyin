package io.github.xiwei753.pinyin.t9.data

import io.github.xiwei753.pinyin.t9.core.Candidate

class BuiltinDictionary {
    private val dictionary = mapOf(
        "64426" to listOf(
            Candidate("你好", "64426", 100000),
            Candidate("妮好", "64426", 1000)
        ).sortedByDescending { it.score },
        "748732" to listOf(
            Candidate("输入法", "748732", 90000)
        ).sortedByDescending { it.score },
        "746946" to listOf(
            Candidate("拼音", "746946", 80000)
        ).sortedByDescending { it.score },
        "9466446" to listOf(
            Candidate("中国", "9466446", 70000)
        ).sortedByDescending { it.score },
        "866428" to listOf(
            Candidate("同步", "866428", 60000)
        ).sortedByDescending { it.score }
    )

    fun getCandidates(code: String): List<Candidate> {
        return dictionary[code] ?: emptyList()
    }
}
