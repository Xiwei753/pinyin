package io.github.xiwei753.pinyin.imecore

sealed interface ImeInputAction {
    data class DigitPressed(val digit: String) : ImeInputAction
    data object SeparatorPressed : ImeInputAction
    data object ZeroPressed : ImeInputAction
    data object DeletePressed : ImeInputAction
    data object EnterShortPressed : ImeInputAction
    data object EnterLongPressed : ImeInputAction
    data object SpacePressed : ImeInputAction
    data object ToggleSymbol : ImeInputAction
    data object ToggleNumber : ImeInputAction
    data object ToggleChineseEnglish : ImeInputAction
    data class SymbolCommitted(val text: String) : ImeInputAction
    data class SymbolCategorySelected(val category: String) : ImeInputAction
    data class CandidateSelected(val index: Int) : ImeInputAction
    data class ReadingSelected(val index: Int) : ImeInputAction
    data object ClearComposing : ImeInputAction
    data object LifecycleStartInput : ImeInputAction
    data object LifecycleFinishInput : ImeInputAction
}
