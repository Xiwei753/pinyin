package io.github.xiwei753.pinyin.t9

import android.graphics.Rect

data class KeyboardLayoutModel(
    val keys: List<KeyboardKey>,
    val leftRailKeys: List<KeyboardKey>,
    val bottomLeftKey: KeyboardKey?,
    val panelWidth: Int,
    val panelHeight: Int,
)

class KeyboardLayoutBuilder {

    fun build(
        state: KeyboardUiState,
        panelWidth: Int,
        panelHeight: Int,
        rowHeight: Int,
        bottomRowHeight: Int,
        horizontalGap: Int,
        verticalGap: Int,
        categoryToPage: Map<SymbolKeyRegistry.Category, String>,
        registry: SymbolKeyRegistry,
        density: Float,
        symbolEntries: List<Pair<Int, String>> = emptyList(),
    ): KeyboardLayoutModel {
        return when (state.keyboardMode) {
            KeyboardMode.ChineseT9, KeyboardMode.EnglishT9 -> buildT9(
                panelWidth = panelWidth,
                panelHeight = panelHeight,
                rowHeight = rowHeight,
                bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap,
                verticalGap = verticalGap,
                readings = state.railState.labels.takeIf { state.railState.kind.name == "Readings" } ?: state.readings,
                isComposing = state.isComposing,
                keyboardMode = state.keyboardMode,
                activeReading = state.activeReading,
            )
            KeyboardMode.Number -> buildNumber(
                panelWidth = panelWidth,
                panelHeight = panelHeight,
                rowHeight = rowHeight,
                bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap,
                verticalGap = verticalGap,
                keyboardMode = state.keyboardMode,
                lastTextMode = state.lastTextMode,
            )
            KeyboardMode.Symbol -> buildSymbol(
                panelWidth = panelWidth,
                panelHeight = panelHeight,
                rowHeight = rowHeight,
                bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap,
                verticalGap = verticalGap,
                symbolEntries = symbolEntries,
                activeCategory = state.currentSymCategory,
                lastTextMode = state.lastTextMode,
                categoryToPage = categoryToPage,
                registry = registry,
                density = density,
            )
        }
    }

