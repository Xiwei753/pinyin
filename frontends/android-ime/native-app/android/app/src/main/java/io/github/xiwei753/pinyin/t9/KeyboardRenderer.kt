package io.github.xiwei753.pinyin.t9

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

class KeyboardRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = false
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val defaultCornerRadius = 14f

    fun getCornerRadiusPx(density: Float): Float = defaultCornerRadius * density

    fun drawKeyboard(
        canvas: Canvas,
        layoutModel: KeyboardLayoutModel,
        palette: ThemePalette,
        density: Float,
        keyboardMode: KeyboardMode,
        activeSymCategory: String?,
        pressedKeyId: String? = null,
        longPressedKeyId: String? = null,
        leftRailScrollY: Int = 0,
    ) {
        canvas.drawColor(palette.bgColor)

        val radius = palette.layoutTokens.keyCornerRadius * density

        val allKeys = mutableListOf<KeyboardKey>()

        for (key in layoutModel.leftRailKeys) {
            val shiftedRect = Rect(key.rect).apply { offset(0, leftRailScrollY) }
            allKeys.add(key.copy(rect = shiftedRect))
        }

        if (layoutModel.bottomLeftKey != null) {
            allKeys.add(layoutModel.bottomLeftKey)
        }

        allKeys.addAll(layoutModel.keys)

        for (key in allKeys) {
            drawKey(canvas, key, palette, radius, density, keyboardMode, activeSymCategory, pressedKeyId, longPressedKeyId)
        }
    }

    private fun drawKey(
        canvas: Canvas,
        key: KeyboardKey,
        palette: ThemePalette,
        radius: Float,
        density: Float,
        keyboardMode: KeyboardMode,
        activeSymCategory: String?,
        pressedKeyId: String?,
        longPressedKeyId: String?,
    ) {
        if (key.role == KeyboardKeyRole.PLACEHOLDER) return
        if (key.role == KeyboardKeyRole.RAIL_READING && key.label.isEmpty()) return

        val isPressed = pressedKeyId == key.id
        val isLongPressed = longPressedKeyId == key.id

        val rect = key.rect
        if (rect.isEmpty) return

        val bgColor = getKeyBgColor(key, palette, isPressed, isLongPressed, keyboardMode, activeSymCategory)
        val rf = RectF(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat())

        bgPaint.style = Paint.Style.FILL
        bgPaint.color = bgColor
        canvas.drawRoundRect(rf, radius, radius, bgPaint)

        drawKeyLabel(canvas, key, palette, rect, radius, density)
    }

    private fun getKeyBgColor(
        key: KeyboardKey,
        palette: ThemePalette,
        isPressed: Boolean,
        isLongPressed: Boolean,
        keyboardMode: KeyboardMode,
        activeSymCategory: String?,
    ): Int {
        if (isLongPressed || isPressed) {
            return when (key.role) {
                KeyboardKeyRole.SPECIAL, KeyboardKeyRole.RAIL_SYMBOL_CATEGORY, KeyboardKeyRole.RAIL_READING -> palette.specialKeyPressedBgColor
                else -> palette.keyPressedBgColor
            }
        }

        if (key.isSelected) {
            return palette.symTabActiveBg
        }

        return when (key.role) {
            KeyboardKeyRole.SPECIAL, KeyboardKeyRole.RAIL_PUNCT,
            KeyboardKeyRole.RAIL_READING, KeyboardKeyRole.RAIL_SYMBOL_CATEGORY,
            KeyboardKeyRole.RAIL_NUMBER_AUX -> {
                palette.specialKeyBgColor
            }
            KeyboardKeyRole.SYMBOL_KEY -> palette.keyBgColor
            KeyboardKeyRole.SPACE -> palette.keyBgColor
            else -> palette.keyBgColor
        }
    }

    private fun drawKeyLabel(
        canvas: Canvas,
        key: KeyboardKey,
        palette: ThemePalette,
        rect: Rect,
        radius: Float,
        density: Float,
    ) {
        val textColor = when {
            key.isSelected -> palette.symTabActiveText
            else -> palette.textColor
        }
        val subColor = palette.subColor

        canvas.save()
        canvas.clipRect(rect)

        when (key.role) {
            KeyboardKeyRole.NORMAL -> {
                if (key.subLabel != null) {
                    val centerX = rect.centerX().toFloat()
                    val digitSize = rect.height() * palette.layoutTokens.subKeyTextSize
                    val lettersSize = rect.height() * palette.layoutTokens.mainKeyTextSize

                    subTextPaint.color = subColor
                    subTextPaint.textSize = digitSize
                    val digitY = rect.top + digitSize * 1.2f
                    canvas.drawText(key.label, centerX, digitY, subTextPaint)

                    textPaint.color = textColor
                    textPaint.textSize = lettersSize
                    val textY = rect.centerY().toFloat() + lettersSize * 0.38f
                    canvas.drawText(key.subLabel, centerX, textY, textPaint)
                } else {
                    textPaint.color = textColor
                    textPaint.textSize = rect.height() * 0.42f
                    val centerX = rect.centerX().toFloat()
                    val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                    canvas.drawText(key.label, centerX, textY, textPaint)
                }
            }
            KeyboardKeyRole.SPECIAL -> {
                textPaint.color = textColor
                textPaint.textSize = rect.height() * 0.38f
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.SPACE -> {
            }
            KeyboardKeyRole.RAIL_PUNCT -> {
                textPaint.color = textColor
                textPaint.textSize = minOf(rect.height() * 0.62f, rect.width() * 0.75f)
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.RAIL_READING -> {
                textPaint.color = textColor

                // Need to scale text dynamically to fit inside rect width with padding
                var ts = minOf(rect.height() * palette.layoutTokens.railTextSize, rect.width() * 0.65f)
                textPaint.textSize = ts
                val padding = rect.width() * 0.1f // 10% padding
                val maxTextWidth = rect.width() - padding

                // Shrink text size until it fits the width or reaches a minimum
                while (textPaint.measureText(key.label) > maxTextWidth && ts > 10f) {
                    ts -= 1f
                    textPaint.textSize = ts
                }

                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.SYMBOL_KEY -> {
                textPaint.color = textColor
                textPaint.textSize = rect.height() * palette.layoutTokens.symbolTextSize
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.RAIL_SYMBOL_CATEGORY -> {
                textPaint.color = textColor
                textPaint.textSize = rect.height() * 0.30f
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.RAIL_NUMBER_AUX -> {
                textPaint.color = textColor
                textPaint.textSize = rect.height() * 0.42f
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.PLACEHOLDER -> {}
        }

        canvas.restore()
    }

    fun hitTest(keys: List<KeyboardKey>, leftRailKeys: List<KeyboardKey>, bottomLeftKey: KeyboardKey?, x: Float, y: Float, leftRailScrollY: Int): KeyboardKey? {
        val ix = x.toInt()
        val iy = y.toInt()

        for (k in leftRailKeys) {
            val shifted = Rect(k.rect).apply { offset(0, leftRailScrollY) }
            if (shifted.contains(ix, iy)) {
                return k
            }
        }

        if (bottomLeftKey != null && bottomLeftKey.rect.contains(ix, iy)) return bottomLeftKey

        for (k in keys) {
            if (k.rect.contains(ix, iy)) return k
        }

        return null
    }
}
