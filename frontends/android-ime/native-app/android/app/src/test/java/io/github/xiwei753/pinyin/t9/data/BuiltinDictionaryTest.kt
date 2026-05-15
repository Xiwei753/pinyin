package io.github.xiwei753.pinyin.t9.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinDictionaryTest {

    @Test
    fun testParseLines() {
        val lines = listOf(
            "你好\tni hao\t100000",
            "妮好\tni hao\t1000",
            "输入法\tshu ru fa\t90000",
            "invalid_line_no_tabs",
            "text\tpinyin\tnot_a_number"
        )
        val dictionary = BuiltinDictionary(lines)

        val candidates64426 = dictionary.getCandidates("64426")
        assertEquals(2, candidates64426.size)
        assertEquals("你好", candidates64426[0].text)
        assertEquals("妮好", candidates64426[1].text)
        assertTrue(candidates64426[0].score > candidates64426[1].score)

        val candidates7487832 = dictionary.getCandidates("7487832")
        assertEquals(1, candidates7487832.size)
        assertEquals("输入法", candidates7487832[0].text)

        val candidates746946 = dictionary.getCandidates("746946") // code for "pinyin"
        assertEquals(1, candidates746946.size)
        assertEquals("text", candidates746946[0].text)
        assertEquals(0, candidates746946[0].score) // fallback score
    }

    @Test
    fun testFallback() {
        val dictionary = BuiltinDictionary(emptyList<String>())
        val candidates64426 = dictionary.getCandidates("64426")
        assertEquals(1, candidates64426.size)
        assertEquals("你好", candidates64426[0].text)

        val candidates7487832 = dictionary.getCandidates("7487832")
        assertEquals(1, candidates7487832.size)
        assertEquals("输入法", candidates7487832[0].text)
    }
}
