package io.github.xiwei753.pinyin.t9

import android.graphics.Rect
import io.github.xiwei753.pinyin.imecore.RailKind

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
        clipboardHistory: List<String> = emptyList(),
        clipboardPage: Int = 0,
    ): KeyboardLayoutModel {
        return when (state.keyboardMode) {
            KeyboardMode.ChinesePinyin -> buildQwerty(
                panelWidth = panelWidth, panelHeight = panelHeight,
                rowHeight = rowHeight, bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap, verticalGap = verticalGap,
                isEnglish = false,
            )
            KeyboardMode.EnglishQWERTY -> buildQwerty(
                panelWidth = panelWidth, panelHeight = panelHeight,
                rowHeight = rowHeight, bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap, verticalGap = verticalGap,
                isEnglish = true,
            )
            KeyboardMode.ChineseT9, KeyboardMode.EnglishT9 -> buildT9(
                panelWidth = panelWidth,
                panelHeight = panelHeight,
                rowHeight = rowHeight,
                bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap,
                verticalGap = verticalGap,
                readings = when (state.railState.kind) {
                    RailKind.Readings -> state.railState.labels
                    else -> state.readings
                },
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
            KeyboardMode.ClipboardPanel -> buildClipboard(
                panelWidth = panelWidth,
                panelHeight = panelHeight,
                rowHeight = rowHeight,
                bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap,
                verticalGap = verticalGap,
                history = clipboardHistory,
                currentPage = clipboardPage,
            )
            KeyboardMode.SelectionPanel -> buildSelection(
                panelWidth = panelWidth,
                panelHeight = panelHeight,
                rowHeight = rowHeight,
                bottomRowHeight = bottomRowHeight,
                horizontalGap = horizontalGap,
                verticalGap = verticalGap,
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
            val totalGapPunct = (puncts.size - 1) * verticalGap
            val punctHeight = (availableScrollHeight - totalGapPunct) / puncts.size
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
                y += punctHeight + verticalGap
            }
        } else {
            // Composing: display readings
            if (readings.isNotEmpty()) {
                val displayCount = minOf(readings.size, 5) // Show up to 5 readings in rail
                val totalGapRead = maxOf(displayCount - 1, 0) * verticalGap
                val itemHeight = (availableScrollHeight - totalGapRead) / maxOf(displayCount, 1)
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
                    y += itemHeight + verticalGap
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

    fun buildQwerty(
        panelWidth: Int, panelHeight: Int, rowHeight: Int, bottomRowHeight: Int,
        horizontalGap: Int, verticalGap: Int, isEnglish: Boolean,
    ): KeyboardLayoutModel {
        val geo = QwertyKeyboardGeometry.calculate(panelWidth, panelHeight, rowHeight, bottomRowHeight, horizontalGap, verticalGap)
        val keys = mutableListOf<KeyboardKey>()

        val labels = if (isEnglish) {
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        } else {
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
        }
        for (i in 0..9) {
            keys.add(KeyboardKey(
                id = "letter_${labels[i]}", role = KeyboardKeyRole.NORMAL,
                rect = geo.row1Keys[i], label = labels[i],
                action = "letter:${labels[i].lowercase()}",
            ))
        }

        keys.add(KeyboardKey(
            id = "del", role = KeyboardKeyRole.SPECIAL,
            rect = geo.delRect, label = "\u232B", action = "del", isRightRail = true,
        ))

        val row2Labels = if (isEnglish) {
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        } else {
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
        }
        for (i in 0..8) {
            keys.add(KeyboardKey(
                id = "letter_${row2Labels[i]}", role = KeyboardKeyRole.NORMAL,
                rect = geo.row2Keys[i], label = row2Labels[i],
                action = "letter:${row2Labels[i].lowercase()}",
            ))
        }

        keys.add(KeyboardKey(
            id = "enter", role = KeyboardKeyRole.SPECIAL,
            rect = geo.enterRect, label = "\u21B5", action = "enter", isRightRail = true,
        ))

        val row3Labels = if (isEnglish) {
            listOf("z", "x", "c", "v", "b", "n", "m")
        } else {
            listOf("Z", "X", "C", "V", "B", "N", "M")
        }
        for (i in 0..6) {
            keys.add(KeyboardKey(
                id = "letter_${row3Labels[i]}", role = KeyboardKeyRole.NORMAL,
                rect = geo.row3Keys[i], label = row3Labels[i],
                action = "letter:${row3Labels[i].lowercase()}",
            ))
        }

        val bottomLeftLabel = "九键"
        val bottomRightLabel = if (isEnglish) "英/中" else "中/英"

        keys.add(
            KeyboardKey(
                id = "toggle_keyboard_type", role = KeyboardKeyRole.SPECIAL,
                rect = geo.bottomLeftRect, label = bottomLeftLabel,
                action = "toggle:keyboardtype", isBottomRow = true,
            )
        )

        keys.add(
            KeyboardKey(
                id = "space", role = KeyboardKeyRole.SPACE,
                rect = geo.spaceRect, label = "",
                action = "space", isBottomRow = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "toggle_english", role = KeyboardKeyRole.SPECIAL,
                rect = geo.bottomRightRect, label = bottomRightLabel,
                action = "toggle:english", isBottomRow = true,
            )
        )

        return KeyboardLayoutModel(
            keys = keys, leftRailKeys = emptyList(), bottomLeftKey = null,
            panelWidth = panelWidth, panelHeight = panelHeight,
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

        val metrics = SymbolGridLayoutMetrics.fromDp(
            density = density,
            symbolPanelWidth = geo.symbolContentRect.width() - geo.leftRailWidth,
            rowHeight = rowHeight,
        )

        val tabCategories = listOf(
            "punct" to "标点",
            "math" to "数学",
            "bracket" to "括号",
            "other" to "其他",
        )
        val tabCellHeight = metrics.cellHeight
        val tabVGap = metrics.verticalGap
        val tabStartY = geo.key1Rect.top
        for (i in tabCategories.indices) {
            val (cat, label) = tabCategories[i]
            val tabTop = tabStartY + i * (tabCellHeight + tabVGap)
            val r = Rect(geo.leftRailScrollRect.left, tabTop, geo.leftRailScrollRect.right, tabTop + tabCellHeight)
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

        val columns = metrics.columnCount
        val cellWidth = metrics.cellWidth
        val cellHeight = metrics.cellHeight
        val hGap = metrics.horizontalGap
        val vGap = metrics.verticalGap
        val startX = geo.leftRailWidth + metrics.contentInsetLeft
        var startY = geo.key1Rect.top

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

    fun buildClipboard(
        panelWidth: Int,
        panelHeight: Int,
        rowHeight: Int,
        bottomRowHeight: Int,
        horizontalGap: Int,
        verticalGap: Int,
        history: List<String>,
        currentPage: Int,
    ): KeyboardLayoutModel {
        val keys = mutableListOf<KeyboardKey>()
        val geo = T9KeyboardGeometry.calculate(panelWidth, panelHeight, rowHeight, bottomRowHeight, horizontalGap, verticalGap)
        val rowHeights = listOf(geo.key1Rect.height(), geo.key4Rect.height(), geo.key7Rect.height())
        val rowTops = listOf(geo.key1Rect.top, geo.key4Rect.top, geo.key7Rect.top)
        val rightColLeft = geo.keyDelRect.left
        val contentRight = rightColLeft - horizontalGap
        
        if (history.isEmpty()) {
            val firstRowRect = Rect(horizontalGap, rowTops[0], contentRight, rowTops[0] + rowHeights[0])
            keys.add(
                KeyboardKey(
                    id = "clip_empty",
                    role = KeyboardKeyRole.NORMAL,
                    rect = firstRowRect,
                    label = "剪贴板为空",
                    action = "none",
                )
            )
        } else {
            val startIndex = currentPage * 3
            val pageItems = history.drop(startIndex).take(3)
            for (i in pageItems.indices) {
                val text = pageItems[i]
                val rowRect = Rect(horizontalGap, rowTops[i], contentRight, rowTops[i] + rowHeights[i])
                keys.add(
                    KeyboardKey(
                        id = "clip_item_${startIndex + i}",
                        role = KeyboardKeyRole.CLIPBOARD_ITEM,
                        rect = rowRect,
                        label = text,
                        action = "clip:commit",
                        actionPayload = text,
                    )
                )
            }
        }
        
        // Right rail: del and enter (no retype)
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
                id = "enter",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyEnterRect,
                label = "\u21B5",
                action = "enter",
                isRightRail = true,
            )
        )
        
        val bottomRowTop = geo.keyNumberToggleRect.top
        val bottomRowHeightVal = geo.keyNumberToggleRect.height()
        val bottomY = bottomRowTop + bottomRowHeightVal
        
        val availableWidth = contentRight - horizontalGap
        val buttonWidth = (availableWidth - 2 * horizontalGap) / 3
        val x0 = horizontalGap
        val x1 = x0 + buttonWidth + horizontalGap
        val x2 = x1 + buttonWidth + horizontalGap
        
        keys.add(
            KeyboardKey(
                id = "clip_prev",
                role = KeyboardKeyRole.SPECIAL,
                rect = Rect(x0, bottomRowTop, x0 + buttonWidth, bottomY),
                label = "上一页",
                action = "clip:prev",
                isSelected = false,
                isBottomRow = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "clip_back",
                role = KeyboardKeyRole.SPECIAL,
                rect = Rect(x1, bottomRowTop, x1 + buttonWidth, bottomY),
                label = "返回",
                action = "clip:back",
                isSelected = false,
                isBottomRow = true,
            )
        )
        keys.add(
            KeyboardKey(
                id = "clip_next",
                role = KeyboardKeyRole.SPECIAL,
                rect = Rect(x2, bottomRowTop, x2 + buttonWidth, bottomY),
                label = "下一页",
                action = "clip:next",
                isSelected = false,
                isBottomRow = true,
            )
        )
        
        return KeyboardLayoutModel(
            keys = keys,
            leftRailKeys = emptyList(),
            bottomLeftKey = null,
            panelWidth = panelWidth,
            panelHeight = panelHeight,
        )
    }

    fun buildSelection(
        panelWidth: Int,
        panelHeight: Int,
        rowHeight: Int,
        bottomRowHeight: Int,
        horizontalGap: Int,
        verticalGap: Int,
    ): KeyboardLayoutModel {
        val keys = mutableListOf<KeyboardKey>()
        val geo = T9KeyboardGeometry.calculate(panelWidth, panelHeight, rowHeight, bottomRowHeight, horizontalGap, verticalGap)
        val rowHeights = listOf(geo.key1Rect.height(), geo.key4Rect.height(), geo.key7Rect.height())
        val rowTops = listOf(geo.key1Rect.top, geo.key4Rect.top, geo.key7Rect.top)
        val rightColLeft = geo.keyDelRect.left
        val contentRight = rightColLeft - horizontalGap
        val availableWidth = contentRight - horizontalGap
        val buttonWidth = (availableWidth - 2 * horizontalGap) / 3
        val x0 = horizontalGap
        val x1 = x0 + buttonWidth + horizontalGap
        val x2 = x1 + buttonWidth + horizontalGap
        
        // Row 1: [←] [↑] [→]
        keys.add(KeyboardKey("select_left", KeyboardKeyRole.NORMAL, Rect(x0, rowTops[0], x0 + buttonWidth, rowTops[0] + rowHeights[0]), "←", null, "select:left"))
        keys.add(KeyboardKey("select_up", KeyboardKeyRole.NORMAL, Rect(x1, rowTops[0], x1 + buttonWidth, rowTops[0] + rowHeights[0]), "↑", null, "select:up"))
        keys.add(KeyboardKey("select_right", KeyboardKeyRole.NORMAL, Rect(x2, rowTops[0], x2 + buttonWidth, rowTops[0] + rowHeights[0]), "→", null, "select:right"))
        
        // Row 2: [全选] [复制] [粘贴]
        keys.add(KeyboardKey("select_all", KeyboardKeyRole.NORMAL, Rect(x0, rowTops[1], x0 + buttonWidth, rowTops[1] + rowHeights[1]), "全选", null, "select:selectAll"))
        keys.add(KeyboardKey("select_copy", KeyboardKeyRole.NORMAL, Rect(x1, rowTops[1], x1 + buttonWidth, rowTops[1] + rowHeights[1]), "复制", null, "select:copy"))
        keys.add(KeyboardKey("select_paste", KeyboardKeyRole.NORMAL, Rect(x2, rowTops[1], x2 + buttonWidth, rowTops[1] + rowHeights[1]), "粘贴", null, "select:paste"))
        
        // Row 3: [剪切] [↓] [返回]
        keys.add(KeyboardKey("select_cut", KeyboardKeyRole.NORMAL, Rect(x0, rowTops[2], x0 + buttonWidth, rowTops[2] + rowHeights[2]), "剪切", null, "select:cut"))
        keys.add(KeyboardKey("select_down", KeyboardKeyRole.NORMAL, Rect(x1, rowTops[2], x1 + buttonWidth, rowTops[2] + rowHeights[2]), "↓", null, "select:down"))
        keys.add(KeyboardKey("select_back", KeyboardKeyRole.NORMAL, Rect(x2, rowTops[2], x2 + buttonWidth, rowTops[2] + rowHeights[2]), "返回", null, "select:back"))
        
        // Right rail: del and enter (no retype)
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
                id = "enter",
                role = KeyboardKeyRole.SPECIAL,
                rect = geo.keyEnterRect,
                label = "\u21B5",
                action = "enter",
                isRightRail = true,
            )
        )
        
        return KeyboardLayoutModel(
            keys = keys,
            leftRailKeys = emptyList(),
            bottomLeftKey = null,
            panelWidth = panelWidth,
            panelHeight = panelHeight,
        )
    }
}
