package io.github.xiwei753.pinyin.imecore

interface T9InputEngine {
    val buffer: String
    val lockedSyllables: List<String>
    val readings: List<String>
    val activeReading: String?
    fun getPreedit(): String
    fun getPreeditHint(): String
    fun inputDigit(digit: String)
    fun backspace()
    fun clear()
    fun getVisibleCandidates(limit: Int): List<CandidateSelection>
    fun commitCandidate(candidate: CandidateSnapshotItem)
    fun setActiveReading(reading: String): Boolean
}
