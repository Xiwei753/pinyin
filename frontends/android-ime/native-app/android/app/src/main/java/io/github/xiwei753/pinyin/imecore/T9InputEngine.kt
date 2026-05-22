package io.github.xiwei753.pinyin.imecore

import io.github.xiwei753.pinyin.t9.core.Candidate

interface T9InputEngine {
    val buffer: String
    val readings: List<String>
    val activeReading: String?
    fun getPreedit(): String
    fun inputDigit(digit: String)
    fun backspace()
    fun clear()
    fun getVisibleCandidates(limit: Int): List<Candidate>
    fun commitCandidate(candidate: Candidate)
    fun setActiveReading(reading: String): Boolean
}
