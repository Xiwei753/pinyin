package io.github.xiwei753.pinyin.t9

import android.view.inputmethod.EditorInfo

object EnterActionPolicy {

    fun shouldSend(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        val imeOptions = editorInfo.imeOptions
        if (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return false
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        return action == EditorInfo.IME_ACTION_SEND
    }

    fun shouldRunExplicitAction(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        val imeOptions = editorInfo.imeOptions
        if (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return false
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        return action == EditorInfo.IME_ACTION_SEARCH ||
                action == EditorInfo.IME_ACTION_GO ||
                action == EditorInfo.IME_ACTION_NEXT ||
                action == EditorInfo.IME_ACTION_DONE
    }

    fun shouldInsertNewline(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return true
        val imeOptions = editorInfo.imeOptions
        if (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return true
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        return action == EditorInfo.IME_ACTION_NONE ||
                action == EditorInfo.IME_ACTION_UNSPECIFIED
    }

    fun getAction(editorInfo: EditorInfo?): Int {
        if (editorInfo == null) return EditorInfo.IME_ACTION_NONE
        return editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
    }
}
