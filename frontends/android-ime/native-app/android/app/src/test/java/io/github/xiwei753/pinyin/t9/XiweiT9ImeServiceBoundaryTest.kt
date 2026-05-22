package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.imecore.ImeInputAction
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class XiweiT9ImeServiceBoundaryTest {
    @Test
    fun serviceDoesNotMaintainDuplicateSymbolCategoryField() {
        val source = sourceFile("io/github/xiwei753/pinyin/t9/XiweiT9ImeService.kt").readText()

        assertFalse(source.contains("private var currentSymCategory"))
        assertFalse(source.contains("currentSymCategory = coreState"))
    }

    @Test
    fun symbolCategoryRenderStateComesFromImeUiState() {
        val dictionary = mock(DictionaryProvider::class.java)
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getPinyinPrefixCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getPrefixCandidates(anyString())).thenReturn(emptyList())
        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply {
            attachEngine(T9Engine(dictionary))
        }

        handler.handle(ImeInputAction.ToggleSymbol)
        handler.handle(ImeInputAction.SymbolCategorySelected("math"))

        val androidState = handler.uiState().toAndroidKeyboardUiState(defaultPalette())
        assertEquals("math", androidState.currentSymCategory)
        assertEquals("math", androidState.symbolPanelState.category)
    }

    @Test
    fun enterPolicyStaysInAndroidAdapter() {
        val handlerSource = sourceFile("io/github/xiwei753/pinyin/t9/KeyboardActionHandler.kt").readText()
        val enterPolicySource = sourceFile("io/github/xiwei753/pinyin/t9/EnterActionPolicy.kt").readText()
        val coreSource = sourceFile("io/github/xiwei753/pinyin/imecore/ImeStateMachine.kt").readText()

        assertTrue(handlerSource.contains("performEditorActionOrNewline()"))
        assertTrue(enterPolicySource.contains("EditorInfo"))
        assertFalse(coreSource.contains("EnterActionPolicy"))
        assertFalse(coreSource.contains("EditorInfo"))
    }

    private fun defaultPalette() = ThemePalette(
        bgColor = 0,
        candidateBarColor = 0,
        textColor = 0,
        subColor = 0,
        preeditBgColor = 0,
        symTabActiveBg = 0,
        symTabInactiveBg = 0,
        symTabActiveText = 0,
        symTabInactiveText = 0,
        isDark = false,
        keyBgColor = 0,
        specialKeyBgColor = 0,
        keyPressedBgColor = 0,
        specialKeyPressedBgColor = 0,
    )

    private fun sourceFile(relativePath: String): File {
        val fromRoot = File("app/src/main/java/$relativePath")
        if (fromRoot.exists()) return fromRoot
        return File("src/main/java/$relativePath")
    }
}
