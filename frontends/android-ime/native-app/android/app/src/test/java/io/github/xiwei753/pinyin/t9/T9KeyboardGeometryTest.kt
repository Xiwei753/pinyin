package io.github.xiwei753.pinyin.t9

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class T9KeyboardGeometryTest {

    @Test
    fun testSpaceKeyWiderThanNumberKey() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("Space should be wider than 123", geo.keySpaceRect.width() > geo.keyNumberToggleRect.width())
    }

    @Test
    fun testSpaceKeyWiderThanEnglishKey() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("Space should be wider than 中/英", geo.keySpaceRect.width() > geo.keyEnglishToggleRect.width())
    }

    @Test
    fun testBottomRowKeysPositiveWidth() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("123 width should be positive", geo.keyNumberToggleRect.width() > 0)
        assertTrue("Space width should be positive", geo.keySpaceRect.width() > 0)
        assertTrue("中/英 width should be positive", geo.keyEnglishToggleRect.width() > 0)
    }

    @Test
    fun testEnterSpansRow3ToBottomRow() {
        val panelHeight = 480
        val bottomRowHeight = 88
        val vGap = 8
        val geo = T9KeyboardGeometry.calculate(1080, panelHeight, 96, bottomRowHeight, 8, vGap)

        assertEquals("Enter should start at row 3 top", geo.key7Rect.top, geo.keyEnterRect.top)
        assertEquals("Enter should end at bottom row bottom", panelHeight, geo.keyEnterRect.bottom)
    }

    @Test
    fun testDelAlignedWithRow1() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertEquals("Del and row 1 should have same top", geo.key1Rect.top, geo.keyDelRect.top)
        assertEquals("Del and row 1 should have same bottom", geo.key1Rect.bottom, geo.keyDelRect.bottom)
    }

    @Test
    fun testRetypeAlignedWithRow4() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertEquals("Retype and row 4 should have same top", geo.key4Rect.top, geo.keyRetypeRect.top)
        assertEquals("Retype and row 4 should have same bottom", geo.key4Rect.bottom, geo.keyRetypeRect.bottom)
    }

    @Test
    fun testSymbolButtonHeightNonNegative() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("Symbol button height should be non-negative", geo.symbolButtonRect.height() >= 0)
    }

    @Test
    fun testLeftRailRegionsDoNotOverlap() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("Scroll bottom <= symbol top", geo.leftRailScrollRect.bottom <= geo.symbolButtonRect.top)
    }

    @Test
    fun testLowHeightDifferentFromNormal() {
        val lowGeo = T9KeyboardGeometry.calculate(1080, 400, 88, 80, 8, 8)
        val normalGeo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("Low row height should be smaller", lowGeo.rowHeight < normalGeo.rowHeight)
    }

    @Test
    fun testHighHeightDifferentFromNormal() {
        val highGeo = T9KeyboardGeometry.calculate(1080, 560, 112, 96, 8, 8)
        val normalGeo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("High row height should be larger", highGeo.rowHeight > normalGeo.rowHeight)
    }

    @Test
    fun testKeyWidthReasonable() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        val w = geo.key1Rect.width()
        assertTrue("Key width should be positive", w > 0)
        assertTrue("Key width should be less than panel width / 3", w < 1080 / 3)
    }

    @Test
    fun testEnterHeightEqualsTwoRowsPlusGap() {
        val panelHeight = 480
        val bottomRowHeight = 88
        val vGap = 8
        val actualRowHeight = (panelHeight - bottomRowHeight - 3 * vGap) / 3
        val expectedHeight = actualRowHeight + vGap + bottomRowHeight

        val geo = T9KeyboardGeometry.calculate(1080, panelHeight, 96, bottomRowHeight, 8, vGap)
        val enterHeight = geo.keyEnterRect.height()
        assertTrue("Enter height = row3 + gap + bottom row", Math.abs(expectedHeight - enterHeight) <= 3)
    }

    @Test
    fun testRowTopsEvenlySpaced() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        val actualRowHeight = geo.rowHeight
        val vGap = 8

        assertEquals("row2Top", geo.key1Rect.top + actualRowHeight + vGap, geo.key4Rect.top)
        assertEquals("row3Top", geo.key4Rect.top + actualRowHeight + vGap, geo.key7Rect.top)
        assertEquals("bottomRowTop", geo.key7Rect.top + actualRowHeight + vGap, geo.keySpaceRect.top)
    }

    @Test
    fun testLeftRailWidthIsOneSeventh() {
        val panelWidth = 1080
        val geo = T9KeyboardGeometry.calculate(panelWidth, 480, 96, 88, 8, 8)
        assertEquals("Left rail should be 1/7 of panel", panelWidth / 7, geo.leftRailWidth)
    }

    @Test
    fun testSymbolButtonBottomEqualsLeftRailBottom() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertEquals("Symbol bottom should equal left rail bottom", geo.leftRailRect.bottom, geo.symbolButtonRect.bottom)
    }

    @Test
    fun testSymbolButtonHeightEqualsBottomRowHeight() {
        val bottomRowHeight = 88
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, bottomRowHeight, 8, 8)
        assertEquals("Symbol button height should equal bottomRowHeight", bottomRowHeight, geo.symbolButtonRect.height())
    }

    @Test
    fun testSymbolButtonBottomEqualsKeySpaceBottom() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertEquals("Symbol button bottom must equal key space bottom", geo.keySpaceRect.bottom, geo.symbolButtonRect.bottom)
    }

    @Test
    fun testSymbolButtonBottomEqualsKeyEnterBottom() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertEquals("Symbol button bottom must equal key enter bottom", geo.keyEnterRect.bottom, geo.symbolButtonRect.bottom)
    }

    @Test
    fun testSymbolButtonTopEqualsKeySpaceTop() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("Symbol button top must equal key space top", Math.abs(geo.keySpaceRect.top - geo.symbolButtonRect.top) <= 3)
    }

    @Test
    fun testLeftRailBottomEqualsKeySpaceBottom() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertEquals("Left rail bottom must equal content bottom", geo.keySpaceRect.bottom, geo.leftRailRect.bottom)
    }

    @Test
    fun testScrollRailBottomLessThanSymbolTop() {
        val geo = T9KeyboardGeometry.calculate(1080, 480, 96, 88, 8, 8)
        assertTrue("Scroll rail bottom must be less than symbol top", geo.leftRailScrollRect.bottom < geo.symbolButtonRect.top)
    }

    @Test
    fun testContentBottomAlignment() {
        val panelHeight = 480
        val geo = T9KeyboardGeometry.calculate(1080, panelHeight, 96, 88, 8, 8)
        assertEquals("Content bottom", panelHeight, geo.keySpaceRect.bottom)
    }

    @Test
    fun testLeftRailDoesNotUsePanelHeightForBottom() {
        val panelH = 512
        val geo = T9KeyboardGeometry.calculate(1080, panelH, 96, 88, 8, 8)
        assertEquals("Left rail bottom must be equal to panel height when fixing height", panelH, geo.leftRailRect.bottom)
    }
}
