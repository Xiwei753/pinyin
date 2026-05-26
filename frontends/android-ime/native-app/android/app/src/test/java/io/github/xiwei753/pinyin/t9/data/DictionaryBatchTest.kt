package io.github.xiwei753.pinyin.t9.data

import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import io.github.xiwei753.pinyin.t9.testutil.TestSQLiteDictionary
import org.junit.Test
import org.junit.Assert.assertTrue

class DictionaryBatchTest {
    @Test
    fun testBatchQuery() {
        val dict = TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath)
        val queries = listOf("ni", "ni hao", "hao")
        val results = dict.getPinyinExactCandidatesMultiple(queries)
        println("Results for 'ni': " + (results["ni"]?.size ?: 0))
        println("Results for 'hao': " + (results["hao"]?.size ?: 0))
        println("Results for 'ni hao': " + (results["ni hao"]?.size ?: 0))
    }
}
