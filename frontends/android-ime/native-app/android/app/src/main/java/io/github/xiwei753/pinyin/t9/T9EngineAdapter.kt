package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.imecore.T9InputEngine
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9Engine

class T9EngineAdapter(private val engine: T9Engine) : T9InputEngine {
    val rawEngine: T9Engine get() = engine
    override val buffer: String get() = engine.buffer
    override val readings: List<String> get() = engine.readings
    override val activeReading: String? get() = engine.activeReading
    override fun getPreedit(): String = engine.getPreedit()
    override fun inputDigit(digit: String) = engine.inputDigit(digit)
    override fun backspace() = engine.backspace()
    override fun clear() = engine.clear()
    override fun getVisibleCandidates(limit: Int): List<Candidate> = engine.getVisibleCandidates(limit)
    override fun commitCandidate(candidate: Candidate) {
        engine.commitCandidate(candidate)
    }
    override fun setActiveReading(reading: String): Boolean = engine.setActiveReading(reading)
}
