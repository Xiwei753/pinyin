package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class T9EngineTest {

    class MockDict : DictionaryProvider {
        private val list = mutableListOf<Candidate>()

        fun add(c: Candidate, pinyin: String) {
            // We use the pinyin string clean as the code for the mock to emulate pinyin index
            list.add(Candidate(c.text, pinyin.replace(" ", ""), c.score, c.type))
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
    fun testPreedit() {
        val dict = MockDict()
        val engine = T9Engine(dict)

        engine.inputDigit("2")
        engine.inputDigit("8")
        assertEquals("bu", engine.getPreedit())

        engine.inputDigit("8")
        engine.inputDigit("2")
        engine.inputDigit("4")
        assertEquals("bu tai", engine.getPreedit())

        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("4")
        assertEquals("bu tai xing", engine.getPreedit())

        engine.clear()

        // Test jin tian wan shang
        val digits = "546842692674264"
        for (d in digits) {
            engine.inputDigit(d.toString())
        }
        assertEquals("jin tian wan shang", engine.getPreedit())
    }

    @Test
    fun testSeparator() {
        val dict = MockDict()
        val engine = T9Engine(dict)

        engine.inputDigit("2")
        engine.inputDigit("8")
        engine.inputDigit("1")
        engine.inputDigit("8")
        engine.inputDigit("2")
        engine.inputDigit("4")
        assertEquals("bu tai", engine.getPreedit())

        engine.clear()

        val sepDigits = "28182419464"
        for (d in sepDigits) {
            engine.inputDigit(d.toString())
        }
        assertEquals("bu tai xing", engine.getPreedit())

        // Test backspacing separator updates preedit
        engine.backspace() // removes '4'
        engine.backspace() // removes '6'
        engine.backspace() // removes '4'
        engine.backspace() // removes '9'
        engine.backspace() // removes '1'
        assertEquals("bu tai", engine.getPreedit())
    }

    @Test
    fun testFallback() {
        val dict = MockDict()
        val engine = T9Engine(dict)
        engine.inputDigit("2")
        engine.inputDigit("8")
        val cands = engine.getCandidates()
        assertEquals("28", cands.last().text)
    }

    @Test
    fun testShortCandidates() {
        val dict = MockDict()
        dict.add(Candidate("不", "28", 1000, CandidateType.SINGLE_CHAR), "bu")
        dict.add(Candidate("部", "28", 900, CandidateType.SINGLE_CHAR), "bu")
        dict.add(Candidate("不太", "28824", 500, CandidateType.NORMAL), "bu tai")

        val engine = T9Engine(dict)
        engine.inputDigit("2")
        engine.inputDigit("8")

        val cands = engine.getCandidates()
        assertEquals("不", cands[0].text)
        assertEquals("部", cands[1].text)
    }

    @Test
    fun testLongSentenceCandidatesNotMatchingRawDigits() {
        val dict = MockDict()
        dict.add(Candidate("不太行", "288249464", 1000, CandidateType.NORMAL), "bu tai xing")
        // This is a word that matches the raw digits but NOT the pinyin sequence bu tai xing
        dict.add(Candidate("不太新股", "288249464", 900, CandidateType.NORMAL), "bu tai xin gu")
        dict.add(Candidate("不太英语", "288249464", 800, CandidateType.NORMAL), "bu tai ying yu")

        val engine = T9Engine(dict)
        engine.inputDigit("2")
        engine.inputDigit("8")
        engine.inputDigit("8")
        engine.inputDigit("2")
        engine.inputDigit("4")
        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val cands = engine.getCandidates()
        assertEquals("不太行", cands[0].text)
        // Should not have 不太新股 or 不太英语
        assertTrue(cands.none { it.text == "不太新股" })
        assertTrue(cands.none { it.text == "不太英语" })
    }
}