    fun buildT9(
        panelWidth: Int,
        panelHeight: Int,
        rowHeight: Int,
        bottomRowHeight: Int,
        horizontalGap: Int,
        verticalGap: Int,
        readings: List<String>,
        isComposing: Boolean,
        keyboardMode: KeyboardMode,
        activeReading: String? = null,
    ): KeyboardLayoutModel {
        val geo = T9KeyboardGeometry.calculate(panelWidth, panelHeight, rowHeight, bottomRowHeight, horizontalGap, verticalGap)
        val keys = mutableListOf<KeyboardKey>()
        val leftRailKeys = mutableListOf<KeyboardKey>()

        val availableScrollHeight = geo.leftRailScrollRect.height()

        if (!isComposing) {
            // Empty buffer: display punctuation
            val puncts = listOf("，", "。", "？", "！")
            val punctHeight = availableScrollHeight / puncts.size
            var y = geo.leftRailScrollRect.top
            for (punctText in puncts) {
                val r = Rect(geo.leftRailScrollRect.left, y, geo.leftRailScrollRect.right, y + punctHeight)
                leftRailKeys.add(
                    KeyboardKey(
                        id = "punct_$punctText",
                        role = KeyboardKeyRole.RAIL_PUNCT,
                        rect = r,
                        label = punctText,
                        action = "punct:$punctText",
                        isLeftRail = true,
                    )
                )
                y += punctHeight
            }
        } else {
            // Composing: display readings
            if (readings.isNotEmpty()) {
                val displayCount = minOf(readings.size, 5) // Show up to 5 readings in rail
                val itemHeight = availableScrollHeight / maxOf(displayCount, 1)
                var y = geo.leftRailScrollRect.top
                for (i in 0 until displayCount) {
                    val reading = readings[i]
                    val r = Rect(geo.leftRailScrollRect.left, y, geo.leftRailScrollRect.right, y + itemHeight)
                    leftRailKeys.add(
                        KeyboardKey(
                            id = "reading_$i",
                            role = KeyboardKeyRole.RAIL_READING,
                            rect = r,
                            label = reading,
                            action = "reading:$i",
                            isLeftRail = true,
                            isSelected = reading == activeReading
                        )
                    )
                    y += itemHeight
                }
            }
        }

        val bottomLeftKey = KeyboardKey(
            id = "symbol_toggle",
            role = KeyboardKeyRole.SPECIAL,
            rect = geo.symbolButtonRect,
            label = "符",
            action = "toggle:symbol",
            isLeftRail = true,
            isBottomRow = true,
        )

        keys.add(
            KeyboardKey(
                id = "key_1",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.key1Rect,
                label = "分词",
                action = "separator",
            )
        )

        for (d in 2..9) {
            val letters = when (d) {
                2 -> "ABC"
                3 -> "DEF"
                4 -> "GHI"
                5 -> "JKL"
                6 -> "MNO"
                7 -> "PQRS"
                8 -> "TUV"
                9 -> "WXYZ"
                else -> ""
            }
            val keyRect = when (d) {
                2 -> geo.key2Rect
                3 -> geo.key3Rect
                4 -> geo.key4Rect
                5 -> geo.key5Rect
                6 -> geo.key6Rect
                7 -> geo.key7Rect
                8 -> geo.key8Rect
                9 -> geo.key9Rect
                else -> Rect()
            }
            keys.add(
                KeyboardKey(
                    id = "key_$d",
                    role = KeyboardKeyRole.NORMAL,
                    rect = keyRect,
                    label = d.toString(),
                    subLabel = letters,
                    action = "digit:$d",
                )
            )
        }

        keys.add(
            KeyboardKey(
                id = "del",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyDelRect,
                label = "\u232B",
                action = "del",
                isRightRail = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "retype",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyRetypeRect,
                label = "\u91CD\u8F93",
                action = "retype",
                isRightRail = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "enter",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyEnterRect,
                label = "\u21B5",
                action = "enter",
                isRightRail = true,
            )
        )

        keys.add(
            KeyboardKey(
                id = "toggle_number",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyNumberToggleRect,
                label = "123",
                action = "toggle:number",
                isBottomRow = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "space",
                role = KeyboardKeyRole.SPACE,
                rect = geo.keySpaceRect,
                label = "",
                action = "space",
                isBottomRow = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "toggle_english",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyEnglishToggleRect,
                label = if (keyboardMode == KeyboardMode.EnglishT9) "英/中" else "中/英",
                action = "toggle:english",
                isBottomRow = true,
            )
        )

        return KeyboardLayoutModel(
            keys = keys,
            leftRailKeys = leftRailKeys,
            bottomLeftKey = bottomLeftKey,
            panelWidth = panelWidth,
            panelHeight = panelHeight,
        )
    }

