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
            keyboardShell = mockShell,
            panelT9 = mockPanelT9,
            panelSymbol = mockPanelSymbol,
            panelNumber = mockPanelNumber,
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
            symPagePunct = mockSymPagePunct,
            symPageMath = mockSymPageMath,
            symPageBracket = mockSymPageBracket,
            symPageOther = mockSymPageOther,
            symScrollContent = mockSymScroll,
            leftScrollRail = mockLeftScrollRail,
            leftScrollContent = mockLeftScrollContent,
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
            symBack = mock(android.widget.TextView::class.java),
            symNumber = mock(android.widget.TextView::class.java),
            symDel = mock(android.view.View::class.java),
            symEnter = mock(android.view.View::class.java),
            symHide = mock(android.view.View::class.java),
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
            numDel = mock(android.view.View::class.java),
            numBack = mock(android.view.View::class.java),
            numSymbol = mock(android.view.View::class.java),
            numHide = mock(android.view.View::class.java),
            numEnter = mock(android.view.View::class.java),
t9LeftColumn = mock(),
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
