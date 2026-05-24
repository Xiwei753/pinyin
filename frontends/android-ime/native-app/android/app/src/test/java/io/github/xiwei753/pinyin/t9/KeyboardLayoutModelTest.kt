package io.github.xiwei753.pinyin.t9

import android.graphics.Rect
import io.github.xiwei753.pinyin.imecore.RailKind
import io.github.xiwei753.pinyin.imecore.RailState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyboardLayoutModelTest {

    private val builder = KeyboardLayoutBuilder()
    private val registry = SymbolKeyRegistry()

    private val categoryToPage = mapOf(
        SymbolKeyRegistry.Category.FULLWIDTH_PUNCT to "punct",
        SymbolKeyRegistry.Category.HALFWIDTH_PUNCT to "punct",
        SymbolKeyRegistry.Category.MATH to "math",
        SymbolKeyRegistry.Category.BRACKET to "bracket",
        SymbolKeyRegistry.Category.CURRENCY to "other",
        SymbolKeyRegistry.Category.UNIT to "other",
        SymbolKeyRegistry.Category.NETWORK to "other",
        SymbolKeyRegistry.Category.SEQUENCE to "other",
        SymbolKeyRegistry.Category.ARROW to "other",
        SymbolKeyRegistry.Category.GREEK to "other",
        SymbolKeyRegistry.Category.OTHER to "other",
    )

    @Test
    fun t9ModeGeneratesCorrectNumberOfKeys() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)

        assertTrue("T9 mode should have keys in main area", model.keys.size >= 15)
        assertTrue("T9 mode should have left rail keys", model.leftRailKeys.isNotEmpty())
        assertNotNull("T9 mode should have bottom left key", model.bottomLeftKey)

        val digitKeys = allKeys.filter { it.action.startsWith("digit:") }
        assertEquals("Should have 8 digit keys (2-9)", 8, digitKeys.size)

        val separatorKey = allKeys.find { it.action == "separator" }
        assertNotNull("Should have separator key", separatorKey)
        assertEquals("key_1", separatorKey!!.id)
    }

    @Test
    fun numberModeGeneratesCorrectNumberOfKeys() {
        val model = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)

        val digitKeys = model.keys.filter { it.action.startsWith("digit:") }
        assertEquals("Number mode should have 9 digit keys", 9, digitKeys.size)

        val leftRailNumberKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_NUMBER_AUX }
        assertEquals("Number mode should have dot and 0 in left rail", 2, leftRailNumberKeys.size)

        val hasDel = model.keys.any { it.action == "del" }
        assertTrue("Should have delete key", hasDel)

        val hasEnter = model.keys.any { it.action == "enter" }
        assertTrue("Should have enter key", hasEnter)
    }

    @Test
    fun symbolModeGeneratesCategoryTabsAndSymbolKeys() {
        val entries = registry.getAllSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)

        val tabKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_SYMBOL_CATEGORY }
        assertEquals("Should have 4 category tabs", 4, tabKeys.size)

        val symbolKeys = model.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }
        assertTrue("Should have symbol keys", symbolKeys.isNotEmpty())

        val placeholderKeys = model.keys.filter { it.role == KeyboardKeyRole.PLACEHOLDER }
        placeholderKeys.forEach {
            assertEquals("Placeholder should have 'none' action", "none", it.action)
        }
    }

    @Test
    fun buildUsesRailKindReadingsForLeftRail() {
        val model = builder.build(
            state = state(railState = RailState(RailKind.Readings, listOf("wo", "yo")), isComposing = true, readings = emptyList()),
            panelWidth = 1080,
            panelHeight = 480,
            rowHeight = 96,
            bottomRowHeight = 88,
            horizontalGap = 8,
            verticalGap = 8,
            categoryToPage = categoryToPage,
            registry = registry,
            density = 2.5f,
        )

        assertTrue(model.leftRailKeys.any { it.role == KeyboardKeyRole.RAIL_READING && it.label == "wo" })
    }

    @Test
    fun buildUsesRailKindPunctuationForLeftRail() {
        val model = builder.build(
            state = state(railState = RailState(RailKind.Punctuation), isComposing = false),
            panelWidth = 1080,
            panelHeight = 480,
            rowHeight = 96,
            bottomRowHeight = 88,
            horizontalGap = 8,
            verticalGap = 8,
            categoryToPage = categoryToPage,
            registry = registry,
            density = 2.5f,
        )

        assertTrue(model.leftRailKeys.any { it.role == KeyboardKeyRole.RAIL_PUNCT && it.label == "，" })
    }

    @Test
    fun buildSymbolCategoriesAndNumberAuxFromStateModes() {
        val symbolModel = builder.build(
            state = state(keyboardMode = KeyboardMode.Symbol, railState = RailState(RailKind.SymbolCategories), currentSymCategory = "punct"),
            panelWidth = 1080,
            panelHeight = 480,
            rowHeight = 96,
            bottomRowHeight = 88,
            horizontalGap = 8,
            verticalGap = 8,
            categoryToPage = categoryToPage,
            registry = registry,
            density = 2.5f,
            symbolEntries = listOf(1 to "，"),
        )
        val numberModel = builder.build(
            state = state(keyboardMode = KeyboardMode.Number, railState = RailState(RailKind.NumberAux)),
            panelWidth = 1080,
            panelHeight = 480,
            rowHeight = 96,
            bottomRowHeight = 88,
            horizontalGap = 8,
            verticalGap = 8,
            categoryToPage = categoryToPage,
            registry = registry,
            density = 2.5f,
        )

        assertEquals(4, symbolModel.leftRailKeys.count { it.role == KeyboardKeyRole.RAIL_SYMBOL_CATEGORY })
        assertTrue(numberModel.leftRailKeys.any { it.role == KeyboardKeyRole.RAIL_NUMBER_AUX && it.label == "." })
        assertTrue(numberModel.leftRailKeys.any { it.role == KeyboardKeyRole.RAIL_NUMBER_AUX && it.label == "0" })
    }

    @Test
    fun symbolBottomLeftKeyLabelFollowsLastTextMode() {
        val entries = listOf(1 to "，")

        val chineseModel = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val englishModel = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.EnglishT9, categoryToPage, registry, density = 2.5f)

        assertEquals("中", chineseModel.bottomLeftKey!!.label)
        assertEquals("英", englishModel.bottomLeftKey!!.label)
        assertEquals("toggle:symbol", chineseModel.bottomLeftKey!!.action)
        assertEquals("toggle:symbol", englishModel.bottomLeftKey!!.action)
    }

    @Test
    fun symbolModeDoesNotGenerateDuplicateSymbolButton() {
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, listOf(1 to "，"), "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)

        assertEquals(0, allKeys.count { it.label == "符" })
        assertEquals(1, allKeys.count { it.action == "toggle:symbol" })
    }

    @Test
    fun symbolBottomRowActionsRemainNumberAndSpace() {
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, listOf(1 to "，"), "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)

        val numberKey = model.keys.find { it.id == "toggle_number" }
        val spaceKey = model.keys.find { it.id == "space" }

        assertNotNull(numberKey)
        assertEquals("123", numberKey!!.label)
        assertEquals("toggle:number", numberKey.action)
        assertNotNull(spaceKey)
        assertEquals("space", spaceKey!!.action)
    }

    @Test
    fun symbolBottomRightKeyIsDisabledPlaceholder() {
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, listOf(1 to "，"), "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)

        val placeholder = model.keys.find { it.id == "symbol_bottom_right_placeholder" }

        assertNotNull(placeholder)
        assertEquals(KeyboardKeyRole.PLACEHOLDER, placeholder!!.role)
        assertEquals("", placeholder.label)
        assertEquals("none", placeholder.action)
    }

    @Test
    fun buildSymbolBottomLeftKeyLabelDependsOnLastTextMode() {
        val modelChinese = builder.buildSymbol(1080, 480, 96, 88, 8, 8, listOf(1 to "，"), "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        assertNotNull(modelChinese.bottomLeftKey)
        assertEquals("中", modelChinese.bottomLeftKey!!.label)
        assertEquals("toggle:symbol", modelChinese.bottomLeftKey!!.action)

        val modelEnglish = builder.buildSymbol(1080, 480, 96, 88, 8, 8, listOf(1 to "，"), "punct", KeyboardMode.EnglishT9, categoryToPage, registry, density = 2.5f)
        assertNotNull(modelEnglish.bottomLeftKey)
        assertEquals("英", modelEnglish.bottomLeftKey!!.label)
        assertEquals("toggle:symbol", modelEnglish.bottomLeftKey!!.action)
    }

    @Test
    fun buildSymbolNoDuplicateSymbolButton() {
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, listOf(1 to "，"), "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)

        // Assert that there is no key with label "符" in Symbol mode layout.
        val fuKeys = allKeys.filter { it.label == "符" }
        assertTrue("Symbol mode should not generate duplicate '符' button", fuKeys.isEmpty())
    }

    @Test
    fun buildSymbolKeyActionsAndSpace() {
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, listOf(1 to "，"), "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val numberKey = model.keys.find { it.id == "toggle_number" }
        val spaceKey = model.keys.find { it.id == "space" }

        assertNotNull(numberKey)
        assertEquals("123", numberKey!!.label)
        assertEquals("toggle:number", numberKey.action)

        assertNotNull(spaceKey)
        assertEquals("space", spaceKey!!.action)
    }

    @Test
    fun allKeyRectsAreWithinKeyboardBounds() {
        val panelW = 1080
        val panelH = 480
        val model = builder.buildT9(panelW, panelH, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)

        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)
        for (key in allKeys) {
            assertTrue("Key ${key.id} left >= 0", key.rect.left >= 0)
            assertTrue("Key ${key.id} right <= panelWidth", key.rect.right <= panelW)
            assertTrue("Key ${key.id} top >= 0", key.rect.top >= 0)
            assertTrue("Key ${key.id} bottom <= panelHeight", key.rect.bottom <= panelH)
        }
    }

    @Test
    fun digitKeys2Through9AllHaveCompleteRects() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)

        for (d in 2..9) {
            val key = model.keys.find { it.id == "key_$d" }
            assertNotNull("Key $d should exist", key)
            assertTrue("Key $d should have non-empty rect", !key!!.rect.isEmpty)
            assertTrue("Key $d width > 0", key.rect.width() > 0)
            assertTrue("Key $d height > 0", key.rect.height() > 0)
        }
    }

    @Test
    fun keyRectsDoNotOverlap() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)

        for (i in allKeys.indices) {
            for (j in i + 1 until allKeys.size) {
                val a = allKeys[i]
                val b = allKeys[j]
                val overlap = Rect.intersects(a.rect, b.rect)
                assertFalse("Keys ${a.id} and ${b.id} should not overlap", overlap)
            }
        }
    }

    @Test
    fun symbolPlaceholderNotClickable() {
        val entries = listOf(1 to "X")
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 2.5f)

        val placeholders = model.keys.filter { it.role == KeyboardKeyRole.PLACEHOLDER }
        for (ph in placeholders) {
            assertEquals("none", ph.action)
            assertEquals("", ph.label)
        }
    }

    @Test
    fun numberModeKeysAllHavePositiveDimensions() {
        val model = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)

        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)
        for (key in allKeys) {
            if (key.role == KeyboardKeyRole.PLACEHOLDER) continue
            assertTrue("Key ${key.id} should have positive width", key.rect.width() > 0)
            assertTrue("Key ${key.id} should have positive height", key.rect.height() > 0)
        }
    }

    @Test
    fun symbolKeysHaveActionPayload() {
        val entries = listOf(1 to "A", 2 to "B")
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 2.5f)

        val symbolKeys = model.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }
        assertTrue("Should have 2 symbol keys", symbolKeys.size >= 2)
        for (key in symbolKeys) {
            assertEquals("symbol:commit", key.action)
            assertNotNull("Symbol key should have actionPayload", key.actionPayload)
        }
    }

    @Test
    fun spaceKeyHasCorrectAction() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        val spaceKey = model.keys.find { it.action == "space" }
        assertNotNull("Should have space key", spaceKey)
        assertEquals(KeyboardKeyRole.SPACE, spaceKey!!.role)
    }

    @Test
    fun buildT9WithComposingTrueGeneratesReadingsKeys() {
        val readings = listOf("mi", "qu", "a")
        val model = builder.buildT9(
            panelWidth = 1080,
            panelHeight = 480,
            rowHeight = 96,
            bottomRowHeight = 88,
            horizontalGap = 8,
            verticalGap = 8,
            readings = readings,
            isComposing = true,
            keyboardMode = KeyboardMode.ChineseT9,
            activeReading = "qu"
        )

        assertEquals("Should have exactly 3 reading keys when composing with 3 readings", 3, model.leftRailKeys.size)
        val expectedReadings = listOf("mi", "qu", "a")
        for (i in 0 until 3) {
            val key = model.leftRailKeys[i]
            assertEquals(KeyboardKeyRole.RAIL_READING, key.role)
            assertEquals("reading_$i", key.id)
            assertEquals(expectedReadings[i], key.label)
        }
    }

    @Test
    fun buildT9GeneratesLeftRailReadingsWhenComposing() {
        val readings = listOf("mi", "qu", "a", "xian", "sheng")
        val model = builder.buildT9(
            panelWidth = 1080,
            panelHeight = 480,
            rowHeight = 96,
            bottomRowHeight = 88,
            horizontalGap = 8,
            verticalGap = 8,
            readings = readings,
            isComposing = true,
            keyboardMode = KeyboardMode.ChineseT9
        )

        val readingKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_READING }
        assertEquals("Should contain 5 reading keys in left rail", 5, readingKeys.size)
        
        val punctKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_PUNCT }
        assertTrue("Should not contain punct keys when composing", punctKeys.isEmpty())

        val preeditKeys = model.leftRailKeys.filter { it.role.name == "RAIL_PREEDIT" }
        assertTrue("Should not contain preedit keys in left rail", preeditKeys.isEmpty())
    }

    @Test
    fun t9PunctItemsHaveVerticalGap() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        val punctKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_PUNCT }
        assertEquals(4, punctKeys.size)
        for (i in 0 until punctKeys.size - 1) {
            val gap = punctKeys[i + 1].rect.top - punctKeys[i].rect.bottom
            assertEquals("Gap between punct items should equal verticalGap", 8, gap)
        }
    }

    @Test
    fun t9PunctItemWidthEqualsBottomLeftButtonWidth() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        val punctKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_PUNCT }
        val bottomLeftW = model.bottomLeftKey!!.rect.width()
        for (key in punctKeys) {
            assertEquals("Punct item width should equal bottom-left button width", bottomLeftW, key.rect.width())
        }
    }

    @Test
    fun t9ReadingItemsHaveVerticalGap() {
        val readings = listOf("mi", "qu", "a", "xian", "sheng")
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, readings, true, KeyboardMode.ChineseT9)
        val readingKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_READING }
        assertTrue("Should have reading keys", readingKeys.size >= 2)
        for (i in 0 until readingKeys.size - 1) {
            val gap = readingKeys[i + 1].rect.top - readingKeys[i].rect.bottom
            assertEquals("Gap between reading items should equal verticalGap", 8, gap)
        }
    }

    @Test
    fun t9ReadingItemWidthEqualsSideColumnWidth() {
        val readings = listOf("mi", "qu", "a")
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, readings, true, KeyboardMode.ChineseT9)
        val readingKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_READING }
        assertTrue(readingKeys.isNotEmpty())
        val w = readingKeys[0].rect.width()
        for (key in readingKeys) {
            assertEquals("All reading items should have same width", w, key.rect.width())
        }
    }

    @Test
    fun symbolCategoryTabHeightEqualsRowHeight() {
        val entries = registry.getAllSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val tabKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_SYMBOL_CATEGORY }
        assertEquals(4, tabKeys.size)
        val h = tabKeys[0].rect.height()
        for (key in tabKeys) {
            assertEquals("All category tabs should have same height", h, key.rect.height())
        }
        assertEquals("Category tab height should equal rowHeight", 96, h)
    }

    @Test
    fun symbolCategoryTabsAlignedWithGridRows() {
        val entries = registry.getAllSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val tabKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_SYMBOL_CATEGORY }
        val symbolKeys = model.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }
        assertEquals(4, tabKeys.size)
        assertTrue("Should have symbol keys", symbolKeys.isNotEmpty())

        val gridRows = symbolKeys.groupBy { it.rect.top }.entries.sortedBy { it.key }
        val firstGridRowTop = gridRows.first().key
        val gridRowHeight = gridRows.first().value.first().rect.height()

        val expectedIconInset = (4 * 2.5f).toInt()
        assertEquals("First tab top should equal symbol grid contentInsetTop", expectedIconInset, tabKeys[0].rect.top)
        assertEquals("Tab height should equal symbol grid row height", gridRowHeight, tabKeys[0].rect.height())
    }

    @Test
    fun symbolCategoryTabWidthEqualsBottomLeftButtonWidth() {
        val entries = registry.getAllSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val tabKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_SYMBOL_CATEGORY }
        val bottomLeftW = model.bottomLeftKey!!.rect.width()
        for (key in tabKeys) {
            assertEquals("Category tab width should equal bottom-left button width", bottomLeftW, key.rect.width())
        }
    }

    @Test
    fun t9BottomLeftFuHeightEqualsBottomRowHeight() {
        val bottomRowHeight = 88
        val model = builder.buildT9(1080, 480, 96, bottomRowHeight, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        assertEquals("Bottom-left '符' height should equal bottomRowHeight", bottomRowHeight, model.bottomLeftKey!!.rect.height())
    }

    @Test
    fun t9BottomLeftFuBottomAlignedWithSpace() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        val spaceKey = model.keys.find { it.action == "space" }!!
        assertEquals("Bottom-left '符' bottom should equal space bottom", spaceKey.rect.bottom, model.bottomLeftKey!!.rect.bottom)
    }

    @Test
    fun symbolBottomLeftHeightEqualsBottomRowHeight() {
        val entries = registry.getAllSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        assertEquals("Bottom-left '中/英' height should equal bottomRowHeight", 88, model.bottomLeftKey!!.rect.height())
    }

    @Test
    fun symbolBottomLeftBottomAlignedWithSpace() {
        val entries = registry.getAllSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val spaceKey = model.keys.find { it.action == "space" }!!
        assertEquals("Bottom-left bottom should equal space bottom", spaceKey.rect.bottom, model.bottomLeftKey!!.rect.bottom)
    }

    @Test
    fun symbolCategoryTabGapMatchesVerticalGap() {
        val entries = registry.getAllSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val tabKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_SYMBOL_CATEGORY }
        assertEquals(4, tabKeys.size)
        val vGap = tabKeys[1].rect.top - tabKeys[0].rect.bottom
        for (i in 0 until tabKeys.size - 1) {
            val gap = tabKeys[i + 1].rect.top - tabKeys[i].rect.bottom
            assertEquals("All category tab gaps should be equal", vGap, gap)
        }
    }

    @Test
    fun symbolModeHasNoDuplicateChineseEnglishToggle() {
        val entries = registry.getAllSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry, density = 2.5f)
        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)
        val toggleKeys = allKeys.filter { it.action == "toggle:english" || it.action == "toggle:chinese" }
        assertEquals("Symbol mode should have no duplicate toggle", 0, toggleKeys.size)
    }

    @Test
    fun buildT9WithComposingFalseGeneratesPunctKeys() {
        val model = builder.buildT9(
            panelWidth = 1080,
            panelHeight = 480,
            rowHeight = 96,
            bottomRowHeight = 88,
            horizontalGap = 8,
            verticalGap = 8,
            readings = listOf("mi", "qu"),
            isComposing = false,
            keyboardMode = KeyboardMode.ChineseT9
        )

        assertEquals("Should have 4 punctuation keys when not composing", 4, model.leftRailKeys.size)
        val expectedPunct = listOf("，", "。", "？", "！")
        for (i in 0 until 4) {
            val key = model.leftRailKeys[i]
            assertEquals(KeyboardKeyRole.RAIL_PUNCT, key.role)
            assertEquals("punct_${expectedPunct[i]}", key.id)
            assertEquals(expectedPunct[i], key.label)
        }
    }

    private fun state(
        keyboardMode: KeyboardMode = KeyboardMode.ChineseT9,
        lastTextMode: KeyboardMode = KeyboardMode.ChineseT9,
        railState: RailState = RailState(RailKind.Punctuation),
        isComposing: Boolean = false,
        readings: List<String> = emptyList(),
        currentSymCategory: String = "punct",
    ): KeyboardUiState = KeyboardUiState(
        keyboardMode = keyboardMode,
        lastTextMode = lastTextMode,
        rawBuffer = if (isComposing) "96" else "",
        preedit = if (isComposing) "wo" else "",
        readings = readings,
        activeReading = readings.firstOrNull(),
        candidatesSnapshot = emptyList(),
        currentSymCategory = currentSymCategory,
        isComposing = isComposing,
        themePalette = ThemePalette(
            bgColor = 0,
            candidateBarColor = 0,
            textColor = 0,
            subColor = 0,
            preeditBgColor = 0,
            symTabActiveBg = 0,
            symTabInactiveBg = 0,
            symTabActiveText = 0,
            symTabInactiveText = 0,
            isDark = false,
            keyBgColor = 0,
            specialKeyBgColor = 0,
            keyPressedBgColor = 0,
            specialKeyPressedBgColor = 0,
        ),
        railState = railState,
    )

    @Test
    fun testModeLeftRailWidthConsistency() {
        val builder = KeyboardLayoutBuilder()
        val t9Model = builder.build(state = KeyboardUiState(keyboardMode = KeyboardMode.ChineseT9, lastTextMode = KeyboardMode.EnglishT9, rawBuffer = "", preedit = "", readings = emptyList(), activeReading = null, candidatesSnapshot = emptyList(), currentSymCategory = "punct", isComposing = false, themePalette = ThemePalette(0,0,0,0,0,0,0,0,0,false,0,0,0,0)), panelWidth = 1080, panelHeight = 480, rowHeight = 96, bottomRowHeight = 88, horizontalGap = 8, verticalGap = 8, categoryToPage = emptyMap(), registry = SymbolKeyRegistry(), density = 1.0f)
        val numModel = builder.build(state = KeyboardUiState(keyboardMode = KeyboardMode.Number, lastTextMode = KeyboardMode.ChineseT9, rawBuffer = "", preedit = "", readings = emptyList(), activeReading = null, candidatesSnapshot = emptyList(), currentSymCategory = "punct", isComposing = false, themePalette = ThemePalette(0,0,0,0,0,0,0,0,0,false,0,0,0,0)), panelWidth = 1080, panelHeight = 480, rowHeight = 96, bottomRowHeight = 88, horizontalGap = 8, verticalGap = 8, categoryToPage = emptyMap(), registry = SymbolKeyRegistry(), density = 1.0f)
        val symModel = builder.build(state = KeyboardUiState(keyboardMode = KeyboardMode.Symbol, lastTextMode = KeyboardMode.ChineseT9, rawBuffer = "", preedit = "", readings = emptyList(), activeReading = null, candidatesSnapshot = emptyList(), currentSymCategory = "punct", isComposing = false, themePalette = ThemePalette(0,0,0,0,0,0,0,0,0,false,0,0,0,0)), panelWidth = 1080, panelHeight = 480, rowHeight = 96, bottomRowHeight = 88, horizontalGap = 8, verticalGap = 8, categoryToPage = emptyMap(), registry = SymbolKeyRegistry(), density = 1.0f)

        val expectedWidth = 1080 / 7

        val t9MaxRight = t9Model.leftRailKeys.maxOfOrNull { it.rect.right } ?: 0
        assertTrue("T9 left rail width should match expected", t9MaxRight <= expectedWidth)

        val numMaxRight = numModel.leftRailKeys.maxOfOrNull { it.rect.right } ?: 0
        assertTrue("Number left rail width should match expected", numMaxRight <= expectedWidth)

        val symMaxRight = symModel.leftRailKeys.maxOfOrNull { it.rect.right } ?: 0
        assertTrue("Symbol left rail width should match expected", symMaxRight <= expectedWidth)
    }

    @Test
    fun testClipboardPanelLayoutConstraints() {
        val model = builder.buildClipboard(1080, 480, 96, 88, 8, 8, listOf("item1", "item2"), 0)
        
        assertTrue("ClipboardPanel should have no left rail keys", model.leftRailKeys.isEmpty())
        assertNull("ClipboardPanel should have no bottom left key", model.bottomLeftKey)
        
        val hasDigitKeys = model.keys.any { it.id.startsWith("key_") && it.id.removePrefix("key_").toIntOrNull() != null }
        assertFalse("ClipboardPanel should not have T9 digit keys", hasDigitKeys)
        
        val hasFu = model.keys.any { it.label == "符" }
        val has123 = model.keys.any { it.label == "123" }
        val hasChEng = model.keys.any { it.label == "中/英" || it.label == "英/中" }
        val hasRetype = model.keys.any { it.label == "重输" }
        
        assertFalse("ClipboardPanel should not have '符'", hasFu)
        assertFalse("ClipboardPanel should not have '123'", has123)
        assertFalse("ClipboardPanel should not have '中/英'", hasChEng)
        assertFalse("ClipboardPanel should not have '重输'", hasRetype)
        
        val backKey = model.keys.find { it.id == "clip_back" }
        assertNotNull("ClipboardPanel should have back button", backKey)
        assertEquals("返回", backKey!!.label)
    }

    @Test
    fun testSelectionPanelLayoutConstraints() {
        val model = builder.buildSelection(1080, 480, 96, 88, 8, 8)
        
        assertTrue("SelectionPanel should have no left rail keys", model.leftRailKeys.isEmpty())
        assertNull("SelectionPanel should have no bottom left key", model.bottomLeftKey)
        
        val hasDigitKeys = model.keys.any { it.id.startsWith("key_") && it.id.removePrefix("key_").toIntOrNull() != null }
        assertFalse("SelectionPanel should not have T9 digit keys", hasDigitKeys)
        
        val hasFu = model.keys.any { it.label == "符" }
        val has123 = model.keys.any { it.label == "123" }
        val hasChEng = model.keys.any { it.label == "中/英" || it.label == "英/中" }
        val hasRetype = model.keys.any { it.label == "重输" }
        
        assertFalse("SelectionPanel should not have '符'", hasFu)
        assertFalse("SelectionPanel should not have '123'", has123)
        assertFalse("SelectionPanel should not have '中/英'", hasChEng)
        assertFalse("SelectionPanel should not have '重输'", hasRetype)
        
        val backKey = model.keys.find { it.id == "select_back" }
        assertNotNull("SelectionPanel should have back button", backKey)
        assertEquals("返回", backKey!!.label)
    }
}
