package io.github.xiwei753.pinyin.t9

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardViews constructor(
    val imeRoot: View,
    val candidateBar: LinearLayout,
    val candidateContainer: LinearLayout,
    val pinyinFloatingBar: View,
    val pinyinFloatingText: TextView,
    val xiweiKeyboardView: XiweiKeyboardView? = null,
) {
    companion object {
        fun bind(rootView: View): KeyboardViews {
            val imeRoot = rootView.findViewById<View>(R.id.ime_root)
                ?: error("Required view ime_root not found")
            val candidateBar = rootView.findViewById<LinearLayout>(R.id.candidate_bar)
                ?: error("Required view candidate_bar not found")
            val candidateContainer = rootView.findViewById<LinearLayout>(R.id.candidate_container)
                ?: error("Required view candidate_container not found")
            val pinyinFloatingBar = rootView.findViewById<View>(R.id.pinyin_floating_bar)
                ?: error("Required view pinyin_floating_bar not found")
            val pinyinFloatingText = rootView.findViewById<TextView>(R.id.pinyin_floating_text)
                ?: error("Required view pinyin_floating_text not found")
            val xiweiKeyboardView = rootView.findViewById<XiweiKeyboardView>(R.id.xiwei_keyboard_view)

            return KeyboardViews(
                imeRoot = imeRoot,
                candidateBar = candidateBar,
                candidateContainer = candidateContainer,
                pinyinFloatingBar = pinyinFloatingBar,
                pinyinFloatingText = pinyinFloatingText,
                xiweiKeyboardView = xiweiKeyboardView,
            )
        }
    }
}
