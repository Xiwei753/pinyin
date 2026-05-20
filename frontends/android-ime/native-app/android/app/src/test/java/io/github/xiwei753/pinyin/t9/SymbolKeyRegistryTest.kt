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
}
