package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileInputStream
import io.github.xiwei753.pinyin.t9.testutil.TestPaths

class T9EngineGoldenRealDictTest {
    private fun realEngine(): T9Engine {
        return T9Engine(BuiltinDictionary(FileInputStream(TestPaths.assetDictionary())))
    }

    private fun typeDigits(engine: T9Engine, digits: String) {
        digits.forEach { ch ->
            if (ch != ' ') {
                engine.inputDigit(ch.toString())
            }
        }
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
        assertTrue(
            "visible candidates for $digits must be exact real dictionary candidates, actual=${engine.getVisibleCandidates()}",
            engine.getVisibleCandidates().all { it.origin == CandidateOrigin.EXACT_SINGLE || it.origin == CandidateOrigin.EXACT_PHRASE }
        )
    }

    @Test
    fun goldenSingleSyllableCasesUseRealDictionary() {
        assertCase("96", "wo", setOf("我"))
        assertCase("64", "ni", setOf("你"))
        assertCase("82", "ta", setOf("他", "她"))
        assertCase("33", "de", setOf("的", "得", "地"))
        assertCase("744", "shi", setOf("是"))
        assertCase("28", "bu", setOf("不"))
    }

    @Test
    fun goldenPhraseCasesUseRealDictionary() {
        assertCase("28824", "bu tai", setOf("不太"))
        assertCase("546842692674264", "jin tian wan shang", setOf("今天晚上"))
    }

    @Test
    fun buTaiXingIsFirstRealDictionaryCandidate() {
        val engine = realEngine()
        typeDigits(engine, "288249464")

        assertEquals("bu tai xing", engine.getPreedit())
        val visibleTexts = engine.getVisibleCandidates().map { it.text }
        assertEquals("不太行", visibleTexts.firstOrNull())
        assertFalse("不应出现错误长词候选: $visibleTexts", visibleTexts.contains("不太新股"))
        assertFalse("不应出现错误长词候选: $visibleTexts", visibleTexts.contains("不太英语"))
    }

    @Test
    fun separatorCaseUsesRealDictionary() {
        assertCase("28182419464", "bu tai xing", setOf("不太行"))
    }
}
