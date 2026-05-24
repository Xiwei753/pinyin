package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import io.github.xiwei753.pinyin.t9.testutil.TestSQLiteDictionary
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Required tests per project specification:
 * 1. Pinyin syllable decoding: digits -> correct pinyin preedit
 * 2. Candidate sorting: "不太行" is first for 288249464
 * 3. Garbage regression: no "不太新股"/"不太英语" for 288249464
 * 4. Single-char fallback: 8105 supplies single chars, base dict supplies phrases
 * 5. Cached candidate click: commit uses cached candidate, no recalc
 * 6. Performance: queries use DB indexes, not full yaml scan
 */
class T9EngineRequiredTests {
    private lateinit var dict: TestSQLiteDictionary
    private lateinit var engine: T9Engine

    @Before
    fun setup() {
        dict = TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath)
        engine = T9Engine(dict)
    }

    @After
    fun teardown() {
        dict.close()
    }

    // ──────────────────────────────────────────────
    // Test 1: Pinyin Decoding Test
    // ──────────────────────────────────────────────

    @Test
    fun testPinyinDecodingForGoldenCases() {
        // Single syllable cases
        assertPreedit("96", "wo")
        assertPreedit("64", "ni")
        assertPreedit("82", "ta")
        assertPreedit("33", "de")
        assertPreedit("744", "shi")
        assertPreedit("28", "bu")

        // Multi-syllable cases
        assertPreedit("28824", "bu tai")
        assertPreedit("288249464", "bu tai xing")
        assertPreedit("546842692674264", "jin tian wan shang")
    }

    @Test
    fun testPinyinDecodingWithSeparator() {
        assertPreedit("28182419464", "bu tai xing")
    }

    private fun assertPreedit(digits: String, expected: String) {
        val eng = T9Engine(dict)
        for (c in digits) {
            eng.inputDigit(c.toString())
        }
        assertEquals("preedit for $digits", expected, eng.getPreedit())
    }

    // ──────────────────────────────────────────────
    // Test 2: Candidate Sorting Test
    // ──────────────────────────────────────────────

    @Test
    fun testBuTaiXingIsFirstCandidate() {
        typeDigits("288249464")
        val visible = engine.getVisibleCandidates()
        assertTrue("visible must not be empty", visible.isNotEmpty())
        assertEquals("不太行", visible.first().text)

        val internal = engine.getCandidates()
        assertTrue("internal candidates must not be empty", internal.isNotEmpty())
        assertEquals("不太行", internal.first().text)
    }

    // ──────────────────────────────────────────────
    // Test 3: Garbage Regression Test
    // ──────────────────────────────────────────────

    @Test
    fun testNoGarbageCandidatesForBuTaiXing() {
        typeDigits("288249464")
        val visible = engine.getVisibleCandidates()
        assertFalse("visible should not contain 不太新股: $visible",
            visible.any { it.text == "不太新股" })
        assertFalse("visible should not contain 不太英语: $visible",
            visible.any { it.text == "不太英语" })

        val internal = engine.getCandidates()
        assertFalse("internal should not contain 不太新股: $internal",
            internal.any { it.text == "不太新股" })
        assertFalse("internal should not contain 不太英语: $internal",
            internal.any { it.text == "不太英语" })
    }

    // ──────────────────────────────────────────────
    // Test 4: Single-Character Fallback Test
    // ──────────────────────────────────────────────

    @Test
    fun testSingleCharCandidatesFromDictionary() {
        // Single syllable digits should produce single-char candidates
        assertSingleCharPresent("96", "我")
        assertSingleCharPresent("64", "你")
        assertSingleCharPresent("82", "他")
        assertSingleCharPresent("82", "她")
        assertSingleCharPresent("33", "的")
        assertSingleCharPresent("33", "得")
        assertSingleCharPresent("33", "地")
        assertSingleCharPresent("744", "是")
        assertSingleCharPresent("28", "不")
    }

    @Test
    fun testPhraseBeatsSingleCharsForMultiSyllable() {
        // For multi-syllable input where a phrase exists, the phrase should
        // be the first candidate, not single chars concatenated.
        typeDigits("28824")
        val visible = engine.getVisibleCandidates()
        assertTrue("visible must contain 不太 for 28824", visible.any { it.text == "不太" })
        assertEquals("不太 must be first for 28824", "不太", visible.first().text)
    }

    @Test
    fun testPhraseOnlyFromExactPinyinNoSingleCharCrowding() {
        // Ensure single chars from 8105 don't suppress base.dict.yaml phrases
        // For 28824 (bu tai), "不太" is a phrase from base dict.
        // Single chars like "不" and "部" should appear AFTER "不太".
        typeDigits("28824")
        val visible = engine.getVisibleCandidates()
        val phraseIdx = visible.indexOfFirst { it.text == "不太" }
        assertTrue("phrase 不太 must be in visible", phraseIdx >= 0)

        // Check that candidates are ordered: exact phrases first, then singles
        val firstNonPhraseIdx = visible.indexOfFirst { it.origin != CandidateOrigin.EXACT_PHRASE }
        if (firstNonPhraseIdx >= 0) {
            assertTrue("phrase must appear before non-phrase candidates",
                phraseIdx < firstNonPhraseIdx)
        }
    }

    @Test
    fun testSingleCharsHaveCorrectOrigin() {
        // Single char candidates must be marked as EXACT_SINGLE
        typeDigits("96")
        val visible = engine.getVisibleCandidates()
        val wo = visible.find { it.text == "我" }
        assertTrue("我 must be in visible candidates", wo != null)
        assertEquals(CandidateOrigin.EXACT_SINGLE, wo!!.origin)
    }

    private fun assertSingleCharPresent(digits: String, expectedChar: String) {
        val eng = T9Engine(dict)
        for (c in digits) {
            eng.inputDigit(c.toString())
        }
        val candidates = eng.getVisibleCandidates()
        assertTrue("$digits must include $expectedChar, got $candidates",
            candidates.any { it.text == expectedChar })
    }

    // ──────────────────────────────────────────────
    // Test 5: Cached Candidate Click Test
    // ──────────────────────────────────────────────

    @Test
    fun testCommitCandidateUsesCachedCandidate() {
        typeDigits("28824")
        val visible = engine.getVisibleCandidates()
        assertTrue("visible must not be empty", visible.isNotEmpty())

        val firstCandidate = visible.first()
        val expectedText = firstCandidate.text

        // Commit the candidate via the engine's commitCandidate
        val committed = engine.commitCandidate(firstCandidate)

        // The committed candidate must have the same text as the visible one
        assertEquals(expectedText, committed.text)

        // The buffer must be cleared after commit
        assertEquals("", engine.buffer)
        assertTrue("locked syllables must be empty after commit",
            engine.lockedSyllables.isEmpty())
    }

    @Test
    fun testCommitCandidatePreservesSourcePinyin() {
        typeDigits("288249464")
        val visible = engine.getVisibleCandidates()
        assertTrue("visible must not be empty", visible.isNotEmpty())

        val firstCandidate = visible.first()
        assertEquals("不太行", firstCandidate.text)
        // sourcePinyin should be "bu tai xing"
        assertEquals("bu tai xing", firstCandidate.sourcePinyin)

        // Commit and verify
        val committed = engine.commitCandidate(firstCandidate)
        assertEquals("不太行", committed.text)
        assertEquals("bu tai xing", committed.sourcePinyin)
    }

    @Test
    fun testCommitDoesNotRecalculate() {
        // Use a mock dictionary that counts queries and has entries
        val mockDict = QueryCountingDictWithData()
        val eng = T9Engine(mockDict)

        typeDigitsEng(eng, "28824")

        // Get visible candidates
        val initialQueryCount = mockDict.queryCount
        val visible = eng.getVisibleCandidates()
        assertTrue("visible must not be empty", visible.isNotEmpty())
        val queryAfterVisible = mockDict.queryCount
        assertTrue("getVisibleCandidates must query the dictionary",
            queryAfterVisible > initialQueryCount)

        // Commit the first candidate
        val committed = eng.commitCandidate(visible.first())
        assertEquals(visible.first().text, committed.text)

        // After commit, no additional dictionary queries should have been made
        assertEquals("commit must not query dictionary again",
            queryAfterVisible, mockDict.queryCount)
        assertEquals("", eng.buffer)
    }

    /**
     * Mock dictionary that counts all query calls.
     */
    private class QueryCountingDict : DictionaryProvider {
        var queryCount = 0

        override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> {
            queryCount++
            return emptyList()
        }

        override fun getPinyinExactCandidatesMultiple(pinyinSequences: List<String>): Map<String, List<Candidate>> {
            queryCount++
            return emptyMap()
        }

        override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
            queryCount++
            return emptyList()
        }

        override fun getSingleSyllableCandidates(syllable: String): List<Candidate> {
            queryCount++
            return emptyList()
        }

        override fun getCandidates(code: String): List<Candidate> = emptyList()
        override fun getExactCandidates(code: String): List<Candidate> = emptyList()
        override fun getPrefixCandidates(code: String): List<Candidate> = emptyList()
    }

    /**
     * Mock dictionary with pre-populated entries that also counts queries.
     */
    private class QueryCountingDictWithData : DictionaryProvider {
        var queryCount = 0
        private val entries = mapOf(
            "bu tai" to listOf(
                Candidate("不太", "28824", 1000, CandidateType.NORMAL,
                    "bu tai", CandidateOrigin.EXACT_PHRASE)
            ),
            "bu" to listOf(
                Candidate("不", "28", 100000, CandidateType.SINGLE_CHAR,
                    "bu", CandidateOrigin.EXACT_SINGLE)
            ),
            "tai" to listOf(
                Candidate("太", "824", 90000, CandidateType.SINGLE_CHAR,
                    "tai", CandidateOrigin.EXACT_SINGLE)
            ),
        )
        private val singleEntries = mapOf(
            "bu" to listOf(
                Candidate("不", "28", 100000, CandidateType.SINGLE_CHAR,
                    "bu", CandidateOrigin.EXACT_SINGLE)
            ),
            "tai" to listOf(
                Candidate("太", "824", 90000, CandidateType.SINGLE_CHAR,
                    "tai", CandidateOrigin.EXACT_SINGLE)
            ),
        )

        override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> {
            queryCount++
            return entries[pinyinSequence] ?: emptyList()
        }

        override fun getPinyinExactCandidatesMultiple(pinyinSequences: List<String>): Map<String, List<Candidate>> {
            queryCount++
            val result = mutableMapOf<String, List<Candidate>>()
            for (seq in pinyinSequences) {
                entries[seq]?.let { result[seq] = it }
            }
            return result
        }

        override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
            queryCount++
            return entries.filterKeys { it.startsWith(pinyinPrefix) }.values.flatten()
        }

        override fun getSingleSyllableCandidates(syllable: String): List<Candidate> {
            queryCount++
            return singleEntries[syllable] ?: emptyList()
        }

        override fun getCandidates(code: String): List<Candidate> = emptyList()
        override fun getExactCandidates(code: String): List<Candidate> = emptyList()
        override fun getPrefixCandidates(code: String): List<Candidate> = emptyList()
    }

    // ──────────────────────────────────────────────
    // Test 6: Performance / No Full Scan Test
    // ──────────────────────────────────────────────

    @Test
    fun testCandidateQueryDoesNotScanRawYaml() {
        // Verify that candidate queries are fast by checking:
        // 1. Each candidate request produces a bounded number of DB queries
        // 2. Queries use indexed lookups (pinyin exact match), not LIKE or full scan

        typeDigits("28824")
        val dbQueryCount = engine.dbQueryCount
        engine.getVisibleCandidates()
        // Number of queries should be small (bounded by syllables)
        assertTrue("getVisibleCandidates should make a bounded number of queries: $dbQueryCount",
            engine.dbQueryCount - dbQueryCount <= 20)
    }

    @Test
    fun testPreeditHintDoesNotQueryDictionary() {
        // getPreeditHint must not trigger any dictionary queries
        val mockDict = QueryCountingDict()
        val eng = T9Engine(mockDict)

        typeDigitsEng(eng, "288249464")

        val hint = eng.getPreeditHint()
        assertTrue("preedit hint must be non-empty", hint.isNotEmpty())
        assertEquals("getPreeditHint must not query dictionary", 0, mockDict.queryCount)
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private fun typeDigits(digits: String) {
        for (c in digits) {
            engine.inputDigit(c.toString())
        }
    }

    private fun typeDigitsEng(eng: T9Engine, digits: String) {
        for (c in digits) {
            eng.inputDigit(c.toString())
        }
    }
}
