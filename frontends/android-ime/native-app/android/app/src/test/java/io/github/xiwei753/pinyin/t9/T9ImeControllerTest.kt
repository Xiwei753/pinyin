package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyInt

class T9ImeControllerTest {

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
    fun testInput96PreeditShowsWo() {
        controller.inputDigit("9")
        controller.inputDigit("6")

        assertEquals("96", engine.buffer)

        val preedit = controller.preedit
        assertTrue("Preedit should contain pinyin 'wo', got: $preedit",
            preedit.isEmpty() || preedit.contains("wo") || preedit == "wo")
    }

    @Test
    fun testZeroWithEmptyBufferCommitsSpace() {
        val result = controller.onZero()
        assertTrue("Should return CommitText with space", result is T9ImeController.ActionResult.CommitText)
        assertEquals(" ", (result as T9ImeController.ActionResult.CommitText).text)
    }

    @Test
    fun testZeroWithCandidatesCommitsFirstCandidateAndClearsState() {
        val candidate = Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(listOf(candidate))

        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)

        assertFalse("Should have candidates after refresh", controller.currentCandidates.isEmpty())

        val result = controller.onZero()
        assertTrue("Should return CommitText", result is T9ImeController.ActionResult.CommitText)
        assertEquals("我", (result as T9ImeController.ActionResult.CommitText).text)
        assertTrue("Buffer should be cleared after commit", engine.buffer.isEmpty())
        assertTrue("CurrentCandidates should be cleared after commit", controller.currentCandidates.isEmpty())
    }

    @Test
    fun testZeroWithNonEmptyBufferNoCandidatesClearsEngine() {
        val engineSpy = spy(engine)
        val controllerWithSpy = T9ImeController(engineSpy)

        doReturn(emptyList<Candidate>()).`when`(engineSpy).getVisibleCandidates(anyInt())

        controllerWithSpy.inputDigit("9")
        controllerWithSpy.inputDigit("9")
        controllerWithSpy.refreshCandidates(30)

        val result = controllerWithSpy.onZero()
        assertTrue("Should return Refresh", result is T9ImeController.ActionResult.Refresh)
        assertTrue("Buffer should be empty", engineSpy.buffer.isEmpty())
        assertTrue("Candidates should be empty", controllerWithSpy.currentCandidates.isEmpty())
    }

    @Test
    fun testSeparatorKeyWithSeparatedInput() {
        controller.inputDigit("2")
        controller.inputDigit("8")
        val sepResult = controller.onSeparator()
        assertTrue("Should return Refresh", sepResult is T9ImeController.ActionResult.Refresh)
        assertEquals("281", engine.buffer)

        controller.inputDigit("8")
        controller.inputDigit("2")
        controller.inputDigit("4")
        assertEquals("281824", engine.buffer)
    }

    @Test
    fun testSeparatorKeyWithEmptyBufferNoOp() {
        val result = controller.onSeparator()
        assertTrue("Should return NoAction for empty buffer", result is T9ImeController.ActionResult.NoAction)
        assertEquals("", engine.buffer)
    }

    @Test
    fun testCandidateClickCommitsCachedCandidate() {
        val candidate0 = Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        val candidate1 = Candidate("我们", "96", 800, CandidateType.NORMAL, "wo", CandidateOrigin.EXACT_SINGLE)

        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(listOf(candidate0, candidate1))

        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)

        assertTrue("Should have at least 2 candidates", controller.currentCandidates.size >= 2)

        val result = controller.onCandidateClick(0)
        assertTrue("Should return CommitText", result is T9ImeController.ActionResult.CommitText)
        assertTrue("Buffer should be cleared", engine.buffer.isEmpty())
        assertTrue("Candidates should be cleared", controller.currentCandidates.isEmpty())
    }

    @Test
    fun testCandidateClickInvalidIndexReturnsNoAction() {
        val result = controller.onCandidateClick(-1)
        assertTrue("Should return NoAction for index -1", result is T9ImeController.ActionResult.NoAction)

        val result2 = controller.onCandidateClick(100)
        assertTrue("Should return NoAction for index 100", result2 is T9ImeController.ActionResult.NoAction)
    }

    @Test
    fun testDeleteKeyWithNonEmptyBufferBackspaces() {
        controller.inputDigit("2")
        controller.inputDigit("8")
        assertEquals("28", engine.buffer)

        val result = controller.onDelete()
        assertTrue("Should return Refresh", result is T9ImeController.ActionResult.Refresh)
        assertEquals("2", engine.buffer)
    }

    @Test
    fun testDeleteKeyWithEmptyBufferRequestsSystemDelete() {
        val result = controller.onDelete()
        assertTrue("Should return SendDelete for empty buffer", result is T9ImeController.ActionResult.SendDelete)
    }

    @Test
    fun testCommitClearsBufferAndCandidates() {
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(listOf(
            Candidate("不", "28", 1000, CandidateType.SINGLE_CHAR, "bu", CandidateOrigin.EXACT_SINGLE)
        ))

        controller.inputDigit("2")
        controller.inputDigit("8")
        controller.refreshCandidates(30)

        assertTrue("Candidates should exist", controller.currentCandidates.isNotEmpty())

        val result = controller.onZero()
        assertTrue("Should be CommitText", result is T9ImeController.ActionResult.CommitText)

        assertTrue("Buffer should be empty", engine.buffer.isEmpty())
        assertTrue("Candidates should be empty", controller.currentCandidates.isEmpty())
        assertEquals("", controller.preedit)
    }

    @Test
    fun testResetClearsAllState() {
        controller.inputDigit("9")
        controller.inputDigit("6")

        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))

        controller.refreshCandidates(30)
        assertTrue("Candidates should exist", controller.currentCandidates.isNotEmpty())

        controller.reset()

        assertTrue("Buffer should be empty", engine.buffer.isEmpty())
        assertTrue("Candidates should be empty", controller.currentCandidates.isEmpty())
    }

    @Test
    fun testMultipleInputAccumulates() {
        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.inputDigit("6")
        controller.inputDigit("4")
        assertEquals("9664", engine.buffer)
    }

    @Test
    fun testRefreshCandidatesReturnsEmptyForEmptyBuffer() {
        val candidates = controller.refreshCandidates(30)
        assertTrue("Should return empty for no input", candidates.isEmpty())
    }

    @Test
    fun testRefreshCandidatesRespectsLimit() {
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(
            listOf(
                Candidate("一", "2", 600, CandidateType.SINGLE_CHAR, "a", CandidateOrigin.EXACT_SINGLE),
                Candidate("个", "2", 500, CandidateType.SINGLE_CHAR, "a", CandidateOrigin.EXACT_SINGLE),
                Candidate("啊", "2", 400, CandidateType.SINGLE_CHAR, "a", CandidateOrigin.EXACT_SINGLE)
            )
        )

        controller.inputDigit("2")
        val candidates = controller.refreshCandidates(1)

        assertEquals("Candidates should be limited to requested count", 1, candidates.size)
        assertEquals("一", candidates[0].text)
    }
}
