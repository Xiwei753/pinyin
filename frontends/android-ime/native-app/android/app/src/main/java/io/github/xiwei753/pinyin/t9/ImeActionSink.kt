package io.github.xiwei753.pinyin.t9

interface ImeActionSink {
    fun commitText(text: String)
    fun commitNewline()
    fun sendDelete()
    fun performEditorActionOrNewline()
    fun finishComposingText()
    fun refreshUi()
    fun scheduleEnglishTimeout(runnable: Runnable, delayMs: Long)
    fun cancelEnglishTimeout()
}
