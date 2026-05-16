package io.github.xiwei753.pinyin.t9.core

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class T9EngineTest6364 {
    @Test
    fun test6364NengOverMeng() {
        val dict = T9EngineTest.MockDict()
        // Mock dict matches pinyin exactly because we override getSingleSyllableCandidates
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 100000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.isNotEmpty())
        assertEquals("能", visible[0].text)

        val preedit = engine.getPreedit()
        assertEquals("neng", preedit)
        assertTrue(preedit != "meng")
        assertTrue(preedit != "menge")
    }

    @Test
    fun test6364MengOverNeng() {
        val dict = T9EngineTest.MockDict()
        // Here we artificially make meng score higher to ensure it dynamically uses the score
        dict.add(Candidate("梦", "meng", 100000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 50000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.isNotEmpty())
        assertEquals("梦", visible[0].text)

        val preedit = engine.getPreedit()
        assertEquals("meng", preedit)
    }
}
