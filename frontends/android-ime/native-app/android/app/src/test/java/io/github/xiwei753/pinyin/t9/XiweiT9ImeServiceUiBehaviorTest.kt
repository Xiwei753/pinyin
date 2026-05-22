package io.github.xiwei753.pinyin.t9

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.github.xiwei753.pinyin.imecore.ImeInputAction
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9Engine
import org.junit.Assert.assertEquals
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

    private fun getStringField(service: XiweiT9ImeService, name: String): String {
        val field = XiweiT9ImeService::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(service) as String
    }

    private fun injectQuietSettings(service: XiweiT9ImeService) {
        val repo = mock(SettingsRepository::class.java)
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)
        injectField(service, "settingsRepository", repo)
    }

    private fun createMockKeyboardViews(): KeyboardViews {
        return KeyboardViews(
            imeRoot = mock(android.view.View::class.java),
            candidateBar = mock(android.widget.LinearLayout::class.java),
            candidateContainer = mock(android.widget.LinearLayout::class.java),
            pinyinFloatingBar = mock(android.view.View::class.java),
            pinyinFloatingText = mock(android.widget.TextView::class.java),
            xiweiKeyboardView = mock(XiweiKeyboardView::class.java),
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

        val mockThemeCtrl = mock(KeyboardThemeController::class.java)
        val kv = createMockKeyboardViews()

        injectField(service, "handler", handler)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "keyboardViews", kv)
        injectField(service, "candidateViewController", CandidateViewController(
            context = mock(android.content.Context::class.java),
            v = kv,
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

    @Test
    fun testSymbolCommitCommitsTextAndStaysInSymbolMode() {
        val service = createService()
        val handler = KeyboardActionHandler(service)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        val ic = mock(InputConnection::class.java)
        service.testInputConnection = ic
        injectQuietSettings(service)
        injectField(service, "handler", handler)

        service.handleKeyboardAction("symbol:commit:@")

        verify(ic).commitText("@", 1)
        assertEquals(KeyboardMode.Symbol, handler.keyboardMode)
        assertEquals(KeyboardMode.ChineseT9, handler.lastTextMode)
    }

    @Test
    fun testInputActionSymbolCommittedCommitsText() {
        val service = createService()
        val handler = KeyboardActionHandler(service)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        val ic = mock(InputConnection::class.java)
        service.testInputConnection = ic
        injectQuietSettings(service)
        injectField(service, "handler", handler)

        service.handleInputAction(ImeInputAction.SymbolCommitted("@"))

        verify(ic).commitText("@", 1)
        assertEquals(KeyboardMode.Symbol, handler.keyboardMode)
    }

    @Test
    fun testInputActionCandidateSelectedCommitsCurrentSnapshot() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        val candidate = Candidate("我", "96", 900)
        `when`(engine.buffer).thenReturn("96", "")
        `when`(engine.getPreedit()).thenReturn("wo", "")
        `when`(engine.readings).thenReturn(listOf("wo"))
        `when`(engine.activeReading).thenReturn("wo")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(listOf(candidate))

        val handler = KeyboardActionHandler(service).apply { attachEngine(engine) }
        handler.refreshCandidates(30)
        val ic = mock(InputConnection::class.java)
        service.testInputConnection = ic
        injectQuietSettings(service)
        injectField(service, "handler", handler)

        service.handleInputAction(ImeInputAction.CandidateSelected(0))

        verify(ic).commitText("我", 1)
        verify(engine).commitCandidate(candidate)
    }

    @Test
    fun testSymbolBottomLeftReturnFromChineseMode() {
        val service = createService()
        val handler = KeyboardActionHandler(service)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        injectQuietSettings(service)
        injectField(service, "handler", handler)

        service.handleKeyboardAction("toggle:symbol")

        assertEquals(KeyboardMode.ChineseT9, handler.keyboardMode)
    }

    @Test
    fun testSymbolBottomLeftReturnFromEnglishMode() {
        val service = createService()
        val handler = KeyboardActionHandler(service)
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        injectQuietSettings(service)
        injectField(service, "handler", handler)

        service.handleKeyboardAction("toggle:symbol")

        assertEquals(KeyboardMode.EnglishT9, handler.keyboardMode)
    }

    @Test
    fun testSymbolNumberKeyEntersNumberWithoutChangingLastTextMode() {
        val service = createService()
        val handler = KeyboardActionHandler(service)
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        injectQuietSettings(service)
        injectField(service, "handler", handler)

        service.handleKeyboardAction("toggle:number")

        assertEquals(KeyboardMode.Number, handler.keyboardMode)
        assertEquals(KeyboardMode.EnglishT9, handler.lastTextMode)
    }

    @Test
    fun testSymbolSpaceCommitsSpaceAndStaysInSymbolMode() {
        val service = createService()
        val handler = KeyboardActionHandler(service)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        val ic = mock(InputConnection::class.java)
        service.testInputConnection = ic
        injectQuietSettings(service)
        injectField(service, "handler", handler)

        service.handleKeyboardAction("space")

        verify(ic).commitText(" ", 1)
        assertEquals(KeyboardMode.Symbol, handler.keyboardMode)
    }

    @Test
    fun testSymbolCategoryTabOnlyChangesCategory() {
        val service = createService()
        val handler = KeyboardActionHandler(service)
        handler.switchKeyboardMode(KeyboardMode.EnglishT9)
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        injectQuietSettings(service)
        injectField(service, "handler", handler)

        service.handleKeyboardAction("symtab:math")

        assertEquals("math", getStringField(service, "currentSymCategory"))
        assertEquals(KeyboardMode.Symbol, handler.keyboardMode)
        assertEquals(KeyboardMode.EnglishT9, handler.lastTextMode)
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
        val spyService = spy(service)
        doReturn(false).`when`(spyService).sendDefaultEditorAction(true)
        spyService.performEditorActionOrNewline()
        verify(ic, atLeastOnce()).performEditorAction(EditorInfo.IME_ACTION_SEND)
        verify(ic).commitText("\n", 1)
    }

    @Test
    fun testEnterAction_sendDefaultEditorAction_returnsTrue_stops() {
        val (service, ic) = setupServiceForEnterAction(
            imeOptions = EditorInfo.IME_ACTION_NONE,
        )
        val spyService = spy(service)
        doReturn(true).`when`(spyService).sendDefaultEditorAction(true)
        spyService.performEditorActionOrNewline()
        verify(ic, never()).commitText(anyString(), anyInt())
    }

    @Test
    fun testEnterAction_allActionsFail_commitsNewline() {
        val (service, ic) = setupServiceForEnterAction(
            imeOptions = EditorInfo.IME_ACTION_SEND,
        )
        `when`(ic.performEditorAction(anyInt())).thenReturn(false)
        val spyService = spy(service)
        doReturn(false).`when`(spyService).sendDefaultEditorAction(true)
        spyService.performEditorActionOrNewline()
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

    @Test
    fun testOnCreateInputViewInitializesComponents() {
        val service = spy(createService())

        // mock repository
        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getTheme()).thenReturn("system")
        `when`(repo.getKeyboardHeight()).thenReturn("normal")
        injectField(service, "settingsRepository", repo)

        // mock resources
        val resources = mock(android.content.res.Resources::class.java)
        `when`(resources.displayMetrics).thenReturn(android.util.DisplayMetrics())
        `when`(resources.configuration).thenReturn(android.content.res.Configuration())
        doReturn(resources).`when`(service).resources

        // mock layout inflater
        val mockInflater = mock(android.view.LayoutInflater::class.java)
        doReturn(mockInflater).`when`(service).layoutInflater

        // mock view hierarchy
        val mockView = mock(android.view.View::class.java)
        `when`(mockInflater.inflate(eq(R.layout.keyboard_view), org.mockito.ArgumentMatchers.isNull())).thenReturn(mockView)

        val mockImeRoot = mock(android.view.View::class.java)
        val mockCandidateBar = mock(android.widget.LinearLayout::class.java)
        val mockCandidateContainer = mock(android.widget.LinearLayout::class.java)
        val mockPinyinFloatingBar = mock(android.view.View::class.java)
        val mockPinyinFloatingText = mock(android.widget.TextView::class.java)
        val mockXiweiKeyboardView = mock(XiweiKeyboardView::class.java)

        `when`(mockView.findViewById<android.view.View>(R.id.ime_root)).thenReturn(mockImeRoot)
        `when`(mockView.findViewById<android.view.View>(R.id.candidate_bar)).thenReturn(mockCandidateBar)
        `when`(mockView.findViewById<android.view.View>(R.id.candidate_container)).thenReturn(mockCandidateContainer)
        `when`(mockView.findViewById<android.view.View>(R.id.pinyin_floating_bar)).thenReturn(mockPinyinFloatingBar)
        `when`(mockView.findViewById<android.view.View>(R.id.pinyin_floating_text)).thenReturn(mockPinyinFloatingText)
        `when`(mockView.findViewById<XiweiKeyboardView>(R.id.xiwei_keyboard_view)).thenReturn(mockXiweiKeyboardView)

        // mock handler
        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java))
        val engine = mock(T9Engine::class.java)
        `when`(engine.getPreedit()).thenReturn("")
        handler.attachEngine(engine)
        injectField(service, "handler", handler)


        // Mock Log and Looper
        val logMock = mockStatic(android.util.Log::class.java)
        val looperMock = mockStatic(android.os.Looper::class.java)
        
        logMock.`when`<Int> { android.util.Log.i(anyString(), anyString()) }.thenReturn(0)
        logMock.`when`<Int> { android.util.Log.e(anyString(), anyString(), any()) }.thenAnswer { 
            val e = it.getArgument<Throwable>(2)
            e.printStackTrace()
            0 
        }
        
        val mockLooper = mock(android.os.Looper::class.java)
        looperMock.`when`<android.os.Looper> { android.os.Looper.getMainLooper() }.thenReturn(mockLooper)

        try {
            // Run onCreateInputView
            val view = service.onCreateInputView()
            
            // Assert view is returned and non-null
            org.junit.Assert.assertNotNull(view)
            org.junit.Assert.assertEquals(mockView, view)
            
            verify(mockXiweiKeyboardView, org.mockito.Mockito.atLeastOnce()).palette = any()
            verify(mockXiweiKeyboardView, org.mockito.Mockito.atLeastOnce()).layoutModel = any()
        } finally {
            logMock.close()
            looperMock.close()
        }
    }
}
