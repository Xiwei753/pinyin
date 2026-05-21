package io.github.xiwei753.pinyin.t9

import android.view.View

class KeyboardPanelController(
    private val v: KeyboardViews,
) {
    var currentSymCategory: String = "punct"
        private set

    fun updatePanel(keyboardMode: KeyboardMode) {
        v.panelT9.visibility = if (keyboardMode == KeyboardMode.ChineseT9 || keyboardMode == KeyboardMode.EnglishT9) View.VISIBLE else View.GONE
        v.panelSymbol.visibility = if (keyboardMode == KeyboardMode.Symbol) View.VISIBLE else View.GONE
        v.panelNumber.visibility = if (keyboardMode == KeyboardMode.Number) View.VISIBLE else View.GONE

        if (keyboardMode == KeyboardMode.Symbol || keyboardMode == KeyboardMode.Number) {
            v.pinyinFloatingBar.visibility = View.GONE
        }


        if (keyboardMode == KeyboardMode.ChineseT9 || keyboardMode == KeyboardMode.EnglishT9) {
            (v.keyToggleNumber as? android.widget.TextView)?.text = "123"
        } else {
            (v.keyToggleNumber as? android.widget.TextView)?.text = "中文"
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
