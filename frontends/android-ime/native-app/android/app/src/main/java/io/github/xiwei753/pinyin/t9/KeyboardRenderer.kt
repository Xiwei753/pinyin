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

    private val cornerRadius = 14f

    fun getCornerRadiusPx(density: Float): Float = cornerRadius * density

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

        val radius = cornerRadius * density

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
        if (key.role == KeyboardKeyRole.LEFT_RAIL_READING && key.label.isEmpty()) return

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
                KeyboardKeyRole.SPECIAL, KeyboardKeyRole.SYMBOL_TAB -> palette.specialKeyPressedBgColor
                else -> palette.keyPressedBgColor
            }
        }

        return when (key.role) {
            KeyboardKeyRole.SPECIAL, KeyboardKeyRole.LEFT_RAIL_PUNCT,
            KeyboardKeyRole.LEFT_RAIL_READING, KeyboardKeyRole.SYMBOL_TAB,
            KeyboardKeyRole.NUMBER_LEFT_RAIL -> {
                if (key.role == KeyboardKeyRole.SYMBOL_TAB && activeSymCategory != null) {
                    if (key.id.contains(activeSymCategory)) {
                        palette.symTabActiveBg
                    } else {
                        palette.symTabInactiveBg
                    }
                } else {
                    palette.specialKeyBgColor
                }
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
        val textColor = when (key.role) {
            KeyboardKeyRole.SYMBOL_TAB -> palette.textColor
            else -> palette.textColor
        }
        val subColor = palette.subColor

        when (key.role) {
            KeyboardKeyRole.NORMAL -> {
                if (key.subLabel != null) {
                    val centerX = rect.centerX().toFloat()
                    val mainLabelSize = rect.height() * 0.35f
                    val subLabelSize = rect.height() * 0.22f

                    textPaint.color = textColor
                    textPaint.textSize = mainLabelSize
                    val textY = rect.centerY().toFloat() + mainLabelSize * 0.38f
                    canvas.drawText(key.label, centerX, textY, textPaint)

                    subTextPaint.color = subColor
                    subTextPaint.textSize = subLabelSize
                    val subY = rect.top + subLabelSize * 1.2f
                    canvas.drawText(key.subLabel, centerX, subY, subTextPaint)
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
            KeyboardKeyRole.LEFT_RAIL_PUNCT -> {
                textPaint.color = textColor
                textPaint.textSize = minOf(rect.height() * 0.45f, rect.width() * 0.45f)
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.LEFT_RAIL_READING -> {
                textPaint.color = textColor
                textPaint.textSize = minOf(rect.height() * 0.38f, rect.width() * 0.40f)
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.SYMBOL_KEY -> {
                textPaint.color = textColor
                textPaint.textSize = rect.height() * 0.40f
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.SYMBOL_TAB -> {
                textPaint.color = textColor
                textPaint.textSize = rect.height() * 0.30f
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.NUMBER_LEFT_RAIL -> {
                textPaint.color = textColor
                textPaint.textSize = rect.height() * 0.42f
                val centerX = rect.centerX().toFloat()
                val textY = rect.centerY().toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(key.label, centerX, textY, textPaint)
            }
            KeyboardKeyRole.PLACEHOLDER -> {}
        }
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
