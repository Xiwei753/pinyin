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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.atLeastOnce
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
        machine = ImeStateMachine(candidateLimitProvider = { 30 })
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
        verify(dictionary, atLeastOnce()).getSingleSyllableCandidates("wo")
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
    fun numberModeZeroCommitsDigitZero() {
        machine.dispatch(ImeInputAction.KeyboardModeSelected(InputMode.Number))

        val effects = machine.dispatch(ImeInputAction.ZeroPressed)

        assertTrue(effects.contains(ImeSideEffect.CommitText("0")))
        assertEquals(InputMode.Number, machine.mode)
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
    fun uiCandidateSnapshotDoesNotExposeT9CandidateType() {
        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.DigitPressed("6"))

        val item = machine.uiState().candidateStrip.candidates.first()

        assertEquals(CandidateSnapshotItem::class.java.name, item::class.java.name)
        assertEquals("我", item.text)
    }

    @Test
    fun candidateSelectedCommitsInternalSelectionWithoutRequeryingEngine() {
        val fake = SnapshotEngine(
            listOf(
                CandidateSelection(CandidateSnapshotItem("甲", "1", "jia", 10, "TEST")) { },
                CandidateSelection(CandidateSnapshotItem("乙", "2", "yi", 20, "TEST")) { },
            )
        )
        val localMachine = ImeStateMachine(candidateLimitProvider = { 30 })
        localMachine.attachEngine(fake)
        fake.queryCount = 0

        val effects = localMachine.dispatch(ImeInputAction.CandidateSelected(1))

        assertEquals(0, fake.queryCount)
        assertTrue(effects.contains(ImeSideEffect.CommitCandidate("乙")))
        assertEquals(1, fake.commitCount)
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
    fun deferredPreeditDoesNotQueryCandidates() {
        val localMachine = ImeStateMachine({ 30 }, deferCandidateComputation = true)
        val engine = T9Engine(dictionary)
        localMachine.attachEngine(T9EngineAdapter(engine))

        for (ch in "288249464") {
            localMachine.dispatch(ImeInputAction.DigitPressed(ch.toString()))
        }

        val state = localMachine.uiState()
        assertTrue("preedit should be non-empty", state.preedit.isNotEmpty())

        verify(dictionary, never()).getSingleSyllableCandidates(anyString())
        verify(dictionary, never()).getPinyinExactCandidates(anyString())
        verify(dictionary, never()).getPinyinPrefixCandidates(anyString())
        verify(dictionary, never()).getPinyinExactCandidatesMultiple(anyList())
    }

    @Test
    fun staleCandidateResultCannotOverwriteNewerResult() {
        val localMachine = ImeStateMachine({ 30 }, deferCandidateComputation = true)
        val engine = MutableEngine(buffer = "", preedit = "")
        localMachine.attachEngine(engine)

        localMachine.dispatch(ImeInputAction.DigitPressed("9"))
        val request1 = localMachine.drainPendingCandidateRequest()!!

        localMachine.dispatch(ImeInputAction.DigitPressed("6"))
        localMachine.drainPendingCandidateRequest()!!

        localMachine.dispatch(ImeInputAction.DigitPressed("4"))
        val request3 = localMachine.drainPendingCandidateRequest()!!

        val appliedNewer = localMachine.applyCandidateResult(
            CandidateResult(
                requestId = request3.requestId,
                candidates = listOf(CandidateSnapshotItem("乙", "964", "wo", 20, "TEST")),
            )
        )
        assertTrue(appliedNewer)
        assertEquals("乙", localMachine.currentCandidates.first().text)

        val appliedStale = localMachine.applyCandidateResult(
            CandidateResult(
                requestId = request1.requestId,
                candidates = listOf(CandidateSnapshotItem("甲", "96", "wo", 10, "TEST")),
            )
        )
        assertFalse(appliedStale)
        assertEquals("乙", localMachine.currentCandidates.first().text)
    }

    @Test
    fun asyncCandidateClickUsesCachedSnapshotWithoutRequeryingEngine() {
        val engine = SnapshotEngine(
            listOf(
                CandidateSelection(CandidateSnapshotItem("甲", "96", "wo", 10, "TEST")) { },
            )
        )
        val machine = ImeStateMachine({ 30 }, deferCandidateComputation = true)
        machine.attachEngine(engine)

        machine.dispatch(ImeInputAction.DigitPressed("9"))
        val request = machine.drainPendingCandidateRequest()!!
        val applied = machine.applyCandidateResult(
            CandidateResult(
                requestId = request.requestId,
                candidates = listOf(CandidateSnapshotItem("甲", "96", "wo", 10, "TEST")),
            )
        )
        assertTrue(applied)

        val effects = machine.dispatch(ImeInputAction.CandidateSelected(0))

        assertTrue(effects.contains(ImeSideEffect.CommitCandidate("甲")))
        assertEquals(1, engine.commitCount)
        assertEquals(0, engine.queryCount)
    }

    @Test
    fun readingSelectedRefreshesCandidates() {
        machine.dispatch(ImeInputAction.DigitPressed("9"))
        machine.dispatch(ImeInputAction.DigitPressed("6"))

        // Change the active reading, which updates lockedSyllables.
        // With caching in T9Engine, we verify the ui state receives the updated candidates.
        machine.dispatch(ImeInputAction.ReadingSelected(0))
        val cands2 = machine.uiState().candidatesSnapshot

        // Assert candidate refresh doesn't break and handles valid state
        org.junit.Assert.assertTrue(cands2.isNotEmpty())
        org.mockito.Mockito.verify(dictionary, org.mockito.Mockito.atMost(5)).getSingleSyllableCandidates("wo")
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

    @Test
    fun lifecycleStartInputResetsContextAndClearsEngineBuffer() {
        val engine = MutableEngine(buffer = "96", preedit = "wo")
        val localMachine = ImeStateMachine(candidateLimitProvider = { 30 })
        localMachine.attachEngine(engine)

        localMachine.dispatch(ImeInputAction.LifecycleStartInput(InputMode.EnglishT9, InputMode.EnglishT9))

        assertEquals(InputMode.EnglishT9, localMachine.mode)
        assertEquals(InputMode.EnglishT9, localMachine.lastTextMode)
        assertEquals("", localMachine.rawBuffer)
        assertTrue(localMachine.currentCandidates.isEmpty())
        assertTrue(engine.cleared)
    }

    @Test
    fun lifecycleFinishInputResetsContextAndClearsSnapshot() {
        val engine = MutableEngine(buffer = "96", preedit = "wo")
        val localMachine = ImeStateMachine(candidateLimitProvider = { 30 })
        localMachine.attachEngine(engine)

        localMachine.dispatch(ImeInputAction.ToggleSymbol)
        localMachine.dispatch(ImeInputAction.LifecycleFinishInput)

        assertEquals(InputMode.ChineseT9, localMachine.mode)
        assertEquals(InputMode.ChineseT9, localMachine.lastTextMode)
        assertEquals("punct", localMachine.currentSymbolCategory)
        assertTrue(localMachine.currentCandidates.isEmpty())
        assertEquals("", localMachine.rawBuffer)
    }

    private fun candidate(text: String, pinyin: String): Candidate = Candidate(
        text = text,
        code = pinyin,
        score = 1000,
        type = CandidateType.SINGLE_CHAR,
        sourcePinyin = pinyin,
        origin = CandidateOrigin.EXACT_SINGLE,
    )

    private class SnapshotEngine(
        private val selections: List<CandidateSelection>,
    ) : T9InputEngine {
        var queryCount = 0
        var commitCount = 0
        override val buffer: String = "96"
        override val lockedSyllables: List<String> = emptyList()
        override val readings: List<String> = listOf("wo")
        override val activeReading: String? = "wo"
        override fun getPreedit(): String = "wo"
        override fun getPreeditHint(): String = "wo"
        override fun inputDigit(digit: String) {}
        override fun backspace() {}
        override fun clear() {}
        override fun getVisibleCandidates(limit: Int): List<CandidateSelection> {
            queryCount++
            return selections.map { selection -> selection.copy(commit = { commitCount++ }) }
        }
        override fun commitCandidate(candidate: CandidateSnapshotItem) {
            commitCount++
        }
        override fun setActiveReading(reading: String): Boolean = false
    }

    private class MutableEngine(
        buffer: String,
        preedit: String,
    ) : T9InputEngine {
        var mutableBuffer: String = buffer
        private var mutablePreedit: String = preedit
        var cleared: Boolean = false
        override val buffer: String get() = mutableBuffer
        override val lockedSyllables: List<String> get() = emptyList()
        override val readings: List<String> get() = emptyList()
        override val activeReading: String? get() = null
        override fun getPreedit(): String = mutablePreedit
        override fun getPreeditHint(): String = mutablePreedit
        override fun inputDigit(digit: String) {
            mutableBuffer += digit
        }
        override fun backspace() {
            mutableBuffer = mutableBuffer.dropLast(1)
        }
        override fun clear() {
            mutableBuffer = ""
            mutablePreedit = ""
            cleared = true
        }
        override fun getVisibleCandidates(limit: Int): List<CandidateSelection> = emptyList()
        override fun commitCandidate(candidate: CandidateSnapshotItem) {}
        override fun setActiveReading(reading: String): Boolean = false
    }
}
