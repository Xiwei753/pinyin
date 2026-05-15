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
            "今天	jin tian	100000",
            "晚上	wan shang	90000",
            "手机	shou ji	80000",
            "输入法	shu ru fa	70000",
            "你好	ni hao	60000",
            "妮好	ni hao	5000",
            "的	de	200000",
            "因为	yin wei	50000",
            "音	yin	60000",
            "为	wei	60000",
            "江泽民同志	jiang ze min tong zhi	50000",
            "江泽民	jiang ze min	40000",
            "监督	jian du	30000",
            "轿车	jiao che	20000",
            "交	jiao	60000"
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
        // Expect: only single characters or COMMON_SHORT. No "江泽民同志" etc.
        "5".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()
        assertFalse(candidates.any { it.text == "江泽民同志" })
        assertFalse(candidates.any { it.text == "江泽民" })
        assertFalse(candidates.any { it.text == "监督" })
        assertFalse(candidates.any { it.text == "轿车" })
        assertFalse(candidates.any { it.text == "今天" })
        assertTrue(candidates.last().text == "5")
    }

    @Test
    fun testShortInput_Length2() {
        // input: 54
        engine.clear()
        "54".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()
        // No sentence composition, no long words
        assertFalse(candidates.any { it.text == "江泽民同志" })
        assertFalse(candidates.any { it.text == "江泽民" })
        assertFalse(candidates.any { it.text.contains(" ") })
        assertTrue(candidates.last().text == "54")
    }

    @Test
    fun testShortInput_Length3() {
        // input: 542
        engine.clear()
        "542".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()
        // No low frequency or long words
        assertFalse(candidates.any { it.text == "江泽民同志" })
        assertFalse(candidates.any { it.text.contains(" ") })
        assertTrue(candidates.last().text == "542")
    }

    @Test
    fun testShortInput_Length4_Combinations() {
        // input: 5468
        "5468".forEach { engine.inputDigit(it.toString()) }
        val candidates = engine.getCandidates()
        // Ensure some 4+ rules kick in
        assertTrue(candidates.isNotEmpty())
    }
}
