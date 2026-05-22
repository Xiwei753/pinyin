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
}
