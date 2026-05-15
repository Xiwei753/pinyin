package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class T9EngineTest {

    private lateinit var engine: T9Engine

    @Before
    fun setUp() {
        val testDictionary = BuiltinDictionary(listOf(
            "你好\tni hao\t100000",
            "妮好\tni hao\t1000",
            "输入法\tshu ru fa\t90000"
        ))
        engine = T9Engine(testDictionary)
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
        // We know we have 2 candidates for 64426, and 1 for 7487832.
        // Let's add more to dictionary dynamically or rely on sorting logic.
        val largeDictionary = BuiltinDictionary((1..50).map { "测试$it\tce shi\t${100 - it}" })
        val limitEngine = T9Engine(largeDictionary)
        "23744".forEach { limitEngine.inputDigit(it.toString()) } // ce shi
        val candidates = limitEngine.getCandidates(10)
        assertEquals(10, candidates.size)
    }

    @Test
    fun testInputAndCandidates_NiHao() {
        engine.inputDigit("6")
        engine.inputDigit("4")
        engine.inputDigit("4")
        engine.inputDigit("2")
        engine.inputDigit("6")
        assertEquals("64426", engine.buffer)
        val candidates = engine.getCandidates()
        assertTrue(candidates.size >= 2)
        assertEquals("你好", candidates[0].text)
        assertEquals("妮好", candidates[1].text)
        assertTrue(candidates[0].score > candidates[1].score)
    }

    @Test
    fun testInputAndCandidates_ShuRuFa() {
        "7487832".forEach { engine.inputDigit(it.toString()) }
        assertEquals("7487832", engine.buffer)
        val candidates = engine.getCandidates()
        assertEquals(1, candidates.size)
        assertEquals("输入法", candidates[0].text)
    }

    @Test
    fun testBackspace() {
        "748".forEach { engine.inputDigit(it.toString()) }
        engine.backspace()
        assertEquals("74", engine.buffer)
    }

    @Test
    fun testClear() {
        "748".forEach { engine.inputDigit(it.toString()) }
        engine.clear()
        assertEquals("", engine.buffer)
        assertTrue(engine.getCandidates().isEmpty())
    }

    @Test
    fun testSelectCandidate() {
        "64426".forEach { engine.inputDigit(it.toString()) }
        val selected = engine.selectCandidate(0)
        assertEquals("你好", selected?.text)
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
}
