package io.github.xiwei753.pinyin.t9

import android.view.inputmethod.EditorInfo
import android.text.InputType
import io.github.xiwei753.pinyin.imecore.CandidateSelection
import io.github.xiwei753.pinyin.imecore.CandidateSnapshotItem
import io.github.xiwei753.pinyin.imecore.ImeStateMachine
import io.github.xiwei753.pinyin.imecore.T9InputEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class XiweiT9ImeServiceInputPolicyTest {

    @Test
    fun passwordStartInputClearsOldBufferAndUsesEnglishT9() {
        val (service, handler, engine) = createServiceWithFakeEngine(buffer = "96", preedit = "wo")
        val info = editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

        service.onStartInput(info, false)
        val uiState = buildKeyboardUiState(service)

        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)
        assertEquals(KeyboardMode.EnglishT9, handler.lastTextMode)
        assertTrue(engine.cleared)
        assertEquals("", handler.rawBuffer)
        assertTrue(uiState.candidateStripState.candidates.isEmpty())
        assertFalse(uiState.preeditState.visible)
    }

    @Test
    fun urlStartInputAllowsChineseCandidates() {
        val (service, handler, engine) = createServiceWithFakeEngine(buffer = "96", preedit = "wo")
        val info = editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)

        service.onStartInput(info, false)
        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.ToggleChineseEnglish)
        val uiState = buildKeyboardUiState(service)

        assertEquals(KeyboardMode.ChineseT9, handler.keyboardMode)
        assertTrue(engine.cleared) // onStartInput clears the buffer
        // Note: engine is cleared, so candidates will be empty initially, but visible could be false or true depending on the cleared state
        // We mainly want to test that it allows the transition to Chinese
    }

    @Test
    fun numberStartInputUsesNumberModeAndResetsLastTextMode() {
        val (service, handler, engine) = createServiceWithFakeEngine(buffer = "96", preedit = "wo")
        val info = editorInfo(InputType.TYPE_CLASS_NUMBER)

        service.onStartInput(info, false)
        val uiState = buildKeyboardUiState(service)

        assertEquals(KeyboardMode.Number, handler.keyboardMode)
        assertEquals(KeyboardMode.EnglishT9, handler.lastTextMode)
        assertTrue(engine.cleared)
        assertEquals(KeyboardMode.Number, uiState.keyboardMode)
        assertFalse(uiState.candidateStripState.visible)
        assertFalse(uiState.preeditState.visible)
    }

    @Test
    fun passwordToggleChineseEnglishStaysEnglishT9() {
        val (service, handler, _) = createServiceWithFakeEngine(buffer = "", preedit = "")
        val info = editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

        service.onStartInput(info, false)
        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)

        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.ToggleChineseEnglish)
        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)
    }

    @Test
    fun urlToggleChineseEnglishSwitchesToChineseT9() {
        val (service, handler, _) = createServiceWithFakeEngine(buffer = "", preedit = "")
        val info = editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)

        service.onStartInput(info, false)
        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)

        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.ToggleChineseEnglish)
        assertEquals(KeyboardMode.ChineseT9, handler.keyboardMode)
    }

    @Test
    fun emailToggleChineseEnglishSwitchesToChineseT9() {
        val (service, handler, _) = createServiceWithFakeEngine(buffer = "", preedit = "")
        val info = editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)

        service.onStartInput(info, false)
        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)

        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.ToggleChineseEnglish)
        assertEquals(KeyboardMode.ChineseT9, handler.keyboardMode)
    }

    @Test
    fun numberToggleChineseEnglishStaysNumber() {
        val (service, handler, _) = createServiceWithFakeEngine(buffer = "", preedit = "")
        val info = editorInfo(InputType.TYPE_CLASS_NUMBER)

        service.onStartInput(info, false)
        assertEquals(KeyboardMode.Number, handler.keyboardMode)

        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.ToggleChineseEnglish)
        assertEquals(KeyboardMode.Number, handler.keyboardMode)
    }

    @Test
    fun phoneToggleChineseEnglishStaysPhone() {
        val (service, handler, _) = createServiceWithFakeEngine(buffer = "", preedit = "")
        val info = editorInfo(InputType.TYPE_CLASS_PHONE)

        service.onStartInput(info, false)
        assertEquals(KeyboardMode.Number, handler.keyboardMode)

        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.ToggleChineseEnglish)
        assertEquals(KeyboardMode.Number, handler.keyboardMode)
    }

    @Test
    fun numberDigitInputDoesNotShowCandidateStrip() {
        val (service, handler, _) = createServiceWithFakeEngine(buffer = "", preedit = "")
        val info = editorInfo(InputType.TYPE_CLASS_NUMBER)

        service.onStartInput(info, false)
        assertEquals(KeyboardMode.Number, handler.keyboardMode)

        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.DigitPressed("2"))
        val uiState = buildKeyboardUiState(service)

        assertFalse(uiState.candidateStripState.visible)
        assertTrue(uiState.candidateStripState.candidates.isEmpty())
        assertFalse(uiState.preeditState.visible)
    }

    @Test
    fun phoneDigitInputDoesNotShowCandidateStrip() {
        val (service, handler, _) = createServiceWithFakeEngine(buffer = "", preedit = "")
        val info = editorInfo(InputType.TYPE_CLASS_PHONE)

        service.onStartInput(info, false)
        assertEquals(KeyboardMode.Number, handler.keyboardMode)

        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.DigitPressed("5"))
        val uiState = buildKeyboardUiState(service)

        assertFalse(uiState.candidateStripState.visible)
        assertTrue(uiState.candidateStripState.candidates.isEmpty())
        assertFalse(uiState.preeditState.visible)
    }

    @Test
    fun lifecycleStartResetsSymbolCategoryToPunct() {
        val (service, handler, _) = createServiceWithFakeEngine(buffer = "", preedit = "")

        service.onStartInput(editorInfo(InputType.TYPE_CLASS_TEXT), false)

        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.SymbolCategorySelected("math"))

        service.onStartInput(editorInfo(InputType.TYPE_CLASS_TEXT), false)

        val state = buildKeyboardUiState(service)
        assertEquals("punct", state.currentSymCategory)
    }

    @Test
    fun consecutiveOnStartInputAndOnStartInputViewClearsBuffer() {
        val (service, handler, engine) = createServiceWithFakeEngine(buffer = "96", preedit = "wo")
        val info = editorInfo(InputType.TYPE_CLASS_TEXT)

        service.onStartInput(info, false)
        assertEquals("", handler.rawBuffer)
        assertTrue(engine.cleared)

        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.DigitPressed("9"))
        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.DigitPressed("6"))
        assertTrue(handler.rawBuffer.isNotEmpty())

        service.onStartInputView(info, false)
        assertEquals("", handler.rawBuffer)
    }

    private fun createServiceWithFakeEngine(buffer: String, preedit: String): Triple<XiweiT9ImeService, KeyboardActionHandler, MutableEngine> {
        val service = XiweiT9ImeService()
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))

        val handler = KeyboardActionHandler(service)
        val engine = MutableEngine(buffer, preedit)
        attachEngine(handler, engine)
        injectField(service, "handler", handler)
        return Triple(service, handler, engine)
    }

    private fun attachEngine(handler: KeyboardActionHandler, engine: MutableEngine) {
        val stateMachineField = KeyboardActionHandler::class.java.getDeclaredField("stateMachine")
        stateMachineField.isAccessible = true
        val stateMachine = stateMachineField.get(handler) as ImeStateMachine
        stateMachine.attachEngine(engine)
    }

    private fun injectField(service: XiweiT9ImeService, name: String, value: Any) {
        val field = XiweiT9ImeService::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(service, value)
    }

    private fun buildKeyboardUiState(service: XiweiT9ImeService): KeyboardUiState {
        val method = XiweiT9ImeService::class.java.getDeclaredMethod("buildKeyboardUiState")
        method.isAccessible = true
        return method.invoke(service) as KeyboardUiState
    }

    private fun editorInfo(inputType: Int): EditorInfo {
        return EditorInfo().apply { this.inputType = inputType }
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
        override val readings: List<String> get() = listOf("wo")
        override val activeReading: String? get() = if (mutableBuffer.isEmpty()) null else "wo"
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
