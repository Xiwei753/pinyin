package io.github.xiwei753.pinyin.t9

import android.content.res.Resources
import android.view.View
import android.widget.FrameLayout

class KeyboardHeightController(
    private val settingsRepository: SettingsRepository,
    private val resources: Resources,
) {
    data class HeightMetrics(
        val rowHeightPx: Int,
        val bottomRowHeightPx: Int,
        val shellHeight: Int,
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
        val rowHeightPx = when (heightSetting) {
            "low" -> (44 * density).toInt()
            "high" -> (56 * density).toInt()
            else -> (48 * density).toInt()
        }
        val interRowMarginPx = (4 * density).toInt()
        val numberModeHeight = 4 * rowHeightPx + 3 * interRowMarginPx + bottomRowHeightPx + 2 * panelPaddingPx
        val symbolModeHeight = 3 * rowHeightPx + 2 * interRowMarginPx + bottomRowHeightPx + 2 * panelPaddingPx
        val shellHeight = maxOf(numberModeHeight, symbolModeHeight)
        return HeightMetrics(
            rowHeightPx = rowHeightPx,
            bottomRowHeightPx = bottomRowHeightPx,
            shellHeight = shellHeight,
        )
    }

    fun applyHeight(v: KeyboardViews, metrics: HeightMetrics) {
        v.keyboardShell.layoutParams?.height = metrics.shellHeight

        val reused = applyT9Geometry(v, metrics)

        v.imeRoot.requestLayout()

        if (!reused) {
            v.panelT9.post {
                val w = v.panelT9.width
                if (w > 0) {
                    applyT9Geometry(v, metrics)
                    v.imeRoot.requestLayout()
                }
            }
        }
    }

    private fun applyT9Geometry(v: KeyboardViews, metrics: HeightMetrics): Boolean {
        val density = resources.displayMetrics.density
        val hGap = (4 * density).toInt()
        val vGap = (4 * density).toInt()

        val panelWidth = v.panelT9.width
        val panelHeight = v.panelT9.height

        val width: Int
        val height: Int
        val usedRealDimensions: Boolean

        if (panelWidth > 0 && panelHeight > 0) {
            width = panelWidth
            height = panelHeight
            usedRealDimensions = true
        } else {
            width = resources.displayMetrics.widthPixels
            height = metrics.shellHeight
            usedRealDimensions = false
        }

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

        // T9 panel and shared keys
        v.t9LeftScrollFrame.setFrame(geo.leftRailScrollRect)
        v.t9SymbolButtonFrame.setFrame(geo.symbolButtonRect)
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

        // Number panel
        v.numKey1Frame.setFrame(geo.key1Rect)
        v.numKey2Frame.setFrame(geo.key2Rect)
        v.numKey3Frame.setFrame(geo.key3Rect)
        v.numKey4Frame.setFrame(geo.key4Rect)
        v.numKey5Frame.setFrame(geo.key5Rect)
        v.numKey6Frame.setFrame(geo.key6Rect)
        v.numKey7Frame.setFrame(geo.key7Rect)
        v.numKey8Frame.setFrame(geo.key8Rect)
        v.numKey9Frame.setFrame(geo.key9Rect)
        v.numDotFrame.setFrame(geo.numberLeftTopRect)
        v.num0Frame.setFrame(geo.numberLeftBottomRect)

        // Symbol panel
        val symContentFrame = (v.panelSymbol as? android.view.ViewGroup)?.getChildAt(0)
        symContentFrame?.setFrame(geo.symbolContentRect)
        v.symCategoryTabs.layoutParams?.width = geo.leftRailWidth

        return usedRealDimensions
    }
}
