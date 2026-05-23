package io.github.xiwei753.pinyin.t9.data

import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import io.github.xiwei753.pinyin.t9.testutil.TestSQLiteDictionary
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class DictionaryBatchTest {
    private lateinit var dictionary: TestSQLiteDictionary

    @Before
    fun setup() {
        dictionary = TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath)
    }

    @After
    fun teardown() {
        dictionary.close()
    }

    @Test
    fun testGetPinyinExactCandidatesMultiple() {
        val pinyins = listOf("wo", "ni", "ta", "shi", "de", "bu", "invalidpinyin")
        val result = dictionary.getPinyinExactCandidatesMultiple(pinyins)

        assertEquals(pinyins.size - 1, result.size) // "invalidpinyin" shouldn't return anything

        assertTrue(result["wo"]!!.any { it.text == "我" })
        assertTrue(result["ni"]!!.any { it.text == "你" })
        assertTrue(result["ta"]!!.any { it.text == "他" } || result["ta"]!!.any { it.text == "她" })
        assertTrue(result["shi"]!!.any { it.text == "是" })
        assertTrue(result["de"]!!.any { it.text == "的" })
        assertTrue(result["bu"]!!.any { it.text == "不" })

        // Ensure the fields are correct (origin, score)
        val woCand = result["wo"]!!.first { it.text == "我" }
        assertTrue(woCand.score > 0)

        // Empty query
        val emptyResult = dictionary.getPinyinExactCandidatesMultiple(emptyList())
        assertTrue(emptyResult.isEmpty())
    }
}
