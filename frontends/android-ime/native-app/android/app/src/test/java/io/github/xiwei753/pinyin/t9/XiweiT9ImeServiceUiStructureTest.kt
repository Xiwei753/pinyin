package io.github.xiwei753.pinyin.t9

import android.view.View
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.core.T9Engine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq

class XiweiT9ImeServiceUiStructureTest {

    private fun createService(): XiweiT9ImeService {
        return XiweiT9ImeService()
    }

    private fun injectField(service: XiweiT9ImeService, name: String, value: Any) {
        val field = XiweiT9ImeService::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(service, value)
    }

    @Test
    fun testRefreshUi_hidesPreeditOnSymbolMode() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply {
            attachEngine(engine)
            switchKeyboardMode(KeyboardMode.Symbol)
        }

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getCandidateCount()).thenReturn(15)
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)
        `when`(repo.getTheme()).thenReturn("system")

        val floatingBar = mock(View::class.java)
        val floatingText = mock(android.widget.TextView::class.java)
        val candidateContainer = mock(android.widget.LinearLayout::class.java)

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "pinyinFloatingBar", floatingBar)
        injectField(service, "pinyinFloatingText", floatingText)
        injectField(service, "candidateContainer", candidateContainer)

        val method = XiweiT9ImeService::class.java.getDeclaredMethod("refreshUi")
        method.isAccessible = true
        method.invoke(service)

        verify(floatingBar).visibility = View.GONE
    }

    @Test
    fun testRefreshUi_hidesPreeditOnNumberMode() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply {
            attachEngine(engine)
            switchKeyboardMode(KeyboardMode.Number)
        }

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getCandidateCount()).thenReturn(15)
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)
        `when`(repo.getTheme()).thenReturn("system")

        val floatingBar = mock(View::class.java)
        val floatingText = mock(android.widget.TextView::class.java)
        val candidateContainer = mock(android.widget.LinearLayout::class.java)

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "pinyinFloatingBar", floatingBar)
        injectField(service, "pinyinFloatingText", floatingText)
        injectField(service, "candidateContainer", candidateContainer)

        val method = XiweiT9ImeService::class.java.getDeclaredMethod("refreshUi")
        method.isAccessible = true
        method.invoke(service)

        verify(floatingBar).visibility = View.GONE
    }

    @Test
    fun testRefreshUi_showsPreeditOnChineseT9Mode() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply {
            attachEngine(engine)
        }

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getCandidateCount()).thenReturn(15)
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)
        `when`(repo.getTheme()).thenReturn("system")

        val floatingBar = mock(View::class.java)
        val floatingText = mock(android.widget.TextView::class.java)
        val candidateContainer = mock(android.widget.LinearLayout::class.java)

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "pinyinFloatingBar", floatingBar)
        injectField(service, "pinyinFloatingText", floatingText)
        injectField(service, "candidateContainer", candidateContainer)

        val method = XiweiT9ImeService::class.java.getDeclaredMethod("refreshUi")
        method.isAccessible = true
        method.invoke(service)

        verify(floatingBar).visibility = View.VISIBLE
    }

    @Test
    fun testRefreshUi_hidesPreeditWhenBufferEmpty() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply {
            attachEngine(engine)
        }

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getCandidateCount()).thenReturn(15)
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)
        `when`(repo.getTheme()).thenReturn("system")

        val floatingBar = mock(View::class.java)
        val floatingText = mock(android.widget.TextView::class.java)
        val candidateContainer = mock(android.widget.LinearLayout::class.java)

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "pinyinFloatingBar", floatingBar)
        injectField(service, "pinyinFloatingText", floatingText)
        injectField(service, "candidateContainer", candidateContainer)

        val method = XiweiT9ImeService::class.java.getDeclaredMethod("refreshUi")
        method.isAccessible = true
        method.invoke(service)

        verify(floatingBar).visibility = View.GONE
    }
}
