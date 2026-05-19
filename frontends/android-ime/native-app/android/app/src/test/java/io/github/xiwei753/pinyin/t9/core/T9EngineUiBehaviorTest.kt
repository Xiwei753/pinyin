package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class T9EngineUiBehaviorTest {

    class MockDict : DictionaryProvider {
        private val list = mutableListOf<Candidate>()

        fun add(c: Candidate, pinyin: String) {
            val origin = if (c.text.length == 1) CandidateOrigin.EXACT_SINGLE else CandidateOrigin.EXACT_PHRASE
            list.add(Candidate(c.text, pinyin, c.score, c.type, "", origin))
        }

        override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> {
            return list.filter { it.code == pinyinSequence }
        }

        override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
            return list.filter { it.code.startsWith(pinyinPrefix) }
        }

        override fun getSingleSyllableCandidates(syllable: String): List<Candidate> {
            return list.filter { it.code == syllable }
        }

        override fun getCandidates(code: String): List<Candidate> = emptyList()
        override fun getExactCandidates(code: String): List<Candidate> = emptyList()
        override fun getPrefixCandidates(code: String): List<Candidate> = emptyList()
    }

    @Test
    fun testPreeditIsWoFor96() {
        val dict = MockDict()
        dict.add(Candidate("我", "96", 100000, CandidateType.SINGLE_CHAR), "wo")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("6")

        val preedit = engine.getPreedit()
        assertEquals("wo", preedit)
        assertTrue(preedit != "96")

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.any { it.text == "我" })
        assertTrue(visible.none { it.text == "96" })
    }

    @Test
    fun testPreeditIsBuTaiXingFor288249464() {
        val dict = MockDict()
        dict.add(Candidate("不太行", "288249464", 100000, CandidateType.NORMAL), "bu tai xing")

        val engine = T9Engine(dict)
        val digits = "288249464"
        for (d in digits) {
            engine.inputDigit(d.toString())
        }

        val preedit = engine.getPreedit()
        assertEquals("bu tai xing", preedit)
        assertTrue(preedit != "288249464")

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.any { it.text == "不太行" })
        assertTrue(visible.none { it.text == "288249464" })
    }

    @Test
    fun testSeparatorKey1ProducesSamePreedit() {
        val dict = MockDict()
        dict.add(Candidate("不太行", "288249464", 100000, CandidateType.NORMAL), "bu tai xing")

        val engine1 = T9Engine(dict)
        for (d in "288249464") engine1.inputDigit(d.toString())
        val preedit1 = engine1.getPreedit()

        val engine2 = T9Engine(dict)
        for (d in "28182419464") engine2.inputDigit(d.toString())
        val preedit2 = engine2.getPreedit()

        assertEquals("bu tai xing", preedit1)
        assertEquals("bu tai xing", preedit2)

        val visible1 = engine1.getVisibleCandidates()
        val visible2 = engine2.getVisibleCandidates()
        assertTrue(visible1.any { it.text == "不太行" })
        assertTrue(visible2.any { it.text == "不太行" })
    }

    @Test
    fun testCommitCandidateClearsState() {
        val dict = MockDict()
        dict.add(Candidate("我", "96", 100000, CandidateType.SINGLE_CHAR), "wo")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("6")

        val preBefore = engine.getPreedit()
        assertEquals("wo", preBefore)

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.isNotEmpty())
        val candidateToCommit = visible[0]

        val committed = engine.commitCandidate(candidateToCommit)
        assertEquals(candidateToCommit.text, committed.text)

        assertEquals("", engine.getPreedit())
        assertEquals("", engine.buffer)
        assertTrue(engine.getVisibleCandidates().isEmpty())
    }

    @Test
    fun testVisibleCandidatesNeverContainRawDigits() {
        val dict = MockDict()
        dict.add(Candidate("我", "96", 100000, CandidateType.SINGLE_CHAR), "wo")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("6")

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.none { it.text == "96" })
        assertTrue(visible.none { it.text == "9" })
        assertTrue(visible.none { it.text.matches(Regex("^\\d+$")) })
    }

    @Test
    fun testVisibleCandidatesNeverContainPinyinAsCandidate() {
        val dict = MockDict()
        dict.add(Candidate("wo", "96", 10000, CandidateType.NORMAL), "wo")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("6")

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.none { it.text.matches(Regex("^[a-zA-Z]+$")) })
    }

    @Test
    fun testBufferEmptyGetPreeditReturnsEmpty() {
        val dict = MockDict()
        val engine = T9Engine(dict)
        assertEquals("", engine.getPreedit())
        assertTrue(engine.getVisibleCandidates().isEmpty())
    }

    @Test
    fun testVisibilityAfterClear() {
        val dict = MockDict()
        dict.add(Candidate("我", "96", 100000, CandidateType.SINGLE_CHAR), "wo")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("6")

        assertTrue(engine.getVisibleCandidates().isNotEmpty())
        assertTrue(engine.getPreedit().isNotEmpty())

        engine.clear()
        assertEquals("", engine.getPreedit())
        assertTrue(engine.getVisibleCandidates().isEmpty())
    }

    @Test
    fun testCommitFirstCandidatePreservesCorrectText() {
        val dict = MockDict()
        dict.add(Candidate("不太行", "288249464", 100000, CandidateType.NORMAL), "bu tai xing")
        dict.add(Candidate("不太新", "288249464", 90000, CandidateType.NORMAL), "bu tai xin")
        dict.add(Candidate("不太信", "288249464", 80000, CandidateType.NORMAL), "bu tai xin")

        val engine = T9Engine(dict)
        for (d in "288249464") engine.inputDigit(d.toString())

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.isNotEmpty())
        assertEquals("不太行", visible[0].text)

        val committed = engine.commitCandidate(visible[0])
        assertEquals("不太行", committed.text)
        assertEquals("", engine.buffer)
    }

    @Test
    fun testRepeatInputAndCommitCycles() {
        val dict = MockDict()
        dict.add(Candidate("我", "96", 100000, CandidateType.SINGLE_CHAR), "wo")

        val engine = T9Engine(dict)
        for (cycle in 1..3) {
            engine.inputDigit("9")
            engine.inputDigit("6")
            assertEquals("wo", engine.getPreedit())
            val cands = engine.getVisibleCandidates()
            assertTrue(cands.any { it.text == "我" })
            engine.commitCandidate(cands[0])
            assertEquals("", engine.buffer)
            assertEquals("", engine.getPreedit())
            assertTrue(engine.getVisibleCandidates().isEmpty())
        }
    }
}
