package io.github.xiwei753.pinyin.t9.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinDictionaryTest {

    @Test
    fun testParseLines() {
        val lines = listOf(
            "64426\t你好\t100000",
            "64426\t妮好\t1000",
            "748732\t输入法\t90000",
            "invalid_line_no_tabs",
            "123\ttext\tnot_a_number"
        )
        val dictionary = BuiltinDictionary(lines)

        val candidates64426 = dictionary.getCandidates("64426")
        assertEquals(2, candidates64426.size)
        assertEquals("你好", candidates64426[0].text)
        assertEquals("妮好", candidates64426[1].text)
        assertTrue(candidates64426[0].score > candidates64426[1].score)

        val candidates748732 = dictionary.getCandidates("748732")
        assertEquals(1, candidates748732.size)
        assertEquals("输入法", candidates748732[0].text)

        val candidates123 = dictionary.getCandidates("123")
        assertEquals(1, candidates123.size)
        assertEquals("text", candidates123[0].text)
        assertEquals(0, candidates123[0].score) // fallback score
    }

    @Test
    fun testFallback() {
        val dictionary = BuiltinDictionary(emptyList<String>())
        val candidates64426 = dictionary.getCandidates("64426")
        assertEquals(1, candidates64426.size)
        assertEquals("你好", candidates64426[0].text)

        val candidates748732 = dictionary.getCandidates("748732")
        assertEquals(1, candidates748732.size)
        assertEquals("输入法", candidates748732[0].text)
    }
}
