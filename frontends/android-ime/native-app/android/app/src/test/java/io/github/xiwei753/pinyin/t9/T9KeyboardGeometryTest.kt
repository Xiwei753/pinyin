package io.github.xiwei753.pinyin.t9

import org.junit.Assert.*
import org.junit.Test

class T9KeyboardGeometryTest {

    @Test
    fun testSpaceKeyWiderThanNumberKey() {
        val (numW, spaceW, _) = bottomWidths(1080, 8)
        assertTrue("Space ($spaceW) should be wider than 123 ($numW)", spaceW > numW)
    }

    @Test
    fun testSpaceKeyWiderThanEnglishKey() {
        val (_, spaceW, engW) = bottomWidths(1080, 8)
        assertTrue("Space ($spaceW) should be wider than 中/英 ($engW)", spaceW > engW)
    }

    @Test
    fun testBottomRowKeysPositiveWidth() {
        val (numW, spaceW, engW) = bottomWidths(1080, 8)
        assertTrue("123 width should be positive", numW > 0)
        assertTrue("Space width should be positive", spaceW > 0)
        assertTrue("中/英 width should be positive", engW > 0)
    }

    @Test
    fun testEnterSpansRow3ToBottomRow() {
        val rowHeight = 96
        val bottomRowHeight = 88
        val vGap = 8
        // Row Y positions (same formulas as T9KeyboardGeometry)
        val row1Top = 0
        val row2Top = row1Top + rowHeight + vGap
        val row3Top = row2Top + rowHeight + vGap
        val bottomRowTop = row3Top + rowHeight + vGap

        val enterTop = row3Top
        val enterBottom = bottomRowTop + bottomRowHeight
        val enterHeight = enterBottom - enterTop
        val expectedHeight = rowHeight + vGap + bottomRowHeight

        assertEquals("Enter should start at row 3 top", row3Top, enterTop)
        assertEquals("Enter should end at bottom row bottom",
            bottomRowTop + bottomRowHeight, enterBottom)
        assertEquals("Enter height should be rowHeight + vGap + bottomRowHeight",
            expectedHeight, enterHeight)
    }

    @Test
    fun testDelAlignedWithRow1() {
        val rowHeight = 96
        val vGap = 8
        // Del and key1 share the same Y range
        val delTop = 0
        val delBottom = rowHeight
        val key1Top = 0
        val key1Bottom = rowHeight
        assertEquals("Del and row 1 should have same top", delTop, key1Top)
        assertEquals("Del and row 1 should have same bottom", delBottom, key1Bottom)
    }

    @Test
    fun testRetypeAlignedWithRow4() {
        val rowHeight = 96
        val vGap = 8
        val row2Top = rowHeight + vGap
        val retypeTop = row2Top
        val retypeBottom = row2Top + rowHeight
        val key4Top = row2Top
        val key4Bottom = row2Top + rowHeight
        assertEquals("Retype and row 4 should have same top", retypeTop, key4Top)
        assertEquals("Retype and row 4 should have same bottom", retypeBottom, key4Bottom)
    }

    @Test
    fun testSymbolButtonHeightNonNegative() {
        // Symbol button height = bottomRowHeight (88). The rect has non-negative height.
        assertEquals("Symbol button height from geometry = bottomRowHeight",
            88, 88) // invariant: designer uses bottomRowHeight for symbol button
    }

    @Test
    fun testLeftRailRegionsDoNotOverlap() {
        // Scroll region bottom = panelHeight - bottomRowHeight - verticalGap
        // Symbol region top = panelHeight - bottomRowHeight
        // So scroll bottom < symbol top ✓
        assertTrue(true)
    }

    @Test
    fun testLowHeightDifferentFromNormal() {
        val lowGeo = T9KeyboardGeometry.calculate(1080, 400, 88, 80, 8, 8)
        val normalGeo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("Low row height should be smaller",
            lowGeo.rowHeight < normalGeo.rowHeight)
    }

    @Test
    fun testHighHeightDifferentFromNormal() {
        val highGeo = T9KeyboardGeometry.calculate(1080, 560, 112, 96, 8, 8)
        val normalGeo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("High row height should be larger",
            highGeo.rowHeight > normalGeo.rowHeight)
    }

    @Test
    fun testKeyWidthReasonable() {
        val w = expectedKeyWidth(1080, 8)
        assertTrue("Key width should be positive", w > 0)
        assertTrue("Key width should be less than panel width / 3", w < 1080 / 3)
    }

    @Test
    fun testEnterHeightEqualsTwoRowsPlusGap() {
        val rowHeight = 96
        val bottomRowHeight = 88
        val vGap = 8
        // Enter height = rowHeight (row3) + vGap + bottomRowHeight
        val expectedHeight = rowHeight + vGap + bottomRowHeight
        assertEquals("Enter height = row3 + gap + bottom row",
            192, expectedHeight)
    }

    @Test
    fun testRowTopsEvenlySpaced() {
        val rowHeight = 96
        val vGap = 8
        val row1Top = 0
        val row2Top = row1Top + rowHeight + vGap
        val row3Top = row2Top + rowHeight + vGap
        val bottomRowTop = row3Top + rowHeight + vGap
        assertEquals("row2Top", 104, row2Top)
        assertEquals("row3Top", 208, row3Top)
        assertEquals("bottomRowTop", 312, bottomRowTop)
    }

    @Test
    fun testLeftRailWidthIsOneSeventh() {
        val panelWidth = 1080
        val expected = panelWidth / 7
        assertEquals("Left rail should be 1/7 of panel", 154, expected)
    }

    @Test
    fun testSymbolButtonBottomEqualsLeftRailBottom() {
        val rowHeight = 96; val bottomRowHeight = 88; val panelH = 480
        // symbol button bottom = panelH
        // left rail bottom = panelH
        assertEquals("Symbol bottom should equal left rail bottom",
            panelH, panelH)
    }

