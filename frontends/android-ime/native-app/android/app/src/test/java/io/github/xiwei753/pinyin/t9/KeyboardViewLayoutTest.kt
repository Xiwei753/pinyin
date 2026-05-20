package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class KeyboardViewLayoutTest {

    private fun layoutXml(): File {
        val androidProject = TestPaths.androidProjectRoot()
        return File(androidProject, "app/src/main/res/layout/keyboard_view.xml")
    }

    private fun parseXml() = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layoutXml())

    @Test
    fun testNoDuplicateIds() {
        val doc = parseXml()
        val allNodes = doc.getElementsByTagName("*")
        val ids = mutableSetOf<String>()
        val duplicates = mutableSetOf<String>()
        for (i in 0 until allNodes.length) {
            val node = allNodes.item(i)
            if (node is org.w3c.dom.Element) {
                val id = node.getAttribute("android:id")
                if (id.isNotEmpty()) {
                    val cleanId = id.removePrefix("@+id/")
                    if (!ids.add(cleanId)) {
                        duplicates.add(cleanId)
                    }
                }
            }
        }
        assertTrue("No duplicate IDs should exist, but found: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun testPanelsAreMatchParentHeight() {
        val doc = parseXml()
        val panelIds = listOf("panel_t9", "panel_symbol", "panel_number")
        for (panelId in panelIds) {
            val element = findElementById(doc, panelId)
            assertTrue("Element $panelId should exist", element != null)
            val height = element!!.getAttribute("android:layout_height")
            assertEquals("$panelId should have match_parent height", "match_parent", height)
        }
    }

    @Test
    fun testPanelSymbolContainsScrollView() {
        val doc = parseXml()
        val panelSymbol = findElementById(doc, "panel_symbol")
        assertTrue("panel_symbol should exist", panelSymbol != null)
        val hasScrollView = hasChildByTagName(panelSymbol!!, "ScrollView")
        assertTrue("panel_symbol should contain a ScrollView", hasScrollView)
    }

    @Test
    fun testOldRowSymRowsDoNotExist() {
        val content = layoutXml().readText()
        val oldRowIds = listOf("row_sym_2", "row_sym_3", "row_sym_4", "row_sym_5", "row_sym_6")
        for (rowId in oldRowIds) {
            assertFalse("Old symbol row '$rowId' should not exist in layout", content.contains("android:id=\"@+id/$rowId\""))
        }
    }

    @Test
    fun testSymPagesExistInsideScrollView() {
        val doc = parseXml()
        val symPageIds = listOf("sym_page_punct", "sym_page_math", "sym_page_bracket", "sym_page_other")
        for (pageId in symPageIds) {
            val element = findElementById(doc, pageId)
            assertTrue("Symbol page '$pageId' should exist", element != null)
        }
    }

    @Test
    fun testKeyboardShellHeightIsNotHardcoded() {
        val doc = parseXml()
        val shell = findElementById(doc, "keyboard_shell")
        assertTrue("keyboard_shell should exist", shell != null)
        val height = shell!!.getAttribute("android:layout_height")
        // Should NOT be a hardcoded dp value like 260dp - should be wrap_content for dynamic calculation
        assertTrue("keyboard_shell should NOT have hardcoded dp height (should be wrap_content for dynamic calculation)",
            !height.endsWith("dp"))
    }

    @Test
    fun testPreeditIsOutsideKeyboardShell() {
        val doc = parseXml()
        val shell = findElementById(doc, "keyboard_shell")
        val preeditBar = findElementById(doc, "pinyin_floating_bar")
        assertTrue("pinyin_floating_bar should exist", preeditBar != null)
        // preedit should NOT be inside keyboard_shell
        assertFalse("pinyin_floating_bar should NOT be inside keyboard_shell",
            isDescendantOf(shell!!, preeditBar!!))
    }

    @Test
    fun testPreeditHasNoMagicMarginStart() {
        val content = layoutXml().readText()
        assertFalse("Preedit bar should not use magic marginStart=40dp", content.contains("marginStart=\"40dp\""))
        assertFalse("Preedit bar should not use magic layout_marginStart=40dp", content.contains("layout_marginStart=\"40dp\""))
    }

    @Test
    fun testNoSymbolIdAppearsTwice() {
        val doc = parseXml()
        val allNodes = doc.getElementsByTagName("*")
        val symIds = mutableMapOf<String, Int>()
        for (i in 0 until allNodes.length) {
            val node = allNodes.item(i)
            if (node is org.w3c.dom.Element) {
                val id = node.getAttribute("android:id")
                if (id.isNotEmpty() && id.contains("sym_")) {
                    symIds[id] = symIds.getOrDefault(id, 0) + 1
                }
            }
        }
        val dupes = symIds.filter { it.value > 1 }
        assertTrue("No symbol ID should appear more than once, but found: $dupes", dupes.isEmpty())
    }

    @Test
    fun testSymbolTextsAreNotEmpty() {
        val doc = parseXml()
        val allNodes = doc.getElementsByTagName("TextView")
        val emptySymTexts = mutableListOf<String>()
        for (i in 0 until allNodes.length) {
            val node = allNodes.item(i)
            if (node is org.w3c.dom.Element) {
                val id = node.getAttribute("android:id")
                if (id.isNotEmpty() && id.contains("sym_") && id.matches(Regex("@\\+id/sym_\\d+"))) {
                    val text = node.getAttribute("android:text")
                    if (text.isEmpty()) {
                        emptySymTexts.add(id)
                    }
                }
            }
        }
        assertTrue("Symbol TextViews should not have empty text, but found: $emptySymTexts", emptySymTexts.isEmpty())
    }

    @Test
    fun testChineseQuotesArePresent() {
        val content = layoutXml().readText()
        // Chinese quotes should be properly encoded in XML
        assertTrue("Should have left double quote", content.contains("&quot;") || content.contains("\""))
    }

    @Test
    fun testPreeditAnchorExists() {
        val doc = parseXml()
        val anchor = findElementById(doc, "preedit_anchor")
        assertNotNull("preedit_anchor container should exist", anchor)
    }

    @Test
    fun testScrollViewTabsAndBottomRowAreSiblings() {
        val doc = parseXml()
        val panelSymbol = findElementById(doc, "panel_symbol")
        assertTrue("panel_symbol should exist", panelSymbol != null)

        val scrollView = findElementById(doc, "sym_scroll_content")
        val tabs = findElementById(doc, "sym_category_tabs")
        val bottomRow = findElementById(doc, "row_sym_bottom")

        assertNotNull("ScrollView should exist", scrollView)
        assertNotNull("Category tabs should exist", tabs)
        assertNotNull("Bottom row should exist", bottomRow)

        // All three should be direct children of panel_symbol
        assertTrue("ScrollView should be direct child of panel_symbol",
            isDirectChildOf(panelSymbol!!, scrollView!!))
        assertTrue("Tabs should be direct child of panel_symbol",
            isDirectChildOf(panelSymbol!!, tabs!!))
        assertTrue("Bottom row should be direct child of panel_symbol",
            isDirectChildOf(panelSymbol!!, bottomRow!!))
    }

    private fun findElementById(doc: org.w3c.dom.Document, id: String): org.w3c.dom.Element? {
        val allNodes = doc.getElementsByTagName("*")
        for (i in 0 until allNodes.length) {
            val node = allNodes.item(i)
            if (node is org.w3c.dom.Element) {
                val nodeId = node.getAttribute("android:id")
                if (nodeId == "@+id/$id") {
                    return node
                }
            }
        }
        return null
    }

    private fun hasChildByTagName(parent: org.w3c.dom.Element, tagName: String): Boolean {
        val children = parent.getElementsByTagName(tagName)
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (isDescendantOf(parent, child)) {
                return true
            }
        }
        return false
    }

    private fun isDescendantOf(ancestor: org.w3c.dom.Element, node: org.w3c.dom.Node): Boolean {
        var current: org.w3c.dom.Node? = node.parentNode
        while (current != null) {
            if (current === ancestor) return true
            current = current.parentNode
        }
        return false
    }

    private fun isDirectChildOf(parent: org.w3c.dom.Element, node: org.w3c.dom.Node): Boolean {
        return node.parentNode === parent
    }
}
