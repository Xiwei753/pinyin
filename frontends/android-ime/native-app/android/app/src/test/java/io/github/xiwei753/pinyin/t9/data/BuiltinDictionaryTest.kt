package io.github.xiwei753.pinyin.t9.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinDictionaryTest {

    @Test
    fun testParseLinesAndSorting() {
        val lines = listOf(
            "你好\tni hao\t1000",
            "妮好\tni hao\t100000", // higher score
            "输入法\tshu ru fa\t90000",
            "invalid_line_no_tabs",
            "text\tpinyin\tnot_a_number"
        )
        val dictionary = BuiltinDictionary(lines)

        val candidates64426 = dictionary.getPrefixCandidates("64426")
        assertEquals(2, candidates64426.size)
        // Test sorting: higher score should be first
        assertEquals("妮好", candidates64426[0].text)
        assertEquals("你好", candidates64426[1].text)
        assertTrue(candidates64426[0].score > candidates64426[1].score)

        val candidates7487832 = dictionary.getPrefixCandidates("7487832")
        assertEquals(1, candidates7487832.size)
        assertEquals("输入法", candidates7487832[0].text)

        val exactCandidates7487832 = dictionary.getExactCandidates("7487832")
        assertEquals(1, exactCandidates7487832.size)
        assertEquals("输入法", exactCandidates7487832[0].text)

        val exactCandidates748 = dictionary.getExactCandidates("748") // incomplete code
        assertTrue(exactCandidates748.isEmpty())

        val prefixCandidates748 = dictionary.getPrefixCandidates("748") // incomplete code
        assertEquals(1, prefixCandidates748.size)

        val candidates746946 = dictionary.getPrefixCandidates("746946") // code for "pinyin"
        assertEquals(1, candidates746946.size)
        assertEquals("text", candidates746946[0].text)
        assertEquals(0, candidates746946[0].score) // fallback score
    }

    @Test
    fun testPrefixMatching() {
        val lines = listOf(
            "你好\tni hao\t100000",
            "你号\tni hao\t50000",
            "输入法\tshu ru fa\t90000",
            "拼音\tpin yin\t80000"
        )
        val dictionary = BuiltinDictionary(lines)

        // 64 is prefix of 64426 (ni hao)
        val prefixCandidates = dictionary.getPrefixCandidates("64")
        assertEquals(2, prefixCandidates.size)
        assertEquals("你好", prefixCandidates[0].text)
        assertEquals("你号", prefixCandidates[1].text)

        // Empty prefix should return empty list
        val emptyCandidates = dictionary.getPrefixCandidates("")
        assertTrue(emptyCandidates.isEmpty())
    }

    @Test
    fun testFallback() {
        val dictionary = BuiltinDictionary(emptyList<String>())
        val candidates64426 = dictionary.getPrefixCandidates("64426")
        assertEquals(1, candidates64426.size)
        assertEquals("你好", candidates64426[0].text)

        val exactCandidates64426 = dictionary.getExactCandidates("64426")
        assertEquals(1, exactCandidates64426.size)
        assertEquals("你好", exactCandidates64426[0].text)

        val candidates7487832 = dictionary.getPrefixCandidates("7487832")
        assertEquals(1, candidates7487832.size)
        assertEquals("输入法", candidates7487832[0].text)
    }

    @Test
    fun testLargeDictionaryPrefixQueryPerformance() {
        // Generate a large dictionary dataset
        val lines = mutableListOf<String>()
        for (i in 0 until 50000) {
            // Using different pinyins and scores
            val text = "词语$i"
            val pinyin = if (i % 2 == 0) "ce shi" else "an zhuo" // 23 744 vs 26 9486
            val score = 50000 - i
            lines.add("$text\t$pinyin\t$score")
        }

        val dictionary = BuiltinDictionary(lines)

        // Measure query time for a known prefix
        val startTime = System.currentTimeMillis()
        val candidates = dictionary.getPrefixCandidates("26") // prefix for 'an zhuo'
        val endTime = System.currentTimeMillis()

        // Assert that the candidate limit logic is working and fast
        assertTrue("Query took too long: ${endTime - startTime}ms", (endTime - startTime) < 100)
        assertEquals("Should be limited to 100 candidates", 100, candidates.size)

        val exactCandidates = dictionary.getExactCandidates("26")
        assertTrue(exactCandidates.isEmpty())

        val exactFullCandidates = dictionary.getExactCandidates("269486")
        assertEquals(100, exactFullCandidates.size)
    }
}
