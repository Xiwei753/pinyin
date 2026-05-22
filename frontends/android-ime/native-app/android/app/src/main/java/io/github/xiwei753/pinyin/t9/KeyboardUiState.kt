package io.github.xiwei753.pinyin.t9

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
    val themePalette: ThemePalette
)
