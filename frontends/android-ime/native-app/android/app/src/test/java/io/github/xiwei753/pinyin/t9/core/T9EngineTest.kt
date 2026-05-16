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
            val origin = if (c.text.length == 1) CandidateOrigin.EXACT_SINGLE else CandidateOrigin.EXACT_PHRASE
            list.add(Candidate(c.text, pinyin.replace(" ", ""), c.score, c.type, "", origin))
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
    fun testCandidateLimitCaching() {
        val dict = MockDict()
        for (i in 1..20) {
            dict.add(Candidate("测$i", "28", 1000 - i, CandidateType.SINGLE_CHAR), "bu")
        }

        val engine = T9Engine(dict)
        engine.inputDigit("2")
        engine.inputDigit("8")

        // Test 1: getPreedit() shouldn't pollute cache with 30 candidates
        val preedit = engine.getPreedit()
        assertEquals("bu", preedit)
        var cands = engine.getCandidates(5)
        assertTrue(cands.size <= 5)

        engine.clear()
        engine.inputDigit("2")
        engine.inputDigit("8")

        // Test 2: changing from larger to smaller limit
        cands = engine.getCandidates(30)
        assertTrue(cands.size > 5)
        cands = engine.getCandidates(5)
        assertTrue(cands.size <= 5)

        engine.clear()
        engine.inputDigit("2")
        engine.inputDigit("8")

        // Test 3: changing from smaller to larger limit
        cands = engine.getCandidates(5)
        assertTrue(cands.size <= 5)
        cands = engine.getCandidates(10)
        assertTrue(cands.size > 5)
        assertTrue(cands.size <= 10)
    }

    @Test
    fun testPreeditSourceSyncWithoutCache() {
        val dict = MockDict()
        dict.add(Candidate("周", "9468", 100000, CandidateType.SINGLE_CHAR), "zhou")
        dict.add(Candidate("字母", "9468", 90000, CandidateType.NORMAL), "zi mu")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("8")

        // Call getPreedit() first to ensure it computes correctly without an existing limit cache
        val preedit = engine.getPreedit()

        // Then call getCandidates() with a small limit
        val cands = engine.getCandidates(5)

        // Preedit must correctly reflect the first real candidate
        if (cands[0].text == "周") {
            assertEquals("zhou", preedit)
        } else if (cands[0].text == "字母") {
            assertEquals("zi mu", preedit)
        }

        // Cache must not be polluted, limit should be respected
        assertTrue(cands.size <= 5)
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
    fun testSingleCharactersMustBePresent() {
        val dict = MockDict()
        dict.add(Candidate("我", "96", 100000, CandidateType.SINGLE_CHAR), "wo")
        dict.add(Candidate("你", "64", 90000, CandidateType.SINGLE_CHAR), "ni")
        dict.add(Candidate("他", "82", 80000, CandidateType.SINGLE_CHAR), "ta")
        dict.add(Candidate("她", "82", 80000, CandidateType.SINGLE_CHAR), "ta")
        dict.add(Candidate("的", "33", 100000, CandidateType.SINGLE_CHAR), "de")
        dict.add(Candidate("得", "33", 90000, CandidateType.SINGLE_CHAR), "de")
        dict.add(Candidate("地", "33", 80000, CandidateType.SINGLE_CHAR), "de")
        dict.add(Candidate("不", "28", 100000, CandidateType.SINGLE_CHAR), "bu")
        dict.add(Candidate("是", "744", 100000, CandidateType.SINGLE_CHAR), "shi")
        dict.add(Candidate("不太", "28824", 40000, CandidateType.NORMAL), "bu tai")

        val engine = T9Engine(dict)

        // 96 -> 我
        engine.inputDigit("9")
        engine.inputDigit("6")
        assertTrue(engine.getVisibleCandidates().any { it.text == "我" })
        assertEquals("wo", engine.getPreedit())
        engine.clear()

        // 64 -> 你
        engine.inputDigit("6")
        engine.inputDigit("4")
        assertTrue(engine.getVisibleCandidates().any { it.text == "你" })
        assertEquals("ni", engine.getPreedit())
        engine.clear()

        // 82 -> 他/她
        engine.inputDigit("8")
        engine.inputDigit("2")
        assertTrue(engine.getVisibleCandidates().any { it.text == "他" })
        assertTrue(engine.getVisibleCandidates().any { it.text == "她" })
        engine.clear()

        // 33 -> 的/得/地
        engine.inputDigit("3")
        engine.inputDigit("3")
        assertTrue(engine.getVisibleCandidates().any { it.text == "的" })
        assertTrue(engine.getVisibleCandidates().any { it.text == "得" })
        assertTrue(engine.getVisibleCandidates().any { it.text == "地" })
        assertEquals("de", engine.getPreedit())
        engine.clear()

        // 744 -> 是
        engine.inputDigit("7")
        engine.inputDigit("4")
        engine.inputDigit("4")
        assertTrue(engine.getCandidates().any { it.text == "是" })
        engine.clear()

        // 28 -> 不
        engine.inputDigit("2")
        engine.inputDigit("8")
        assertTrue(engine.getCandidates().any { it.text == "不" })
        engine.clear()

        // 28824 -> 不太
        engine.inputDigit("2")
        engine.inputDigit("8")
        engine.inputDigit("8")
        engine.inputDigit("2")
        engine.inputDigit("4")
        assertTrue(engine.getCandidates().any { it.text == "不太" })
        engine.clear()
    }

    @Test
    fun testShortAmbiguousPath() {
        val dict = MockDict()
        dict.add(Candidate("周", "9468", 100000, CandidateType.SINGLE_CHAR), "zhou")
        dict.add(Candidate("字母", "9468", 90000, CandidateType.NORMAL), "zi mu")
        // These are low quality dynamic phrases that could be constructed but should be penalized
        dict.add(Candidate("一", "94", 80000, CandidateType.SINGLE_CHAR), "yi")
        dict.add(Candidate("母", "68", 80000, CandidateType.SINGLE_CHAR), "mu")
        dict.add(Candidate("木", "68", 80000, CandidateType.SINGLE_CHAR), "mu")
        dict.add(Candidate("目", "68", 80000, CandidateType.SINGLE_CHAR), "mu")
        dict.add(Candidate("怒", "68", 80000, CandidateType.SINGLE_CHAR), "nu")
        dict.add(Candidate("榜", "68", 80000, CandidateType.SINGLE_CHAR), "mu") // Just for test

        // Add some more valid candidates to fill the top 5
        dict.add(Candidate("猪", "9468", 70000, CandidateType.SINGLE_CHAR), "zhou")
        dict.add(Candidate("轴", "9468", 60000, CandidateType.SINGLE_CHAR), "zhou")
        dict.add(Candidate("昼", "9468", 50000, CandidateType.SINGLE_CHAR), "zhou")
        dict.add(Candidate("肘", "9468", 40000, CandidateType.SINGLE_CHAR), "zhou")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("8")

        val cands = engine.getCandidates()
        val preedit = engine.getPreedit()

        assertTrue(cands.any { it.text == "周" } || cands.any { it.text == "字母" })
        assertTrue(cands[0].text == "周" || cands[0].text == "字母")

        if (cands[0].text == "周") {
            assertEquals("zhou", preedit)
        } else if (cands[0].text == "字母") {
            assertEquals("zi mu", preedit)
        }

        val topCandsText = cands.take(5).map { it.text }

        assertTrue("一母" !in topCandsText)
        assertTrue("一 母" !in topCandsText)
        assertTrue("一木" !in topCandsText)
        assertTrue("一 木" !in topCandsText)
        assertTrue("一目" !in topCandsText)
        assertTrue("一 目" !in topCandsText)
        assertTrue("一怒" !in topCandsText)
        assertTrue("一 怒" !in topCandsText)
        assertTrue("一榜" !in topCandsText)
        assertTrue("一 榜" !in topCandsText)
    }

    @Test
    fun testCandidateDisplayAndCommitHasNoSpaces() {
        val dict = MockDict()
        // High scores so they become candidates
        dict.add(Candidate("一", "94", 80000, CandidateType.SINGLE_CHAR), "yi")
        dict.add(Candidate("母", "68", 80000, CandidateType.SINGLE_CHAR), "mu")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("8")

        val cands = engine.getCandidates()
        // Text shouldn't have space, except in DYNAMIC_COMPOSITION where it's retained internally
        // But visible candidates won't contain DYNAMIC_COMPOSITION.
        val visible = engine.getVisibleCandidates()
        assertTrue(visible.none { it.text == "一 母" })
        assertTrue(visible.none { it.text == "一母" })

        // Find if any internal candidate is "一 母", and commit it
        val cand = cands.find { it.text == "一 母" }
        if (cand != null) {
            val committed = engine.commitCandidate(cand)
            // It should NOT replace spaces
            assertEquals("一 母", committed.text)
        }
    }

    @Test
    fun testCandidateOriginLogic() {
        val dict = MockDict()
        dict.add(Candidate("一", "94", 80000, CandidateType.SINGLE_CHAR), "yi")
        dict.add(Candidate("母", "68", 80000, CandidateType.SINGLE_CHAR), "mu")
        dict.add(Candidate("一母", "9468", 90000, CandidateType.NORMAL), "yi mu")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("8")

        val visible = engine.getVisibleCandidates()
        // Should only have EXACT_SINGLE or EXACT_PHRASE origins
        assertTrue(visible.all { it.origin == CandidateOrigin.EXACT_SINGLE || it.origin == CandidateOrigin.EXACT_PHRASE })

        // "一母" is exactly from dict, so it should be visible
        assertTrue(visible.any { it.text == "一母" })

        val internal = engine.getCandidates()
        // Should contain dynamic compositions like "一 母"
        val dynamic = internal.find { it.text == "一 母" }
        assertTrue(dynamic != null)
        assertEquals(CandidateOrigin.DYNAMIC_COMPOSITION, dynamic!!.origin)
    }

    @Test
    fun testVisibleCandidatesGate() {
        val dict = MockDict()
        dict.add(Candidate("不", "28", 1000, CandidateType.SINGLE_CHAR), "bu")
        dict.add(Candidate("版权", "2267826", 10000, CandidateType.NORMAL), "ban quan")
        dict.add(Candidate("ban", "226", 9000, CandidateType.NORMAL), "ban")

        val engine = T9Engine(dict)

        // Input length 1
        engine.inputDigit("2")

        // We should add a candidate "版" to test that prefix single-char doesn't get shown for 2
        dict.add(Candidate("版", "226", 8000, CandidateType.SINGLE_CHAR), "ban")

        val visibleCandsLength1 = engine.getVisibleCandidates()
        // Should not have "版权" (multi-char for length 1)
        assertTrue(visibleCandsLength1.none { it.text == "版权" })
        // Should not have "ban" (pinyin)
        assertTrue(visibleCandsLength1.none { it.text == "ban" })
        // Should not have "2" (numeric fallback)
        assertTrue(visibleCandsLength1.none { it.text == "2" })
        // Should not have "版" because its code length > 1
        assertTrue(visibleCandsLength1.none { it.text == "版" })

        // Ensure preedit is not "ban"
        assertTrue(engine.getPreedit() != "ban")

        val internalCandsLength1 = engine.getCandidates()
        // Internal candidates WILL have the numeric fallback
        assertEquals("2", internalCandsLength1.last().text)

        engine.clear()

        // Input length 2
        engine.inputDigit("2")
        engine.inputDigit("8")

        val visibleCandsLength2 = engine.getVisibleCandidates()
        // Should not have "28" (numeric fallback)
        assertTrue(visibleCandsLength2.none { it.text == "28" })
        // "不" should be visible
        assertTrue(visibleCandsLength2.any { it.text == "不" })
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


    @Test
    fun testShaShiHou() {
        val dict = MockDict()
        // Provide candidate to match "啥时候" -> sha shi hou
        dict.add(Candidate("啥时候", "742744468", 100000, CandidateType.NORMAL), "sha shi hou")
        // These are dynamic sentence generations in reality, let's pretend they have lower score
        dict.add(Candidate("啥是狗", "742744468", 8000, CandidateType.NORMAL), "sha shi gou")
        dict.add(Candidate("啥是构", "742744468", 7000, CandidateType.NORMAL), "sha shi gou")
        dict.add(Candidate("啥是够", "742744468", 6000, CandidateType.NORMAL), "sha shi gou")

        // Add some dummy ones so we fill the top 5
        dict.add(Candidate("沙石后", "742744468", 90000, CandidateType.NORMAL), "sha shi hou")
        dict.add(Candidate("杀师后", "742744468", 80000, CandidateType.NORMAL), "sha shi hou")
        dict.add(Candidate("刹时后", "742744468", 70000, CandidateType.NORMAL), "sha shi hou")
        dict.add(Candidate("傻事后", "742744468", 60000, CandidateType.NORMAL), "sha shi hou")


        val engine = T9Engine(dict)
        engine.inputDigit("7")
        engine.inputDigit("4")
        engine.inputDigit("2")
        engine.inputDigit("7")
        engine.inputDigit("4")
        engine.inputDigit("4")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("8")

        val cands = engine.getCandidates()
        val preedit = engine.getPreedit()

        assertTrue(cands.isNotEmpty())
        assertEquals("啥时候", cands[0].text)
        assertEquals("sha shi hou", preedit)

        val topCandsText = cands.take(5).map { it.text }

        assertTrue("啥是狗" !in topCandsText)
        assertTrue("啥是够" !in topCandsText)
        assertTrue("啥是构" !in topCandsText)
    }
}
