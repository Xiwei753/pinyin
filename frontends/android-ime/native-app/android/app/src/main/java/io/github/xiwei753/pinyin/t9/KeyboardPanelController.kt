package io.github.xiwei753.pinyin.t9

import android.view.View
import android.widget.TextView

class KeyboardPanelController(
    private val v: KeyboardViews,
) {
    var currentSymCategory: String = "punct"

    fun updatePanel(keyboardMode: KeyboardMode, lastTextMode: KeyboardMode = KeyboardMode.ChineseT9) {
        v.panelT9.visibility = if (keyboardMode == KeyboardMode.ChineseT9 || keyboardMode == KeyboardMode.EnglishT9) View.VISIBLE else View.GONE
        v.panelSymbol.visibility = if (keyboardMode == KeyboardMode.Symbol) View.VISIBLE else View.GONE
        v.panelNumber.visibility = if (keyboardMode == KeyboardMode.Number) View.VISIBLE else View.GONE

        if (keyboardMode == KeyboardMode.Symbol || keyboardMode == KeyboardMode.Number) {
            v.pinyinFloatingBar.visibility = View.GONE
        }

        val symText = v.keyToggleSymbol as? TextView
        val numText = v.keyToggleNumber as? TextView

        when (keyboardMode) {
            KeyboardMode.ChineseT9 -> {
                symText?.text = "符"
                numText?.text = "123"
            }
            KeyboardMode.EnglishT9 -> {
                symText?.text = "符"
                numText?.text = "123"
            }
            KeyboardMode.Symbol -> {
                symText?.text = if (lastTextMode == KeyboardMode.EnglishT9) "英" else "中"
                numText?.text = "123"
            }
            KeyboardMode.Number -> {
                symText?.text = "符"
                numText?.text = if (lastTextMode == KeyboardMode.EnglishT9) "英" else "中"
            }
        }

        v.keyToggleEnglish.text = if (keyboardMode == KeyboardMode.EnglishT9) "英/中" else "中/英"
    }

    fun setSymbolCategory(category: String, themeController: KeyboardThemeController, palette: ThemePalette) {
        currentSymCategory = category
        v.symPagePunct.visibility = if (category == "punct") View.VISIBLE else View.GONE
        v.symPageMath.visibility = if (category == "math") View.VISIBLE else View.GONE
        v.symPageBracket.visibility = if (category == "bracket") View.VISIBLE else View.GONE
        v.symPageOther.visibility = if (category == "other") View.VISIBLE else View.GONE

        themeController.applySymbolTabColors(v, palette, category)
    }
}
