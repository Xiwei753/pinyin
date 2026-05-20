package io.github.xiwei753.pinyin.t9

import org.junit.Assert.*
import org.junit.Test
import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class KeyboardViewsTest {

    private fun layoutXml(): File {
        val androidProject = TestPaths.androidProjectRoot()
        return File(androidProject, "app/src/main/res/layout/keyboard_view.xml")
    }

    @Test
    fun testAllRequiredViewsExistInLayout() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layoutXml())
        val allNodes = doc.getElementsByTagName("*")

        fun hasId(id: String): Boolean {
            for (i in 0 until allNodes.length) {
                val node = allNodes.item(i)
                if (node is org.w3c.dom.Element) {
                    val nodeId = node.getAttribute("android:id")
                    if (nodeId == "@+id/$id") return true
                }
            }
            return false
        }

        val requiredViews = listOf(
            "ime_root", "candidate_bar", "candidate_container",
            "pinyin_floating_bar", "pinyin_floating_text",
            "keyboard_shell", "panel_t9", "panel_symbol", "panel_number",
            "reading_1", "reading_2", "reading_3", "reading_4", "reading_5", "reading_6",
            "punct_1", "punct_2", "punct_3", "punct_4",
            "sym_page_punct", "sym_page_math", "sym_page_bracket", "sym_page_other",
            "sym_scroll_content",
            "left_scroll_rail", "left_scroll_content",
            "t9_left_column",
            "t9_left_scroll_frame",
            "t9_symbol_button_frame",
            "key_1_text",
            "t9_key_2_frame", "t9_key_3_frame", "t9_key_4_frame", "t9_key_5_frame",
            "t9_key_6_frame", "t9_key_7_frame", "t9_key_8_frame", "t9_key_9_frame",
            "key_2_number", "key_3_number", "key_4_number", "key_5_number",
            "key_6_number", "key_7_number", "key_8_number", "key_9_number",
            "key_2_letters", "key_3_letters", "key_4_letters", "key_5_letters",
            "key_6_letters", "key_7_letters", "key_8_letters", "key_9_letters",
            "key_del", "key_retype", "key_enter", "key_space",
            "key_toggle_symbol", "key_toggle_number", "key_toggle_english",
            "enter_container",
            "t9_del_frame", "t9_retype_frame",
            "t9_number_frame", "t9_space_frame", "t9_english_frame",
            "sym_tab_punct", "sym_tab_math", "sym_tab_bracket", "sym_tab_other",
            "sym_back", "sym_number", "sym_del", "sym_enter", "sym_hide",
            "num_0", "num_1", "num_2", "num_3", "num_4", "num_5",
            "num_6", "num_7", "num_8", "num_9", "num_dot",
            "num_del", "num_back", "num_symbol", "num_hide", "num_enter",
        )

        val missing = requiredViews.filterNot { hasId(it) }
        assertTrue("Required views missing from layout: $missing", missing.isEmpty())
    }

    @Test
    fun testAllSymbolTextViewsExist() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layoutXml())
        val allNodes = doc.getElementsByTagName("*")

        for (i in 1..60) {
            val id = "sym_$i"
            var found = false
            for (j in 0 until allNodes.length) {
                val node = allNodes.item(j)
                if (node is org.w3c.dom.Element) {
                    if (node.getAttribute("android:id") == "@+id/$id") {
                        found = true
                        break
                    }
                }
            }
            assertTrue("Symbol TextView $id must exist in layout", found)
        }
    }

}
