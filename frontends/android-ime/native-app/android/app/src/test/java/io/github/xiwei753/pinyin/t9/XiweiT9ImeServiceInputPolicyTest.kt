package io.github.xiwei753.pinyin.t9

import android.view.inputmethod.EditorInfo
import android.text.InputType
import io.github.xiwei753.pinyin.imecore.CandidateSelection
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
    fun urlStartInputDoesNotShowChineseCandidates() {
        val (service, handler, engine) = createServiceWithFakeEngine(buffer = "96", preedit = "wo")
        val info = editorInfo(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)

        service.onStartInput(info, false)
        service.handleInputAction(io.github.xiwei753.pinyin.imecore.ImeInputAction.ToggleChineseEnglish)
        val uiState = buildKeyboardUiState(service)

        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)
        assertTrue(engine.cleared)
        assertTrue(uiState.candidateStripState.candidates.isEmpty())
        assertFalse(uiState.candidateStripState.visible)
        assertFalse(uiState.preeditState.visible)
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
        override val readings: List<String> get() = listOf("wo")
        override val activeReading: String? get() = if (mutableBuffer.isEmpty()) null else "wo"
        override fun getPreedit(): String = mutablePreedit
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
        override fun setActiveReading(reading: String): Boolean = false
    }
}
