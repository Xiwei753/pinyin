package io.github.xiwei753.pinyin.t9

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ClipboardHistoryManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        val sharedPrefs = context.getSharedPreferences("xiwei_clipboard_history", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
    }

    @Test
    fun testEmptyState() {
        val history = ClipboardHistoryManager.getHistory(context)
        assertEquals(0, history.size)
    }

    @Test
    fun testAddAndRetrieve() {
        ClipboardHistoryManager.addText(context, "hello")
        val history = ClipboardHistoryManager.getHistory(context)
        assertEquals(1, history.size)
        assertEquals("hello", history[0])
    }

    @Test
    fun testHistoryDeduplication() {
        ClipboardHistoryManager.addText(context, "hello")
        ClipboardHistoryManager.addText(context, "world")
        ClipboardHistoryManager.addText(context, "hello")

        val history = ClipboardHistoryManager.getHistory(context)
        assertEquals(2, history.size)
        assertEquals("hello", history[0])
        assertEquals("world", history[1])
    }

    @Test
    fun testHistoryLimitOf20() {
        for (i in 1..25) {
            ClipboardHistoryManager.addText(context, "text_$i")
        }

        val history = ClipboardHistoryManager.getHistory(context)
        assertEquals(20, history.size)
        assertEquals("text_25", history[0])
        assertEquals("text_6", history[19])
    }
}
