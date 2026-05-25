package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.testutil.TestSQLiteDictionary
import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class T9EngineNihaoTest {
    private lateinit var engine: T9Engine

    @Before
    fun setup() {
        val dictionary = TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath)
        engine = T9Engine(dictionary)
    }

    @Test
    fun testNihao() {
        engine.inputDigit("6")
        engine.inputDigit("4")
        engine.inputDigit("4")
        engine.inputDigit("2")
        println("--- Input: ${engine.buffer} ---")
        val comps = engine.getValidCompositions()
        comps.forEach { println("Comp: ${it.pinyinString}, isComplete: ${it.isComplete}") }

        val cands = engine.getVisibleCandidates(30)
        cands.take(10).forEach { println(it) }
    }
}
