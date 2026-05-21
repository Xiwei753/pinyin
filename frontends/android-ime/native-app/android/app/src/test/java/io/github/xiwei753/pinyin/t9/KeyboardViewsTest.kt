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
            "xiwei_keyboard_view"
        )

        val missing = requiredViews.filterNot { hasId(it) }
        assertTrue("Required views missing from layout: $missing", missing.isEmpty())
    }

    @Test
    fun testOldViewsRemovedFromLayout() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layoutXml())
        val allNodes = doc.getElementsByTagName("*")

        fun hasId(id: String): Boolean {
            for (i in 0 until allNodes.length) {
                val node = allNodes.item(i)
                if (node is org.w3c.dom.Element) {
                    if (node.getAttribute("android:id") == "@+id/$id") return true
                }
            }
            return false
        }

        assertFalse("keyboard_shell should be removed", hasId("keyboard_shell"))
        assertFalse("panel_t9 should be removed", hasId("panel_t9"))
        assertFalse("t9_key_2_frame should be removed", hasId("t9_key_2_frame"))
        assertFalse("sym_tab_punct should be removed", hasId("sym_tab_punct"))
    }
}
