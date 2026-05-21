package io.github.xiwei753.pinyin.t9

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SymbolKeyRegistryTest {

    private lateinit var registry: SymbolKeyRegistry

    @Before
    fun setUp() {
        registry = SymbolKeyRegistry()
    }

    @Test
    fun testAllSymbolEntriesMoreThan60() {
        val entries = registry.getAllSymbolEntries()
        assertTrue("Should have more than 60 symbol entries, got ${entries.size}", entries.size > 60)
    }

    @Test
    fun testSpecificSymbolTexts() {
        val allTexts = registry.getAllSymbolEntries().map { it.second }
        assertTrue(allTexts.contains("\uFF0C"))
        assertTrue(allTexts.contains("\u3002"))
        assertTrue(allTexts.contains("\uFF1F"))
        assertTrue(allTexts.contains("\uFF01"))
        assertTrue(allTexts.contains("+"))
        assertTrue(allTexts.contains("-"))
        assertTrue(allTexts.contains("{"))
        assertTrue(allTexts.contains("}"))
        assertTrue(allTexts.contains("@"))
        assertTrue(allTexts.contains("#"))
    }

    @Test
    fun testInvalidIdReturnsNull() {
        assertNull(registry.getSymbolText(-1))
    }

    @Test
    fun testGetSymbolCountMoreThan60() {
        assertTrue("Symbol count should be > 60, got ${registry.getSymbolCount()}", registry.getSymbolCount() > 60)
    }

    @Test
    fun testGetSymbolsByCategory_fullwidthPunct() {
        val punct = registry.getSymbolsByCategory(SymbolKeyRegistry.Category.FULLWIDTH_PUNCT)
        assertTrue(punct.isNotEmpty())
        assertTrue(punct.any { it.second == "\uFF0C" })
        assertTrue(punct.any { it.second == "\u3002" })
    }

    @Test
    fun testGetSymbolsByCategory_math() {
        val math = registry.getSymbolsByCategory(SymbolKeyRegistry.Category.MATH)
        assertTrue(math.isNotEmpty())
        assertTrue(math.any { it.second == "+" })
        assertTrue(math.any { it.second == "-" })
    }

    @Test
    fun testGetSymbolsByCategory_bracket() {
        val bracket = registry.getSymbolsByCategory(SymbolKeyRegistry.Category.BRACKET)
        assertTrue(bracket.isNotEmpty())
        assertTrue(bracket.any { it.second == "\uFF08" })
        assertTrue(bracket.any { it.second == "\uFF09" })
    }

    @Test
    fun testGetSymbolsByCategory_currency() {
        val currency = registry.getSymbolsByCategory(SymbolKeyRegistry.Category.CURRENCY)
        assertTrue(currency.isNotEmpty())
        assertTrue(currency.any { it.second == "$" })
    }

    @Test
    fun testGetAllCategories() {
        val cats = registry.getAllCategories()
        assertTrue(cats.contains(SymbolKeyRegistry.Category.FULLWIDTH_PUNCT))
        assertTrue(cats.contains(SymbolKeyRegistry.Category.MATH))
        assertTrue(cats.contains(SymbolKeyRegistry.Category.BRACKET))
    }

    @Test
    fun testCategoryEntriesSumToTotal() {
        var total = 0
        for (cat in registry.getAllCategories()) {
            total += registry.getSymbolsByCategory(cat).size
        }
        assertEquals(registry.getSymbolCount(), total)
    }

    @Test
    fun testAllCategoriesHaveDisplayName() {
        for (cat in registry.getAllCategories()) {
            assertTrue(cat.displayName.isNotEmpty())
        }
    }
}
