package io.github.xiwei753.pinyin.t9

import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class KeyboardHeightControllerTest {

    private lateinit var mockRepo: SettingsRepository
    private lateinit var mockResources: Resources
    private lateinit var controller: KeyboardHeightController
    private val testDensity = 2.0f

    @Before
    fun setUp() {
        mockRepo = mock(SettingsRepository::class.java)
        mockResources = mock(Resources::class.java)
        val metrics = DisplayMetrics().apply { density = testDensity }
        `when`(mockResources.displayMetrics).thenReturn(metrics)
        controller = KeyboardHeightController(mockRepo, mockResources)
    }

    @Test
    fun testLowHeightDifferentFromNormal() {
        `when`(mockRepo.getKeyboardHeight()).thenReturn("low")
        val lowMetrics = controller.calculateHeight()

        `when`(mockRepo.getKeyboardHeight()).thenReturn("normal")
        val normalMetrics = controller.calculateHeight()

        assertTrue("Low row height should be less than normal",
            lowMetrics.rowHeightPx < normalMetrics.rowHeightPx)
        assertTrue("Low bottom row height should be less than normal",
            lowMetrics.bottomRowHeightPx < normalMetrics.bottomRowHeightPx)
    }

    @Test
    fun testHighHeightDifferentFromNormal() {
        `when`(mockRepo.getKeyboardHeight()).thenReturn("high")
        val highMetrics = controller.calculateHeight()

        `when`(mockRepo.getKeyboardHeight()).thenReturn("normal")
        val normalMetrics = controller.calculateHeight()

        assertTrue("High row height should be greater than normal",
            highMetrics.rowHeightPx > normalMetrics.rowHeightPx)
        assertTrue("High bottom row height should be greater than normal",
            highMetrics.bottomRowHeightPx > normalMetrics.bottomRowHeightPx)
    }

    @Test
    fun testShellHeightGreaterThanZero() {
        `when`(mockRepo.getKeyboardHeight()).thenReturn("normal")
        val metrics = controller.calculateHeight()

        assertTrue("Shell height should be > 0", metrics.shellHeight > 0)
    }

    @Test
    fun testSymbolScrollHeightGreaterThanZero() {
        `when`(mockRepo.getKeyboardHeight()).thenReturn("normal")
        val metrics = controller.calculateHeight()

        assertTrue("Symbol scroll height should be > 0", metrics.symbolScrollHeight > 0)
    }

    @Test
    fun testApplyHeightDoesNotCrash() {
        `when`(mockRepo.getKeyboardHeight()).thenReturn("normal")
        val metrics = controller.calculateHeight()

        val mockView = mock(android.view.View::class.java)
        val mockFlp = mock(android.widget.FrameLayout.LayoutParams::class.java)
        val mockVglp = mock(android.view.ViewGroup.LayoutParams::class.java)

        val mockKv = mock(KeyboardViews::class.java)

        // Stub all views used by applyHeight
        fun stub(id: String): View = mockView
        `when`(mockKv.keyboardShell).thenReturn(mockView)
        `when`(mockKv.keyToggleSymbol).thenReturn(mockView)
        `when`(mockKv.keyToggleNumber).thenReturn(mockView)
        `when`(mockKv.keySpace).thenReturn(mockView)
        `when`(mockKv.keyToggleEnglish).thenReturn(mock(android.widget.TextView::class.java))
        `when`(mockKv.imeRoot).thenReturn(mockView)
        `when`(mockKv.symBack).thenReturn(mock(android.widget.TextView::class.java))
        `when`(mockKv.symNumber).thenReturn(mock(android.widget.TextView::class.java))
        `when`(mockKv.symDel).thenReturn(mockView)
        `when`(mockKv.symEnter).thenReturn(mock(android.widget.TextView::class.java))
        `when`(mockKv.symHide).thenReturn(mockView)
        `when`(mockKv.numBack).thenReturn(mockView)
        `when`(mockKv.numSymbol).thenReturn(mockView)
        `when`(mockKv.numHide).thenReturn(mockView)
        `when`(mockKv.numEnter).thenReturn(mockView)
        `when`(mockKv.symPagePunct).thenReturn(mockView)
        `when`(mockKv.symPageMath).thenReturn(mockView)
        `when`(mockKv.symPageBracket).thenReturn(mockView)
        `when`(mockKv.symPageOther).thenReturn(mockView)
        `when`(mockKv.symScrollContent).thenReturn(mock(android.widget.ScrollView::class.java))

        // Stub T9 geometry views
        `when`(mockKv.panelT9).thenReturn(mockView)
        `when`(mockKv.t9LeftColumn).thenReturn(mockView)
        `when`(mockKv.t9LeftScrollFrame).thenReturn(mockView)
        `when`(mockKv.t9SymbolButtonFrame).thenReturn(mockView)
        `when`(mockKv.t9Key1Frame).thenReturn(mockView)
        `when`(mockKv.t9Key2Frame).thenReturn(mockView)
        `when`(mockKv.t9Key3Frame).thenReturn(mockView)
        `when`(mockKv.t9Key4Frame).thenReturn(mockView)
        `when`(mockKv.t9Key5Frame).thenReturn(mockView)
        `when`(mockKv.t9Key6Frame).thenReturn(mockView)
        `when`(mockKv.t9Key7Frame).thenReturn(mockView)
        `when`(mockKv.t9Key8Frame).thenReturn(mockView)
        `when`(mockKv.t9Key9Frame).thenReturn(mockView)
        `when`(mockKv.t9DelFrame).thenReturn(mockView)
        `when`(mockKv.t9RetypeFrame).thenReturn(mockView)
        `when`(mockKv.enterContainer).thenReturn(mockView)
        `when`(mockKv.t9NumberFrame).thenReturn(mockView)
        `when`(mockKv.t9SpaceFrame).thenReturn(mockView)
        `when`(mockKv.t9EnglishFrame).thenReturn(mockView)

        `when`(mockKv.keyboardShell.layoutParams).thenReturn(mockVglp)
        `when`(mockKv.symScrollContent.layoutParams).thenReturn(mockVglp)

        try {
            controller.applyHeight(mockKv, metrics)
        } catch (e: Exception) {
            fail("applyHeight should not crash: ${e.message}")
        }
    }
}
