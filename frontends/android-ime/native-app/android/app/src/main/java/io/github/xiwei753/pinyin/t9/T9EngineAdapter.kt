package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.imecore.CandidateSelection
import io.github.xiwei753.pinyin.imecore.CandidateSnapshotItem
import io.github.xiwei753.pinyin.imecore.T9InputEngine
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9Engine

object CandidateSnapshotMapper {
    fun toSnapshotItem(candidate: Candidate): CandidateSnapshotItem = CandidateSnapshotItem(
        text = candidate.text,
        code = candidate.code,
        sourcePinyin = candidate.sourcePinyin,
        score = candidate.score,
        origin = candidate.origin.name,
    )
}

class T9EngineAdapter(private val engine: T9Engine) : T9InputEngine {
    val rawEngine: T9Engine get() = engine
    override val buffer: String get() = engine.buffer
    override val readings: List<String> get() = engine.readings
    override val activeReading: String? get() = engine.activeReading
    override fun getPreedit(): String = engine.getPreedit()
    override fun inputDigit(digit: String) = engine.inputDigit(digit)
    override fun backspace() = engine.backspace()
    override fun clear() = engine.clear()
    override fun getVisibleCandidates(limit: Int): List<CandidateSelection> = engine.getVisibleCandidates(limit).map { candidate ->
        CandidateSelection(
            snapshot = CandidateSnapshotMapper.toSnapshotItem(candidate),
            commit = { engine.commitCandidate(candidate) },
        )
    }
    override fun setActiveReading(reading: String): Boolean = engine.setActiveReading(reading)
}
