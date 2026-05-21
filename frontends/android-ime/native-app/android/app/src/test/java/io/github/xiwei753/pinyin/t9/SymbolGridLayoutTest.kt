package io.github.xiwei753.pinyin.t9

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolGridLayoutTest {

    @Test
    fun symbolGridLayoutMetricsCalculatedCorrectly() {
        val density = 2.0f
        val panelWidth = 800
        val rowHeight = 120

        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = density,
            symbolPanelWidth = panelWidth,
            rowHeight = rowHeight,
        )

        val expectedInset = (4 * density).toInt()
        val expectedHGap = (4 * density).toInt()

        assertEquals("inset left", expectedInset, metrics.contentInsetLeft)
        assertEquals("inset right", expectedInset, metrics.contentInsetRight)
        assertEquals("cell height", rowHeight, metrics.cellHeight)
        assertEquals("columns", 5, metrics.columnCount)

        val expectedCellWidth = (panelWidth - 2 * expectedInset - 4 * expectedHGap) / 5
        assertEquals("cell width", expectedCellWidth, metrics.cellWidth)
        assertTrue("cell width should be positive", metrics.cellWidth > 0)
    }

    @Test
    fun symbolGridLayoutMetricsFromDp() {
        val density = 3.0f
        val panelWidth = 900

        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = density,
            symbolPanelWidth = panelWidth,
            rowHeight = 144,
            hInsetDp = 8f,
            vInsetDp = 4f,
            hGapDp = 6f,
            vGapDp = 2f,
        )

        assertEquals("inset left", (8 * density).toInt(), metrics.contentInsetLeft)
        assertEquals("inset right", (8 * density).toInt(), metrics.contentInsetRight)
        assertEquals("inset top", (4 * density).toInt(), metrics.contentInsetTop)
        assertEquals("h gap", (6 * density).toInt(), metrics.horizontalGap)
        assertEquals("v gap", (2 * density).toInt(), metrics.verticalGap)
        assertEquals("cell height", 144, metrics.cellHeight)
    }

    @Test
    fun columnCountIsAlways5() {
        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = 2.0f,
            symbolPanelWidth = 800,
            rowHeight = 100,
        )
        assertEquals(5, metrics.columnCount)
    }

    @Test
    fun cellWidthPositiveForReasonableWidth() {
        val widths = listOf(400, 600, 800, 1080, 1440)
        for (w in widths) {
            val metrics = SymbolGridLayoutMetrics.fromDp(
                density = 2.0f,
                symbolPanelWidth = w,
                rowHeight = 100,
            )
            assertTrue("cell width should be positive for panel width $w", metrics.cellWidth > 0)
        }
    }

    @Test
    fun contentInsetsAppliedSymmetrically() {
        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = 2.0f,
            symbolPanelWidth = 800,
            rowHeight = 100,
            hInsetDp = 10f,
        )
        assertEquals("left and right insets equal", metrics.contentInsetLeft, metrics.contentInsetRight)
    }

    @Test
    fun fromGeometrySameAsFromDp() {
        val density = 2.5f
        val panelWidth = 720
        val rowHeight = 96
        val fromGeo = SymbolGridLayoutMetrics.fromGeometry(density, panelWidth, rowHeight)
        val fromDp = SymbolGridLayoutMetrics.fromDp(density, panelWidth, rowHeight)
        assertEquals(fromGeo, fromDp)
    }

    @Test
    fun defaultVerticalGapIsPositive() {
        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = 2.0f,
            symbolPanelWidth = 800,
            rowHeight = 100,
        )
        assertTrue("default verticalGap should be > 0", metrics.verticalGap > 0)
    }

    @Test
    fun defaultVerticalGapMatchesHorizontalGap() {
        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = 3.0f,
            symbolPanelWidth = 1080,
            rowHeight = 144,
        )
        assertEquals("default vGap should equal hGap", metrics.horizontalGap, metrics.verticalGap)
    }

    @Test
    fun customVerticalGapOverridesDefault() {
        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = 2.0f,
            symbolPanelWidth = 800,
            rowHeight = 100,
            vGapDp = 8f,
        )
        assertEquals("custom vGap should be 8dp * density", (8 * 2.0f).toInt(), metrics.verticalGap)
    }
}
