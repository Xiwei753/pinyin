package io.github.xiwei753.pinyin.t9

import org.junit.Assert.assertEquals
import org.junit.Test

class T9DebugLoggerTest {

    class FakeDebugLogger : T9DebugLogger {
        val logs = mutableListOf<Pair<String, String>>()

        override fun log(tag: String, message: String) {
            logs.add(Pair(tag, message))
        }
    }

    @Test
    fun testFakeDebugLogger() {
        val logger = FakeDebugLogger()
        logger.log("TestTag", "TestMessage")

        assertEquals(1, logger.logs.size)
        assertEquals("TestTag", logger.logs[0].first)
        assertEquals("TestMessage", logger.logs[0].second)
    }
}
