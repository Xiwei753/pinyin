package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class T9EngineTest {

    private lateinit var engine: T9Engine

    @Before
    fun setUp() {
        engine = T9Engine()
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
        assertTrue(candidates.contains("你好"))
    }

    @Test
    fun testInputAndCandidates_ShuRuFa() {
        "748732".forEach { engine.inputDigit(it.toString()) }
        assertEquals("748732", engine.buffer)
        val candidates = engine.getCandidates()
        assertEquals(listOf("输入法"), candidates)
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
        assertEquals("你好", selected)
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