    fun buildNumber(
        panelWidth: Int,
        panelHeight: Int,
        rowHeight: Int,
        bottomRowHeight: Int,
        horizontalGap: Int,
        verticalGap: Int,
        keyboardMode: KeyboardMode,
        lastTextMode: KeyboardMode,
    ): KeyboardLayoutModel {
        val geo = T9KeyboardGeometry.calculate(panelWidth, panelHeight, rowHeight, bottomRowHeight, horizontalGap, verticalGap)
        val keys = mutableListOf<KeyboardKey>()
        val leftRailKeys = mutableListOf<KeyboardKey>()

        val dotRect = geo.numberLeftTopRect
        leftRailKeys.add(
            KeyboardKey(
                id = "num_dot",
                role = KeyboardKeyRole.RAIL_NUMBER_AUX,
                rect = dotRect,
                label = ".",
                action = "digit:.",
                isLeftRail = true,
            )
        )
        val zeroRect = geo.numberLeftBottomRect
        leftRailKeys.add(
            KeyboardKey(
                id = "num_0",
                role = KeyboardKeyRole.RAIL_NUMBER_AUX,
                rect = zeroRect,
                label = "0",
                action = "digit:0",
                isLeftRail = true,
            )
        )

        val bottomLeftKey = KeyboardKey(
            id = "symbol_toggle",
            role = KeyboardKeyRole.SPECIAL,
            rect = geo.symbolButtonRect,
            label = "符",
            action = "toggle:symbol",
            isLeftRail = true,
            isBottomRow = true,
        )

        for (d in 1..9) {
            val keyRect = when (d) {
                1 -> geo.key1Rect
                2 -> geo.key2Rect
                3 -> geo.key3Rect
                4 -> geo.key4Rect
                5 -> geo.key5Rect
                6 -> geo.key6Rect
                7 -> geo.key7Rect
                8 -> geo.key8Rect
                9 -> geo.key9Rect
                else -> Rect()
            }
            keys.add(
                KeyboardKey(
                    id = "num_$d",
                    role = KeyboardKeyRole.NORMAL,
                    rect = keyRect,
                    label = d.toString(),
                    action = "digit:$d",
                )
            )
        }

        keys.add(
            KeyboardKey(
                id = "del",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyDelRect,
                label = "\u232B",
                action = "del",
                isRightRail = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "retype",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyRetypeRect,
                label = "\u91CD\u8F93",
                action = "retype",
                isRightRail = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "enter",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyEnterRect,
                label = "\u21B5",
                action = "enter",
                isRightRail = true,
            )
        )

        val numToggleLabel = if (lastTextMode == KeyboardMode.EnglishT9) "英" else "中"
        keys.add(
            KeyboardKey(
                id = "toggle_number",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyNumberToggleRect,
                label = numToggleLabel,
                action = "toggle:number",
                isBottomRow = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "space",
                role = KeyboardKeyRole.SPACE,
                rect = geo.keySpaceRect,
                label = "",
                action = "space",
                isBottomRow = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "toggle_english",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyEnglishToggleRect,
                label = "中/英",
                action = "toggle:english",
                isBottomRow = true,
            )
        )

        return KeyboardLayoutModel(
            keys = keys,
            leftRailKeys = leftRailKeys,
            bottomLeftKey = bottomLeftKey,
            panelWidth = panelWidth,
            panelHeight = panelHeight,
        )
    }

    fun buildSymbol(
        panelWidth: Int,
        panelHeight: Int,
        rowHeight: Int,
        bottomRowHeight: Int,
        horizontalGap: Int,
        verticalGap: Int,
        symbolEntries: List<Pair<Int, String>>,
        activeCategory: String,
        lastTextMode: KeyboardMode,
        categoryToPage: Map<SymbolKeyRegistry.Category, String>,
        registry: SymbolKeyRegistry,
        density: Float,
    ): KeyboardLayoutModel {
        val geo = T9KeyboardGeometry.calculate(panelWidth, panelHeight, rowHeight, bottomRowHeight, horizontalGap, verticalGap)
        val keys = mutableListOf<KeyboardKey>()
        val leftRailKeys = mutableListOf<KeyboardKey>()

        val availableScrollHeight = geo.leftRailScrollRect.height()

        val tabCategories = listOf(
            "punct" to "标点",
            "math" to "数学",
            "bracket" to "括号",
            "other" to "其他",
        )
        val tabHeight = availableScrollHeight / tabCategories.size
        var y = geo.leftRailScrollRect.top
        for ((cat, label) in tabCategories) {
            val r = Rect(geo.leftRailScrollRect.left, y, geo.leftRailScrollRect.right, y + tabHeight)
            leftRailKeys.add(
                KeyboardKey(
                    id = "sym_tab_$cat",
                    role = KeyboardKeyRole.RAIL_SYMBOL_CATEGORY,
                    rect = r,
                    label = label,
                    action = "symtab:$cat",
                    isLeftRail = true,
                    isSelected = activeCategory == cat
                )
            )
            y += tabHeight
        }

        val returnLabel = if (lastTextMode == KeyboardMode.EnglishT9) "英" else "中"
        val bottomLeftKey = KeyboardKey(
            id = "symbol_toggle",
            role = KeyboardKeyRole.SPECIAL,
            rect = geo.symbolButtonRect,
            label = returnLabel,
            action = "toggle:symbol",
            isLeftRail = true,
            isBottomRow = true,
        )

        val pageName = activeCategory
        val catEntries = if (symbolEntries.isNotEmpty()) {
            symbolEntries
        } else {
            getEntriesForPage(pageName, registry, categoryToPage)
        }

        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = density,
            symbolPanelWidth = geo.symbolContentRect.width() - geo.leftRailWidth,
            rowHeight = rowHeight,
        )
        val columns = metrics.columnCount
        val cellWidth = metrics.cellWidth
        val cellHeight = metrics.cellHeight
        val hGap = metrics.horizontalGap
        val vGap = metrics.verticalGap
        val startX = geo.leftRailWidth + metrics.contentInsetLeft
        var startY = geo.symbolContentRect.top + metrics.contentInsetTop

