package io.github.xiwei753.pinyin.t9

import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Test

class EnterActionPolicyTest {

    @Test
    fun shouldSend_nullEditorInfo_returnsFalse() {
        assertFalse(EnterActionPolicy.shouldSend(null))
    }

    @Test
    fun shouldSend_sendAction_returnsTrue() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEND }
        assertTrue(EnterActionPolicy.shouldSend(info))
    }

    @Test
    fun shouldSend_doneAction_returnsFalse() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_DONE }
        assertFalse(EnterActionPolicy.shouldSend(info))
    }

    @Test
    fun shouldSend_noEnterActionFlag_returnsFalse() {
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        }
        assertFalse(EnterActionPolicy.shouldSend(info))
    }

    @Test
    fun shouldSend_searchAction_returnsFalse() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEARCH }
        assertFalse(EnterActionPolicy.shouldSend(info))
    }

    @Test
    fun shouldSend_goAction_returnsFalse() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_GO }
        assertFalse(EnterActionPolicy.shouldSend(info))
    }

    @Test
    fun shouldRunExplicitAction_null_returnsFalse() {
        assertFalse(EnterActionPolicy.shouldRunExplicitAction(null))
    }

    @Test
    fun shouldRunExplicitAction_search_returnsTrue() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEARCH }
        assertTrue(EnterActionPolicy.shouldRunExplicitAction(info))
    }

    @Test
    fun shouldRunExplicitAction_go_returnsTrue() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_GO }
        assertTrue(EnterActionPolicy.shouldRunExplicitAction(info))
    }

    @Test
    fun shouldRunExplicitAction_next_returnsTrue() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_NEXT }
        assertTrue(EnterActionPolicy.shouldRunExplicitAction(info))
    }

    @Test
    fun shouldRunExplicitAction_done_returnsTrue() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_DONE }
        assertTrue(EnterActionPolicy.shouldRunExplicitAction(info))
    }

    @Test
    fun shouldRunExplicitAction_send_returnsFalse() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEND }
        assertFalse(EnterActionPolicy.shouldRunExplicitAction(info))
    }

    @Test
    fun shouldRunExplicitAction_noEnterActionFlag_returnsFalse() {
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        }
        assertFalse(EnterActionPolicy.shouldRunExplicitAction(info))
    }

    @Test
    fun shouldInsertNewline_null_returnsTrue() {
        assertTrue(EnterActionPolicy.shouldInsertNewline(null))
    }

    @Test
    fun shouldInsertNewline_none_returnsTrue() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_NONE }
        assertTrue(EnterActionPolicy.shouldInsertNewline(info))
    }

    @Test
    fun shouldInsertNewline_unspecified_returnsTrue() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED }
        assertTrue(EnterActionPolicy.shouldInsertNewline(info))
    }

    @Test
    fun shouldInsertNewline_done_returnsFalse() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_DONE }
        assertFalse(EnterActionPolicy.shouldInsertNewline(info))
    }

    @Test
    fun shouldInsertNewline_send_returnsFalse() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEND }
        assertFalse(EnterActionPolicy.shouldInsertNewline(info))
    }

    @Test
    fun shouldInsertNewline_search_returnsFalse() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEARCH }
        assertFalse(EnterActionPolicy.shouldInsertNewline(info))
    }

    @Test
    fun shouldInsertNewline_noEnterActionFlag_returnsTrue() {
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        }
        assertTrue(EnterActionPolicy.shouldInsertNewline(info))
    }

    @Test
    fun getAction_null_returnsNone() {
        assertEquals(EditorInfo.IME_ACTION_NONE, EnterActionPolicy.getAction(null))
    }

    @Test
    fun getAction_send_returnsSend() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEND }
        assertEquals(EditorInfo.IME_ACTION_SEND, EnterActionPolicy.getAction(info))
    }

    @Test
    fun getAction_done_returnsDone() {
        val info = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_DONE }
        assertEquals(EditorInfo.IME_ACTION_DONE, EnterActionPolicy.getAction(info))
    }
}
