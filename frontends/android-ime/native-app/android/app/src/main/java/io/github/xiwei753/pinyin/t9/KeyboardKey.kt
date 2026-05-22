package io.github.xiwei753.pinyin.t9

import android.graphics.Rect

enum class KeyboardKeyRole {
    NORMAL,
    SPECIAL,
    SPACE,
    RAIL_PUNCT,
    RAIL_READING,
    RAIL_SYMBOL_CATEGORY,
    RAIL_NUMBER_AUX,
    SYMBOL_KEY,
    PLACEHOLDER,
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
