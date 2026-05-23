package io.github.xiwei753.pinyin.imecore

data class CandidateRequest(
    val requestId: Long,
    val buffer: String,
    val lockedSyllables: List<String>,
    val limit: Int,
)

data class CandidateResult(
    val requestId: Long,
    val candidates: List<CandidateSnapshotItem>,
)
