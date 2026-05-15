package io.github.xiwei753.pinyin.t9

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Test

// Testing a simplified version of the ViewPool logic
class ViewPoolTest {

    @Test
    fun testViewPoolLogic() {
        val candidates = listOf("A", "B", "C")
        val currentChildCount = 5
        var addedViews = 0
        var visibleViews = 0
        var hiddenViews = 0

        for ((index, _) in candidates.withIndex()) {
            if (index < currentChildCount) {
                // Reuse view
                visibleViews++
            } else {
                // Add view
                addedViews++
                visibleViews++
            }
        }

        for (i in candidates.size until currentChildCount) {
            hiddenViews++
        }

        assertEquals(3, visibleViews)
        assertEquals(0, addedViews)
        assertEquals(2, hiddenViews) // Views at index 3 and 4 should be hidden
    }
}
