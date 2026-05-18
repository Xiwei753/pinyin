package io.github.xiwei753.pinyin.t9

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.Before
import java.io.File

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
        assertEquals("", T9DebugLogStore.dumpMemory())
    }

    @Test
    fun testLogStoreAppendAndDump() {
        T9DebugLogStore.append("TestTag", "message one")
        T9DebugLogStore.append("TestTag", "message two")

        assertFalse(T9DebugLogStore.isEmpty())
        val dump = T9DebugLogStore.dumpMemory()
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
        assertEquals("", T9DebugLogStore.dumpMemory())
    }

    @Test
    fun testT9DebugLogStoreRecordsAppendedData() {
        T9DebugLogStore.append("XiweiT9", "test log entry")
        val dump = T9DebugLogStore.dumpMemory()
        assertTrue("T9DebugLogStore should contain appended data", dump.contains("test log entry"))
        assertTrue("T9DebugLogStore should contain tag", dump.contains("XiweiT9"))
    }

    @Test
    fun testLogStoreDoesNotCrashWhenEmpty() {
        assertEquals("", T9DebugLogStore.dumpMemory())
        T9DebugLogStore.clear()
        assertTrue(T9DebugLogStore.isEmpty())
    }

    @Test
    fun testFileLoggingInitAndWrite() {
        val tempDir = createTempDir()
        T9DebugLogStore.initFileLogging(tempDir)

        T9DebugLogStore.append("FileTest", "file log entry one")
        T9DebugLogStore.append("FileTest", "file log entry two")

        val dump = T9DebugLogStore.dumpFromFile()
        assertTrue("File dump should contain entries", dump.contains("file log entry one"))
        assertTrue("File dump should contain entries", dump.contains("file log entry two"))

        val logFile = File(tempDir, "t9_debug.log")
        assertTrue("Log file should exist", logFile.exists())
        assertTrue("Log file should have content", logFile.readText().isNotEmpty())
    }

    @Test
    fun testFileLoggingClearRemovesFile() {
        val tempDir = createTempDir()
        T9DebugLogStore.initFileLogging(tempDir)

        T9DebugLogStore.append("FileTest", "entry")
        val logFile = File(tempDir, "t9_debug.log")
        assertTrue("File should exist before clear", logFile.exists())

        T9DebugLogStore.clear()
        assertFalse("File should be deleted after clear", logFile.exists())
        assertTrue("Memory should be cleared", T9DebugLogStore.isEmpty())
    }

    @Test
    fun testDumpWithoutFileInitFallsBackToMemory() {
        T9DebugLogStore.append("MemOnly", "memory entry")
        val dump = T9DebugLogStore.dumpFromFile()
        assertTrue("Should fall back to memory", dump.contains("memory entry"))
    }

    @Test
    fun testLogStoreMemoryTruncatesWhenLarge() {
        for (i in 1..2000) {
            T9DebugLogStore.append("Stress", "this is a test message that will fill up the buffer $i")
        }

        val dump = T9DebugLogStore.dumpMemory()
        assertTrue("Dump should not be empty", dump.isNotEmpty())
        assertTrue("Dump should be truncated", dump.length <= 30_000)
    }

    @Test
    fun testDumpNoFileNoMemoryReturnsEmpty() {
        val dump = T9DebugLogStore.dump()
        assertEquals("", dump)
    }
}
