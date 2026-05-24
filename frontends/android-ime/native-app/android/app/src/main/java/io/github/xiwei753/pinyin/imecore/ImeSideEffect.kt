package io.github.xiwei753.pinyin.imecore

sealed interface ImeSideEffect {
    data class CommitText(val text: String) : ImeSideEffect
    data class CommitCandidate(val text: String) : ImeSideEffect
    data object SendDelete : ImeSideEffect
    data object CommitNewline : ImeSideEffect
    data object FinishComposingText : ImeSideEffect
    data class PerformEditorAction(val action: Int) : ImeSideEffect
    data object RefreshUi : ImeSideEffect
    data object CancelEnglishTimeout : ImeSideEffect
    data class ScheduleEnglishTimeout(val delayMs: Long) : ImeSideEffect
    
    // Independent Panel Side Effects
    data object ClipboardPageUp : ImeSideEffect
    data object ClipboardPageDown : ImeSideEffect
    data object SelectionMoveLeft : ImeSideEffect
    data object SelectionMoveRight : ImeSideEffect
    data object SelectionMoveUp : ImeSideEffect
    data object SelectionMoveDown : ImeSideEffect
    data object SelectionSelectAll : ImeSideEffect
    data object SelectionCopy : ImeSideEffect
    data object SelectionCut : ImeSideEffect
    data object SelectionPaste : ImeSideEffect
    data object SelectionUndo : ImeSideEffect

}
