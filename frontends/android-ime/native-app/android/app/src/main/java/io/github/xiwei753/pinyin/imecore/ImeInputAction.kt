package io.github.xiwei753.pinyin.imecore

sealed interface ImeInputAction {
    data class DigitPressed(val digit: String) : ImeInputAction
    data class LetterPressed(val letter: Char) : ImeInputAction
    data object SeparatorPressed : ImeInputAction
    data object ZeroPressed : ImeInputAction
    data object DeletePressed : ImeInputAction
    data object EnterShortPressed : ImeInputAction
    data object EnterLongPressed : ImeInputAction
    data object SpacePressed : ImeInputAction
    data object ToggleSymbol : ImeInputAction
    data object ToggleNumber : ImeInputAction
    data object ToggleChineseEnglish : ImeInputAction
    data object ToggleKeyboardType : ImeInputAction
    data class KeyboardModeSelected(val mode: InputMode) : ImeInputAction
    data class CandidateLimitChanged(val limit: Int) : ImeInputAction
    data class SymbolCommitted(val text: String) : ImeInputAction
    data class PunctuationCommitted(val text: String) : ImeInputAction
    data class SymbolCategorySelected(val category: String) : ImeInputAction
    data class CandidateSelected(val index: Int) : ImeInputAction
    data class ReadingSelected(val index: Int) : ImeInputAction
    data object ClearComposing : ImeInputAction
    
    // Independent Panel Actions
    data class ClipboardItemClicked(val text: String) : ImeInputAction
    data object ClipboardPageUp : ImeInputAction
    data object ClipboardPageDown : ImeInputAction
    data object ClosePanel : ImeInputAction
    data object SelectionMoveLeft : ImeInputAction
    data object SelectionMoveRight : ImeInputAction
    data object SelectionMoveUp : ImeInputAction
    data object SelectionMoveDown : ImeInputAction
    data object SelectionSelectAll : ImeInputAction
    data object SelectionCopy : ImeInputAction
    data object SelectionCut : ImeInputAction
    data object SelectionPaste : ImeInputAction
    data object SelectionUndo : ImeInputAction

    data class LifecycleStartInput(
        val initialMode: InputMode,
        val initialLastTextMode: InputMode = initialMode,
    ) : ImeInputAction
    data object LifecycleFinishInput : ImeInputAction
    data object HideKeyboard : ImeInputAction
}
