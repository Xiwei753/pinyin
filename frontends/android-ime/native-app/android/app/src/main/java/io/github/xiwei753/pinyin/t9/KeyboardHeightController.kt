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
        v.imeRoot.requestLayout()
    }
}
