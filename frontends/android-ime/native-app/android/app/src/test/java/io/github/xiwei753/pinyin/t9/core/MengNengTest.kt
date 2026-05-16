package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.xiwei753.pinyin.t9.core.T9EngineTest.MockDict

class MengNengTest {

    @Test
    fun testMengNengGeneric() {
        val dict = MockDict()
        // test meng
        dict.add(Candidate("梦", "meng", 1000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("蒙", "meng", 900, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 800, CandidateType.SINGLE_CHAR), "neng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val visible = engine.getVisibleCandidates()
        val preedit = engine.getPreedit()

        assertEquals("meng", preedit)
        assertTrue(visible.any { it.text == "梦" })
        assertTrue(visible.any { it.text == "蒙" })
        assertTrue(visible.none { it.text == "能" && it.sourcePinyin == "meng" })
        assertEquals("meng", visible[0].sourcePinyin)

        engine.clear()

        val dict2 = MockDict()
        dict2.add(Candidate("能", "neng", 1000, CandidateType.SINGLE_CHAR), "neng")
        dict2.add(Candidate("梦", "meng", 900, CandidateType.SINGLE_CHAR), "meng")

        val engine2 = T9Engine(dict2)
        engine2.inputDigit("6")
        engine2.inputDigit("3")
        engine2.inputDigit("6")
        engine2.inputDigit("4")

        val visible2 = engine2.getVisibleCandidates()
        val preedit2 = engine2.getPreedit()

        assertEquals("neng", preedit2)
        assertTrue(visible2.any { it.text == "能" })
        assertTrue(visible2.any { it.text == "梦" })
        assertEquals("neng", visible2[0].sourcePinyin)
    }

    @Test
    fun testNoMasqueradingDynamicComposition() {
        val dict = MockDict()
        dict.add(Candidate("梦", "meng", 1000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 800, CandidateType.SINGLE_CHAR), "neng")

        dict.add(Candidate("么", "me", 5000, CandidateType.SINGLE_CHAR), "me")
        dict.add(Candidate("嗯", "ng", 4000, CandidateType.SINGLE_CHAR), "ng")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val visible = engine.getVisibleCandidates()
        assertTrue(visible.none { it.origin == CandidateOrigin.DYNAMIC_COMPOSITION })

        // DYNAMIC_COMPOSITION masquerading as EXACT_SINGLE will cause the preedit to be "me ng",
        // while the candidate would be "梦". We must ensure "梦" comes from "meng"
        val mengCandidate = visible.find { it.text == "梦" }
        assertTrue(mengCandidate != null)
        assertEquals("meng", mengCandidate?.sourcePinyin)
        assertEquals(CandidateOrigin.EXACT_SINGLE, mengCandidate?.origin)
    }
}
