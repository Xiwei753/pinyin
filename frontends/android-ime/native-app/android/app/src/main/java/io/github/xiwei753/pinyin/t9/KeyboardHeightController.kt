package io.github.xiwei753.pinyin.t9

import android.content.res.Resources
import android.view.View
import android.widget.FrameLayout
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

        applyT9Geometry(v, metrics)
        applySymbolPanelHeights(v, metrics)
        applyNumberPanelHeights(v, metrics)

        v.imeRoot.requestLayout()
    }

    private fun applyT9Geometry(v: KeyboardViews, metrics: HeightMetrics) {
        val density = resources.displayMetrics.density
        val hGap = (4 * density).toInt()
        val vGap = (4 * density).toInt()

        val panelWidth = v.panelT9.width
        val panelHeight = v.panelT9.height

        val width = if (panelWidth > 0) panelWidth else resources.displayMetrics.widthPixels
        val height = if (panelHeight > 0) panelHeight else metrics.shellHeight

        val geo = T9KeyboardGeometry.calculate(
            panelWidth = width,
            panelHeight = height,
            rowHeight = metrics.rowHeightPx,
            bottomRowHeight = metrics.bottomRowHeightPx,
            horizontalGap = hGap,
            verticalGap = vGap,
        )

        fun View.setFrame(r: android.graphics.Rect) {
            val lp = layoutParams as? FrameLayout.LayoutParams
            if (lp != null) {
                lp.leftMargin = r.left
                lp.topMargin = r.top
                lp.width = r.width()
                lp.height = r.height()
                layoutParams = lp
            }
        }

        v.t9LeftColumn.setFrame(geo.leftRailRect)
        v.t9Key1Frame.setFrame(geo.key1Rect)
        v.t9Key2Frame.setFrame(geo.key2Rect)
        v.t9Key3Frame.setFrame(geo.key3Rect)
        v.t9Key4Frame.setFrame(geo.key4Rect)
        v.t9Key5Frame.setFrame(geo.key5Rect)
        v.t9Key6Frame.setFrame(geo.key6Rect)
        v.t9Key7Frame.setFrame(geo.key7Rect)
        v.t9Key8Frame.setFrame(geo.key8Rect)
        v.t9Key9Frame.setFrame(geo.key9Rect)
        v.t9DelFrame.setFrame(geo.keyDelRect)
        v.t9RetypeFrame.setFrame(geo.keyRetypeRect)
        v.enterContainer.setFrame(geo.keyEnterRect)
        v.t9NumberFrame.setFrame(geo.keyNumberToggleRect)
        v.t9SpaceFrame.setFrame(geo.keySpaceRect)
        v.t9EnglishFrame.setFrame(geo.keyEnglishToggleRect)

        // Bottom row heights for function buttons (used when visible)
        v.keyToggleSymbol.layoutParams?.height = metrics.bottomRowHeightPx
    }

    private fun applySymbolPanelHeights(v: KeyboardViews, metrics: HeightMetrics) {
        val symBottomChildren = listOf(v.symBack, v.symNumber, v.symDel, v.symEnter, v.symHide)
        for (child in symBottomChildren) {
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
    }

    private fun applyNumberPanelHeights(v: KeyboardViews, metrics: HeightMetrics) {
        val numBottomChildren = listOf(v.numBack, v.numSymbol, v.numHide, v.numEnter)
        for (child in numBottomChildren) {
            child.layoutParams?.height = metrics.bottomRowHeightPx
        }
    }
}
