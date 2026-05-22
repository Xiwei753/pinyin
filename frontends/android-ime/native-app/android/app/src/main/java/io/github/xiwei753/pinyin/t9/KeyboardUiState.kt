package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.imecore.CandidateStripState
import io.github.xiwei753.pinyin.imecore.CompositionState
import io.github.xiwei753.pinyin.imecore.ImeUiState
import io.github.xiwei753.pinyin.imecore.InputMode
import io.github.xiwei753.pinyin.imecore.KeyboardSurfaceState
import io.github.xiwei753.pinyin.imecore.LayoutTokens
import io.github.xiwei753.pinyin.imecore.PreeditState
import io.github.xiwei753.pinyin.imecore.RailState
import io.github.xiwei753.pinyin.imecore.SymbolPanelState
import io.github.xiwei753.pinyin.imecore.ThemeTokens
import io.github.xiwei753.pinyin.t9.core.Candidate

data class KeyboardUiState(
    val keyboardMode: KeyboardMode,
    val lastTextMode: KeyboardMode,
    val rawBuffer: String,
    val preedit: String,
    val readings: List<String>,
    val activeReading: String?,
    val candidatesSnapshot: List<Candidate>,
    val currentSymCategory: String,
    val isComposing: Boolean,
    val themePalette: ThemePalette,
    val compositionState: CompositionState = CompositionState(rawBuffer, preedit, readings, activeReading),
    val candidateStripState: CandidateStripState = CandidateStripState(candidatesSnapshot.isNotEmpty(), candidatesSnapshot),
    val preeditState: PreeditState = PreeditState(isComposing && preedit.isNotEmpty(), preedit),
    val keyboardSurfaceState: KeyboardSurfaceState = KeyboardSurfaceState(keyboardMode.toInputMode(), lastTextMode.toInputMode()),
    val railState: RailState = keyboardSurfaceState.railState,
    val symbolPanelState: SymbolPanelState = SymbolPanelState(currentSymCategory),
    val isDictionaryPreparing: Boolean = false,
    val themeTokens: ThemeTokens = ThemeTokens(),
    val layoutTokens: LayoutTokens = LayoutTokens(),
) {
    val inputMode: InputMode get() = keyboardMode.toInputMode()
}

fun ImeUiState.toAndroidKeyboardUiState(themePalette: ThemePalette): KeyboardUiState = KeyboardUiState(
    keyboardMode = mode.toKeyboardMode(),
    lastTextMode = lastTextMode.toKeyboardMode(),
    rawBuffer = rawBuffer,
    preedit = preedit,
    readings = readings,
    activeReading = activeReading,
    candidatesSnapshot = candidatesSnapshot,
    currentSymCategory = currentSymbolCategory,
    isComposing = rawBuffer.isNotEmpty(),
    themePalette = themePalette,
    compositionState = composition,
    candidateStripState = candidateStrip,
    preeditState = preeditState,
    keyboardSurfaceState = keyboardSurface,
    railState = keyboardSurface.railState,
    symbolPanelState = symbolPanel,
    isDictionaryPreparing = isDictionaryPreparing,
    themeTokens = themeTokens,
    layoutTokens = layoutTokens,
)
