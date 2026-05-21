package io.github.xiwei753.pinyin.t9

import android.graphics.Rect

class T9KeyboardGeometry(
    val panelWidth: Int,
    val panelHeight: Int,
    val leftRailWidth: Int,
    val leftRailRect: Rect,
    val leftRailScrollRect: Rect,
    val symbolContentRect: Rect,
    val numberLeftTopRect: Rect,
    val numberLeftBottomRect: Rect,
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

            val availableHeight = panelHeight
            val actualRowHeight = (availableHeight - bottomRowHeight - 3 * verticalGap) / 3
            val row1Top = 0
            val row2Top = row1Top + actualRowHeight + verticalGap
            val row3Top = row2Top + actualRowHeight + verticalGap
            val bottomRowTop = row3Top + actualRowHeight + verticalGap
            val contentBottom = availableHeight

            val leftRailRect = Rect(0, 0, leftRailWidth, contentBottom)
            val symbolButtonRect = Rect(0, contentBottom - bottomRowHeight, leftRailWidth, contentBottom)
            val leftRailScrollRect = Rect(0, 0, leftRailWidth, symbolButtonRect.top - verticalGap)

            val numLeftTop = Rect(0, row1Top, leftRailWidth - horizontalGap, row2Top + actualRowHeight)
            val numLeftBottom = Rect(0, row3Top, leftRailWidth - horizontalGap, row3Top + actualRowHeight)

            val keyWidth = (midAreaWidth - 4 * horizontalGap) / 3
            val midLeft = leftRailWidth
            val rightColLeft = midLeft + midAreaWidth
            val symbolContentRect = Rect(0, 0, rightColLeft, symbolButtonRect.top - verticalGap)

            fun colX(col: Int) = midLeft + col * (keyWidth + horizontalGap) + horizontalGap

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
                symbolContentRect = symbolContentRect,
                numberLeftTopRect = numLeftTop,
                numberLeftBottomRect = numLeftBottom,
                symbolButtonRect = symbolButtonRect,
                key1Rect = Rect(colX(0), row1Top, colX(0) + keyWidth, row1Top + actualRowHeight),
                key2Rect = Rect(colX(1), row1Top, colX(1) + keyWidth, row1Top + actualRowHeight),
                key3Rect = Rect(colX(2), row1Top, colX(2) + keyWidth, row1Top + actualRowHeight),
                key4Rect = Rect(colX(0), row2Top, colX(0) + keyWidth, row2Top + actualRowHeight),
                key5Rect = Rect(colX(1), row2Top, colX(1) + keyWidth, row2Top + actualRowHeight),
                key6Rect = Rect(colX(2), row2Top, colX(2) + keyWidth, row2Top + actualRowHeight),
                key7Rect = Rect(colX(0), row3Top, colX(0) + keyWidth, row3Top + actualRowHeight),
                key8Rect = Rect(colX(1), row3Top, colX(1) + keyWidth, row3Top + actualRowHeight),
                key9Rect = Rect(colX(2), row3Top, colX(2) + keyWidth, row3Top + actualRowHeight),
                keyDelRect = Rect(rightColLeft, row1Top, rightColLeft + rightKeyWidth, row1Top + actualRowHeight),
                keyRetypeRect = Rect(rightColLeft, row2Top, rightColLeft + rightKeyWidth, row2Top + actualRowHeight),
                keyEnterRect = Rect(rightColLeft, row3Top, rightColLeft + rightKeyWidth, contentBottom),
                keyNumberToggleRect = Rect(bottomCol0, bottomRowTop, bottomCol0 + numW, contentBottom),
                keySpaceRect = Rect(bottomCol1, bottomRowTop, bottomCol1 + spaceW, contentBottom),
                keyEnglishToggleRect = Rect(bottomCol2, bottomRowTop, bottomCol2 + engW, contentBottom),
                rowHeight = actualRowHeight,
                bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap,
                verticalGap = verticalGap,
            )
        }
    }
}
