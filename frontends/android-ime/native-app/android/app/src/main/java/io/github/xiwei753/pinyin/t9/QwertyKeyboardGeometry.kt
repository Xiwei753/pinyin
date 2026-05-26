package io.github.xiwei753.pinyin.t9

import android.graphics.Rect

class QwertyKeyboardGeometry(
    val row1Keys: List<Rect>,
    val row2Keys: List<Rect>,
    val row3Keys: List<Rect>,
    val delRect: Rect,
    val enterRect: Rect,
    val bottomLeftRect: Rect,
    val spaceRect: Rect,
    val bottomRightRect: Rect,
    val rowHeight: Int,
    val bottomRowHeight: Int,
) {
    companion object {
        fun calculate(
            panelWidth: Int,
            panelHeight: Int,
            rowHeight: Int,
            bottomRowHeight: Int,
            horizontalGap: Int,
            verticalGap: Int,
        ): QwertyKeyboardGeometry {
            val availableHeight = panelHeight
            val actualRowHeight = (availableHeight - bottomRowHeight - 3 * verticalGap) / 3
            val row1Top = 0
            val row2Top = row1Top + actualRowHeight + verticalGap
            val row3Top = row2Top + actualRowHeight + verticalGap
            val bottomRowTop = row3Top + actualRowHeight + verticalGap

            val rightColWidth = panelWidth / 6
            val keyAreaWidth = panelWidth - rightColWidth

            // Row 1: 10 letter keys (Q-P) + del on right
            val row1KeyWidth = (keyAreaWidth - 11 * horizontalGap) / 10
            val row1Keys = mutableListOf<Rect>()
            for (col in 0..9) {
                val x = horizontalGap + col * (row1KeyWidth + horizontalGap)
                row1Keys.add(Rect(x, row1Top, x + row1KeyWidth, row1Top + actualRowHeight))
            }

            val delRect = Rect(
                keyAreaWidth + horizontalGap, row1Top,
                panelWidth - horizontalGap, row1Top + actualRowHeight
            )

            // Row 2: 9 letter keys (A-L), indented by half key to center
            val row2KeyWidth = (keyAreaWidth - 10 * horizontalGap) / 9
            val row2Indent = (row1KeyWidth + horizontalGap) / 2
            val row2Keys = mutableListOf<Rect>()
            for (col in 0..8) {
                val x = horizontalGap + row2Indent + col * (row2KeyWidth + horizontalGap)
                row2Keys.add(Rect(x, row2Top, x + row2KeyWidth, row2Top + actualRowHeight))
            }

            val enterRect = Rect(
                keyAreaWidth + horizontalGap, row2Top,
                panelWidth - horizontalGap, row3Top + actualRowHeight
            )

            // Row 3: 7 letter keys (Z-M), indented more to center
            val row3KeyWidth = (keyAreaWidth - 8 * horizontalGap) / 7
            val row3Indent = (row1KeyWidth + horizontalGap) * 3 / 2
            val row3Keys = mutableListOf<Rect>()
            for (col in 0..6) {
                val x = horizontalGap + row3Indent + col * (row3KeyWidth + horizontalGap)
                row3Keys.add(Rect(x, row3Top, x + row3KeyWidth, row3Top + actualRowHeight))
            }

            // Bottom row: 3 items
            val bottomAvailable = panelWidth - 4 * horizontalGap
            val bottomLeftW = bottomAvailable / 6
            val spaceW = bottomAvailable * 3 / 6
            val bottomRightW = bottomAvailable - bottomLeftW - spaceW

            val bottomCol0 = horizontalGap
            val bottomCol1 = bottomCol0 + bottomLeftW + horizontalGap
            val bottomCol2 = bottomCol1 + spaceW + horizontalGap

            val bottomLeftRect = Rect(bottomCol0, bottomRowTop, bottomCol0 + bottomLeftW, bottomRowTop + bottomRowHeight)
            val spaceRect = Rect(bottomCol1, bottomRowTop, bottomCol1 + spaceW, bottomRowTop + bottomRowHeight)
            val bottomRightRect = Rect(bottomCol2, bottomRowTop, bottomCol2 + bottomRightW, bottomRowTop + bottomRowHeight)

            return QwertyKeyboardGeometry(
                row1Keys = row1Keys,
                row2Keys = row2Keys,
                row3Keys = row3Keys,
                delRect = delRect,
                enterRect = enterRect,
                bottomLeftRect = bottomLeftRect,
                spaceRect = spaceRect,
                bottomRightRect = bottomRightRect,
                rowHeight = actualRowHeight,
                bottomRowHeight = bottomRowHeight,
            )
        }
    }
}
