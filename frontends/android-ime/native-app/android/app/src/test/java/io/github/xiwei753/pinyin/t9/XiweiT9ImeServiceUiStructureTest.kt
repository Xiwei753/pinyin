package io.github.xiwei753.pinyin.t9

import android.view.View
import io.github.xiwei753.pinyin.t9.core.T9Engine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyInt

class XiweiT9ImeServiceUiStructureTest {

    private fun createService(): XiweiT9ImeService {
        return XiweiT9ImeService()
    }

    private fun injectField(service: XiweiT9ImeService, name: String, value: Any) {
        val field = XiweiT9ImeService::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(service, value)
    }

    private fun createMockKeyboardViews(
        floatingBar: View = mock(),
        floatingText: android.widget.TextView = mock(),
        candidateContainer: android.widget.LinearLayout = mock(),
    ): KeyboardViews {
        return KeyboardViews(
            imeRoot = mock(),
            candidateBar = mock(android.widget.LinearLayout::class.java),
            candidateContainer = candidateContainer,
            pinyinFloatingBar = floatingBar,
            pinyinFloatingText = floatingText,
            keyboardShell = mock(),
            panelT9 = mock(),
            panelSymbol = mock(),
            panelNumber = mock(),
            readingTextViews = listOf(
                mock(android.widget.TextView::class.java),
                mock(android.widget.TextView::class.java),
                mock(android.widget.TextView::class.java),
                mock(android.widget.TextView::class.java),
            ),
            punctTextViews = listOf(
                mock(android.widget.TextView::class.java),
                mock(android.widget.TextView::class.java),
                mock(android.widget.TextView::class.java),
                mock(android.widget.TextView::class.java),
            ),
            symPagePunct = mock(),
            symPageMath = mock(),
            symPageBracket = mock(),
            symPageOther = mock(),
            symScrollContent = mock(android.widget.ScrollView::class.java),
            leftScrollRail = mock(),
            leftScrollContent = mock(android.widget.LinearLayout::class.java),
            key1Text = mock(android.widget.TextView::class.java),
            key2 = mock(),
            key3 = mock(),
            key4 = mock(),
            key5 = mock(),
            key6 = mock(),
            key7 = mock(),
            key8 = mock(),
            key9 = mock(),
            key2Number = mock(android.widget.TextView::class.java),
            key3Number = mock(android.widget.TextView::class.java),
            key4Number = mock(android.widget.TextView::class.java),
            key5Number = mock(android.widget.TextView::class.java),
            key6Number = mock(android.widget.TextView::class.java),
            key7Number = mock(android.widget.TextView::class.java),
            key8Number = mock(android.widget.TextView::class.java),
            key9Number = mock(android.widget.TextView::class.java),
            key2Letters = mock(android.widget.TextView::class.java),
            key3Letters = mock(android.widget.TextView::class.java),
            key4Letters = mock(android.widget.TextView::class.java),
            key5Letters = mock(android.widget.TextView::class.java),
            key6Letters = mock(android.widget.TextView::class.java),
            key7Letters = mock(android.widget.TextView::class.java),
            key8Letters = mock(android.widget.TextView::class.java),
            key9Letters = mock(android.widget.TextView::class.java),
            keyDel = mock(),
            keyRetype = mock(),
            keyEnter = mock(),
            keySpace = mock(),
            keyToggleSymbol = mock(),
            keyToggleNumber = mock(),
            keyToggleEnglish = mock(android.widget.TextView::class.java),
            enterContainer = mock(),
            symTabPunct = mock(android.widget.TextView::class.java),
            symTabMath = mock(android.widget.TextView::class.java),
            symTabBracket = mock(android.widget.TextView::class.java),
            symTabOther = mock(android.widget.TextView::class.java),
            symTextViews = emptyMap(),
            symBack = mock(android.widget.TextView::class.java),
            symNumber = mock(android.widget.TextView::class.java),
            symDel = mock(),
            symEnter = mock(),
            symHide = mock(),
            num0 = mock(android.widget.TextView::class.java),
            num1 = mock(android.widget.TextView::class.java),
            num2 = mock(android.widget.TextView::class.java),
            num3 = mock(android.widget.TextView::class.java),
            num4 = mock(android.widget.TextView::class.java),
            num5 = mock(android.widget.TextView::class.java),
            num6 = mock(android.widget.TextView::class.java),
            num7 = mock(android.widget.TextView::class.java),
            num8 = mock(android.widget.TextView::class.java),
            num9 = mock(android.widget.TextView::class.java),
            numDot = mock(android.widget.TextView::class.java),
            numDel = mock(),
            numBack = mock(),
            numSymbol = mock(),
            numHide = mock(),
            numEnter = mock(),
            rowBand1 = mock(),
            rowBand2 = mock(),
            rowBand3 = mock(),
            rowBand4 = mock(),
        )
    }

    private fun createCandidateViewController(
        kv: KeyboardViews,
        repo: SettingsRepository,
    ): CandidateViewController {
        val mockKeyBinder = mock(KeyboardKeyBinder::class.java)
        val mockThemeCtrl = mock(KeyboardThemeController::class.java)
        return CandidateViewController(
            context = mock(android.content.Context::class.java),
            v = kv,
            keyBinder = mockKeyBinder,
            themeController = mockThemeCtrl,
            settingsRepository = repo,
        )
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
        val kv = createMockKeyboardViews(
            floatingBar = floatingBar,
            floatingText = floatingText,
            candidateContainer = candidateContainer,
        )

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "keyboardViews", kv)
        injectField(service, "candidateViewController", createCandidateViewController(kv, repo))

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
        val kv = createMockKeyboardViews(
            floatingBar = floatingBar,
            floatingText = floatingText,
            candidateContainer = candidateContainer,
        )

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "keyboardViews", kv)
        injectField(service, "candidateViewController", createCandidateViewController(kv, repo))

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
        val kv = createMockKeyboardViews(
            floatingBar = floatingBar,
            floatingText = floatingText,
            candidateContainer = candidateContainer,
        )

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "keyboardViews", kv)
        injectField(service, "candidateViewController", createCandidateViewController(kv, repo))

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
        val kv = createMockKeyboardViews(
            floatingBar = floatingBar,
            floatingText = floatingText,
            candidateContainer = candidateContainer,
        )

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "keyboardViews", kv)
        injectField(service, "candidateViewController", createCandidateViewController(kv, repo))

        val method = XiweiT9ImeService::class.java.getDeclaredMethod("refreshUi")
        method.isAccessible = true
        method.invoke(service)

        verify(floatingBar).visibility = View.GONE
    }

    @Test
    fun testOnStartInputViewAppliesThemeAndHeight() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply {
            attachEngine(engine)
        }

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getKeyboardHeight()).thenReturn("normal")
        `when`(repo.getTheme()).thenReturn("system")
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)

        val floatingBar = mock(View::class.java)
        val floatingText = mock(android.widget.TextView::class.java)
        val candidateContainer = mock(android.widget.LinearLayout::class.java)
        val kv = createMockKeyboardViews(
            floatingBar = floatingBar,
            floatingText = floatingText,
            candidateContainer = candidateContainer,
        )

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "keyboardViews", kv)
        injectField(service, "themeController", mock(KeyboardThemeController::class.java))
        injectField(service, "heightController", mock(KeyboardHeightController::class.java))

        val method = XiweiT9ImeService::class.java.getDeclaredMethod("applyThemeAndHeight")
        method.isAccessible = true
        method.invoke(service)
    }
}
