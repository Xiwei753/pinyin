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

    val t9LeftScrollFrame: View,
    val numKey1Frame: View,
    val numKey2Frame: View,
    val numKey3Frame: View,
    val numKey4Frame: View,
    val numKey5Frame: View,
    val numKey6Frame: View,
    val numKey7Frame: View,
    val numKey8Frame: View,
    val numKey9Frame: View,
    val numDotFrame: View,
    val num0Frame: View,

    val t9SymbolButtonFrame: View,
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
    val enterContainer: View,
    // Geometry frame containers
    val t9Key1Frame: View,
    val t9Key2Frame: View,
    val t9Key3Frame: View,
    val t9Key4Frame: View,
    val t9Key5Frame: View,
    val t9Key6Frame: View,
    val t9Key7Frame: View,
    val t9Key8Frame: View,
    val t9Key9Frame: View,
    val t9DelFrame: View,
    val t9RetypeFrame: View,
    val t9NumberFrame: View,
    val t9SpaceFrame: View,
    val t9EnglishFrame: View,
    // Symbol tab views
    val symTabPunct: TextView,
    val symTabMath: TextView,
    val symTabBracket: TextView,
    val symTabOther: TextView,
    // Symbol category rail container
    val symCategoryTabs: View,
    // Dynamically generated symbol button views
    val generatedSymbolViews: MutableList<View>,
    // Symbol bottom row



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
                rootView.findViewById<TextView>(R.id.reading_5) ?: error("reading_5 not found"),
                rootView.findViewById<TextView>(R.id.reading_6) ?: error("reading_6 not found"),
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
            val symCategoryTabs = rootView.findViewById<View>(R.id.sym_category_tabs)
                ?: error("sym_category_tabs not found")

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

                t9LeftScrollFrame = req(R.id.t9_left_scroll_frame),
                numKey1Frame = req(R.id.num_key_1_frame),
                numKey2Frame = req(R.id.num_key_2_frame),
                numKey3Frame = req(R.id.num_key_3_frame),
                numKey4Frame = req(R.id.num_key_4_frame),
                numKey5Frame = req(R.id.num_key_5_frame),
                numKey6Frame = req(R.id.num_key_6_frame),
                numKey7Frame = req(R.id.num_key_7_frame),
                numKey8Frame = req(R.id.num_key_8_frame),
                numKey9Frame = req(R.id.num_key_9_frame),
                numDotFrame = req(R.id.num_dot_frame),
                num0Frame = req(R.id.num_0_frame),

                t9SymbolButtonFrame = req(R.id.t9_symbol_button_frame),
                key1Text = req(R.id.key_1_text),
                key2 = req(R.id.t9_key_2_frame),
                key3 = req(R.id.t9_key_3_frame),
                key4 = req(R.id.t9_key_4_frame),
                key5 = req(R.id.t9_key_5_frame),
                key6 = req(R.id.t9_key_6_frame),
                key7 = req(R.id.t9_key_7_frame),
                key8 = req(R.id.t9_key_8_frame),
                key9 = req(R.id.t9_key_9_frame),
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
                enterContainer = req(R.id.enter_container),
                t9Key1Frame = req(R.id.t9_key_1_frame),
                t9Key2Frame = req(R.id.t9_key_2_frame),
                t9Key3Frame = req(R.id.t9_key_3_frame),
                t9Key4Frame = req(R.id.t9_key_4_frame),
                t9Key5Frame = req(R.id.t9_key_5_frame),
                t9Key6Frame = req(R.id.t9_key_6_frame),
                t9Key7Frame = req(R.id.t9_key_7_frame),
                t9Key8Frame = req(R.id.t9_key_8_frame),
                t9Key9Frame = req(R.id.t9_key_9_frame),
                t9DelFrame = req(R.id.t9_del_frame),
                t9RetypeFrame = req(R.id.t9_retype_frame),
                t9NumberFrame = req(R.id.t9_number_frame),
                t9SpaceFrame = req(R.id.t9_space_frame),
                t9EnglishFrame = req(R.id.t9_english_frame),
                symTabPunct = symTabPunct,
                symTabMath = symTabMath,
                symTabBracket = symTabBracket,
                symTabOther = symTabOther,
                symCategoryTabs = symCategoryTabs,
                generatedSymbolViews = mutableListOf(),





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





            )
        }
    }
}
