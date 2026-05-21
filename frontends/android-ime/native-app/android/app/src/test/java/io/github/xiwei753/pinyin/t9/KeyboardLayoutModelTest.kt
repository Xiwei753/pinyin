package io.github.xiwei753.pinyin.t9

import android.graphics.Rect
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
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)
        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)

        assertTrue("T9 mode should have keys in main area", model.keys.size >= 15)
        assertTrue("T9 mode should have left rail keys", model.leftRailKeys.isNotEmpty())
        assertNotNull("T9 mode should have bottom left key", model.bottomLeftKey)

        val digitKeys = allKeys.filter { it.action.startsWith("digit:") }
        assertEquals("Should have 9 digit keys (2-9 + 1 separator)", 9, digitKeys.size)
    }

    @Test
    fun numberModeGeneratesCorrectNumberOfKeys() {
        val model = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)

        val digitKeys = model.keys.filter { it.action.startsWith("digit:") }
        assertEquals("Number mode should have 9 digit keys", 9, digitKeys.size)

        val leftRailNumberKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.NUMBER_LEFT_RAIL }
        assertEquals("Number mode should have dot and 0 in left rail", 2, leftRailNumberKeys.size)

        val hasDel = model.keys.any { it.action == "del" }
        assertTrue("Should have delete key", hasDel)

        val hasEnter = model.keys.any { it.action == "enter" }
        assertTrue("Should have enter key", hasEnter)
    }

    @Test
    fun symbolModeGeneratesCategoryTabsAndSymbolKeys() {
        val entries = registry.getAllSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, categoryToPage, registry)

        val tabKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.SYMBOL_TAB }
        assertEquals("Should have 4 category tabs", 4, tabKeys.size)

        val symbolKeys = model.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }
        assertTrue("Should have symbol keys", symbolKeys.isNotEmpty())

        val placeholderKeys = model.keys.filter { it.role == KeyboardKeyRole.PLACEHOLDER }
        placeholderKeys.forEach {
            assertEquals("Placeholder should have 'none' action", "none", it.action)
        }
    }

    @Test
    fun allKeyRectsAreWithinKeyboardBounds() {
        val panelW = 1080
        val panelH = 480
        val model = builder.buildT9(panelW, panelH, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)

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
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)

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
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)
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
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, emptyMap(), registry)

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
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, emptyMap(), registry)

        val symbolKeys = model.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }
        assertTrue("Should have 2 symbol keys", symbolKeys.size >= 2)
        for (key in symbolKeys) {
            assertEquals("symbol:commit", key.action)
            assertNotNull("Symbol key should have actionPayload", key.actionPayload)
        }
    }

    @Test
    fun spaceKeyHasCorrectAction() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)
        val spaceKey = model.keys.find { it.action == "space" }
        assertNotNull("Should have space key", spaceKey)
        assertEquals(KeyboardKeyRole.SPACE, spaceKey!!.role)
    }
}
