package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.view.Gravity
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
        textColor: Int = 0xFF333333.toInt(),
        insetLeft: Int = 0,
        insetTop: Int = 0,
        insetRight: Int = 0,
        insetBottom: Int = 0,
    ): LinearLayout {
        val page = LinearLayout(context)
        page.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        page.orientation = LinearLayout.VERTICAL
        page.setPadding(insetLeft, insetTop, insetRight, insetBottom)

        val rows = entries.chunked(COLUMNS)
        for (row in rows) {
            val rowLayout = LinearLayout(context)
            rowLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                rowHeightPx,
            )
            rowLayout.orientation = LinearLayout.HORIZONTAL

            for ((_, text) in row) {
                val btn = createSymbolButton(context, text, textSize, textColor)
                rowLayout.addView(btn)
                generatedSymbolViews.add(btn)
            }

            val remaining = COLUMNS - row.size
            for (i in 0 until remaining) {
                val placeholder = createPlaceholder(context)
                rowLayout.addView(placeholder)
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
    ): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            )
            setPadding(0, 0, 0, 0)
            gravity = Gravity.CENTER
            setTextSize(textSize)
            setTextColor(textColor)
            setText(text)
            try {
                setBackgroundResource(R.drawable.key_bg)
            } catch (e: android.content.res.Resources.NotFoundException) {
                // Fallback: no background (e.g. in test environment)
            }
            isClickable = true
            isFocusable = true
        }
    }

    private fun createPlaceholder(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            )
            visibility = View.INVISIBLE
            isClickable = false
            isEnabled = false
        }
    }
}
