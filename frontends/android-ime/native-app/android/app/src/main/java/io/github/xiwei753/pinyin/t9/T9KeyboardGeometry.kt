package io.github.xiwei753.pinyin.t9

import android.graphics.Rect

class T9KeyboardGeometry(
    val panelWidth: Int,
    val panelHeight: Int,
    val leftRailWidth: Int,
    val leftRailRect: Rect,
    val leftRailScrollRect: Rect,
    val symbolButtonRect: Rect,
    val key1Rect: Rect,
    val key2Rect: Rect,
    val key3Rect: Rect,
    val key4Rect: Rect,
    val key5Rect: Rect,
    val key6Rect: Rect,
    val key7Rect: Rect,
    val key8Rect: Rect,
    val key9Rect: Rect,
    val keyDelRect: Rect,
    val keyRetypeRect: Rect,
    val keyEnterRect: Rect,
    val keyNumberToggleRect: Rect,
    val keySpaceRect: Rect,
    val keyEnglishToggleRect: Rect,
    val rowHeight: Int,
    val bottomRowHeight: Int,
    val horizontalGap: Int,
    val verticalGap: Int,
) {
    companion object {
        private const val BOTTOM_SPACE_RATIO = 1.5f
        private const val BOTTOM_UNITS = 3.5f

        fun calculate(
            panelWidth: Int,
            panelHeight: Int,
            rowHeight: Int,
            bottomRowHeight: Int,
            horizontalGap: Int,
            verticalGap: Int,
        ): T9KeyboardGeometry {
            val leftRailWidth = panelWidth / 7
            val rightAreaWidth = panelWidth - leftRailWidth
            val rightKeyWidth = rightAreaWidth / 6
            val midAreaWidth = rightAreaWidth - rightKeyWidth

            val row1Top = 0
            val row2Top = row1Top + rowHeight + verticalGap
            val row3Top = row2Top + rowHeight + verticalGap
            val bottomRowTop = row3Top + rowHeight + verticalGap

            val leftRailRect = Rect(0, 0, leftRailWidth, panelHeight)

            // Symbol button at bottom-left, scroll rail fills above
            val symbolButtonHeight = bottomRowHeight
            val scrollRailBottom = panelHeight - symbolButtonHeight - verticalGap
            val leftRailScrollRect = Rect(0, 0, leftRailWidth, scrollRailBottom)
            val symbolButtonRect = Rect(0, panelHeight - symbolButtonHeight, leftRailWidth, panelHeight)

            // Key width in top 3 rows (3 equal columns)
            val keyWidth = (midAreaWidth - 4 * horizontalGap) / 3
            val midLeft = leftRailWidth
            val rightColLeft = midLeft + midAreaWidth

            fun colX(col: Int) = midLeft + col * (keyWidth + horizontalGap) + horizontalGap

            // Bottom row: wider space key, 123 and 中/英 narrower
            val bottomAvailable = midAreaWidth - 4 * horizontalGap
            val numW = (bottomAvailable / BOTTOM_UNITS).toInt()
            val spaceW = (bottomAvailable * BOTTOM_SPACE_RATIO / BOTTOM_UNITS).toInt()
            val engW = bottomAvailable - numW - spaceW
            val bottomCol0 = midLeft + horizontalGap
            val bottomCol1 = bottomCol0 + numW + horizontalGap
            val bottomCol2 = bottomCol1 + spaceW + horizontalGap

            return T9KeyboardGeometry(
                panelWidth = panelWidth,
                panelHeight = panelHeight,
                leftRailWidth = leftRailWidth,
                leftRailRect = leftRailRect,
                leftRailScrollRect = leftRailScrollRect,
                symbolButtonRect = symbolButtonRect,
                key1Rect = Rect(colX(0), row1Top, colX(0) + keyWidth, row1Top + rowHeight),
                key2Rect = Rect(colX(1), row1Top, colX(1) + keyWidth, row1Top + rowHeight),
                key3Rect = Rect(colX(2), row1Top, colX(2) + keyWidth, row1Top + rowHeight),
                key4Rect = Rect(colX(0), row2Top, colX(0) + keyWidth, row2Top + rowHeight),
                key5Rect = Rect(colX(1), row2Top, colX(1) + keyWidth, row2Top + rowHeight),
                key6Rect = Rect(colX(2), row2Top, colX(2) + keyWidth, row2Top + rowHeight),
                key7Rect = Rect(colX(0), row3Top, colX(0) + keyWidth, row3Top + rowHeight),
                key8Rect = Rect(colX(1), row3Top, colX(1) + keyWidth, row3Top + rowHeight),
                key9Rect = Rect(colX(2), row3Top, colX(2) + keyWidth, row3Top + rowHeight),
                keyDelRect = Rect(rightColLeft, row1Top, rightColLeft + rightKeyWidth, row1Top + rowHeight),
                keyRetypeRect = Rect(rightColLeft, row2Top, rightColLeft + rightKeyWidth, row2Top + rowHeight),
                keyEnterRect = Rect(rightColLeft, row3Top, rightColLeft + rightKeyWidth, bottomRowTop + bottomRowHeight),
                keyNumberToggleRect = Rect(bottomCol0, bottomRowTop, bottomCol0 + numW, bottomRowTop + bottomRowHeight),
                keySpaceRect = Rect(bottomCol1, bottomRowTop, bottomCol1 + spaceW, bottomRowTop + bottomRowHeight),
                keyEnglishToggleRect = Rect(bottomCol2, bottomRowTop, bottomCol2 + engW, bottomRowTop + bottomRowHeight),
                rowHeight = rowHeight,
                bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap,
                verticalGap = verticalGap,
            )
        }
    }
}