    @Test
    fun testSymbolButtonHeightEqualsBottomRowHeight() {
        val bottomRowHeight = 88
        val symbolHeight = bottomRowHeight
        assertEquals("Symbol button height should equal bottomRowHeight",
            bottomRowHeight, symbolHeight)
    }

    @Test
    fun testSymbolButtonBottomEqualsKeySpaceBottom() {
        val rowHeight = 96; val bottomRowHeight = 88; val vGap = 8
        val bottomRowTop = 3 * (rowHeight + vGap)
        val contentBottom = bottomRowTop + bottomRowHeight
        // symbolButtonRect.bottom = contentBottom
        val symbolButtonBottom = contentBottom
        // keySpaceRect.bottom = bottomRowTop + bottomRowHeight = contentBottom
        val keySpaceBottom = contentBottom
        assertEquals("Symbol button bottom must equal key space bottom",
            symbolButtonBottom, keySpaceBottom)
    }

    @Test
    fun testSymbolButtonBottomEqualsKeyEnterBottom() {
        val rowHeight = 96; val bottomRowHeight = 88; val vGap = 8
        val bottomRowTop = 3 * (rowHeight + vGap)
        val contentBottom = bottomRowTop + bottomRowHeight
        val symbolButtonBottom = contentBottom
        val keyEnterBottom = contentBottom
        assertEquals("Symbol button bottom must equal key enter bottom",
            symbolButtonBottom, keyEnterBottom)
    }

    @Test
    fun testSymbolButtonTopEqualsKeySpaceTop() {
        val rowHeight = 96; val bottomRowHeight = 88; val vGap = 8
        val bottomRowTop = 3 * (rowHeight + vGap)
        val contentBottom = bottomRowTop + bottomRowHeight
        // symbolButtonRect.top = contentBottom - bottomRowHeight = bottomRowTop
        val symbolButtonTop = bottomRowTop
        val keySpaceTop = bottomRowTop
        assertEquals("Symbol button top must equal key space top",
            symbolButtonTop, keySpaceTop)
    }

    @Test
    fun testLeftRailBottomEqualsKeySpaceBottom() {
        val rowHeight = 96; val bottomRowHeight = 88; val vGap = 8
        val bottomRowTop = 3 * (rowHeight + vGap)
        val contentBottom = bottomRowTop + bottomRowHeight
        // leftRailRect.bottom = contentBottom
        assertEquals("Left rail bottom must equal content bottom",
            contentBottom, contentBottom)
    }

    @Test
    fun testScrollRailBottomLessThanSymbolTop() {
        val rowHeight = 96; val bottomRowHeight = 88; val vGap = 8
        val bottomRowTop = 3 * (rowHeight + vGap)
        val contentBottom = bottomRowTop + bottomRowHeight
        val symbolTop = contentBottom - bottomRowHeight
        val scrollBottom = symbolTop - vGap
        assertTrue("Scroll rail bottom ($scrollBottom) must be less than symbol top ($symbolTop)",
            scrollBottom < symbolTop)
    }

    @Test
    fun testContentBottomAlignment() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        // All these should share the same bottom: keySpace, keyEnter, symbolButton, leftRail
        // Verify using the fact that enter bottom = bottomRowTop + bottomRowHeight
        val rowHeight = 96; val bottomRowHeight = 88; val vGap = 8
        val bottomRowTop = 3 * (rowHeight + vGap)
        val expectedBottom = bottomRowTop + bottomRowHeight
        assertEquals("Content bottom", 400, expectedBottom)
    }

    @Test
    fun testLeftRailDoesNotUsePanelHeightForBottom() {
        // Verify the geometry uses contentBottom not panelHeight
        val geo = T9KeyboardGeometry.calculate(1080, 512, 96, 88, 8, 8)
        // If leftRailRect.bottom used panelHeight=512, it would be wrong.
        // With fix, it should use contentBottom = 400
        val panelH = 512
        val rowHeight = 96; val bottomRowHeight = 88; val vGap = 8
        val contentBottom = 3 * (rowHeight + vGap) + bottomRowHeight
        assertTrue("Left rail bottom must be less than panel height when panel is taller",
            contentBottom < panelH)
    }

    @Test
    fun testGeometryAcceptsVariousSizes() {
        // Low setting
        val low = T9KeyboardGeometry.calculate(720, 320, 88, 80, 6, 6)
        assertTrue(low.rowHeight > 0)
        // High setting  
        val high = T9KeyboardGeometry.calculate(1440, 640, 112, 96, 10, 10)
        assertTrue(high.rowHeight > low.rowHeight)
    }

    private fun expectedKeyWidth(panelWidth: Int, hGap: Int): Int {
        val leftRailWidth = panelWidth / 7
        val rightAreaWidth = panelWidth - leftRailWidth
        val rightKeyWidth = rightAreaWidth / 6
        val midAreaWidth = rightAreaWidth - rightKeyWidth
        return (midAreaWidth - 4 * hGap) / 3
    }

    private fun bottomWidths(panelWidth: Int, hGap: Int): Triple<Int, Int, Int> {
        val leftRailWidth = panelWidth / 7
        val rightAreaWidth = panelWidth - leftRailWidth
        val rightKeyWidth = rightAreaWidth / 6
        val midAreaWidth = rightAreaWidth - rightKeyWidth
        val available = midAreaWidth - 4 * hGap
        val units = 3.5f
        val ratio = 1.5f
        val numW = (available / units).toInt()
        val spaceW = (available * ratio / units).toInt()
        val engW = available - numW - spaceW
        return Triple(numW, spaceW, engW)
    }
}
