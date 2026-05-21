package io.github.xiwei753.pinyin.t9

data class SymbolGridLayoutMetrics(
    val cellWidth: Int,
    val cellHeight: Int,
    val columnCount: Int = 5,
    val contentInsetLeft: Int,
    val contentInsetRight: Int,
    val contentInsetTop: Int,
    val contentInsetBottom: Int,
    val horizontalGap: Int,
    val verticalGap: Int,
) {
    companion object {
        private const val COLUMNS = 5

        fun fromDp(
            density: Float,
            symbolPanelWidth: Int,
            rowHeight: Int,
            hInsetDp: Float = 4f,
            vInsetDp: Float = 4f,
            hGapDp: Float = 4f,
            vGapDp: Float = 0f,
        ): SymbolGridLayoutMetrics {
            val hInset = (hInsetDp * density).toInt()
            val vInset = (vInsetDp * density).toInt()
            val hGap = (hGapDp * density).toInt()
            val vGap = (vGapDp * density).toInt()

            val availableWidth = symbolPanelWidth - hInset * 2 - hGap * (COLUMNS - 1)
            val cellW = if (availableWidth > 0) availableWidth / COLUMNS else 0

            return SymbolGridLayoutMetrics(
                cellWidth = cellW,
                cellHeight = rowHeight,
                columnCount = COLUMNS,
                contentInsetLeft = hInset,
                contentInsetRight = hInset,
                contentInsetTop = vInset,
                contentInsetBottom = vInset,
                horizontalGap = hGap,
                verticalGap = vGap,
            )
        }

        fun fromGeometry(
            density: Float,
            symbolPanelWidth: Int,
            rowHeight: Int,
        ): SymbolGridLayoutMetrics {
            return fromDp(density, symbolPanelWidth, rowHeight)
        }
    }
}
