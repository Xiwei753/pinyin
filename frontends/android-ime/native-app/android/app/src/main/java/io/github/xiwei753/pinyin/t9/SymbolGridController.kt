package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object SymbolGridController {

    private const val COLUMNS = 5

    fun buildPage(
        context: Context,
        entries: List<Pair<Int, String>>,
        rowHeightPx: Int,
        generatedSymbolViews: MutableList<View>,
        textSize: Float = 20f,
        palette: ThemePalette? = null,
        textColor: Int = 0xFF333333.toInt(),
        metrics: SymbolGridLayoutMetrics? = null,
        onSymbolClick: ((String) -> Unit)? = null,
        onSymbolTouch: ((View) -> Unit)? = null,
    ): LinearLayout {
        val page = LinearLayout(context)
        page.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        page.orientation = LinearLayout.VERTICAL

        val rows = entries.chunked(COLUMNS)
        val rowCount = rows.size
        for ((rowIndex, row) in rows.withIndex()) {
            val rowLayout = LinearLayout(context)
            rowLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                rowHeightPx,
            )
            rowLayout.orientation = LinearLayout.HORIZONTAL

            if (metrics != null && rowIndex < rowCount - 1) {
                (rowLayout.layoutParams as LinearLayout.LayoutParams).bottomMargin = metrics.verticalGap
            }

            var cellIndex = 0
            for ((_, text) in row) {
                val btn = createSymbolButton(context, text, textSize, textColor, palette, onSymbolClick, onSymbolTouch)
                if (metrics != null && cellIndex < COLUMNS - 1) {
                    (btn.layoutParams as LinearLayout.LayoutParams).marginEnd = metrics.horizontalGap
                }
                rowLayout.addView(btn)
                generatedSymbolViews.add(btn)
                cellIndex++
            }

            val remaining = COLUMNS - row.size
            for (i in 0 until remaining) {
                val placeholder = createPlaceholder(context)
                if (metrics != null && cellIndex < COLUMNS - 1) {
                    (placeholder.layoutParams as LinearLayout.LayoutParams).marginEnd = metrics.horizontalGap
                }
                rowLayout.addView(placeholder)
                cellIndex++
            }

            page.addView(rowLayout)
        }

        return page
    }

    private fun createSymbolButton(
        context: Context,
        text: String,
        textSize: Float,
        textColor: Int,
        palette: ThemePalette?,
        onSymbolClick: ((String) -> Unit)?,
        onSymbolTouch: ((View) -> Unit)?,
    ): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            )
            gravity = Gravity.CENTER
            setTextSize(textSize)
            setTextColor(if (palette != null) palette.textColor else textColor)
            setText(text)
            if (palette != null) {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    setColor(palette.keyBgColor)
                    cornerRadius = 14f * context.resources.displayMetrics.density
                }
            } else {
                try {
                    setBackgroundResource(R.drawable.key_bg)
                } catch (e: android.content.res.Resources.NotFoundException) {
                    // Fallback: no background (e.g. in test environment)
                }
            }
            isClickable = true
            isFocusable = true

            if (onSymbolClick != null) {
                setOnClickListener { onSymbolClick(text) }
            }
            if (onSymbolTouch != null) {
                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        onSymbolTouch(v)
                    }
                    false
                }
            }
        }
    }

    private fun createPlaceholder(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            isClickable = false
            isEnabled = false
        }
    }
}
