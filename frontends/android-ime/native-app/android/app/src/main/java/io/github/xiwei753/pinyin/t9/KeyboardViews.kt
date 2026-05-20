package io.github.xiwei753.pinyin.t9

import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class KeyboardViews constructor(
    val imeRoot: View,
    val candidateBar: LinearLayout,
    val candidateContainer: LinearLayout,
    val pinyinFloatingBar: View,
    val pinyinFloatingText: TextView,
    val keyboardShell: View,
    val panelT9: View,
    val panelSymbol: View,
    val panelNumber: View,
    val readingTextViews: List<TextView>,
    val punctTextViews: List<TextView>,
    val symPagePunct: View,
    val symPageMath: View,
    val symPageBracket: View,
    val symPageOther: View,
    val symScrollContent: ScrollView,
    val leftScrollRail: View,
    val leftScrollContent: LinearLayout,
    // T9 digit keys
    val key1Text: TextView,
    val key2: View,
    val key3: View,
    val key4: View,
    val key5: View,
    val key6: View,
    val key7: View,
    val key8: View,
    val key9: View,
    val key2Number: TextView,
    val key3Number: TextView,
    val key4Number: TextView,
    val key5Number: TextView,
    val key6Number: TextView,
    val key7Number: TextView,
    val key8Number: TextView,
    val key9Number: TextView,
    val key2Letters: TextView,
    val key3Letters: TextView,
    val key4Letters: TextView,
    val key5Letters: TextView,
    val key6Letters: TextView,
    val key7Letters: TextView,
    val key8Letters: TextView,
    val key9Letters: TextView,
    // Special keys
    val keyDel: View,
    val keyRetype: View,
    val keyEnter: View,
    val keySpace: View,
    val keyToggleSymbol: View,
    val keyToggleNumber: View,
    val keyToggleEnglish: TextView,
    // Symbol tab views
    val symTabPunct: TextView,
    val symTabMath: TextView,
    val symTabBracket: TextView,
    val symTabOther: TextView,
    // Symbol grid text views
    val symTextViews: Map<Int, TextView>,
    // Symbol bottom row
    val symBack: TextView,
    val symNumber: TextView,
    val symDel: View,
    val symEnter: View,
    val symHide: View,
    // Number pad
    val num0: TextView,
    val num1: TextView,
    val num2: TextView,
    val num3: TextView,
    val num4: TextView,
    val num5: TextView,
    val num6: TextView,
    val num7: TextView,
    val num8: TextView,
    val num9: TextView,
    val numDot: TextView,
    val numDel: View,
    val numBack: View,
    val numSymbol: View,
    val numHide: View,
    val numEnter: View,
    // Row bands
    val rowBand1: View,
    val rowBand2: View,
    val rowBand3: View,
    val rowBand4: View,
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
            val keyboardShell = rootView.findViewById<View>(R.id.keyboard_shell)
                ?: error("Required view keyboard_shell not found")
            val panelT9 = rootView.findViewById<View>(R.id.panel_t9)
                ?: error("Required view panel_t9 not found")
            val panelSymbol = rootView.findViewById<View>(R.id.panel_symbol)
                ?: error("Required view panel_symbol not found")
            val panelNumber = rootView.findViewById<View>(R.id.panel_number)
                ?: error("Required view panel_number not found")
            val symPagePunct = rootView.findViewById<View>(R.id.sym_page_punct)
                ?: error("Required view sym_page_punct not found")
            val symPageMath = rootView.findViewById<View>(R.id.sym_page_math)
                ?: error("Required view sym_page_math not found")
            val symPageBracket = rootView.findViewById<View>(R.id.sym_page_bracket)
                ?: error("Required view sym_page_bracket not found")
            val symPageOther = rootView.findViewById<View>(R.id.sym_page_other)
                ?: error("Required view sym_page_other not found")
            val symScrollContent = rootView.findViewById<ScrollView>(R.id.sym_scroll_content)
                ?: error("Required view sym_scroll_content not found")
            val leftScrollRail = rootView.findViewById<View>(R.id.left_scroll_rail)
                ?: error("Required view left_scroll_rail not found")
            val leftScrollContent = rootView.findViewById<LinearLayout>(R.id.left_scroll_content)
                ?: error("Required view left_scroll_content not found")

            val readingTextViews = listOf(
                rootView.findViewById<TextView>(R.id.reading_1) ?: error("reading_1 not found"),
                rootView.findViewById<TextView>(R.id.reading_2) ?: error("reading_2 not found"),
                rootView.findViewById<TextView>(R.id.reading_3) ?: error("reading_3 not found"),
                rootView.findViewById<TextView>(R.id.reading_4) ?: error("reading_4 not found"),
            )
            val punctTextViews = listOf(
                rootView.findViewById<TextView>(R.id.punct_1) ?: error("punct_1 not found"),
                rootView.findViewById<TextView>(R.id.punct_2) ?: error("punct_2 not found"),
                rootView.findViewById<TextView>(R.id.punct_3) ?: error("punct_3 not found"),
                rootView.findViewById<TextView>(R.id.punct_4) ?: error("punct_4 not found"),
            )

            val symTabPunct = rootView.findViewById<TextView>(R.id.sym_tab_punct)
                ?: error("sym_tab_punct not found")
            val symTabMath = rootView.findViewById<TextView>(R.id.sym_tab_math)
                ?: error("sym_tab_math not found")
            val symTabBracket = rootView.findViewById<TextView>(R.id.sym_tab_bracket)
                ?: error("sym_tab_bracket not found")
            val symTabOther = rootView.findViewById<TextView>(R.id.sym_tab_other)
                ?: error("sym_tab_other not found")

            val symIds = listOf(
                R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5,
                R.id.sym_6, R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_10,
                R.id.sym_11, R.id.sym_12, R.id.sym_13, R.id.sym_14, R.id.sym_15,
                R.id.sym_16, R.id.sym_17, R.id.sym_18, R.id.sym_19, R.id.sym_20,
                R.id.sym_21, R.id.sym_22, R.id.sym_23, R.id.sym_24, R.id.sym_25,
                R.id.sym_26, R.id.sym_27, R.id.sym_28, R.id.sym_29, R.id.sym_30,
                R.id.sym_31, R.id.sym_32, R.id.sym_33, R.id.sym_34, R.id.sym_35,
                R.id.sym_36, R.id.sym_37, R.id.sym_38, R.id.sym_39, R.id.sym_40,
                R.id.sym_41, R.id.sym_42, R.id.sym_43, R.id.sym_44, R.id.sym_45,
                R.id.sym_46, R.id.sym_47, R.id.sym_48, R.id.sym_49, R.id.sym_50,
                R.id.sym_51, R.id.sym_52, R.id.sym_53, R.id.sym_54, R.id.sym_55,
                R.id.sym_56, R.id.sym_57, R.id.sym_58, R.id.sym_59, R.id.sym_60,
            )
            val symTextViews = symIds.associateWith { id ->
                rootView.findViewById<TextView>(id) ?: error("Symbol TextView $id not found")
            }

            fun <T : View> req(id: Int): T = rootView.findViewById<T>(id) ?: error("Required view $id not found")

            return KeyboardViews(
                imeRoot = imeRoot,
                candidateBar = candidateBar,
                candidateContainer = candidateContainer,
                pinyinFloatingBar = pinyinFloatingBar,
                pinyinFloatingText = pinyinFloatingText,
                keyboardShell = keyboardShell,
                panelT9 = panelT9,
                panelSymbol = panelSymbol,
                panelNumber = panelNumber,
                readingTextViews = readingTextViews,
                punctTextViews = punctTextViews,
                symPagePunct = symPagePunct,
                symPageMath = symPageMath,
                symPageBracket = symPageBracket,
                symPageOther = symPageOther,
                symScrollContent = symScrollContent,
                leftScrollRail = leftScrollRail,
                leftScrollContent = leftScrollContent,
                key1Text = req(R.id.key_1_text),
                key2 = req(R.id.key_2),
                key3 = req(R.id.key_3),
                key4 = req(R.id.key_4),
                key5 = req(R.id.key_5),
                key6 = req(R.id.key_6),
                key7 = req(R.id.key_7),
                key8 = req(R.id.key_8),
                key9 = req(R.id.key_9),
                key2Number = req(R.id.key_2_number),
                key3Number = req(R.id.key_3_number),
                key4Number = req(R.id.key_4_number),
                key5Number = req(R.id.key_5_number),
                key6Number = req(R.id.key_6_number),
                key7Number = req(R.id.key_7_number),
                key8Number = req(R.id.key_8_number),
                key9Number = req(R.id.key_9_number),
                key2Letters = req(R.id.key_2_letters),
                key3Letters = req(R.id.key_3_letters),
                key4Letters = req(R.id.key_4_letters),
                key5Letters = req(R.id.key_5_letters),
                key6Letters = req(R.id.key_6_letters),
                key7Letters = req(R.id.key_7_letters),
                key8Letters = req(R.id.key_8_letters),
                key9Letters = req(R.id.key_9_letters),
                keyDel = req(R.id.key_del),
                keyRetype = req(R.id.key_retype),
                keyEnter = req(R.id.key_enter),
                keySpace = req(R.id.key_space),
                keyToggleSymbol = req(R.id.key_toggle_symbol),
                keyToggleNumber = req(R.id.key_toggle_number),
                keyToggleEnglish = req(R.id.key_toggle_english),
                symTabPunct = symTabPunct,
                symTabMath = symTabMath,
                symTabBracket = symTabBracket,
                symTabOther = symTabOther,
                symTextViews = symTextViews,
                symBack = req(R.id.sym_back),
                symNumber = req(R.id.sym_number),
                symDel = req(R.id.sym_del),
                symEnter = req(R.id.sym_enter),
                symHide = req(R.id.sym_hide),
                num0 = req(R.id.num_0),
                num1 = req(R.id.num_1),
                num2 = req(R.id.num_2),
                num3 = req(R.id.num_3),
                num4 = req(R.id.num_4),
                num5 = req(R.id.num_5),
                num6 = req(R.id.num_6),
                num7 = req(R.id.num_7),
                num8 = req(R.id.num_8),
                num9 = req(R.id.num_9),
                numDot = req(R.id.num_dot),
                numDel = req(R.id.num_del),
                numBack = req(R.id.num_back),
                numSymbol = req(R.id.num_symbol),
                numHide = req(R.id.num_hide),
                numEnter = req(R.id.num_enter),
                rowBand1 = req(R.id.row_band_1),
                rowBand2 = req(R.id.row_band_2),
                rowBand3 = req(R.id.row_band_3),
                rowBand4 = req(R.id.row_band_4),
            )
        }
    }
}
