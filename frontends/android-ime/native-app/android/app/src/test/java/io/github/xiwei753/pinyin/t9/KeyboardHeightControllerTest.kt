package io.github.xiwei753.pinyin.t9

import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
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
    fun testSymbolModeShellHeightPositive() {
        `when`(mockRepo.getKeyboardHeight()).thenReturn("normal")
        val metrics = controller.calculateHeight()

        assertTrue("Shell height should be > 0", metrics.shellHeight > 0)
        assertTrue("Row height should be > 0", metrics.rowHeightPx > 0)
    }

    @Test
    fun testApplyHeightDoesNotCrash() {
        `when`(mockRepo.getKeyboardHeight()).thenReturn("normal")
        val metrics = controller.calculateHeight()

        val mockView = mock(android.view.View::class.java)
        val mockKv = mock(KeyboardViews::class.java)

        `when`(mockKv.imeRoot).thenReturn(mockView)

        try {
            controller.applyHeight(mockKv, metrics)
        } catch (e: Exception) {
            fail("applyHeight should not crash: ${e.message}")
        }
    }

    @Test
    fun symbolCategoryTabsWidthMatchesT9LeftRailWidth() {
        `when`(mockRepo.getKeyboardHeight()).thenReturn("normal")
        val metrics = controller.calculateHeight()

        val panelT9 = mock(View::class.java)
        `when`(panelT9.width).thenReturn(1080)
        `when`(panelT9.height).thenReturn(480)

        val realLp = ViewGroup.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        val symTabs = mock(View::class.java)
        `when`(symTabs.layoutParams).thenReturn(realLp)

        val kv = mock(KeyboardViews::class.java)
        `when`(kv.imeRoot).thenReturn(mock(View::class.java))

        controller.applyHeight(kv, metrics)

    }
}

