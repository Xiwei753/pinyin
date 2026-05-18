package io.github.xiwei753.pinyin.t9

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.Before

class T9DebugLoggerTest {

    class FakeDebugLogger : T9DebugLogger {
        val logs = mutableListOf<Pair<String, String>>()

        override fun log(tag: String, message: String) {
            logs.add(Pair(tag, message))
        }
    }

    @Before
    fun setUp() {
        T9DebugLogStore.clear()
    }

    @Test
    fun testFakeDebugLogger() {
        val logger = FakeDebugLogger()
        logger.log("TestTag", "TestMessage")

        assertEquals(1, logger.logs.size)
        assertEquals("TestTag", logger.logs[0].first)
        assertEquals("TestMessage", logger.logs[0].second)
    }

    @Test
    fun testLogStoreEmptyInitially() {
        assertTrue(T9DebugLogStore.isEmpty())
        assertEquals("", T9DebugLogStore.dump())
    }

    @Test
    fun testLogStoreAppendAndDump() {
        T9DebugLogStore.append("TestTag", "message one")
        T9DebugLogStore.append("TestTag", "message two")

        assertFalse(T9DebugLogStore.isEmpty())
        val dump = T9DebugLogStore.dump()
        assertTrue(dump.contains("message one"))
        assertTrue(dump.contains("message two"))
        assertTrue(dump.contains("TestTag"))
    }

    @Test
    fun testLogStoreClear() {
        T9DebugLogStore.append("TestTag", "message")
        assertFalse(T9DebugLogStore.isEmpty())

        T9DebugLogStore.clear()
        assertTrue(T9DebugLogStore.isEmpty())
        assertEquals("", T9DebugLogStore.dump())
    }

    @Test
    fun testT9DebugLogStoreRecordsAppendedData() {
        T9DebugLogStore.append("XiweiT9", "test log entry")
        val dump = T9DebugLogStore.dump()
        assertTrue("T9DebugLogStore should contain appended data", dump.contains("test log entry"))
        assertTrue("T9DebugLogStore should contain tag", dump.contains("XiweiT9"))
    }

    @Test
    fun testLogStoreDoesNotCrashWhenEmpty() {
        assertEquals("", T9DebugLogStore.dump())
        T9DebugLogStore.clear()
        assertTrue(T9DebugLogStore.isEmpty())
    }
}
