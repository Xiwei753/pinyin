package io.github.xiwei753.pinyin.t9.core

enum class CandidateType {
    SINGLE_CHAR,
    COMMON_SHORT,
    NORMAL,
    LONG_OR_LOW_FREQ
}

data class Candidate(
    val text: String,
    val code: String,
    val score: Int,
    val type: CandidateType = CandidateType.NORMAL,
    val sourcePinyin: String = ""
)
