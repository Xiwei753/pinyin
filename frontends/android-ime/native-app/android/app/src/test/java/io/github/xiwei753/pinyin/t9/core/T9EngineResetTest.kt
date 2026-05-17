package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class T9EngineResetTest {
    class MockDict : DictionaryProvider {
        private val list = mutableListOf<Candidate>()

        fun add(c: Candidate, pinyin: String) {
            val origin = if (c.text.length == 1) CandidateOrigin.EXACT_SINGLE else CandidateOrigin.EXACT_PHRASE
            list.add(Candidate(c.text, pinyin, c.score, c.type, pinyin, origin))
        }

        override fun getSingleSyllableCandidates(pinyin: String): List<Candidate> {
            return list.filter { it.sourcePinyin == pinyin && it.origin == CandidateOrigin.EXACT_SINGLE }
        }

        override fun getPinyinExactCandidates(pinyinSeq: String): List<Candidate> {
            return list.filter { it.sourcePinyin == pinyinSeq && it.origin == CandidateOrigin.EXACT_PHRASE }
        }

        override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
            return list.filter { it.sourcePinyin.startsWith(pinyinPrefix) }
        }

        override fun getCandidates(code: String): List<Candidate> {
            return list.filter { it.code == code }
        }
        override fun getPrefixCandidates(code: String): List<Candidate> = emptyList()
        override fun getExactCandidates(code: String): List<Candidate> = emptyList()
    }

    @Test
    fun testResetState() {
        val dict = MockDict()
        dict.add(Candidate("我", "96", 100000, CandidateType.SINGLE_CHAR), "wo")
        dict.add(Candidate("能", "6364", 100000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)

        // Input some digits
        engine.inputDigit("9")
        engine.inputDigit("6")

        // Ensure state is dirty
        assertTrue(engine.buffer.isNotEmpty())
        assertTrue(engine.getPreedit().isNotEmpty())
        //assertTrue(engine.getVisibleCandidates(10).isNotEmpty())

        // Reset state
        engine.clear()

        // Verify state is clean
        assertTrue(engine.buffer.isEmpty())
        assertTrue(engine.getPreedit().isEmpty())
        assertTrue(engine.getVisibleCandidates(10).isEmpty())
        assertTrue(engine.getCandidates(10).isEmpty())

        // Input again to verify it works after clear
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        // Should not have "wo" or "我" anywhere
        assertEquals("neng", engine.getPreedit())
        val visible = engine.getVisibleCandidates(10)
        assertTrue(visible.any { it.text == "能" })
        assertTrue(visible.none { it.text == "我" })
    }
}
