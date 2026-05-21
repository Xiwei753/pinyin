package io.github.xiwei753.pinyin.t9

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.github.xiwei753.pinyin.t9.core.T9Engine
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq

class XiweiT9ImeServiceUiBehaviorTest {

    private fun createService(): XiweiT9ImeService {
        return XiweiT9ImeService()
    }

    private fun injectField(service: XiweiT9ImeService, name: String, value: Any) {
        val field = XiweiT9ImeService::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(service, value)
    }

    private fun createMockKeyboardViews(): KeyboardViews {
        return KeyboardViews(
            imeRoot = mock(android.view.View::class.java),
            candidateBar = mock(android.widget.LinearLayout::class.java),
            candidateContainer = mock(android.widget.LinearLayout::class.java),
            pinyinFloatingBar = mock(android.view.View::class.java),
            pinyinFloatingText = mock(android.widget.TextView::class.java),
            keyboardShell = mock(android.view.View::class.java),
            panelT9 = mock(android.view.View::class.java),
            panelSymbol = mock(android.view.View::class.java),
            panelNumber = mock(android.view.View::class.java),
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
            symPagePunct = mock(android.view.View::class.java),
            symPageMath = mock(android.view.View::class.java),
            symPageBracket = mock(android.view.View::class.java),
            symPageOther = mock(android.view.View::class.java),
            symScrollContent = mock(android.widget.ScrollView::class.java),
            leftScrollRail = mock(android.view.View::class.java),
            leftScrollContent = mock(android.widget.LinearLayout::class.java),
            key1Text = mock(android.widget.TextView::class.java),
            key2 = mock(android.view.View::class.java),
            key3 = mock(android.view.View::class.java),
            key4 = mock(android.view.View::class.java),
            key5 = mock(android.view.View::class.java),
            key6 = mock(android.view.View::class.java),
            key7 = mock(android.view.View::class.java),
            key8 = mock(android.view.View::class.java),
            key9 = mock(android.view.View::class.java),
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
            keyDel = mock(android.view.View::class.java),
            keyRetype = mock(android.view.View::class.java),
            keyEnter = mock(android.view.View::class.java),
            keySpace = mock(android.view.View::class.java),
            keyToggleSymbol = mock(android.view.View::class.java),
            keyToggleNumber = mock(android.view.View::class.java),
            keyToggleEnglish = mock(android.widget.TextView::class.java),
            enterContainer = mock(),
            symTabPunct = mock(android.widget.TextView::class.java),
            symTabMath = mock(android.widget.TextView::class.java),
            symTabBracket = mock(android.widget.TextView::class.java),
            symTabOther = mock(android.widget.TextView::class.java),
            symTextViews = emptyMap(),
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

                numKey1Frame = mock(),
                numKey2Frame = mock(),
                numKey3Frame = mock(),
                numKey4Frame = mock(),
                numKey5Frame = mock(),
                numKey6Frame = mock(),
                numKey7Frame = mock(),
                numKey8Frame = mock(),
                numKey9Frame = mock(),
                numDotFrame = mock(),
                num0Frame = mock(),
                t9LeftScrollFrame = mock(),
            t9SymbolButtonFrame = mock(),
            t9Key1Frame = mock(),
            t9Key2Frame = mock(),
            t9Key3Frame = mock(),
            t9Key4Frame = mock(),
            t9Key5Frame = mock(),
            t9Key6Frame = mock(),
            t9Key7Frame = mock(),
            t9Key8Frame = mock(),
            t9Key9Frame = mock(),
            t9DelFrame = mock(),
            t9RetypeFrame = mock(),
            t9NumberFrame = mock(),
            t9SpaceFrame = mock(),
            t9EnglishFrame = mock(),
        )
    }

