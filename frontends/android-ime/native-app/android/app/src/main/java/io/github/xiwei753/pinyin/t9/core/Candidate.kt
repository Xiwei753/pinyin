package io.github.xiwei753.pinyin.t9.core

enum class CandidateType {
    SINGLE_CHAR,
    COMMON_SHORT,
    NORMAL,
    LONG_OR_LOW_FREQ
}

enum class CandidateOrigin {
    EXACT_SINGLE,
    EXACT_PHRASE,
    DYNAMIC_COMPOSITION,
    PREFIX_COMPLETION,
    RAW_FALLBACK,
    UNKNOWN
}

data class Candidate(
    val text: String,
    val code: String,
    val score: Int,
    val type: CandidateType = CandidateType.NORMAL,
    val sourcePinyin: String = "",
    val origin: CandidateOrigin = CandidateOrigin.UNKNOWN
)
