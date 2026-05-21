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
            assertEquals("Placeholder $i should be INVISIBLE", View.INVISIBLE, cell.visibility)
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
            assertEquals(View.INVISIBLE, cell.visibility)
        }

        assertEquals("Should have 1 generated view", 1, generatedViews.size)
    }
}
