package io.github.xiwei753.pinyin.imecore

import io.github.xiwei753.pinyin.t9.core.Candidate

enum class RailKind { Punctuation, Readings, SymbolCategories, NumberAux }

data class CompositionState(
    val rawBuffer: String = "",
    val preedit: String = "",
    val readings: List<String> = emptyList(),
    val activeReading: String? = null,
)

data class CandidateStripState(
    val visible: Boolean = false,
    val candidates: List<Candidate> = emptyList(),
    val isDictionaryPreparing: Boolean = false,
)

data class PreeditState(
    val visible: Boolean = false,
    val text: String = "",
)

data class RailState(
    val kind: RailKind = RailKind.Punctuation,
    val labels: List<String> = listOf("，", "。", "？", "！"),
)

data class SymbolPanelState(
    val category: String = "punct",
    val categories: List<String> = listOf("punct", "math", "bracket", "other"),
)

data class KeyboardSurfaceState(
    val mode: InputMode = InputMode.ChineseT9,
    val lastTextMode: InputMode = InputMode.ChineseT9,
    val railState: RailState = RailState(),
    val symbolPanelState: SymbolPanelState = SymbolPanelState(),
    val bottomRowKeys: List<String> = emptyList(),
    val rightActionColumnKeys: List<String> = listOf("del", "retype", "enter"),
)

data class ThemeTokens(
    val keyColorsRef: String = "system",
)

data class LayoutTokens(
    val candidateBarHeight: Int = 48,
    val preeditBubblePadding: Int = 8,
    val preeditTextSize: Float = 18f,
    val keyCornerRadius: Float = 14f,
    val keyGap: Int = 4,
    val leftRailWidthRatio: Float = 0.16f,
    val rightColumnWidthRatio: Float = 0.14f,
    val bottomRowRatio: Float = 0.2f,
    val mainKeyTextSize: Float = 0.35f,
    val subKeyTextSize: Float = 0.22f,
    val railTextSize: Float = 0.45f,
    val symbolTextSize: Float = 0.40f,
)

data class ImeUiState(
    val mode: InputMode = InputMode.ChineseT9,
    val lastTextMode: InputMode = InputMode.ChineseT9,
    val composition: CompositionState = CompositionState(),
    val candidateStrip: CandidateStripState = CandidateStripState(),
    val preeditState: PreeditState = PreeditState(),
    val keyboardSurface: KeyboardSurfaceState = KeyboardSurfaceState(),
    val symbolPanel: SymbolPanelState = SymbolPanelState(),
    val isDictionaryPreparing: Boolean = false,
    val themeTokens: ThemeTokens = ThemeTokens(),
    val layoutTokens: LayoutTokens = LayoutTokens(),
) {
    val rawBuffer: String get() = composition.rawBuffer
    val preedit: String get() = composition.preedit
    val readings: List<String> get() = composition.readings
    val activeReading: String? get() = composition.activeReading
    val candidatesSnapshot: List<Candidate> get() = candidateStrip.candidates
    val currentSymbolCategory: String get() = symbolPanel.category
}
