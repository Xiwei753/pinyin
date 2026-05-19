package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.testutil.TestSQLiteDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.xiwei753.pinyin.t9.testutil.TestPaths

class T9EnginePhraseAndDynamicTest {
    private fun realEngine(): T9Engine {
        return T9Engine(TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath))
    }

    private fun typeDigits(engine: T9Engine, digits: String) {
        digits.forEach { ch ->
            if (ch != ' ') {
                engine.inputDigit(ch.toString())
            }
        }
    }

    @Test
    fun duiBaContainsDuiBa() {
        val engine = realEngine()
        typeDigits(engine, "38422")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertTrue("dui ba should contain '对吧', actual=$visibleTexts", visibleTexts.contains("对吧"))
    }

    @Test
    fun yeShiContainsYeShi() {
        val engine = realEngine()
        typeDigits(engine, "93744")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertTrue("ye shi should contain '也是', actual=$visibleTexts", visibleTexts.contains("也是"))
    }

    @Test
    fun meiShiContainsMeiShi() {
        val engine = realEngine()
        typeDigits(engine, "634744")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertTrue("mei shi should contain '没事', actual=$visibleTexts", visibleTexts.contains("没事"))
    }

    @Test
    fun buYongContainsBuYong() {
        val engine = realEngine()
        typeDigits(engine, "289664")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertTrue("bu yong should contain '不用', actual=$visibleTexts", visibleTexts.contains("不用"))
    }

    @Test
    fun zenMeBanContainsZenMeBan() {
        val engine = realEngine()
        typeDigits(engine, "93663226")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertTrue("zen me ban should contain '怎么办', actual=$visibleTexts", visibleTexts.contains("怎么办"))
    }

    @Test
    fun woJueDeContainsWoJueDe() {
        val engine = realEngine()
        typeDigits(engine, "9658333")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertTrue("wo jue de should contain '我觉得', actual=$visibleTexts", visibleTexts.contains("我觉得"))
    }

    @Test
    fun buTaiXingFirstCandidateStillBuTaiXing() {
        val engine = realEngine()
        typeDigits(engine, "288249464")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertEquals("第一候选仍是不太行", "不太行", visibleTexts.firstOrNull())
    }

    @Test
    fun buTaiXingNoGarbageCandidates() {
        val engine = realEngine()
        typeDigits(engine, "288249464")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertFalse("不应出现不太新股", visibleTexts.contains("不太新股"))
        assertFalse("不应出现不太英语", visibleTexts.contains("不太英语"))
    }

    @Test
    fun singleCharInputsStable() {
        assertCase("96", "wo", setOf("我"))
        assertCase("64", "ni", setOf("你"))
        assertCase("82", "ta", setOf("他", "她"))
        assertCase("33", "de", setOf("的", "得", "地"))
        assertCase("744", "shi", setOf("是"))
        assertCase("28", "bu", setOf("不"))
    }

    @Test
    fun multiCharPhrasePriority() {
        val engine = realEngine()
        typeDigits(engine, "28824")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertTrue("28824 should contain '不太'", visibleTexts.contains("不太"))
    }

    @Test
    fun jinTianWanShangContainsTodayEvening() {
        val engine = realEngine()
        typeDigits(engine, "546842692674264")
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertTrue("jin tian wan shang should contain '今天晚上'", visibleTexts.contains("今天晚上"))
    }

    private fun assertCase(digits: String, expectedPreedit: String, expectedCandidates: Set<String>) {
        val engine = realEngine()
        typeDigits(engine, digits)
        assertEquals("preedit for $digits", expectedPreedit, engine.getPreedit())
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertTrue(
            "visible candidates for $digits should contain one of $expectedCandidates, actual=$visibleTexts",
            visibleTexts.any { it in expectedCandidates }
        )
    }
}