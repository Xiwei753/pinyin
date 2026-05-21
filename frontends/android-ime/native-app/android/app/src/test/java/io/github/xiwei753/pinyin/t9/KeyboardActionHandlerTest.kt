package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.times

class KeyboardActionHandlerTest {

    private lateinit var sink: ImeActionSink
    private lateinit var handler: KeyboardActionHandler
    private lateinit var dictionary: DictionaryProvider
    private lateinit var engine: T9Engine

    @Before
    fun setUp() {
        sink = mock(ImeActionSink::class.java)
        handler = KeyboardActionHandler(sink)
        dictionary = mock(DictionaryProvider::class.java)
        engine = T9Engine(dictionary)
        handler.attachEngine(engine)
    }

    private fun setupCandidates(candidates: List<Candidate>) {
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(candidates)
    }

    @Test
    fun testChineseT9_96_yields_wo_and_candidate() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        val candidates = handler.refreshCandidates(30)

        assertEquals("wo", handler.preedit)
        assertEquals("96", handler.rawBuffer)
        assertTrue(candidates.any { it.text == "我" })
    }

    @Test
    fun testChineseT9_0_key_commits_candidate() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        handler.refreshCandidates(30)

        handler.onZero()
        verify(sink).commitText("我")
        assertEquals("", handler.rawBuffer)
    }

    @Test
    fun testChineseT9_0_key_commits_space_when_empty() {
        handler.onZero()
        verify(sink).commitText(" ")
    }

    @Test
    fun testChineseT9_1_key_separator() {
        handler.onDigitPressed("2")
        handler.onDigitPressed("8")
        handler.onSeparator()
        assertEquals("281", handler.rawBuffer)
    }

    @Test
    fun testChineseT9_delete_composing() {
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        handler.onDelete()
        assertEquals("9", handler.rawBuffer)
        verify(sink, never()).sendDelete()
    }

    @Test
    fun testChineseT9_delete_empty() {
        handler.onDelete()
        verify(sink).sendDelete()
    }

    @Test
    fun testChineseT9_candidate_click() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        handler.refreshCandidates(30)

        handler.onCandidateClick(0)
        verify(sink).commitText("我")
        assertEquals("", handler.rawBuffer)
    }

    @Test
    fun testModeSwitch_ChineseToSymbol_Commits() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        handler.refreshCandidates(30)

        handler.switchKeyboardMode(KeyboardMode.Symbol)
        verify(sink).commitText("我")
        assertEquals(KeyboardMode.Symbol, handler.keyboardMode)
    }

    @Test
    fun testModeSwitch_SymbolToChinese_BufferEmpty() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        handler.switchKeyboardMode(KeyboardMode.ChineseT9)
        assertEquals("", handler.rawBuffer)
    }

    @Test
    fun testSymbolMode_commitsDirectly() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        handler.onDigitPressed("，")
        verify(sink).commitText("，")
    }

    @Test
    fun testSymbolMode_delete_sendsSystemDelete() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        handler.onDelete()
        verify(sink).sendDelete()
    }

    @Test
    fun testSymbolMode_enter_performsEditorAction() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        handler.onEnter()
        verify(sink).performEditorActionOrNewline()
    }

    @Test
    fun testNumberMode_commitsDirectly() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.onDigitPressed("5")
        verify(sink).commitText("5")
    }

    @Test
    fun testNumberMode_delete_sendsSystemDelete() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.onDelete()
        verify(sink).sendDelete()
    }

    @Test
    fun testNumberMode_enter_performsEditorAction() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.onEnter()
        verify(sink).performEditorActionOrNewline()
    }

    @Test
    fun testEnglishMode_multitap_cycles() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.onDigitPressed("2")
        assertEquals("a", handler.preedit)
        assertTrue(handler.englishPending)

        handler.onDigitPressed("2")
        assertEquals("b", handler.preedit)

        handler.onDigitPressed("2")
        assertEquals("c", handler.preedit)

        handler.onDigitPressed("2")
        assertEquals("a", handler.preedit)
    }

    @Test
    fun testEnglishMode_switchMode_commitsPending() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.onDigitPressed("2") // 'a'
        handler.switchKeyboardMode(KeyboardMode.ChineseT9)
        verify(sink).commitText("a")
        assertEquals(KeyboardMode.ChineseT9, handler.keyboardMode)
    }

    @Test
    fun testEnglishMode_discardCompositionForLifecycle_cancelsPending() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.onDigitPressed("2")
        assertEquals("a", handler.preedit)
        assertTrue(handler.englishPending)

        handler.discardCompositionForLifecycle()
        verify(sink, never()).commitText("a")
        assertEquals(false, handler.englishPending)
        assertEquals("", handler.preedit)
    }

    @Test
    fun testEnglishMode_onHideKey_commitsPending() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.onDigitPressed("2")
        assertEquals("a", handler.preedit)

        handler.onHideKey()
        verify(sink).commitText("a")
        assertEquals(false, handler.englishPending)
        assertEquals("", handler.preedit)
    }

    @Test
    fun testChineseMode_discardCompositionForLifecycle_clearsBuffer() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        handler.refreshCandidates(30)
        assertEquals("96", handler.rawBuffer)

        handler.discardCompositionForLifecycle()
        verify(sink, never()).commitText(anyString())
        assertEquals("", handler.rawBuffer)
        assertEquals(0, handler.currentCandidates.size)
    }

    @Test
    fun testChineseMode_onHideKey_commitsFirstCandidate() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        handler.refreshCandidates(30)

        handler.onHideKey()
        verify(sink).commitText("我")
        assertEquals("", handler.rawBuffer)
    }

    @Test
    fun testChineseT9_clearComposingForRetype_clearsBufferNoCommit() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        handler.refreshCandidates(30)
        assertEquals("96", handler.rawBuffer)
        assertTrue(handler.currentCandidates.isNotEmpty())

        handler.onClearComposingForRetype()
        assertEquals("", handler.rawBuffer)
        assertEquals(0, handler.currentCandidates.size)
        verify(sink, never()).commitText(anyString())
        verify(sink, never()).sendDelete()
    }

    @Test
    fun testEnglishT9_clearComposingForRetype_cancelsPendingNoCommit() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.onDigitPressed("2")
        assertTrue(handler.englishPending)
        assertEquals("a", handler.preedit)

        handler.onClearComposingForRetype()
        assertEquals(false, handler.englishPending)
        assertEquals("", handler.preedit)
        verify(sink, never()).commitText(anyString())
    }

    @Test
    fun testPunct_commma_commitsDirect() {
        handler.onPunctCommit("，")
        verify(sink).commitText("，")
    }

    @Test
    fun testPunct_period_commitsDirect() {
        handler.onPunctCommit("。")
        verify(sink).commitText("。")
    }

    @Test
    fun testPunct_question_commitsDirect() {
        handler.onPunctCommit("？")
        verify(sink).commitText("？")
    }

    @Test
    fun testPunct_exclamation_commitsDirect() {
        handler.onPunctCommit("！")
        verify(sink).commitText("！")
    }

    @Test
    fun testPunct_commitsComposingFirstThenPunct() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        handler.refreshCandidates(30)

        handler.onPunctCommit("，")
        verify(sink).commitText("我")
        verify(sink).commitText("，")
        assertEquals("", handler.rawBuffer)
    }

    @Test
    fun testSpace_chineseEmptyBuffer_commitsSpace() {
        handler.onSpace()
        verify(sink).commitText(" ")
    }

    @Test
    fun testSpace_chineseWithCandidate_commitsFirstCandidate() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("9")
        handler.onDigitPressed("6")
        handler.refreshCandidates(30)

        handler.onSpace()
        verify(sink).commitText("我")
        assertEquals("", handler.rawBuffer)
    }

    @Test
    fun testSpace_englishMode_commitsSpace() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.onSpace()
        verify(sink).commitText(" ")
    }

    @Test
    fun testPunct_doesNotPolluteT9Buffer() {
        handler.onPunctCommit("，")
        assertEquals("", handler.rawBuffer)
        assertEquals("", handler.preedit)
    }

    // --- lastTextMode tests ---

    @Test
    fun testLastTextMode_ChineseToSymbol() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        assertEquals(KeyboardMode.ChineseT9, handler.lastTextMode)
    }

    @Test
    fun testLastTextMode_EnglishToSymbol() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        assertEquals(KeyboardMode.EnglishT9, handler.lastTextMode)
    }

    @Test
    fun testLastTextMode_ChineseToNumber() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        assertEquals(KeyboardMode.ChineseT9, handler.lastTextMode)
    }

    @Test
    fun testLastTextMode_EnglishToNumber() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.switchKeyboardMode(KeyboardMode.Number)
        assertEquals(KeyboardMode.EnglishT9, handler.lastTextMode)
    }

    @Test
    fun testLastTextMode_SymbolToNumber_doesNotChange() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        assertEquals(KeyboardMode.ChineseT9, handler.lastTextMode)
        handler.switchKeyboardMode(KeyboardMode.Number)
        assertEquals(KeyboardMode.ChineseT9, handler.lastTextMode)
    }

    @Test
    fun testLastTextMode_NumberToSymbol_doesNotChange() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        assertEquals(KeyboardMode.ChineseT9, handler.lastTextMode)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        assertEquals(KeyboardMode.ChineseT9, handler.lastTextMode)
    }

    @Test
    fun testLastTextMode_SymbolReturnsToLastTextMode() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        assertEquals(KeyboardMode.ChineseT9, handler.lastTextMode)
        handler.switchKeyboardMode(handler.lastTextMode)
        assertEquals(KeyboardMode.ChineseT9, handler.keyboardMode)
    }

    @Test
    fun testLastTextMode_SymbolReturnsToEnglish() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        assertEquals(KeyboardMode.EnglishT9, handler.lastTextMode)
        handler.switchKeyboardMode(handler.lastTextMode)
        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)
    }

    @Test
    fun testLastTextMode_NumberReturnsToLastTextMode() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        assertEquals(KeyboardMode.ChineseT9, handler.lastTextMode)
        handler.switchKeyboardMode(handler.lastTextMode)
        assertEquals(KeyboardMode.ChineseT9, handler.keyboardMode)
    }

    @Test
    fun testToggleSymbolKey_fromChinese_goesToSymbol() {
        handler.toggleSymbolKey()
        assertEquals(KeyboardMode.Symbol, handler.keyboardMode)
    }

    @Test
    fun testToggleSymbolKey_fromSymbol_returnsToLastTextMode() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.toggleSymbolKey()
        assertEquals(KeyboardMode.Symbol, handler.keyboardMode)
        assertEquals(KeyboardMode.EnglishT9, handler.lastTextMode)
        handler.toggleSymbolKey()
        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)
    }

    @Test
    fun testToggleSymbolKey_fromNumber_goesToSymbol() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.toggleSymbolKey()
        assertEquals(KeyboardMode.Symbol, handler.keyboardMode)
    }

    @Test
    fun testToggleNumberKey_fromChinese_goesToNumber() {
        handler.toggleNumberKey()
        assertEquals(KeyboardMode.Number, handler.keyboardMode)
    }

    @Test
    fun testToggleNumberKey_fromNumber_returnsToLastTextMode() {
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.toggleNumberKey()
        assertEquals(KeyboardMode.Number, handler.keyboardMode)
        assertEquals(KeyboardMode.EnglishT9, handler.lastTextMode)
        handler.toggleNumberKey()
        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)
    }

    @Test
    fun testToggleNumberKey_fromSymbol_goesToNumber() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        handler.toggleNumberKey()
        assertEquals(KeyboardMode.Number, handler.keyboardMode)
    }

    @Test
    fun testSymbolMode_space_commitsSpace() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        handler.onSpace()
        verify(sink).commitText(" ")
    }

    @Test
    fun testNumberMode_space_commitsSpace() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.onSpace()
        verify(sink).commitText(" ")
    }

    @Test
    fun testNumberMode_dot_commitsDot() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.onDigitPressed(".")
        verify(sink).commitText(".")
    }

    @Test
    fun testNumberMode_zero_commitsZero() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.onDigitPressed("0")
        verify(sink).commitText("0")
    }

    @Test
    fun testNumberMode_one_commitsOne() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.onDigitPressed("1")
        verify(sink).commitText("1")
    }

    @Test
    fun testNumberMode_five_commitsFive() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.onDigitPressed("5")
        verify(sink).commitText("5")
    }

    @Test
    fun testNumberMode_nine_commitsNine() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        handler.onDigitPressed("9")
        verify(sink).commitText("9")
    }

    // --- Active reading tests ---

    @Test
    fun testReadingsExposedWhenBufferNonEmpty() {
        setupCandidates(listOf(
            Candidate("梦", "6364", 50000, CandidateType.SINGLE_CHAR, "meng", CandidateOrigin.EXACT_SINGLE),
            Candidate("能", "6364", 40000, CandidateType.SINGLE_CHAR, "neng", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("6")
        handler.onDigitPressed("3")
        handler.onDigitPressed("6")
        handler.onDigitPressed("4")
        val readings = handler.readings
        assertTrue(readings.isNotEmpty())
        assertTrue(readings.any { it == "meng" })
        assertTrue(readings.any { it == "neng" })
    }

    @Test
    fun testReadingsEmptyForEmptyBuffer() {
        assertTrue(handler.readings.isEmpty())
    }

    @Test
    fun testSetActiveReadingDoesNotCommitText() {
        setupCandidates(listOf(
            Candidate("能", "636", 50000, CandidateType.SINGLE_CHAR, "neng", CandidateOrigin.EXACT_SINGLE),
            Candidate("梦", "6364", 40000, CandidateType.SINGLE_CHAR, "meng", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("6")
        handler.onDigitPressed("3")
        handler.onDigitPressed("6")
        handler.onDigitPressed("4")

        val success = handler.setActiveReading("neng")
        assertTrue(success)
        verify(sink, never()).commitText(anyString())
        assertEquals("neng", handler.preedit)
    }

}