        if (catEntries.isNotEmpty()) {
            val rows = catEntries.size / columns + if (catEntries.size % columns > 0) 1 else 0
            for (row in 0 until rows) {
                val rowY = startY + row * (cellHeight + vGap)
                if (rowY + cellHeight > geo.symbolContentRect.bottom) break
                for (col in 0 until columns) {
                    val idx = row * columns + col
                    val cellX = startX + col * (cellWidth + hGap)
                    if (idx < catEntries.size) {
                        val (_, text) = catEntries[idx]
                        val r = Rect(cellX, rowY, cellX + cellWidth, rowY + cellHeight)
                        keys.add(
                            KeyboardKey(
                                id = "symbol_$idx",
                                role = KeyboardKeyRole.SYMBOL_KEY,
                                rect = r,
                                label = text,
                                action = "symbol:commit",
                                actionPayload = text,
                            )
                        )
                    } else if (row == rows - 1) { // Only placeholder in last row
                        val r = Rect(cellX, rowY, cellX + cellWidth, rowY + cellHeight)
                        keys.add(
                            KeyboardKey(
                                id = "symbol_placeholder_${row}_${col}",
                                role = KeyboardKeyRole.PLACEHOLDER,
                                rect = r,
                                label = "",
                                action = "none",
                            )
                        )
                    }
                }
            }
        }

        keys.add(
            KeyboardKey(
                id = "del",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyDelRect,
                label = "\u232B",
                action = "del",
                isRightRail = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "retype",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyRetypeRect,
                label = "\u91CD\u8F93",
                action = "retype",
                isRightRail = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "enter",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyEnterRect,
                label = "\u21B5",
                action = "enter",
                isRightRail = true,
            )
        )

        keys.add(
            KeyboardKey(
                id = "toggle_number",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyNumberToggleRect,
                label = "123",
                action = "toggle:number",
                isBottomRow = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "space",
                role = KeyboardKeyRole.SPACE,
                rect = geo.keySpaceRect,
                label = "",
                action = "space",
                isBottomRow = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "symbol_bottom_right_placeholder",
                role = KeyboardKeyRole.PLACEHOLDER,
                rect = geo.keyEnglishToggleRect,
                label = "",
                action = "none",
                isBottomRow = true,
            )
        )

        return KeyboardLayoutModel(
            keys = keys,
            leftRailKeys = leftRailKeys,
            bottomLeftKey = bottomLeftKey,
            panelWidth = panelWidth,
            panelHeight = panelHeight,
        )
    }

    private fun getEntriesForPage(
        pageName: String,
        registry: SymbolKeyRegistry,
        categoryToPage: Map<SymbolKeyRegistry.Category, String>,
    ): List<Pair<Int, String>> {
        if (categoryToPage.isEmpty()) {
            return registry.getAllSymbolEntries().take(60)
        }
        val results = mutableListOf<Pair<Int, String>>()
        for (category in registry.getAllCategories()) {
            if (categoryToPage[category] == pageName) {
                results.addAll(registry.getSymbolsByCategory(category))
            }
        }
        return results
    }
}
