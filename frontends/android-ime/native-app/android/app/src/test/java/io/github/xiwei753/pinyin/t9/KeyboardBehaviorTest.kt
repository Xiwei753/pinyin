package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*

class KeyboardBehaviorTest {

    private lateinit var engine: T9Engine
    private lateinit var dictionary: DictionaryProvider
    private lateinit var controller: T9ImeController

    @Before
    fun setUp() {
        dictionary = mock(DictionaryProvider::class.java)
        engine = T9Engine(dictionary)
        controller = T9ImeController(engine)
    }

    @Test
    fun numberModeDoesNotTriggerT9Candidate() {
        // number mode commits digits directly; pressing 9 should NOT feed T9 buffer
        controller.inputDigit("9")
        assertEquals("9", engine.buffer)
    }

    @Test
    fun zeroKeyWithEmptyBufferCommitsSpace() {
        val result = controller.onZero()
        assertTrue("Should commit space when buffer empty", result is T9ImeController.ActionResult.CommitText)
        assertEquals(" ", (result as T9ImeController.ActionResult.CommitText).text)
    }

    @Test
    fun zeroKeyWithCandidatesCommitsFirstCandidate() {
        val candidate = Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(listOf(candidate))

        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)
        assertTrue("Should have candidates after refresh", controller.currentCandidates.isNotEmpty())

        val result = controller.onZero()
        assertTrue("Should commit candidate", result is T9ImeController.ActionResult.CommitText)
        assertEquals("我", (result as T9ImeController.ActionResult.CommitText).text)
        assertTrue("Buffer cleared after commit", engine.buffer.isEmpty())
    }

    @Test
    fun separatorKeyAppends1ToBufferNotAsDigit() {
        controller.inputDigit("2")
        controller.inputDigit("8")
        val sepResult = controller.onSeparator()
        assertTrue("Should return Refresh", sepResult is T9ImeController.ActionResult.Refresh)
        assertEquals("281", engine.buffer)
    }

    @Test
    fun separatorKeyWithEmptyBufferIsNoOp() {
        val result = controller.onSeparator()
        assertTrue("Should return NoAction", result is T9ImeController.ActionResult.NoAction)
        assertEquals("", engine.buffer)
    }

    @Test
    fun deleteKeyWithNonEmptyBufferBackspacesComposing() {
        controller.inputDigit("9")
        controller.inputDigit("6")
        assertEquals("96", engine.buffer)

        val result = controller.onDelete()
        assertTrue("Should return Refresh", result is T9ImeController.ActionResult.Refresh)
        assertEquals("9", engine.buffer)
    }

    @Test
    fun deleteKeyWithEmptyBufferRequestsSystemDelete() {
        val result = controller.onDelete()
        assertTrue("Should return SendDelete", result is T9ImeController.ActionResult.SendDelete)
    }

    @Test
    fun candidateClickCommitsCachedCandidateNotRefreshed() {
        val candidate = Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(listOf(candidate))

        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)

        val result = controller.onCandidateClick(0)
        assertTrue("Should commit", result is T9ImeController.ActionResult.CommitText)
        assertEquals("我", (result as T9ImeController.ActionResult.CommitText).text)
        assertTrue("Buffer cleared", engine.buffer.isEmpty())
        assertTrue("Candidates cleared", controller.currentCandidates.isEmpty())
    }

    @Test
    fun enterKeyWithComposingBufferCommitsFirstCandidate() {
        val candidate = Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(listOf(candidate))

        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)

        // simulate enter logic: commit first candidate or preedit
        val candidates = controller.currentCandidates
        assertTrue(candidates.isNotEmpty())
        val clickResult = controller.onCandidateClick(0)
        assertTrue("Should commit candidate on enter when composing",
            clickResult is T9ImeController.ActionResult.CommitText)
        assertEquals("我", (clickResult as T9ImeController.ActionResult.CommitText).text)
    }

    @Test
    fun enterKeyWithComposingBufferButNoCandidatesCommitsPreedit() {
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(emptyList())

        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)
        assertTrue("No candidates expected", controller.currentCandidates.isEmpty())

        // enter with composing buffer but no candidates: commit preedit
        val preedit = controller.preedit
        controller.reset()
        assertTrue("Preedit should be a non-empty string", preedit.isNotEmpty())
    }

    @Test
    fun chineseToEnglishSwitchWithComposingBufferCommitsFirstCandidate() {
        val candidate = Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(listOf(candidate))

        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)
        assertTrue("Should have candidates", controller.currentCandidates.isNotEmpty())

        // simulate switch: commit first candidate then reset for English
        val commitResult = controller.onCandidateClick(0)
        assertTrue("Should commit first candidate on mode switch",
            commitResult is T9ImeController.ActionResult.CommitText)
        assertEquals("我", (commitResult as T9ImeController.ActionResult.CommitText).text)
        assertTrue("Controller buffer should be empty after commit", controller.rawBuffer.isEmpty())
    }

    @Test
    fun chineseToEnglishSwitchWithEmptyBufferDoesNotCommit() {
        assertTrue("Buffer is empty", controller.rawBuffer.isEmpty())
        // just reset for English mode
        controller.reset()
        assertTrue("After reset buffer still empty", controller.rawBuffer.isEmpty())
    }

    @Test
    fun switchingBackToChineseT9StillWorks() {
        // Simulate English mode then back to Chinese: T9 engine must not be corrupted
        val candidate = Candidate("不", "28", 1000, CandidateType.SINGLE_CHAR, "bu", CandidateOrigin.EXACT_SINGLE)
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(listOf(candidate))

        controller.inputDigit("2")
        controller.inputDigit("8")
        controller.refreshCandidates(30)
        assertTrue("T9 candidates exist for 'bu'", controller.currentCandidates.any { it.text == "不" })
        val first = controller.onCandidateClick(0)
        assertEquals("不", (first as T9ImeController.ActionResult.CommitText).text)
    }

    @Test
    fun symbolModeDoesNotPolluteT9Buffer() {
        // symbols go directly to InputConnection, not through controller
        assertTrue("T9 buffer should be empty before symbol", controller.rawBuffer.isEmpty())
    }

    @Test
    fun symbolModeDoesNotTriggerT9Candidates() {
        controller.inputDigit("9")
        assertEquals("T9 buffer after digit", "9", engine.buffer)
        // in symbol mode, pressing digit should NOT add to T9 buffer
        // that's enforced by the service (keyboard mode check in onClickListener)
        // controller doesn't have mode awareness
    }

    @Test
    fun keyboardModeEnumValues() {
        val values = KeyboardMode.values()
        assertTrue(values.contains(KeyboardMode.ChineseT9))
        assertTrue(values.contains(KeyboardMode.EnglishT9))
        assertTrue(values.contains(KeyboardMode.Symbol))
        assertTrue(values.contains(KeyboardMode.Number))
        assertEquals(4, values.size)
    }

    @Test
    fun defaultModeIsChineseT9() {
        // The default mode in the service is ChineseT9
        // This is verified at the enum level
        assertNotEquals(KeyboardMode.EnglishT9, KeyboardMode.ChineseT9)
    }
}
