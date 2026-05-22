package io.github.xiwei753.pinyin.t9

import android.graphics.Rect

enum class KeyboardKeyRole {
    NORMAL,
    SPECIAL,
    SPACE,
    LEFT_RAIL_PUNCT,
    LEFT_RAIL_READING,
    SYMBOL_TAB,
    SYMBOL_KEY,
    PLACEHOLDER,
    NUMBER_LEFT_RAIL
}

data class KeyboardKey(
    val id: String,
    val role: KeyboardKeyRole,
    val rect: Rect,
    val label: String,
    val subLabel: String? = null,
    val action: String,
    val actionPayload: String? = null,
    val isLeftRail: Boolean = false,
    val isRightRail: Boolean = false,
    val isBottomRow: Boolean = false,
    val isSelected: Boolean = false,
)
