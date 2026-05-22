package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.imecore.CandidateSnapshotItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class KeyboardUiStateTest {

    @Test
    fun testKeyboardUiStateProperties() {
        val palette = ThemePalette(
            bgColor = 1, candidateBarColor = 2, textColor = 3, subColor = 4,
            preeditBgColor = 5, symTabActiveBg = 6, symTabInactiveBg = 7,
            symTabActiveText = 8, symTabInactiveText = 9, isDark = false,
            keyBgColor = 10, specialKeyBgColor = 11, keyPressedBgColor = 12,
            specialKeyPressedBgColor = 13
        )

        val candidate = CandidateSnapshotItem("test", "test", "test", 0, "TEST")
        val state = KeyboardUiState(
            keyboardMode = KeyboardMode.ChineseT9,
            lastTextMode = KeyboardMode.ChineseT9,
            rawBuffer = "abc",
            preedit = "a b c",
            readings = listOf("a", "b"),
            activeReading = "a",
            candidatesSnapshot = listOf(candidate),
            currentSymCategory = "punct",
            isComposing = true,
            themePalette = palette
        )

        assertEquals(KeyboardMode.ChineseT9, state.keyboardMode)
        assertEquals(KeyboardMode.ChineseT9, state.lastTextMode)
        assertEquals("abc", state.rawBuffer)
        assertEquals("a b c", state.preedit)
        assertEquals(2, state.readings.size)
        assertEquals("a", state.activeReading)
        assertEquals(1, state.candidatesSnapshot.size)
        assertEquals("punct", state.currentSymCategory)
        assertTrue(state.isComposing)
        assertEquals(palette, state.themePalette)
    }
}
