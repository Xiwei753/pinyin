package io.github.xiwei753.pinyin.imecore

import io.github.xiwei753.pinyin.t9.T9EngineAdapter
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ImeStateMachineTest {
    private lateinit var dictionary: DictionaryProvider
    private lateinit var machine: ImeStateMachine

    @Before
    fun setUp() {
        dictionary = mock(DictionaryProvider::class.java)
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getPinyinPrefixCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getPrefixCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates("wo")).thenReturn(listOf(candidate("我", "wo")))
        machine = ImeStateMachine { 30 }
        machine.attachEngine(T9EngineAdapter(T9Engine(dictionary)))
    }

    @Test
    fun chineseT9_96BuildsUnifiedUiState() {
        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.DigitPressed("6"))
        val state = machine.uiState()

        assertEquals("96", state.rawBuffer)
        assertEquals("wo", state.preedit)
        assertTrue(state.candidatesSnapshot.any { it.text == "我" })
        assertTrue(state.preeditState.visible)
        assertTrue(state.candidateStrip.candidates.all { it.text != "wo" && !it.text.all(Char::isDigit) })
        assertEquals(RailKind.Readings, state.keyboardSurface.railState.kind)
    }

    @Test
    fun chineseT9_emptyBufferShowsPunctuationRailAndNoPreedit() {
        val state = machine.uiState()

        assertEquals(RailKind.Punctuation, state.keyboardSurface.railState.kind)
        assertFalse(state.preeditState.visible)
    }

    @Test
    fun candidateSelectedUsesSnapshotAndClearsComposing() {
        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.DigitPressed("6"))

        val effects = machine.dispatch(ImeInputAction.CandidateSelected(0))

        assertTrue(effects.contains(ImeSideEffect.CommitCandidate("我")))
        assertEquals("", machine.rawBuffer)
        verify(dictionary).getSingleSyllableCandidates("wo")
    }

    @Test
    fun separatorOnlyWorksWithBuffer() {
        machine.dispatch(ImeInputAction.SeparatorPressed)
        assertEquals("", machine.rawBuffer)

        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.SeparatorPressed)

        assertEquals("91", machine.rawBuffer)
    }

    @Test
    fun zeroCommitsCandidateOrSpace() {
        val emptyEffects = machine.dispatch(ImeInputAction.ZeroPressed)
        assertTrue(emptyEffects.contains(ImeSideEffect.CommitText(" ")))

        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.DigitPressed("6"))
        val effects = machine.dispatch(ImeInputAction.ZeroPressed)

        assertTrue(effects.contains(ImeSideEffect.CommitCandidate("我")))
    }

    @Test
    fun symbolModeBehaviorStaysPlatformIndependent() {
        machine.dispatch(ImeInputAction.ToggleSymbol)
        assertEquals(InputMode.Symbol, machine.mode)
        assertEquals(InputMode.ChineseT9, machine.lastTextMode)

        val commitEffects = machine.dispatch(ImeInputAction.SymbolCommitted("@"))
        assertTrue(commitEffects.contains(ImeSideEffect.CommitText("@")))
        assertEquals(InputMode.Symbol, machine.mode)

        machine.dispatch(ImeInputAction.SymbolCategorySelected("math"))
        assertEquals("math", machine.currentSymbolCategory)
        assertEquals(InputMode.Symbol, machine.mode)
        assertEquals(InputMode.ChineseT9, machine.lastTextMode)

        machine.dispatch(ImeInputAction.ToggleNumber)
        assertEquals(InputMode.Number, machine.mode)
        assertEquals(InputMode.ChineseT9, machine.lastTextMode)
    }

    @Test
    fun spaceInSymbolAndNumberCommitsSpaceAndKeepsMode() {
        machine.dispatch(ImeInputAction.ToggleSymbol)
        val symbolEffects = machine.dispatch(ImeInputAction.SpacePressed)
        assertTrue(symbolEffects.contains(ImeSideEffect.CommitText(" ")))
        assertEquals(InputMode.Symbol, machine.mode)

        machine.dispatch(ImeInputAction.ToggleNumber)
        val numberEffects = machine.dispatch(ImeInputAction.SpacePressed)
        assertTrue(numberEffects.contains(ImeSideEffect.CommitText(" ")))
        assertEquals(InputMode.Number, machine.mode)
    }

    @Test
    fun candidateSelectedDoesNotRequeryDictionary() {
        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.DigitPressed("6"))
        clearInvocations(dictionary)

        machine.dispatch(ImeInputAction.CandidateSelected(0))

        verify(dictionary, never()).getSingleSyllableCandidates(anyString())
        verify(dictionary, never()).getPinyinExactCandidates(anyString())
    }

    @Test
    fun renderMultipleTimesDoesNotChangeOrRequeryCandidateSnapshot() {
        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.DigitPressed("6"))
        val before = machine.uiState().candidatesSnapshot
        clearInvocations(dictionary)

        val state1 = machine.uiState()
        val state2 = machine.uiState()

        assertEquals(before, state1.candidatesSnapshot)
        assertEquals(before, state2.candidatesSnapshot)
        verify(dictionary, never()).getSingleSyllableCandidates(anyString())
        verify(dictionary, never()).getPinyinExactCandidates(anyString())
    }

    @Test
    fun readingSelectedRefreshesCandidates() {
        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.DigitPressed("6"))
        clearInvocations(dictionary)

        machine.dispatch(ImeInputAction.ReadingSelected(0))

        verify(dictionary, times(1)).getSingleSyllableCandidates("wo")
    }

    @Test
    fun deletePressedRefreshesCandidatesAndClearComposingClearsSnapshot() {
        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.DigitPressed("6"))
        assertTrue(machine.uiState().candidatesSnapshot.isNotEmpty())

        machine.dispatch(ImeInputAction.DeletePressed)
        assertEquals("9", machine.rawBuffer)

        machine.dispatch(ImeInputAction.ClearComposing)
        assertEquals("", machine.rawBuffer)
        assertTrue(machine.uiState().candidatesSnapshot.isEmpty())
    }

    private fun candidate(text: String, pinyin: String): Candidate = Candidate(
        text = text,
        code = pinyin,
        score = 1000,
        type = CandidateType.SINGLE_CHAR,
        sourcePinyin = pinyin,
        origin = CandidateOrigin.EXACT_SINGLE,
    )
}
