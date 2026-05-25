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
        dict.add(Candidate("们个", "men ge", 200000, CandidateType.NORMAL), "men ge") // Even with very high score, this comes as EXACT_PHRASE now

        // Let's test the dynamic components
        dict.add(Candidate("们", "men", 50000, CandidateType.SINGLE_CHAR), "men")
        dict.add(Candidate("个", "ge", 50000, CandidateType.SINGLE_CHAR), "ge")
        dict.add(Candidate("哦", "o", 50000, CandidateType.SINGLE_CHAR), "o")
        dict.add(Candidate("嗯", "eng", 50000, CandidateType.SINGLE_CHAR), "eng")

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
        assertTrue(preedit != "men ge")
        assertTrue(preedit != "o eng")

        val topCandidates = visible.take(5).map { it.text }
        // Oh/Eng dynamic combination shouldn't appear
        assertTrue("哦嗯" !in topCandidates)
        assertTrue("哦 嗯" !in topCandidates)
        // Men Ge dynamic combination shouldn't appear (men ge space). However, exact phrase "们个" WOULD appear if it was added as exact phrase (like the mock did above).
        // We need to ensure that without the exact phrase, it's not visible.
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
        assertEquals("neng", preedit)
    }

    @Test
    fun test6364OEngDynamicHidden() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("哦", "o", 100000, CandidateType.SINGLE_CHAR), "o")
        dict.add(Candidate("嗯", "eng", 100000, CandidateType.SINGLE_CHAR), "eng")
        dict.add(Candidate("能", "neng", 50000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.isNotEmpty())
        assertEquals("能", visible[0].text)

        val topCandidates = visible.take(5).map { it.text }
        assertTrue("哦嗯" !in topCandidates)
        assertTrue("哦 嗯" !in topCandidates)
    }

    @Test
    fun test6364MenGeDynamicHidden() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("们", "men", 100000, CandidateType.SINGLE_CHAR), "men")
        dict.add(Candidate("个", "ge", 100000, CandidateType.SINGLE_CHAR), "ge")
        dict.add(Candidate("能", "neng", 50000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.isNotEmpty())
        assertEquals("能", visible[0].text)

        val topCandidates = visible.take(5).map { it.text }
        assertTrue("们个" !in topCandidates)
        assertTrue("们 个" !in topCandidates)
    }
}