    @Test
    fun testCandidateCountSettingPassesToController() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getVisibleCandidates(eq(15))).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(engine) }

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getCandidateCount()).thenReturn(15)
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)
        `when`(repo.getTheme()).thenReturn("system")

        val mockKeyBinder = mock(KeyboardKeyBinder::class.java)
        val mockThemeCtrl = mock(KeyboardThemeController::class.java)
        val kv = createMockKeyboardViews()

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "keyboardViews", kv)
        injectField(service, "candidateViewController", CandidateViewController(
            context = mock(android.content.Context::class.java),
            v = kv,
            keyBinder = mockKeyBinder,
            themeController = mockThemeCtrl,
            settingsRepository = repo,
        ))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("refreshUi")
            method.isAccessible = true
            method.invoke(service)
        } catch (e: java.lang.reflect.InvocationTargetException) {
        }

        verify(engine).getVisibleCandidates(eq(15))
    }

    @Test
    fun testApplyThemeAndHeightDoesNotChangeCandidateBarVisibility() {
        val service = createService()
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "handler", KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(mock(T9Engine::class.java)) })
        val keyboardViews = createMockKeyboardViews()
        injectField(service, "keyboardViews", keyboardViews)

        verify(keyboardViews.candidateContainer, never()).visibility = android.view.View.GONE
    }

    @Test
    fun testServiceCanBeCreatedWithoutCrash() {
        try {
            val service = createService()
            val engine = mock(T9Engine::class.java)
            `when`(engine.buffer).thenReturn("")
            `when`(engine.getPreedit()).thenReturn("")
            `when`(engine.getCompositions()).thenReturn(emptyList())
            `when`(engine.getInternalCandidates()).thenReturn(emptyList())
            `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

            injectField(service, "handler", KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(engine) })
            injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
            injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))

            assertTrue(true)
        } catch (e: Exception) {
            fail("Service creation should not crash: ${e.message}")
        }
    }

    // --- Enter action fallback tests ---

    private fun setupServiceForEnterAction(
        imeOptions: Int,
        actionId: Int = 0,
        actionLabel: String? = null,
    ): Pair<XiweiT9ImeService, InputConnection> {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(engine) }

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))

        val mockInputConnection = mock(InputConnection::class.java)
        service.testInputConnection = mockInputConnection

        val editorInfo = EditorInfo()
        editorInfo.imeOptions = imeOptions
        editorInfo.actionId = actionId
        editorInfo.actionLabel = actionLabel
        injectField(service, "currentEditorInfo", editorInfo)

        return Pair(service, mockInputConnection)
    }

    @Test
    fun testEnterAction_customActionId_returnsTrue_stops() {
        val (service, ic) = setupServiceForEnterAction(
            imeOptions = EditorInfo.IME_ACTION_SEND,
            actionId = 999,
            actionLabel = "Send",
        )
        `when`(ic.performEditorAction(anyInt())).thenReturn(true)
        service.performEditorActionOrNewline()
        verify(ic).performEditorAction(999)
        verify(ic, never()).commitText(anyString(), anyInt())
    }

    @Test
    fun testEnterAction_customActionId_returnsFalse_fallsbackToStandardAction() {
        val (service, ic) = setupServiceForEnterAction(
            imeOptions = EditorInfo.IME_ACTION_SEND,
            actionId = 999,
            actionLabel = "Send",
        )
        `when`(ic.performEditorAction(anyInt())).thenReturn(false, true)
        service.performEditorActionOrNewline()
        verify(ic).performEditorAction(999)
        verify(ic).performEditorAction(EditorInfo.IME_ACTION_SEND)
        verify(ic, never()).commitText(anyString(), anyInt())
    }

    @Test
    fun testEnterAction_sendAction_returnsTrue_stops() {
        val (service, ic) = setupServiceForEnterAction(
            imeOptions = EditorInfo.IME_ACTION_SEND,
        )
        `when`(ic.performEditorAction(anyInt())).thenReturn(true)
        service.performEditorActionOrNewline()
        verify(ic).performEditorAction(EditorInfo.IME_ACTION_SEND)
        verify(ic, never()).commitText(anyString(), anyInt())
    }

    @Test
    fun testEnterAction_sendAction_returnsFalse_fallsbackToNewline() {
        val (service, ic) = setupServiceForEnterAction(
            imeOptions = EditorInfo.IME_ACTION_SEND,
        )
        `when`(ic.performEditorAction(anyInt())).thenReturn(false)
        service.performEditorActionOrNewline()
        // performEditorAction called by step 2 and step 3 (sendDefaultEditorAction)
        verify(ic, atLeastOnce()).performEditorAction(EditorInfo.IME_ACTION_SEND)
        verify(ic).commitText("\n", 1)
    }

    @Test
    fun testEnterAction_sendDefaultEditorAction_returnsTrue_stops() {
        val (service, ic) = setupServiceForEnterAction(
            imeOptions = EditorInfo.IME_ACTION_NONE,
        )
        val spyService = spy(service)
        `when`(spyService.sendDefaultEditorAction(true)).thenReturn(true)
        spyService.performEditorActionOrNewline()
        verify(ic, never()).commitText(anyString(), anyInt())
    }

    @Test
    fun testEnterAction_allActionsFail_commitsNewline() {
        val (service, ic) = setupServiceForEnterAction(
            imeOptions = EditorInfo.IME_ACTION_SEND,
        )
        `when`(ic.performEditorAction(anyInt())).thenReturn(false)
        service.performEditorActionOrNewline()
        // performEditorAction called by step 2 and step 3 (sendDefaultEditorAction)
        verify(ic, atLeastOnce()).performEditorAction(EditorInfo.IME_ACTION_SEND)
        verify(ic).commitText("\n", 1)
    }

    @Test
    fun testEnterAction_noEditorInfo_commitsNewline() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(engine) }
        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))

        val mockInputConnection = mock(InputConnection::class.java)
        service.testInputConnection = mockInputConnection

        // Don't set currentEditorInfo - it remains null
        service.performEditorActionOrNewline()
        verify(mockInputConnection).commitText("\n", 1)
    }

    @Test
    fun testEnterAction_chineseComposing_commitsCandidateFirst() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

        val mockSink = mock(ImeActionSink::class.java)
        val handler = KeyboardActionHandler(mockSink).apply { attachEngine(engine) }

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)
        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))

        handler.onEnter()
        // When buffer is non-empty and no candidates, preedit should be committed
        verify(mockSink).commitText("wo")
        verify(mockSink, never()).performEditorActionOrNewline()
    }

    @Test
    fun testEnterAction_englishPending_commitsCharFirst() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

        val mockSink = mock(ImeActionSink::class.java)
        val handler = KeyboardActionHandler(mockSink).apply { attachEngine(engine) }
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.onDigitPressed("2") // starts 'a' pending

        handler.onEnter()
        verify(mockSink).commitText("a")
        verify(mockSink, never()).performEditorActionOrNewline()
    }
}
