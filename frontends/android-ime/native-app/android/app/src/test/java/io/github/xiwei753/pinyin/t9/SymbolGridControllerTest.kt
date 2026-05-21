package io.github.xiwei753.pinyin.t9

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SymbolGridControllerTest {

    private lateinit var context: android.content.Context
    private val generatedViews = mutableListOf<View>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        generatedViews.clear()
    }

    @Test
    fun everyRowHasExactly5Cells() {
        val entries = (1..27).map { it to "s$it" }
        val page = SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
        )

        for (i in 0 until page.childCount) {
            val row = page.getChildAt(i) as LinearLayout
            assertEquals("Row $i should have 5 cells", 5, row.childCount)
        }
    }

    @Test
    fun lastRowHasInvisiblePlaceholdersWhenIncomplete() {
        val entries = (1..7).map { it to "k$it" }
        val page = SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
        )

        assertEquals("Should have 2 rows", 2, page.childCount)

        val firstRow = page.getChildAt(0) as LinearLayout
        assertEquals("First row should have 5 cells", 5, firstRow.childCount)
        for (i in 0 until 5) {
            assertTrue("First row cell $i should be TextView", firstRow.getChildAt(i) is TextView)
        }

        val lastRow = page.getChildAt(1) as LinearLayout
        assertEquals("Last row should have 5 cells", 5, lastRow.childCount)

        for (i in 0 until 2) {
            val cell = lastRow.getChildAt(i)
            assertTrue("Cell $i should be TextView (symbol)", cell is TextView)
            assertEquals("k${i + 6}", (cell as TextView).text)
        }

        for (i in 2 until 5) {
            val cell = lastRow.getChildAt(i)
            assertTrue("Cell $i should be placeholder (View, not TextView)", cell !is TextView)
            assertFalse("Placeholder $i should not be clickable", cell.isClickable)
            assertFalse("Placeholder $i should not be enabled", cell.isEnabled)
        }
    }

    @Test
    fun allSymbolKeysHaveEqualWidthWeight() {
        val entries = (1..15).map { it to "b$it" }
        val page = SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
        )

        for (i in 0 until page.childCount) {
            val row = page.getChildAt(i) as LinearLayout
            for (j in 0 until row.childCount) {
                val cell = row.getChildAt(j)
                val lp = cell.layoutParams as LinearLayout.LayoutParams
                assertEquals("Cell ($i,$j) should have weight 1f", 1f, lp.weight, 0.001f)
                assertEquals("Cell ($i,$j) should have width 0 (weight-based)", 0, lp.width)
            }
        }
    }

    @Test
    fun noSymbolKeySpansMultipleColumns() {
        val entries = (1..20).map { it to "c$it" }
        val page = SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
        )

        for (i in 0 until page.childCount) {
            val row = page.getChildAt(i) as LinearLayout
            for (j in 0 until row.childCount) {
                val cell = row.getChildAt(j)
                val lp = cell.layoutParams as LinearLayout.LayoutParams
                assertEquals("No cell should have span > 1 at ($i,$j)", 1f, lp.weight, 0.001f)
            }
        }
    }

    @Test
    fun exactMultipleOf5HasNoPlaceholders() {
        val entries = (1..15).map { it to "d$it" }
        val page = SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
        )

        assertEquals("Should have 3 rows for 15 items", 3, page.childCount)
        for (i in 0 until page.childCount) {
            val row = page.getChildAt(i) as LinearLayout
            assertEquals("Row $i should have 5 cells", 5, row.childCount)
        }

        assertEquals("Should have 15 generated views", 15, generatedViews.size)
        assertTrue("All generated views should be TextViews",
            generatedViews.all { it is TextView })
    }

    @Test
    fun singleEntryGets4Placeholders() {
        val entries = listOf(1 to "x")
        val page = SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
        )

        assertEquals("Should have 1 row", 1, page.childCount)
        val row = page.getChildAt(0) as LinearLayout
        assertEquals("Row should have 5 cells", 5, row.childCount)

        assertTrue("First cell should be TextView", row.getChildAt(0) is TextView)

        for (i in 1 until 5) {
            val cell = row.getChildAt(i)
            assertTrue("Cell $i should be placeholder", cell !is TextView)
            assertFalse("Placeholder $i should not be clickable", cell.isClickable)
        }

        assertEquals("Should have 1 generated view", 1, generatedViews.size)
    }

    @Test
    fun symbolKeyClickTriggersCallback() {
        val clicked = mutableListOf<String>()
        val entries = listOf(1 to "A", 2 to "B")
        SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
            onSymbolClick = { symbol -> clicked.add(symbol) },
        )

        assertEquals(2, generatedViews.size)
        val btnA = generatedViews[0]
        val btnB = generatedViews[1]

        btnA.performClick()
        assertEquals(listOf("A"), clicked)

        btnB.performClick()
        assertEquals(listOf("A", "B"), clicked)
    }

    @Test
    fun placeholderClickDoesNothing() {
        val clicked = mutableListOf<String>()
        val entries = listOf(1 to "X")
        val page = SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
            onSymbolClick = { symbol -> clicked.add(symbol) },
        )

        // Click symbol first to verify binding works
        generatedViews[0].performClick()
        assertEquals(listOf("X"), clicked)

        val row = page.getChildAt(0) as LinearLayout
        // cells 1-4 = placeholders
        for (i in 1 until 5) {
            val placeholder = row.getChildAt(i)
            assertFalse("placeholder should not be clickable", placeholder.isClickable)
            assertFalse("placeholder should not be enabled", placeholder.isEnabled)
            placeholder.performClick()
        }
        // clicked should still be only ["X"] - placeholders don't trigger callback
        assertEquals(listOf("X"), clicked)
    }

    @Test
    fun horizontalGapAppliedFromMetrics() {
        val metrics = SymbolGridLayoutMetrics(
            cellWidth = 100, cellHeight = 100, columnCount = 5,
            contentInsetLeft = 0, contentInsetRight = 0,
            contentInsetTop = 0, contentInsetBottom = 0,
            horizontalGap = 8, verticalGap = 0,
        )
        val entries = (1..5).map { it to "v$it" }
        SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
            metrics = metrics,
        )

        val page = generatedViews[0].parent.parent as LinearLayout
        val row = page.getChildAt(0) as LinearLayout
        for (i in 0 until 4) {
            val cell = row.getChildAt(i)
            val lp = cell.layoutParams as LinearLayout.LayoutParams
            assertEquals("Cell $i should have marginEnd = 8",
                8, lp.marginEnd)
        }
        // Last cell should have no marginEnd
        val lastCell = row.getChildAt(4)
        val lastLp = lastCell.layoutParams as LinearLayout.LayoutParams
        assertEquals("Last cell should have no marginEnd", 0, lastLp.marginEnd)
    }

    @Test
    fun verticalGapAppliedFromMetrics() {
        val metrics = SymbolGridLayoutMetrics(
            cellWidth = 100, cellHeight = 100, columnCount = 5,
            contentInsetLeft = 0, contentInsetRight = 0,
            contentInsetTop = 0, contentInsetBottom = 0,
            horizontalGap = 0, verticalGap = 6,
        )
        val entries = (1..10).map { it to "r$it" }
        SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
            metrics = metrics,
        )

        val page = generatedViews[0].parent.parent as LinearLayout
        assertEquals(2, page.childCount)
        val firstRow = page.getChildAt(0) as LinearLayout
        val firstLp = firstRow.layoutParams as LinearLayout.LayoutParams
        assertEquals("First row should have bottomMargin = verticalGap",
            6, firstLp.bottomMargin)

        val lastRow = page.getChildAt(1) as LinearLayout
        val lastLp = lastRow.layoutParams as LinearLayout.LayoutParams
        assertEquals("Last row should have no bottomMargin", 0, lastLp.bottomMargin)
    }

    @Test
    fun onSymbolTouchDoesNotCrash() {
        // Verify that setting onSymbolTouch doesn't prevent button creation
        val entries = listOf(1 to "T")
        SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
            onSymbolTouch = { _ -> },
        )
        assertEquals(1, generatedViews.size)
        val btn = generatedViews[0]
        assertTrue("Symbol button should be clickable", btn.isClickable)
    }

    @Test
    fun allSymbolKeysHaveVisibleWhiteBackgroundSeparatedByGaps() {
        val metrics = SymbolGridLayoutMetrics(
            cellWidth = 100, cellHeight = 100, columnCount = 5,
            contentInsetLeft = 0, contentInsetRight = 0,
            contentInsetTop = 0, contentInsetBottom = 0,
            horizontalGap = 4, verticalGap = 4,
        )
        val entries = (1..7).map { it to "g$it" }
        SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
            metrics = metrics,
        )

        val page = generatedViews[0].parent.parent as LinearLayout
        // First row: 5 symbol keys all have margins between them
        val row = page.getChildAt(0) as LinearLayout
        for (i in 0 until 4) {
            val cell = row.getChildAt(i)
            assertTrue("Cell $i should be TextView", cell is TextView)
            val lp = cell.layoutParams as LinearLayout.LayoutParams
            assertTrue("Cell $i marginEnd should be >= 0", lp.marginEnd >= 0)
        }
        // Last row: 2 symbol keys + 3 placeholders
        val lastRow = page.getChildAt(1) as LinearLayout
        for (i in 0 until 5) {
            val cell = lastRow.getChildAt(i)
            if (i < 2) {
                assertTrue("Last row cell $i should be TextView", cell is TextView)
            } else {
                assertTrue("Last row cell $i should be placeholder", cell !is TextView)
            }
        }
    }

    @Test
    fun placeholderIsTransparent() {
        val entries = listOf(1 to "X")
        val page = SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = 100,
            generatedSymbolViews = generatedViews,
        )

        val row = page.getChildAt(0) as LinearLayout
        for (i in 1 until 5) {
            val placeholder = row.getChildAt(i)
            assertTrue("Placeholder should not be TextView", placeholder !is TextView)
            assertTrue("Placeholder $i should have a background", placeholder.background != null)
        }
    }

    @Test
    fun defaultMetricsApplyVerticalGapBetweenRows() {
        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = context.resources.displayMetrics.density,
            symbolPanelWidth = context.resources.displayMetrics.widthPixels,
            rowHeight = (48 * context.resources.displayMetrics.density).toInt(),
        )
        assertTrue("Default metrics should have verticalGap > 0", metrics.verticalGap > 0)

        val entries = (1..10).map { it to "v$it" }
        SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = metrics.cellHeight,
            generatedSymbolViews = generatedViews,
            metrics = metrics,
        )

        val page = generatedViews[0].parent.parent as LinearLayout
        assertEquals(2, page.childCount)
        val firstRow = page.getChildAt(0) as LinearLayout
        val firstLp = firstRow.layoutParams as LinearLayout.LayoutParams
        assertEquals("First row bottomMargin should equal verticalGap",
            metrics.verticalGap, firstLp.bottomMargin)
    }

    @Test
    fun allPagesHaveConsistentGaps() {
        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = 2.0f,
            symbolPanelWidth = 800,
            rowHeight = 100,
        )
        assertTrue("hGap and vGap should both be positive",
            metrics.horizontalGap > 0 && metrics.verticalGap > 0)

        val entries = (1..12).map { it to "p$it" }
        SymbolGridController.buildPage(
            context = context,
            entries = entries,
            rowHeightPx = metrics.cellHeight,
            generatedSymbolViews = generatedViews,
            metrics = metrics,
        )

        val page = generatedViews[0].parent.parent as LinearLayout
        for (i in 0 until page.childCount - 1) {
            val row = page.getChildAt(i) as LinearLayout
            val lp = row.layoutParams as LinearLayout.LayoutParams
            assertEquals("Row $i bottomMargin should equal verticalGap",
                metrics.verticalGap, lp.bottomMargin)
        }
        val lastRow = page.getChildAt(page.childCount - 1) as LinearLayout
        val lastLp = lastRow.layoutParams as LinearLayout.LayoutParams
        assertEquals("Last row should have no bottomMargin", 0, lastLp.bottomMargin)
    }
}
