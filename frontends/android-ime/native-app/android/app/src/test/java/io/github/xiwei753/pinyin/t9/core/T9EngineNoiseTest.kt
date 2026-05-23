package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import io.github.xiwei753.pinyin.t9.testutil.TestSQLiteDictionary
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class T9EngineNoiseTest {
    private lateinit var dictionary: TestSQLiteDictionary
    private lateinit var engine: T9Engine

    @Before
    fun setup() {
        dictionary = TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath)
        engine = T9Engine(dictionary)
    }

    @After
    fun teardown() {
        dictionary.close()
    }

    @Test
    fun testNoiseSuppressionFor288249464() {
        // "bu tai xing"
        val digits = "288249464"
        for (c in digits) {
            engine.inputDigit(c.toString())
        }

        val candidates = engine.getCandidates()
        assertTrue(candidates.isNotEmpty())

        // Output should not contain "不太新股" or "不太英语"
        val allTexts = candidates.map { it.text }
        assertTrue("Should not contain '不太新股'", !allTexts.contains("不太新股"))
        assertTrue("Should not contain '不太英语'", !allTexts.contains("不太英语"))
        assertEquals("不太行", allTexts.first())
    }
}
