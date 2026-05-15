package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class T9EngineTest {

    private lateinit var engine: T9Engine

    @Before
    fun setUp() {
        val testDictionary = BuiltinDictionary(listOf(
            "今天\tjin tian\t100000",
            "晚上\twan shang\t90000",
            "手机\tshou ji\t80000",
            "输入法\tshu ru fa\t70000",
            "你好\tni hao\t60000",
            "妮好\tni hao\t5000",
            "的\tde\t200000",
            "因为\tyin wei\t50000",
            "音\tyin\t60000",
            "为\twei\t60000",
            "江泽民同志\tjiang ze min tong zhi\t50000",
            "江泽民\tjiang ze min\t40000",
            "监督\tjian du\t30000",
            "轿车\tjiao che\t20000"
        ))
        engine = T9Engine(testDictionary)
    }

    @Test
    fun testSentenceSorting_CompleteWordBeatsFragments() {
        // yin(946) wei(934) = 946934
        "946934".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()

        // "因为" should beat "音 为", despite their individual scores potentially summing higher
        // because of the penalty applied to fragmenting
        assertEquals("因为", candidates[0].text)
        assertTrue(candidates.any { it.text == "音 为" })
    }

    @Test
    fun testEmptyInputReturnsNoCandidates() {
        assertTrue(engine.getCandidates().isEmpty())
    }

    @Test
    fun testPrefixMatching() {
        engine.inputDigit("6")
        engine.inputDigit("4")
        val candidates = engine.getCandidates()
        // "64" is prefix of "64426" (ni hao)
        assertTrue(candidates.any { it.text == "你好" })
        assertTrue(candidates.any { it.text == "妮好" })
        assertEquals("你好", candidates[0].text) // Highest score first
    }

    @Test
    fun testCandidateLimit() {
        val largeDictionary = BuiltinDictionary((1..50).map { "测试$it\tce shi\t${100 - it}" })
        val limitEngine = T9Engine(largeDictionary)
        "23744".forEach { limitEngine.inputDigit(it.toString()) } // ce shi
        val candidates = limitEngine.getCandidates(10)
        assertEquals(10, candidates.size)
    }

    @Test
    fun testInputAndCandidates_NiHao() {
        "64426".forEach { engine.inputDigit(it.toString()) }
        assertEquals("64426", engine.buffer)
        val candidates = engine.getCandidates()
        assertTrue(candidates.size >= 2)
        assertEquals("你好", candidates[0].text)
        assertEquals("妮好", candidates[1].text)
        assertTrue(candidates[0].score > candidates[1].score)
    }

    @Test
    fun testSentenceComposition_JinTianWanShang() {
        // jin(546) tian(8426) = 5468426, wan(926) shang(74264) = 92674264
        // Total: 546842692674264
        "546842692674264".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()

        // Exact composition
        assertTrue(candidates.isNotEmpty())
        assertEquals("今天 晚上", candidates[0].text)
    }

    @Test
    fun testSentenceCompositionWithPrefix_JinTianW() {
        // jin(546) tian(8426) = 5468426, wan(9) prefix
        // Total: 54684269
        "54684269".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()

        assertTrue(candidates.isNotEmpty())
        assertEquals("今天 晚上", candidates[0].text)
    }

    @Test
    fun testRawFallback() {
        "22222222".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()
        assertEquals(1, candidates.size)
        assertEquals("22222222", candidates[0].text)
    }

    @Test
    fun testBackspace() {
        "546842692".forEach { engine.inputDigit(it.toString()) } // jintianw
        val beforeBackspace = engine.getCandidates()
        assertEquals("今天 晚上", beforeBackspace[0].text)

        engine.backspace() // 54684269
        engine.backspace() // 5468426

        assertEquals("5468426", engine.buffer)
        val afterBackspace = engine.getCandidates()
        assertEquals("今天", afterBackspace[0].text)
    }

    @Test
    fun testClear() {
        "748".forEach { engine.inputDigit(it.toString()) }
        engine.clear()
        assertEquals("", engine.buffer)
        assertTrue(engine.getCandidates().isEmpty())
    }

    @Test
    fun testCommitCandidateCleansSpaces() {
        "546842692674264".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()
        val selected = engine.commitCandidate(candidates[0])
        assertEquals("今天晚上", selected.text) // Spaces should be removed
        assertEquals("", engine.buffer) // Should clear buffer after selection
    }

    @Test
    fun testInvalidInput() {
        engine.inputDigit("1")
        engine.inputDigit("0")
        engine.inputDigit("*")
        engine.inputDigit("#")
        assertEquals("", engine.buffer)
    }

    @Test
    fun testShortInput_Length1() {
        // input: 5
        // Expect: only single characters. No "江泽民同志" etc.
        "5".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()
        assertFalse(candidates.any { it.text == "江泽民同志" })
        assertFalse(candidates.any { it.text == "江泽民" })
        assertFalse(candidates.any { it.text == "监督" })
        assertFalse(candidates.any { it.text == "轿车" })
        // Since test dict has "今天"(2 chars), it shouldn't show up for len 1
        assertFalse(candidates.any { it.text == "今天" })
        assertTrue(candidates.last().text == "5")
    }

    @Test
    fun testShortInput_Length2() {
        // input: 54
        // Expect: max 2 chars. No "江泽民同志" etc.
        "54".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()
        assertFalse(candidates.any { it.text == "江泽民同志" })
        assertFalse(candidates.any { it.text == "江泽民" })
        // Could show 监督/轿车 but test dict has length 2. Let's see if jian(5426) or jiao(5426) show up
        // yes they can be prefixes, but text.length <= 2 must hold.
        // Also ensure no sentence composition
        assertFalse(candidates.any { it.text.contains(" ") })
        assertTrue(candidates.last().text == "54")
    }

    @Test
    fun testShortInput_Length3() {
        // input: 542
        // Expect: max 3 chars. No "江泽民同志"
        "542".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()
        assertFalse(candidates.any { it.text == "江泽民同志" })
        // But 江泽民 is length 3, could show up depending on score.
        assertFalse(candidates.any { it.text.contains(" ") })
        assertTrue(candidates.last().text == "542")
    }
}
