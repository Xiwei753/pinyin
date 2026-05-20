package io.github.xiwei753.pinyin.t9

import android.content.res.Resources
import android.util.DisplayMetrics
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

        val mockKv = mock(KeyboardViews::class.java)
        `when`(mockKv.keyboardShell).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.keyToggleSymbol).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.keyToggleNumber).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.keySpace).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.keyToggleEnglish).thenReturn(mock(android.widget.TextView::class.java))
        `when`(mockKv.symBack).thenReturn(mock(android.widget.TextView::class.java))
        `when`(mockKv.symNumber).thenReturn(mock(android.widget.TextView::class.java))
        `when`(mockKv.symDel).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.symEnter).thenReturn(mock(android.widget.TextView::class.java))
        `when`(mockKv.symHide).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.numBack).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.numSymbol).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.numHide).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.numEnter).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.symPagePunct).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.symPageMath).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.symPageBracket).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.symPageOther).thenReturn(mock(android.view.View::class.java))
        `when`(mockKv.symScrollContent).thenReturn(mock(android.widget.ScrollView::class.java))
        `when`(mockKv.imeRoot).thenReturn(mock(android.view.View::class.java))

        val shellParams = mock(android.view.ViewGroup.LayoutParams::class.java)
        `when`(mockKv.keyboardShell.layoutParams).thenReturn(shellParams)
        val scrollParams = mock(android.view.ViewGroup.LayoutParams::class.java)
        `when`(mockKv.symScrollContent.layoutParams).thenReturn(scrollParams)

        try {
            controller.applyHeight(mockKv, metrics)
        } catch (e: Exception) {
            fail("applyHeight should not crash: ${e.message}")
        }
    }
}
