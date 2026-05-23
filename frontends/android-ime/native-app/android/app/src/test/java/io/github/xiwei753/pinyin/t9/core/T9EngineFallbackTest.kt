package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class T9EngineFallbackTest {

    class EmptyDict : DictionaryProvider {
    override fun getPinyinExactCandidatesMultiple(pinyinSequences: List<String>): Map<String, List<Candidate>> = emptyMap()
        override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> = emptyList()
        override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> = emptyList()
        override fun getSingleSyllableCandidates(syllable: String): List<Candidate> = emptyList()

        override fun getCandidates(code: String): List<Candidate> = emptyList()
        override fun getExactCandidates(code: String): List<Candidate> = emptyList()
        override fun getPrefixCandidates(code: String): List<Candidate> = emptyList()
    }

    @Test
    fun testFallback() {
        val engine = T9Engine(EmptyDict())
        engine.inputDigit("2")
        engine.inputDigit("8")

        val cands = engine.getCandidates()
        assertEquals(1, cands.size)
        assertEquals("28", cands[0].text)

        // Even when we add separators
        engine.inputDigit("1")
        engine.inputDigit("8")
        val cands2 = engine.getCandidates()
        assertEquals(1, cands2.size)
        assertEquals("2818", cands2[0].text)
    }
}
