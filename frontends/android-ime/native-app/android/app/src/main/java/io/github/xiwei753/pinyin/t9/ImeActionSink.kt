package io.github.xiwei753.pinyin.t9

import android.view.inputmethod.EditorInfo

interface ImeActionSink {
    fun commitText(text: String)
    fun commitNewline()
    fun sendDelete()
    fun performEditorActionOrNewline()
    fun performEnterActionIfAvailable(): Boolean
    fun isEnterActionAvailable(): Boolean
    fun finishComposingText()
    fun refreshUi()
    fun scheduleEnglishTimeout(runnable: Runnable, delayMs: Long)
    fun cancelEnglishTimeout()
    fun getCurrentEditorInfo(): EditorInfo?
    fun performEditorAction(action: Int): Boolean
    
    fun performContextMenuAction(actionId: Int)
    fun sendKeyEvent(keyCode: Int)
    fun clipboardPageUp()
    fun clipboardPageDown()
}
