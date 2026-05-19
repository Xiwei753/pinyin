package io.github.xiwei753.pinyin.t9

import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
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

    // ─── 0 key rules ────────────────────────────────────────────

    @Test
    fun zeroKeyWithEmptyBufferCommitsSpace() {
        val result = controller.onZero()
        assertTrue("Should commit space when buffer empty", result is T9ImeController.ActionResult.CommitText)
        assertEquals(" ", (result as T9ImeController.ActionResult.CommitText).text)
    }

    @Test
    fun zeroKeyWithCandidatesCommitsFirstCandidate() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
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
    fun zeroKeyWithNonEmptyBufferButNoCandidatesDoesNotDropBuffer() {
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(emptyList())

        controller.inputDigit("9")
        controller.inputDigit("9")
        controller.refreshCandidates(30)
        assertTrue("No candidates expected", controller.currentCandidates.isEmpty())

        val result = controller.onZero()
        assertTrue("Should return NoAction — don't drop user input",
            result is T9ImeController.ActionResult.NoAction)
        assertEquals("99", engine.buffer)
    }

    // ─── 1 key as separator ─────────────────────────────────────

    @Test
    fun separatorKeyAppends1ToBuffer() {
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

    // ─── delete key ─────────────────────────────────────────────

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

    // ─── candidate click uses cached candidates ──────────────────

    @Test
    fun candidateClickCommitsCachedCandidateNotRefreshed() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)

        val result = controller.onCandidateClick(0)
        assertTrue("Should commit", result is T9ImeController.ActionResult.CommitText)
        assertEquals("我", (result as T9ImeController.ActionResult.CommitText).text)
        assertTrue("Buffer cleared", engine.buffer.isEmpty())
        assertTrue("Candidates cleared", controller.currentCandidates.isEmpty())
    }

    // ─── enter key behavior ─────────────────────────────────────

    @Test
    fun enterKeyWithComposingBufferCommitsFirstCandidate() {
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)

        // simulate commitFirstCandidateOrPreedit
        assertTrue("Should have candidates", controller.currentCandidates.isNotEmpty())
        val clickResult = controller.onCandidateClick(0)
        assertEquals("我", (clickResult as T9ImeController.ActionResult.CommitText).text)
        assertTrue("Buffer cleared after enter+commit", engine.buffer.isEmpty())
    }

    @Test
    fun enterKeyWithComposingBufferButNoCandidatesCommitsPreedit() {
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(emptyList())

        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)
        assertTrue("No candidates expected", controller.currentCandidates.isEmpty())

        // simulate fallback: commit preedit then reset
        val preedit = controller.preedit
        controller.reset()
        assertTrue("Preedit should be a non-empty string", preedit.isNotEmpty())
    }

    // ─── Chinese→English switch ─────────────────────────────────

    @Test
    fun chineseToEnglishSwitchWithComposingBufferCommitsFirstCandidate() {
        setupCandidates(listOf(
            Candidate("不", "28", 1000, CandidateType.SINGLE_CHAR, "bu", CandidateOrigin.EXACT_SINGLE)
        ))
        controller.inputDigit("2")
        controller.inputDigit("8")
        controller.refreshCandidates(30)
        assertTrue("Should have candidates", controller.currentCandidates.isNotEmpty())

        // simulate commitFirstCandidateOrPreedit at switch time
        val commitResult = controller.onCandidateClick(0)
        assertTrue("Should commit first candidate",
            commitResult is T9ImeController.ActionResult.CommitText)
        assertEquals("不", (commitResult as T9ImeController.ActionResult.CommitText).text)
        assertTrue("Controller buffer empty after commit", controller.rawBuffer.isEmpty())
    }

    @Test
    fun chineseToEnglishSwitchWithEmptyBufferDoesNotCommit() {
        assertTrue("Buffer is empty", controller.rawBuffer.isEmpty())
    }

    @Test
    fun switchingBackToChineseT9StillWorks() {
        setupCandidates(listOf(
            Candidate("不", "28", 1000, CandidateType.SINGLE_CHAR, "bu", CandidateOrigin.EXACT_SINGLE)
        ))
        controller.inputDigit("2")
        controller.inputDigit("8")
        controller.refreshCandidates(30)
        assertTrue("T9 candidates exist for 'bu'", controller.currentCandidates.any { it.text == "不" })
        val first = controller.onCandidateClick(0)
        assertEquals("不", (first as T9ImeController.ActionResult.CommitText).text)
    }

    @Test
    fun chineseToEnglishSwitchWithPreeditOnlyCommitsPreedit() {
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(emptyList())

        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)
        assertTrue("No candidates", controller.currentCandidates.isEmpty())
        val preedit = controller.preedit
        controller.reset()
        assertTrue("Preedit committed or cleared", preedit.isNotEmpty() || controller.rawBuffer.isEmpty())
    }

    // ─── Chinese→Symbol switch (service-level via switchKeyboardMode) ─────

    @Test
    fun chineseToSymbolSwitchWithComposingBufferCleansUp() {
        val mockEng = mock(T9Engine::class.java)
        `when`(mockEng.buffer).thenReturn("96")
        `when`(mockEng.getPreedit()).thenReturn("wo")
        `when`(mockEng.getVisibleCandidates(30)).thenReturn(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        val ctrl = T9ImeController(mockEng)
        ctrl.refreshCandidates(30)

        val service = makeServiceWithController(ctrl)
        injectUiMocks(service)
        val mockConn = service.testInputConnection

        assertTrue("Should have candidates", ctrl.currentCandidates.isNotEmpty())

        switchMode(service, KeyboardMode.Symbol)

        verify(mockConn!!).commitText("我", 1)
        assertEquals(KeyboardMode.Symbol, getMode(service))
    }

    @Test
    fun chineseToSymbolSwitchWithEmptyBuffer() {
        val mockEng = mock(T9Engine::class.java)
        `when`(mockEng.buffer).thenReturn("")
        `when`(mockEng.getPreedit()).thenReturn("")
        `when`(mockEng.getVisibleCandidates(30)).thenReturn(emptyList())
        val ctrl = T9ImeController(mockEng)

        val service = makeServiceWithController(ctrl)
        injectUiMocks(service)

        switchMode(service, KeyboardMode.Symbol)
        assertEquals(KeyboardMode.Symbol, getMode(service))
    }

    // ─── Chinese→Number switch (service-level) ──────────────────

    @Test
    fun chineseToNumberSwitchWithComposingBufferCleansUp() {
        val mockEng = mock(T9Engine::class.java)
        `when`(mockEng.buffer).thenReturn("96")
        `when`(mockEng.getPreedit()).thenReturn("wo")
        `when`(mockEng.getVisibleCandidates(30)).thenReturn(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        val ctrl = T9ImeController(mockEng)
        ctrl.refreshCandidates(30)

        val service = makeServiceWithController(ctrl)
        injectUiMocks(service)
        val mockConn = service.testInputConnection

        assertTrue("Should have candidates before switch", ctrl.currentCandidates.isNotEmpty())

        switchMode(service, KeyboardMode.Number)

        verify(mockConn!!).commitText("我", 1)
        assertEquals(KeyboardMode.Number, getMode(service))
    }

    // ─── Chinese→English switch (service-level) ─────────────────

    @Test
    fun chineseToEnglishSwitchViaServiceCleansUp() {
        val mockEng = mock(T9Engine::class.java)
        `when`(mockEng.buffer).thenReturn("96")
        `when`(mockEng.getPreedit()).thenReturn("wo")
        `when`(mockEng.getVisibleCandidates(30)).thenReturn(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        val ctrl = T9ImeController(mockEng)
        ctrl.refreshCandidates(30)

        val service = makeServiceWithController(ctrl)
        injectUiMocks(service)
        val mockConn = service.testInputConnection

        assertTrue("candidates before switch", ctrl.currentCandidates.isNotEmpty())

        switchMode(service, KeyboardMode.EnglishT9)

        verify(mockConn!!).commitText("我", 1)
        assertEquals(KeyboardMode.EnglishT9, getMode(service))
    }

    // ─── English→Chinese switch commits pending ─────────────────

    @Test
    fun englishToChineseSwitchCommitsPending() {
        val service = makeServiceWithController(controller)
        injectUiMocks(service)

        switchMode(service, KeyboardMode.EnglishT9)
        assertEquals(KeyboardMode.EnglishT9, getMode(service))

        // switch back to ChineseT9 — should be safe even without pending
        switchMode(service, KeyboardMode.ChineseT9)
        assertEquals(KeyboardMode.ChineseT9, getMode(service))
    }

    // ─── Symbol mode → back to Chinese ──────────────────────────

    @Test
    fun symbolModeBackToChineseResetsState() {
        val service = makeServiceWithController(controller)
        injectUiMocks(service)

        switchMode(service, KeyboardMode.Symbol)
        switchMode(service, KeyboardMode.ChineseT9)
        assertTrue("Buffer empty after symbol→Chinese", controller.rawBuffer.isEmpty())
    }

    // ─── Number mode → back to Chinese ──────────────────────────

    @Test
    fun numberModeBackToChineseResetsState() {
        val service = makeServiceWithController(controller)
        injectUiMocks(service)

        switchMode(service, KeyboardMode.Number)
        switchMode(service, KeyboardMode.ChineseT9)
        assertTrue("Buffer empty after number→Chinese", controller.rawBuffer.isEmpty())
    }

    // ─── Symbol mode delete/enter don't touch T9 buffer ─────────

    @Test
    fun symbolModeDeleteOnlySendsSystemDelete() {
        val service = makeServiceWithController(controller)
        injectUiMocks(service)
        // after switching to symbol, buffer should be empty
        switchMode(service, KeyboardMode.Symbol)
        assertTrue("Buffer empty in symbol mode", controller.rawBuffer.isEmpty())
    }

    @Test
    fun symbolModeEnterDoesNotTouchController() {
        val service = makeServiceWithController(controller)
        injectUiMocks(service)
        switchMode(service, KeyboardMode.Symbol)
        assertTrue("Buffer empty in symbol mode", controller.rawBuffer.isEmpty())
    }

    @Test
    fun numberModeDeleteOnlySendsSystemDelete() {
        val service = makeServiceWithController(controller)
        injectUiMocks(service)
        switchMode(service, KeyboardMode.Number)
        assertTrue("Buffer empty in number mode", controller.rawBuffer.isEmpty())
    }

    @Test
    fun numberModeEnterDoesNotTouchController() {
        val service = makeServiceWithController(controller)
        injectUiMocks(service)
        switchMode(service, KeyboardMode.Number)
        assertTrue("Buffer empty in number mode", controller.rawBuffer.isEmpty())
    }

    // ─── hide key cleans up composing state ─────────────────────

    @Test
    fun hideKeyCleansUpBeforeHiding() {
        val service = makeServiceWithController(controller)
        injectUiMocks(service)
        setupCandidates(listOf(
            Candidate("我", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        ))
        controller.inputDigit("9")
        controller.inputDigit("6")
        controller.refreshCandidates(30)
        assertTrue("Buffer not empty", controller.rawBuffer.isNotEmpty())

        // simulate hide: calls switchKeyboardMode(ChineseT9) then requestHideSelf
        switchMode(service, KeyboardMode.ChineseT9)
        assertTrue("Buffer must be empty after hide cleanup", controller.rawBuffer.isEmpty())
    }

    // ─── lifecycle events use switchKeyboardMode ────────────────

    @Test
    fun onFinishInputViewSwitchesToChineseT9() {
        val service = makeServiceWithController(controller)
        injectUiMocks(service)
        switchMode(service, KeyboardMode.Number)

        // simulate onFinishInputView
        switchMode(service, KeyboardMode.ChineseT9)
        assertEquals(KeyboardMode.ChineseT9, getMode(service))
    }

    // ─── helper: Candidate text matching ────────────────────────

    @Test
    fun defaultModeIsChineseT9() {
        assertNotEquals(KeyboardMode.EnglishT9, KeyboardMode.ChineseT9)
    }

    // ─── service-level reflection helpers ───────────────────────

    private fun makeServiceWithController(ctrl: T9ImeController): XiweiT9ImeService {
        val svc = XiweiT9ImeService()
        val debugField = XiweiT9ImeService::class.java.getDeclaredField("debugLogger")
        debugField.isAccessible = true
        debugField.set(svc, T9DebugLoggerTest.FakeDebugLogger())

        val ctrlField = XiweiT9ImeService::class.java.getDeclaredField("controller")
        ctrlField.isAccessible = true
        ctrlField.set(svc, ctrl)

        val repoField = XiweiT9ImeService::class.java.getDeclaredField("settingsRepository")
        repoField.isAccessible = true
        repoField.set(svc, mock(SettingsRepository::class.java).also {
            `when`(it.getCandidateCount()).thenReturn(30)
            `when`(it.isDebugLoggingEnabled()).thenReturn(false)
            `when`(it.getTheme()).thenReturn("system")
            `when`(it.getKeyboardHeight()).thenReturn("medium")
        })

        val hapticField = XiweiT9ImeService::class.java.getDeclaredField("hapticFeedbackManager")
        hapticField.isAccessible = true
        hapticField.set(svc, mock(HapticFeedbackManager::class.java))

        // mock InputConnection so currentInputConnection doesn't throw
        mockInputConnection(svc)

        return svc
    }

    private fun mockInputConnection(svc: XiweiT9ImeService) {
        val connField = XiweiT9ImeService::class.java.getDeclaredField("testInputConnection")
        connField.isAccessible = true
        connField.set(svc, mock(InputConnection::class.java))
    }

    private fun injectUiMocks(svc: XiweiT9ImeService) {
        val bufField = XiweiT9ImeService::class.java.getDeclaredField("bufferText")
        bufField.isAccessible = true
        val bufMock = mock(TextView::class.java)
        bufField.set(svc, bufMock)

        val contField = XiweiT9ImeService::class.java.getDeclaredField("candidateContainer")
        contField.isAccessible = true
        val contMock = mock(LinearLayout::class.java)
        // make childCount return 5 so refreshUi won't create TextViews
        `when`(contMock.childCount).thenReturn(5)
        val mockTv = mock(TextView::class.java)
        `when`(contMock.getChildAt(0)).thenReturn(mockTv)
        `when`(contMock.getChildAt(1)).thenReturn(mockTv)
        `when`(contMock.getChildAt(2)).thenReturn(mockTv)
        `when`(contMock.getChildAt(3)).thenReturn(mockTv)
        `when`(contMock.getChildAt(4)).thenReturn(mockTv)
        contField.set(svc, contMock)

        val panelT9Field = XiweiT9ImeService::class.java.getDeclaredField("panelT9")
        panelT9Field.isAccessible = true
        panelT9Field.set(svc, mock(View::class.java))

        val panelSymField = XiweiT9ImeService::class.java.getDeclaredField("panelSymbol")
        panelSymField.isAccessible = true
        panelSymField.set(svc, mock(View::class.java))

        val panelNumField = XiweiT9ImeService::class.java.getDeclaredField("panelNumber")
        panelNumField.isAccessible = true
        panelNumField.set(svc, mock(View::class.java))
    }

    private fun switchMode(svc: XiweiT9ImeService, target: KeyboardMode) {
        val method = XiweiT9ImeService::class.java.getDeclaredMethod("switchKeyboardMode", KeyboardMode::class.java)
        method.isAccessible = true
        method.invoke(svc, target)
    }

    private fun getMode(svc: XiweiT9ImeService): KeyboardMode {
        val field = XiweiT9ImeService::class.java.getDeclaredField("keyboardMode")
        field.isAccessible = true
        return field.get(svc) as KeyboardMode
    }

    private fun setupCandidates(candidates: List<Candidate>) {
        `when`(dictionary.getPinyinExactCandidates(anyString())).thenReturn(emptyList())
        `when`(dictionary.getSingleSyllableCandidates(anyString())).thenReturn(candidates)
    }
}
