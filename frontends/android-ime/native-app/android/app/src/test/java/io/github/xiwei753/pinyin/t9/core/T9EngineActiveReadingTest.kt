package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class T9EngineActiveReadingTest {

    @Test
    fun test6364ReadingsContainsMengNengMen() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")
        dict.add(Candidate("们", "men", 30000, CandidateType.SINGLE_CHAR), "men")
        dict.add(Candidate("个", "ge", 30000, CandidateType.SINGLE_CHAR), "ge")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val readings = engine.readings
        assertTrue("6364 readings must contain meng", readings.any { it == "meng" })
        assertTrue("6364 readings must contain neng", readings.any { it == "neng" })
        assertTrue("6364 readings must contain men", readings.any { it == "men" })
    }

    @Test
    fun testSetActiveReadingNengChangesPreedit() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        assertEquals("neng", engine.getPreedit())

        val result = engine.setActiveReading("neng")
        assertTrue("setActiveReading(neng) must succeed", result)
        assertEquals("neng", engine.getPreedit())
    }

    @Test
    fun testSetActiveReadingMengKeepsMengPreedit() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val result = engine.setActiveReading("meng")
        assertTrue("setActiveReading(meng) must succeed", result)
        assertEquals("meng", engine.getPreedit())
    }

    @Test
    fun testSetActiveReadingInvalidReturnsFalse() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val result = engine.setActiveReading("xxxx")
        assertFalse("setActiveReading with invalid reading must return false", result)
    }

    @Test
    fun testCandidatesForActiveReadingNengContainNeng() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        engine.setActiveReading("neng")
        val visible = engine.getVisibleCandidates()
        assertTrue("visible candidates must contain 能 when reading is neng", visible.any { it.text == "能" })

        val nengCandidate = visible.find { it.text == "能" }
        assertTrue(nengCandidate != null)
        assertEquals("neng", nengCandidate?.sourcePinyin)
    }

    @Test
    fun testCandidatesForActiveReadingMengContainMengChars() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("蒙", "meng", 45000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("萌", "meng", 40000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        engine.setActiveReading("meng")
        val visible = engine.getVisibleCandidates()
        assertTrue("meng candidates must contain 梦", visible.any { it.text == "梦" })
        assertTrue("meng candidates must contain 蒙", visible.any { it.text == "蒙" })
        assertTrue("meng candidates must contain 萌", visible.any { it.text == "萌" })
        for (c in visible) {
            assertEquals("meng", c.sourcePinyin)
        }
    }

    @Test
    fun testClearResetsActiveReading() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        engine.setActiveReading("neng")
        engine.clear()
        assertEquals("activeReading must be null after clear", null, engine.activeReading)
        assertTrue("readings must be empty after clear", engine.readings.isEmpty())
    }

    @Test
    fun testReadingsEmptyForEmptyBuffer() {
        val dict = T9EngineTest.MockDict()
        val engine = T9Engine(dict)
        assertTrue("readings must be empty for empty buffer", engine.readings.isEmpty())
    }

    @Test
    fun testSetActiveReadingOnEmptyBufferReturnsFalse() {
        val dict = T9EngineTest.MockDict()
        val engine = T9Engine(dict)
        assertFalse("setActiveReading on empty buffer must return false", engine.setActiveReading("meng"))
    }

    @Test
    fun testReadingSwitchDoesNotClearBuffer() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        engine.setActiveReading("neng")
        assertEquals("6364", engine.buffer)
    }

    @Test
    fun testReadingSwitchDoesNotCommitText() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        engine.setActiveReading("neng")
        // Buffer unchanged, no commit happened
        assertEquals("6364", engine.buffer)
    }

    @Test
    fun test96ReadingsContainsWo() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("我", "wo", 50000, CandidateType.SINGLE_CHAR), "wo")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("6")

        val readings = engine.readings
        assertTrue("96 readings must contain wo", readings.any { it == "wo" })
    }

    @Test
    fun testCandidateClickAfterReadingSwitchCommitsCorrectReading() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        engine.setActiveReading("neng")
        val visible = engine.getVisibleCandidates()
        val nengCandidate = visible.find { it.text == "能" }
        assertTrue(nengCandidate != null)

        engine.commitCandidate(nengCandidate!!)
        // Buffer cleared, activeReading reset
        assertEquals("", engine.buffer)
        assertEquals(null, engine.activeReading)
    }
}
