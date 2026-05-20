package io.github.xiwei753.pinyin.t9

import android.content.res.Resources
import android.view.View
import android.widget.LinearLayout

class KeyboardHeightController(
    private val settingsRepository: SettingsRepository,
    private val resources: Resources,
) {
    data class HeightMetrics(
        val rowHeightPx: Int,
        val bottomRowHeightPx: Int,
        val shellHeight: Int,
        val symbolScrollHeight: Int,
    )

    fun calculateHeight(): HeightMetrics {
        val heightSetting = settingsRepository.getKeyboardHeight()
        val density = resources.displayMetrics.density

        val bottomRowHeightPx = when (heightSetting) {
            "low" -> (40 * density).toInt()
            "high" -> (48 * density).toInt()
            else -> (44 * density).toInt()
        }

        val panelPaddingPx = (4 * density).toInt()
        val tabsHeightPx = (36 * density).toInt()

        val rowHeightPx = when (heightSetting) {
            "low" -> (44 * density).toInt()
            "high" -> (56 * density).toInt()
            else -> (48 * density).toInt()
        }
        val interRowMarginPx = (4 * density).toInt()
        val numberModeHeight = 4 * rowHeightPx + 3 * interRowMarginPx + bottomRowHeightPx + 2 * panelPaddingPx
        val symbolModeHeight = tabsHeightPx + 2 * rowHeightPx + bottomRowHeightPx + 2 * panelPaddingPx
        val shellHeight = maxOf(numberModeHeight, symbolModeHeight)

        val scrollHeight = shellHeight - tabsHeightPx - bottomRowHeightPx - 2 * panelPaddingPx

        return HeightMetrics(
            rowHeightPx = rowHeightPx,
            bottomRowHeightPx = bottomRowHeightPx,
            shellHeight = shellHeight,
            symbolScrollHeight = scrollHeight,
        )
    }

    fun applyHeight(v: KeyboardViews, metrics: HeightMetrics) {
        v.keyboardShell.layoutParams?.height = metrics.shellHeight

        val bottomRowChildren = listOf(
            v.keyToggleSymbol, v.keyToggleNumber, v.keySpace, v.keyToggleEnglish
        )
        for (child in bottomRowChildren) {
            child.layoutParams?.height = metrics.bottomRowHeightPx
        }

        val symBottomChildren = listOf(v.symBack, v.symNumber, v.symDel, v.symEnter, v.symHide)
        for (child in symBottomChildren) {
            child.layoutParams?.height = metrics.bottomRowHeightPx
        }

        val numBottomChildren = listOf(v.numBack, v.numSymbol, v.numHide, v.numEnter)
        for (child in numBottomChildren) {
            child.layoutParams?.height = metrics.bottomRowHeightPx
        }

        val symPages = listOf(v.symPagePunct, v.symPageMath, v.symPageBracket, v.symPageOther)
        for (page in symPages) {
            val ll = page as? LinearLayout ?: continue
            for (i in 0 until ll.childCount) {
                ll.getChildAt(i)?.layoutParams?.height = metrics.rowHeightPx
            }
        }

        if (metrics.symbolScrollHeight > 0) {
            v.symScrollContent.layoutParams?.height = metrics.symbolScrollHeight
        }

        v.imeRoot.requestLayout()
    }
}
