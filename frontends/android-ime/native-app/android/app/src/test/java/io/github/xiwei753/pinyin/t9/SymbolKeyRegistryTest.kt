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
    fun testAllSymbolEntriesExist() {
        val entries = registry.getAllSymbolEntries()
        assertEquals("Should have 60 symbol entries", 60, entries.size)
    }

    @Test
    fun testSpecificSymbolTexts() {
        assertEquals("\uFF0C", registry.getSymbolText(R.id.sym_1))
        assertEquals("\u3002", registry.getSymbolText(R.id.sym_2))
        assertEquals("\uFF1F", registry.getSymbolText(R.id.sym_3))
        assertEquals("\uFF01", registry.getSymbolText(R.id.sym_4))
        assertEquals("+", registry.getSymbolText(R.id.sym_16))
        assertEquals("-", registry.getSymbolText(R.id.sym_17))
        assertEquals("{", registry.getSymbolText(R.id.sym_35))
        assertEquals("}", registry.getSymbolText(R.id.sym_36))
        assertEquals("@", registry.getSymbolText(R.id.sym_46))
        assertEquals("#", registry.getSymbolText(R.id.sym_47))
    }

    @Test
    fun testInvalidIdReturnsNull() {
        assertNull(registry.getSymbolText(999999))
    }

    @Test
    fun testAllSymbolIdsSize() {
        val ids = registry.getAllSymbolIds()
        assertEquals("Should have 60 ids", 60, ids.size)
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
        assertEquals(60, total)
    }

    @Test
    fun testAllCategoriesHaveDisplayName() {
        for (cat in registry.getAllCategories()) {
            assertTrue(cat.displayName.isNotEmpty())
        }
    }
}
