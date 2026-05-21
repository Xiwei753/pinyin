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

    @Test
    fun testBindFailsWhenXiweiKeyboardViewIsMissing() {
        val mockRoot = org.mockito.Mockito.mock(android.view.View::class.java)
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.view.View>(R.id.ime_root))
            .thenReturn(org.mockito.Mockito.mock(android.view.View::class.java))
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.widget.LinearLayout>(R.id.candidate_bar))
            .thenReturn(org.mockito.Mockito.mock(android.widget.LinearLayout::class.java))
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.widget.LinearLayout>(R.id.candidate_container))
            .thenReturn(org.mockito.Mockito.mock(android.widget.LinearLayout::class.java))
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.view.View>(R.id.pinyin_floating_bar))
            .thenReturn(org.mockito.Mockito.mock(android.view.View::class.java))
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.widget.TextView>(R.id.pinyin_floating_text))
            .thenReturn(org.mockito.Mockito.mock(android.widget.TextView::class.java))
        
        // Return null for xiwei_keyboard_view
        org.mockito.Mockito.`when`(mockRoot.findViewById<XiweiKeyboardView>(R.id.xiwei_keyboard_view))
            .thenReturn(null)

        val exception = assertThrows(IllegalStateException::class.java) {
            KeyboardViews.bind(mockRoot)
        }
        assertTrue(exception.message!!.contains("Required view xiwei_keyboard_view not found"))
    }

    @Test
    fun testBindSucceedsWithAllRequiredViews() {
        val mockRoot = org.mockito.Mockito.mock(android.view.View::class.java)
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.view.View>(R.id.ime_root))
            .thenReturn(org.mockito.Mockito.mock(android.view.View::class.java))
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.widget.LinearLayout>(R.id.candidate_bar))
            .thenReturn(org.mockito.Mockito.mock(android.widget.LinearLayout::class.java))
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.widget.LinearLayout>(R.id.candidate_container))
            .thenReturn(org.mockito.Mockito.mock(android.widget.LinearLayout::class.java))
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.view.View>(R.id.pinyin_floating_bar))
            .thenReturn(org.mockito.Mockito.mock(android.view.View::class.java))
        org.mockito.Mockito.`when`(mockRoot.findViewById<android.widget.TextView>(R.id.pinyin_floating_text))
            .thenReturn(org.mockito.Mockito.mock(android.widget.TextView::class.java))
        
        val mockKv = org.mockito.Mockito.mock(XiweiKeyboardView::class.java)
        org.mockito.Mockito.`when`(mockRoot.findViewById<XiweiKeyboardView>(R.id.xiwei_keyboard_view))
            .thenReturn(mockKv)

        val kv = KeyboardViews.bind(mockRoot)
        assertNotNull(kv)
        assertEquals(mockKv, kv.xiweiKeyboardView)
    }

    @Test
    fun testXiweiKeyboardViewHasStandardConstructors() {
        val clazz = XiweiKeyboardView::class.java
        
        // Verify standard constructors exist
        val constructor1 = clazz.getConstructor(android.content.Context::class.java)
        assertNotNull(constructor1)

        val constructor2 = clazz.getConstructor(android.content.Context::class.java, android.util.AttributeSet::class.java)
        assertNotNull(constructor2)

        val constructor3 = clazz.getConstructor(android.content.Context::class.java, android.util.AttributeSet::class.java, Int::class.javaPrimitiveType)
        assertNotNull(constructor3)
    }

    @Test
    fun testKeyboardViewsContainsXiweiKeyboardViewTag() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layoutXml())
        val allNodes = doc.getElementsByTagName("io.github.xiwei753.pinyin.t9.XiweiKeyboardView")
        
        assertTrue("XML must contain io.github.xiwei753.pinyin.t9.XiweiKeyboardView to inflate successfully", allNodes.length > 0)
    }
}
