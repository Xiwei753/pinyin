package io.github.xiwei753.pinyin.t9

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class XiweiT9ImeServiceLoggingTest {

    private lateinit var service: XiweiT9ImeService
    private lateinit var fakeLogger: T9DebugLoggerTest.FakeDebugLogger
    private lateinit var mockRepo: SettingsRepository

    @Before
    fun setUp() {
        service = XiweiT9ImeService()

        fakeLogger = T9DebugLoggerTest.FakeDebugLogger()
        service.debugLogger = fakeLogger

        mockRepo = mock(SettingsRepository::class.java)
        val repoField = XiweiT9ImeService::class.java.getDeclaredField("settingsRepository")
        repoField.isAccessible = true
        repoField.set(service, mockRepo)

        val mockDict = mock(DictionaryProvider::class.java)
        val engine = T9Engine(mockDict)
        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(engine) }
        val ctrlField = XiweiT9ImeService::class.java.getDeclaredField("handler")
        ctrlField.isAccessible = true
        ctrlField.set(service, handler)

        val mockContainer = mock(android.widget.LinearLayout::class.java)
        val mockFloatingBar = mock(android.view.View::class.java)
        val mockFloatingText = mock(android.widget.TextView::class.java)
        val mockCandidateBar = mock(android.widget.LinearLayout::class.java)
        val mockImeRoot = mock(android.view.View::class.java)
        val mockShell = mock(android.view.View::class.java)
        val mockPanelT9 = mock(android.view.View::class.java)
        val mockPanelSymbol = mock(android.view.View::class.java)
        val mockPanelNumber = mock(android.view.View::class.java)
        val mockSymPagePunct = mock(android.view.View::class.java)
        val mockSymPageMath = mock(android.view.View::class.java)
        val mockSymPageBracket = mock(android.view.View::class.java)
        val mockSymPageOther = mock(android.view.View::class.java)
        val mockSymScroll = mock(android.widget.ScrollView::class.java)
        val mockLeftScrollRail = mock(android.view.View::class.java)
        val mockLeftScrollContent = mock(android.widget.LinearLayout::class.java)

        val keyboardViews = KeyboardViews(
            imeRoot = mockImeRoot,
            candidateBar = mockCandidateBar,
            candidateContainer = mockContainer,
            pinyinFloatingBar = mockFloatingBar,
            pinyinFloatingText = mockFloatingText,
            xiweiKeyboardView = mock(XiweiKeyboardView::class.java),
        )
        val viewsField = XiweiT9ImeService::class.java.getDeclaredField("keyboardViews")
        viewsField.isAccessible = true
        viewsField.set(service, keyboardViews)
    }

    private fun triggerLogDebugInfo() {
        val method = XiweiT9ImeService::class.java.getDeclaredMethod("logDebugInfo")
        method.isAccessible = true
        method.invoke(service)
    }

    @Test
    fun testLoggerNotCalledWhenDisabled() {
        `when`(mockRepo.isDebugLoggingEnabled()).thenReturn(false)

        triggerLogDebugInfo()

        assertTrue("Logger should not be called when disabled", fakeLogger.logs.isEmpty())
    }

    @Test
    fun testLoggerCalledWhenEnabled() {
        `when`(mockRepo.isDebugLoggingEnabled()).thenReturn(true)

        triggerLogDebugInfo()

        assertTrue("Logger should be called when enabled", fakeLogger.logs.isNotEmpty())
        assertEquals("XiweiT9Debug", fakeLogger.logs[0].first)
        assertTrue(fakeLogger.logs.any { it.second.contains("mode=") })
    }
}
