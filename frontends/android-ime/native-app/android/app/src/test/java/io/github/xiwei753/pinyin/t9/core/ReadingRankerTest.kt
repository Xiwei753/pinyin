package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingRankerTest {

    @Test
    fun testRankPrioritizesCompleteFullSpan() {
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("zhuang"),
                pinyinString = "zhuang",
                isComplete = true,
                rawDigits = "948264",
                score = 10060,
                segmentDigits = listOf("948264"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhuan", "g"),
                pinyinString = "zhuan g",
                isComplete = false,
                rawDigits = "948264",
                score = -13650,
                segmentDigits = listOf("94826", "4"),
            ),
        )

        val ranked = ReadingRanker.rank("948264", comps, 0)
        assertEquals(2, ranked.size)

        // zhuang (complete full span) should come first
        assertEquals("zhuang", ranked[0].syllable)
        assertEquals(ReadingCategory.COMPLETE_FULL_SPAN, ranked[0].category)

        // zhuan (multi-syllable derived) should come second
        assertEquals("zhuan", ranked[1].syllable)
        assertEquals(ReadingCategory.MULTI_SYLLABLE_DERIVED, ranked[1].category)
    }

    @Test
    fun testRankOrdersByLengthDescWithinCategory() {
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("zhuan", "g"),
                pinyinString = "zhuan g",
                isComplete = false,
                rawDigits = "948264",
                score = -13650,
                segmentDigits = listOf("94826", "4"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhua", "ng"),
                pinyinString = "zhua ng",
                isComplete = true,
                rawDigits = "948264",
                score = 9458,
                segmentDigits = listOf("9482", "64"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhu", "ang"),
                pinyinString = "zhu ang",
                isComplete = true,
                rawDigits = "948264",
                score = 9457,
                segmentDigits = listOf("948", "264"),
            ),
        )

        val ranked = ReadingRanker.rank("948264", comps, 0)
        // All are MULTI_SYLLABLE_DERIVED
        // Sorted by length desc: zhuan(5) > zhua(4) > zhu(3)
        val syllables = ranked.map { it.syllable }
        assertTrue("zhuan should come before zhua", syllables.indexOf("zhuan") < syllables.indexOf("zhua"))
        assertTrue("zhua should come before zhu", syllables.indexOf("zhua") < syllables.indexOf("zhu"))
    }

    @Test
    fun testRankDoesNotDuplicateSyllables() {
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("zhuang"),
                pinyinString = "zhuang",
                isComplete = true,
                rawDigits = "948264",
                score = 10060,
                segmentDigits = listOf("948264"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhuang"),
                pinyinString = "zhuang",
                isComplete = true,
                rawDigits = "948264",
                score = 10060,
                segmentDigits = listOf("948264"),
            ),
        )

        val ranked = ReadingRanker.rank("948264", comps, 0)
        assertEquals(1, ranked.count { it.syllable == "zhuang" })
    }

    @Test
    fun testRankKeepsHigherScoreForDuplicateSyllable() {
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("zhuang"),
                pinyinString = "zhuang",
                isComplete = true,
                rawDigits = "948264",
                score = 10060,
                segmentDigits = listOf("948264"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhuang"),
                pinyinString = "zhuang",
                isComplete = true,
                rawDigits = "948264",
                score = 5000,
                segmentDigits = listOf("948264"),
            ),
        )

        val ranked = ReadingRanker.rank("948264", comps, 0)
        val zhuang = ranked.find { it.syllable == "zhuang" }
        assertTrue(zhuang != null)
        assertEquals(10060, zhuang?.score)
    }

    @Test
    fun testRankReturnsEmptyForEmptyBuffer() {
        val ranked = ReadingRanker.rank("", emptyList(), 0)
        assertTrue(ranked.isEmpty())
    }

    @Test
    fun testRankReturnsPrefixFallbackWhenNoCompositions() {
        // Empty compositions with a valid buffer triggers prefix fallback
        val ranked = ReadingRanker.rank("948264", emptyList(), 0)
        // Should return prefix-based readings like "zhuang", "zhuan", etc.
        assertTrue("prefix fallback should produce readings", ranked.isNotEmpty())
        // All should be PREFIX_FALLBACK category
        assertTrue(ranked.all { it.category == ReadingCategory.PREFIX_FALLBACK })
    }

    @Test
    fun testRankWithNextIndex() {
        // Simulate lockedSyllables = ["zhuan"], nextIndex = 1
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("zhuan", "g"),
                pinyinString = "zhuan g",
                isComplete = false,
                rawDigits = "948264",
                score = -13650,
                segmentDigits = listOf("94826", "4"),
            ),
        )

        val ranked = ReadingRanker.rank("948264", comps, 1)
        assertEquals(1, ranked.size)
        assertEquals("g", ranked[0].syllable)
        assertEquals(ReadingCategory.MULTI_SYLLABLE_DERIVED, ranked[0].category)
    }

    @Test
    fun testFull948264ReadingsOrder() {
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("zhuang"),
                pinyinString = "zhuang",
                isComplete = true,
                rawDigits = "948264",
                score = 10060,
                segmentDigits = listOf("948264"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhu", "ang"),
                pinyinString = "zhu ang",
                isComplete = true,
                rawDigits = "948264",
                score = 9457,
                segmentDigits = listOf("948", "264"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhua", "ng"),
                pinyinString = "zhua ng",
                isComplete = true,
                rawDigits = "948264",
                score = 9458,
                segmentDigits = listOf("9482", "64"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhuan", "g"),
                pinyinString = "zhuan g",
                isComplete = false,
                rawDigits = "948264",
                score = -13650,
                segmentDigits = listOf("94826", "4"),
            ),
        )

        val ranked = ReadingRanker.rank("948264", comps, 0)

        assertTrue("zhuang must be in readings", ranked.any { it.syllable == "zhuang" })
        assertTrue("zhuan must be in readings", ranked.any { it.syllable == "zhuan" })
        assertTrue("zhua must be in readings", ranked.any { it.syllable == "zhua" })
        assertTrue("zhu must be in readings", ranked.any { it.syllable == "zhu" })

        val zhuangIdx = ranked.indexOfFirst { it.syllable == "zhuang" }
        val zhuanIdx = ranked.indexOfFirst { it.syllable == "zhuan" }
        val zhuaIdx = ranked.indexOfFirst { it.syllable == "zhua" }
        val zhuIdx = ranked.indexOfFirst { it.syllable == "zhu" }

        assertTrue("zhuang should come before zhuan", zhuangIdx < zhuanIdx)
        assertTrue("zhuan should come before zhua", zhuanIdx < zhuaIdx)
        assertTrue("zhua should come before zhu", zhuaIdx < zhuIdx)
    }

    @Test
    fun testCompleteFullSpanPrioritizedOverMultiSyllable() {
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("meng"),
                pinyinString = "meng",
                isComplete = true,
                rawDigits = "6364",
                score = 9840,
                segmentDigits = listOf("6364"),
            ),
            PinyinComposition(
                pinyinList = listOf("men", "g"),
                pinyinString = "men g",
                isComplete = false,
                rawDigits = "6364",
                score = -10000,
                segmentDigits = listOf("636", "4"),
            ),
        )

        val ranked = ReadingRanker.rank("6364", comps, 0)

        assertTrue(ranked.size >= 2)
        assertEquals("meng", ranked[0].syllable)
        assertEquals(ReadingCategory.COMPLETE_FULL_SPAN, ranked[0].category)
        assertEquals("men", ranked[1].syllable)
        assertEquals(ReadingCategory.MULTI_SYLLABLE_DERIVED, ranked[1].category)
    }

    @Test
    fun testFilterNgFrom64Readings() {
        // 64 can produce both "ni" and "ng" - "ng" must be filtered
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("ni"),
                pinyinString = "ni",
                isComplete = true,
                rawDigits = "64",
                score = 10000,
                segmentDigits = listOf("64"),
            ),
            PinyinComposition(
                pinyinList = listOf("ng"),
                pinyinString = "ng",
                isComplete = true,
                rawDigits = "64",
                score = 5000,
                segmentDigits = listOf("64"),
            ),
        )

        val ranked = ReadingRanker.rank("64", comps, 0)
        assertTrue("ng must be filtered out", ranked.none { it.syllable == "ng" })
        assertTrue("ni must be present", ranked.any { it.syllable == "ni" })
    }

    @Test
    fun testFilterOFailsFrom64() {
        // If "o" somehow appears as a reading, it must be filtered
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("ni"),
                pinyinString = "ni",
                isComplete = true,
                rawDigits = "64",
                score = 10000,
                segmentDigits = listOf("64"),
            ),
            PinyinComposition(
                pinyinList = listOf("o", "g"),
                pinyinString = "o g",
                isComplete = false,
                rawDigits = "64",
                score = 1000,
                segmentDigits = listOf("6", "4"),
            ),
        )

        val ranked = ReadingRanker.rank("64", comps, 0)
        assertTrue("o must be filtered out", ranked.none { it.syllable == "o" })
        assertTrue("ni must still be present", ranked.any { it.syllable == "ni" })
        // Prefer ni over anything else
        assertTrue(ranked.isNotEmpty())
        assertEquals("ni", ranked[0].syllable)
    }

    @Test
    fun testNormalReadingsNotFiltered() {
        // 948264 must still produce zhuang/zhuan/zhua/zhu
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("zhuang"),
                pinyinString = "zhuang",
                isComplete = true,
                rawDigits = "948264",
                score = 10060,
                segmentDigits = listOf("948264"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhu", "ang"),
                pinyinString = "zhu ang",
                isComplete = true,
                rawDigits = "948264",
                score = 9457,
                segmentDigits = listOf("948", "264"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhua", "ng"),
                pinyinString = "zhua ng",
                isComplete = true,
                rawDigits = "948264",
                score = 9458,
                segmentDigits = listOf("9482", "64"),
            ),
            PinyinComposition(
                pinyinList = listOf("zhuan", "g"),
                pinyinString = "zhuan g",
                isComplete = false,
                rawDigits = "948264",
                score = -13650,
                segmentDigits = listOf("94826", "4"),
            ),
        )

        val ranked = ReadingRanker.rank("948264", comps, 0)
        assertTrue("zhuang must be in readings", ranked.any { it.syllable == "zhuang" })
        assertTrue("zhuan must be in readings", ranked.any { it.syllable == "zhuan" })
        assertTrue("zhua must be in readings", ranked.any { it.syllable == "zhua" })
        assertTrue("zhu must be in readings", ranked.any { it.syllable == "zhu" })
    }

    @Test
    fun test6364ReadingsStillContainsMengNengMen() {
        // 6364 readings must not lose meng/neng/men
        val comps = listOf(
            PinyinComposition(
                pinyinList = listOf("meng"),
                pinyinString = "meng",
                isComplete = true,
                rawDigits = "6364",
                score = 9840,
                segmentDigits = listOf("6364"),
            ),
            PinyinComposition(
                pinyinList = listOf("neng"),
                pinyinString = "neng",
                isComplete = true,
                rawDigits = "6364",
                score = 9720,
                segmentDigits = listOf("6364"),
            ),
            PinyinComposition(
                pinyinList = listOf("men", "g"),
                pinyinString = "men g",
                isComplete = false,
                rawDigits = "6364",
                score = -10000,
                segmentDigits = listOf("636", "4"),
            ),
        )

        val ranked = ReadingRanker.rank("6364", comps, 0)
        assertTrue("6364 readings must contain meng", ranked.any { it.syllable == "meng" })
        assertTrue("6364 readings must contain neng", ranked.any { it.syllable == "neng" })
        assertTrue("6364 readings must contain men", ranked.any { it.syllable == "men" })
    }
}
