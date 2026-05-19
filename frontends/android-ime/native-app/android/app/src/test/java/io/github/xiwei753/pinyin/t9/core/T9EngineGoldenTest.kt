package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.testutil.TestSQLiteDictionary
import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileInputStream

class T9EngineGoldenTest {

    private fun loadRealEngine(): T9Engine {
        val dict = TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath)
        return T9Engine(dict)
    }

    @Test
    fun test96WoSingleChar() {
        val engine = loadRealEngine()
        engine.inputDigit("9")
        engine.inputDigit("6")
        assertEquals("wo", engine.getPreedit())
        assertTrue(engine.getVisibleCandidates().any { it.text == "我" })
    }

    @Test
    fun test64NiSingleChar() {
        val engine = loadRealEngine()
        engine.inputDigit("6")
        engine.inputDigit("4")
        assertEquals("ni", engine.getPreedit())
        assertTrue(engine.getVisibleCandidates().any { it.text == "你" })
    }

    @Test
    fun test82TaSingleChar() {
        val engine = loadRealEngine()
        engine.inputDigit("8")
        engine.inputDigit("2")
        assertEquals("ta", engine.getPreedit())
        assertTrue(engine.getVisibleCandidates().any { it.text == "他" })
        assertTrue(engine.getVisibleCandidates().any { it.text == "她" })
    }

    @Test
    fun test33DeSingleChar() {
        val engine = loadRealEngine()
        engine.inputDigit("3")
        engine.inputDigit("3")
        assertEquals("de", engine.getPreedit())
        val visible = engine.getVisibleCandidates()
        assertTrue(visible.any { it.text == "的" })
        assertTrue(visible.any { it.text == "得" })
        assertTrue(visible.any { it.text == "地" })
    }

    @Test
    fun test744ShiSingleChar() {
        val engine = loadRealEngine()
        engine.inputDigit("7")
        engine.inputDigit("4")
        engine.inputDigit("4")
        assertEquals("shi", engine.getPreedit())
        assertTrue(engine.getVisibleCandidates().any { it.text == "是" })
    }

    @Test
    fun test28BuSingleChar() {
        val engine = loadRealEngine()
        engine.inputDigit("2")
        engine.inputDigit("8")
        assertEquals("bu", engine.getPreedit())
        assertTrue(engine.getVisibleCandidates().any { it.text == "不" })
    }

    @Test
    fun test28824BuTaiPhrase() {
        val engine = loadRealEngine()
        engine.inputDigit("2")
        engine.inputDigit("8")
        engine.inputDigit("8")
        engine.inputDigit("2")
        engine.inputDigit("4")
        assertEquals("bu tai", engine.getPreedit())
        assertTrue(engine.getVisibleCandidates().any { it.text == "不太" })
    }

    @Test
    fun test288249464BuTaiXingFirstCandidate() {
        val engine = loadRealEngine()
        engine.inputDigit("2")
        engine.inputDigit("8")
        engine.inputDigit("8")
        engine.inputDigit("2")
        engine.inputDigit("4")
        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("4")
        assertEquals("bu tai xing", engine.getPreedit())
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertEquals("不太行", visibleTexts.firstOrNull())
        assertFalse("不应出现错误长词候选: $visibleTexts", visibleTexts.contains("不太新股"))
        assertFalse("不应出现错误长词候选: $visibleTexts", visibleTexts.contains("不太英语"))
    }

    @Test
    fun testJinTianWanShangPhrase() {
        val engine = loadRealEngine()
        val digits = "546842692674264"
        for (d in digits) {
            engine.inputDigit(d.toString())
        }
        assertEquals("jin tian wan shang", engine.getPreedit())
        val visible = engine.getVisibleCandidates()
        assertTrue(visible.any { it.text == "今天晚上" })
    }

    @Test
    fun testLoadedWordCountGreaterThan30000() {
        val dict = TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath)
        assertTrue(dict.getLoadedWordCount() > 30000)
        assertTrue(dict.getPinyinExactCandidates("wo").isNotEmpty())
    }

    @Test
    fun test6364ReadingsWithRealDict() {
        val engine = loadRealEngine()
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val readings = engine.readings
        assertTrue("readings must contain meng", readings.any { it == "meng" })
        assertTrue("readings must contain neng", readings.any { it == "neng" })
        assertTrue("readings must contain men", readings.any { it == "men" })
    }

    @Test
    fun test6364SelectNengWithRealDict() {
        val engine = loadRealEngine()
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val success = engine.setActiveReading("neng")
        assertTrue("setActiveReading(neng) must succeed", success)
        assertEquals("neng", engine.getPreedit())

        val visible = engine.getVisibleCandidates()
        assertTrue("neng candidates must contain 能", visible.any { it.text == "能" })
        // 能 should be near the top
        val firstText = visible.first().text
        assertTrue("neng first candidate should be 能, was $firstText", firstText == "能")
    }

    @Test
    fun test6364SelectMengWithRealDict() {
        val engine = loadRealEngine()
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val success = engine.setActiveReading("meng")
        assertTrue("setActiveReading(meng) must succeed", success)
        assertEquals("meng", engine.getPreedit())

        val visible = engine.getVisibleCandidates()
        assertTrue("meng candidates must contain 梦", visible.any { it.text == "梦" })
        assertTrue("meng candidates must contain 蒙", visible.any { it.text == "蒙" })
        assertTrue("meng candidates must contain 萌", visible.any { it.text == "萌" })
    }

    @Test
    fun testSeparatorBuTaiXing() {
        val engine = loadRealEngine()
        val sepDigits = "28182419464"
        for (d in sepDigits) {
            engine.inputDigit(d.toString())
        }
        assertEquals("bu tai xing", engine.getPreedit())
        val visible = engine.getVisibleCandidates()
        assertTrue(visible.any { it.text == "不太行" })
    }
}
