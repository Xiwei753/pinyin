package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class KeyboardViewLayoutTest {

    private fun layoutXml(): File {
        val androidProject = TestPaths.androidProjectRoot()
        return File(androidProject, "app/src/main/res/layout/keyboard_view.xml")
    }

    private fun parseXml() = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layoutXml())

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

    private fun findElementByIdInside(parent: org.w3c.dom.Element, id: String): org.w3c.dom.Element? {
        val allNodes = parent.getElementsByTagName("*")
        for (i in 0 until allNodes.length) {
            val node = allNodes.item(i)
            if (node is org.w3c.dom.Element) {
                val nodeId = node.getAttribute("android:id")
                if (nodeId == "@+id/$id" && isDescendantOf(parent, node)) {
                    return node
                }
            }
        }
        return null
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

    private fun getAllElements(doc: org.w3c.dom.Document): List<org.w3c.dom.Element> {
        val allNodes = doc.getElementsByTagName("*")
        val result = mutableListOf<org.w3c.dom.Element>()
        for (i in 0 until allNodes.length) {
            val node = allNodes.item(i)
            if (node is org.w3c.dom.Element) {
                result.add(node)
            }
        }
        return result
    }

    // === Root layout structure tests ===

    @Test
    fun testRootIsLinearLayout() {
        val doc = parseXml()
        val root = doc.documentElement
        assertEquals("Root element should be LinearLayout", "LinearLayout", root.tagName)
    }

    @Test
    fun testRootHasVerticalOrientation() {
        val doc = parseXml()
        val root = doc.documentElement
        val orientation = root.getAttribute("android:orientation")
        assertEquals("Root LinearLayout should have vertical orientation", "vertical", orientation)
    }

    @Test
    fun testRootHasWrapContentHeight() {
        val doc = parseXml()
        val root = doc.documentElement
        val height = root.getAttribute("android:layout_height")
        assertEquals("Root should have wrap_content height", "wrap_content", height)
    }

    @Test
    fun testNoRelativeLayoutAtRoot() {
        val content = layoutXml().readText()
        assertFalse("Root should not use RelativeLayout", content.contains("<RelativeLayout"))
    }

    @Test
    fun testNoRootLevelPreeditOverlay() {
        val doc = parseXml()
        val overlay = findElementById(doc, "preedit_overlay")
        assertNull("preedit_overlay should NOT exist at any level", overlay)
    }

    @Test
    fun testNoMagicMarginBottom() {
        val content = layoutXml().readText()
        assertFalse("Should not use marginBottom=310dp", content.contains("marginBottom=\"310dp\""))
        assertFalse("Should not use marginBottom=300dp", content.contains("marginBottom=\"300dp\""))
        assertFalse("Should not use marginBottom=260dp", content.contains("marginBottom=\"260dp\""))
        assertFalse("Should not use layout_marginBottom=310dp", content.contains("layout_marginBottom=\"310dp\""))
        assertFalse("Should not use layout_marginBottom=300dp", content.contains("layout_marginBottom=\"300dp\""))
        assertFalse("Should not use layout_marginBottom=260dp", content.contains("layout_marginBottom=\"260dp\""))
    }

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

    // === Preedit inside candidate_bar tests ===

    @Test
    fun testPreeditInsideCandidateBar() {
        val doc = parseXml()
        val candidateBar = findElementById(doc, "candidate_bar")
        assertNotNull("candidate_bar should exist", candidateBar)
        val floatingBar = findElementByIdInside(candidateBar!!, "pinyin_floating_bar")
        assertNotNull("pinyin_floating_bar should be inside candidate_bar", floatingBar)
    }

    @Test
    fun testCandidateContainerUsesWeightToAvoidPreedit() {
        val doc = parseXml()
        val candidateBar = findElementById(doc, "candidate_bar")
        assertNotNull("candidate_bar should exist", candidateBar)
        // Find the HorizontalScrollView that is a direct child of candidate_bar
        val allElements = getAllElements(doc)
        var foundWeightedScrollView = false
        for (element in allElements) {
            if (element.tagName == "HorizontalScrollView") {
                val parent = element.parentNode
                if (parent is org.w3c.dom.Element && parent.getAttribute("android:id") == "@+id/candidate_bar") {
                    val width = element.getAttribute("android:layout_width")
                    val weight = element.getAttribute("android:layout_weight")
                    assertEquals("Candidate ScrollView should use 0dp width", "0dp", width)
                    assertEquals("Candidate ScrollView should have weight=1", "1", weight)
                    foundWeightedScrollView = true
                }
            }
        }
        assertTrue("Candidate ScrollView inside candidate_bar should have weight", foundWeightedScrollView)
    }

    @Test
    fun testPreeditNotInKeyboardShell() {
        val doc = parseXml()
        val shell = findElementById(doc, "keyboard_shell")
        val preeditBar = findElementById(doc, "pinyin_floating_bar")
        assertNotNull("pinyin_floating_bar should exist", preeditBar)
        assertFalse("pinyin_floating_bar should NOT be inside keyboard_shell",
            isDescendantOf(shell!!, preeditBar!!))
    }

    @Test
    fun testPreeditHasNoMagicMarginStart() {
        val content = layoutXml().readText()
        assertFalse("Preedit bar should not use magic marginStart=40dp", content.contains("marginStart=\"40dp\""))
        assertFalse("Preedit bar should not use magic layout_marginStart=40dp", content.contains("layout_marginStart=\"40dp\""))
    }

    // === LinearLayout orientation tests ===

    @Test
    fun testAllLinearLayoutsHaveExplicitOrientation() {
        val doc = parseXml()
        val allElements = getAllElements(doc)
        val missingOrientation = mutableListOf<String>()
        for (element in allElements) {
            if (element.tagName == "LinearLayout") {
                val orientation = element.getAttribute("android:orientation")
                if (orientation.isEmpty()) {
                    val id = element.getAttribute("android:id")
                    missingOrientation.add(if (id.isNotEmpty()) id else "unnamed LinearLayout")
                }
            }
        }
        assertTrue("All LinearLayouts must have explicit orientation, but missing in: $missingOrientation",
            missingOrientation.isEmpty())
    }

    @Test
    fun testWeightBasedLayoutParentOrientationCorrect() {
        val doc = parseXml()
        val allElements = getAllElements(doc)
        val errors = mutableListOf<String>()

        for (element in allElements) {
            val height = element.getAttribute("android:layout_height")
            val width = element.getAttribute("android:layout_width")
            val weight = element.getAttribute("android:layout_weight")

            if (weight.isNotEmpty() && weight != "0") {
                val parent = element.parentNode
                if (parent is org.w3c.dom.Element) {
                    if (height == "0dp") {
                        // Using height=0dp + weight -> parent must be vertical
                        val parentOrientation = parent.getAttribute("android:orientation")
                        if (parent.tagName == "LinearLayout" && parentOrientation != "vertical") {
                            val id = element.getAttribute("android:id")
                            errors.add("$id has height=0dp+weight but parent is not vertical (is: $parentOrientation)")
                        }
                    }
                    if (width == "0dp") {
                        // Using width=0dp + weight -> parent must be horizontal
                        val parentOrientation = parent.getAttribute("android:orientation")
                        if (parent.tagName == "LinearLayout" && parentOrientation != "horizontal") {
                            val id = element.getAttribute("android:id")
                            errors.add("$id has width=0dp+weight but parent is not horizontal (is: $parentOrientation)")
                        }
                    }
                }
            }
        }
        assertTrue("All 0dp+weight layouts must have correct parent orientation, but found: $errors",
            errors.isEmpty())
    }

    // === Panel structure tests ===

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
    fun testKeyboardShellHasDefaultHeight() {
        val doc = parseXml()
        val shell = findElementById(doc, "keyboard_shell")
        assertTrue("keyboard_shell should exist", shell != null)
        val height = shell!!.getAttribute("android:layout_height")
        assertTrue("keyboard_shell should have a height attribute", height.isNotEmpty())
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
                if (id.isNotEmpty() && id.matches(Regex("@\\+id/sym_\\d+"))) {
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

        assertTrue("ScrollView should be direct child of panel_symbol",
            isDirectChildOf(panelSymbol!!, scrollView!!))
        assertTrue("Tabs should be direct child of panel_symbol",
            isDirectChildOf(panelSymbol!!, tabs!!))
        assertTrue("Bottom row should be direct child of panel_symbol",
            isDirectChildOf(panelSymbol!!, bottomRow!!))
    }

    // === T9 panel row band structure tests ===

    @Test
    fun testPanelT9HasFourRowBands() {
        val doc = parseXml()
        for (bandId in listOf("row_band_1", "row_band_2", "row_band_3", "row_band_4")) {
            val band = findElementById(doc, bandId)
            assertNotNull("$bandId should exist", band)
            val height = band!!.getAttribute("android:layout_height")
            val weight = band.getAttribute("android:layout_weight")
            assertEquals("$bandId should use 0dp height for weight layout", "0dp", height)
            assertTrue("$bandId should have layout_weight", weight.isNotEmpty())
        }
    }

    @Test
    fun testRowBandsHaveEqualWeight() {
        val doc = parseXml()
        val weights = mutableListOf<Double>()
        for (bandId in listOf("row_band_1", "row_band_2", "row_band_3", "row_band_4")) {
            val band = findElementById(doc, bandId)
            assertNotNull("$bandId should exist", band)
            val weight = band!!.getAttribute("android:layout_weight")
            assertTrue("$bandId should have layout_weight", weight.isNotEmpty())
            weights.add(weight.toDouble())
        }
        assertEquals("All row bands should have equal weight", weights[0], weights[1], 0.01)
        assertEquals("All row bands should have equal weight", weights[1], weights[2], 0.01)
        assertEquals("All row bands should have equal weight", weights[2], weights[3], 0.01)
    }

    @Test
    fun testLeftScrollRailExists() {
        val doc = parseXml()
        val scrollRail = findElementById(doc, "left_scroll_rail")
        assertNotNull("left_scroll_rail should exist", scrollRail)
    }

    @Test
    fun testToggleSymbolInLeftColumn() {
        val doc = parseXml()
        val root = doc.documentElement
        val toggleSymbol = findElementById(doc, "key_toggle_symbol")
        assertNotNull("key_toggle_symbol should exist", toggleSymbol)
        // key_toggle_symbol is in the left column, not in row_band_4
        val band4 = findElementById(doc, "row_band_4")
        assertNotNull("row_band_4 should exist", band4)
        val inBand4 = findElementByIdInside(band4!!, "key_toggle_symbol")
        assertNull("key_toggle_symbol should NOT be in row_band_4", inBand4)
    }

    @Test
    fun testToggleSymbolNotInScrollRail() {
        val doc = parseXml()
        val scrollRail = findElementById(doc, "left_scroll_rail")
        assertNotNull("left_scroll_rail should exist", scrollRail)
        val toggleSymbol = findElementByIdInside(scrollRail!!, "key_toggle_symbol")
        assertNull("key_toggle_symbol should NOT be inside left_scroll_rail", toggleSymbol)
    }

    @Test
    fun testDelExists() {
        val doc = parseXml()
        val keyDel = findElementById(doc, "key_del")
        assertNotNull("key_del should exist", keyDel)
    }

    @Test
    fun testRetypeExists() {
        val doc = parseXml()
        val keyRetype = findElementById(doc, "key_retype")
        assertNotNull("key_retype should exist", keyRetype)
    }

    @Test
    fun testEnterInRightColumn() {
        val doc = parseXml()
        val keyEnter = findElementById(doc, "key_enter")
        assertNotNull("key_enter should exist", keyEnter)
        // key_enter is in the right column, inside enter_container, not in row_band_3
        val enterContainer = findElementById(doc, "enter_container")
        assertNotNull("enter_container should exist", enterContainer)
    }

    @Test
    fun testEnterContainerExists() {
        val doc = parseXml()
        val enterContainer = findElementById(doc, "enter_container")
        assertNotNull("enter_container should exist", enterContainer)
    }

    @Test
    fun testToggleEnglishInBottomRowBand() {
        val doc = parseXml()
        val band4 = findElementById(doc, "row_band_4")
        assertNotNull("row_band_4 should exist", band4)
        val rowT9_4 = findElementByIdInside(band4!!, "row_t9_4")
        assertNotNull("row_t9_4 should be in row_band_4", rowT9_4)
        val toggleEnglish = findElementByIdInside(rowT9_4!!, "key_toggle_english")
        assertNotNull("key_toggle_english should be in row_t9_4 (bottom function row)", toggleEnglish)
    }

    @Test
    fun testRowT9RowsInsideTheirBands() {
        val doc = parseXml()
        val band1 = findElementById(doc, "row_band_1")
        val band2 = findElementById(doc, "row_band_2")
        val band3 = findElementById(doc, "row_band_3")
        val band4 = findElementById(doc, "row_band_4")
        assertNotNull("row_band_1 should exist", band1)
        assertNotNull("row_band_2 should exist", band2)
        assertNotNull("row_band_3 should exist", band3)
        assertNotNull("row_band_4 should exist", band4)

        val row1 = findElementByIdInside(band1!!, "row_t9_1")
        val row2 = findElementByIdInside(band2!!, "row_t9_2")
        val row3 = findElementByIdInside(band3!!, "row_t9_3")
        val row4 = findElementByIdInside(band4!!, "row_t9_4")
        assertNotNull("row_t9_1 should be in row_band_1", row1)
        assertNotNull("row_t9_2 should be in row_band_2", row2)
        assertNotNull("row_t9_3 should be in row_band_3", row3)
        assertNotNull("row_t9_4 should be in row_band_4", row4)
    }

    @Test
    fun testRowT9RowsExist() {
        val doc = parseXml()
        for (rowId in listOf("row_t9_1", "row_t9_2", "row_t9_3", "row_t9_4")) {
            val row = findElementById(doc, rowId)
            assertNotNull("$rowId should exist", row)
        }
    }

    @Test
    fun testMiddleColumnWiderThanRightColumn() {
        val doc = parseXml()
        val row1 = findElementById(doc, "row_t9_1")
        assertNotNull("row_t9_1 should exist", row1)
        // The middle column (weight=5) is wider than the right column (weight=1)
        // in the horizontal LinearLayout with weightSum=6
    }

    // === Number panel tests ===

    @Test
    fun testNumberPanelUsesWeightBasedLayout() {
        val doc = parseXml()
        val numberKeypad = findElementById(doc, "number_keypad")
        assertNotNull("number_keypad should exist", numberKeypad)
        val height = numberKeypad!!.getAttribute("android:layout_height")
        val weight = numberKeypad.getAttribute("android:layout_weight")
        assertEquals("number_keypad should use 0dp height for weight layout", "0dp", height)
        assertTrue("number_keypad should have layout_weight", weight.isNotEmpty())
    }

    @Test
    fun testNumberRowsUseWeightBasedLayout() {
        val doc = parseXml()
        for (rowId in listOf("row_num_1", "row_num_2", "row_num_3", "row_num_4")) {
            val row = findElementById(doc, rowId)
            assertNotNull("$rowId should exist", row)
            val height = row!!.getAttribute("android:layout_height")
            val weight = row.getAttribute("android:layout_weight")
            assertEquals("$rowId should use 0dp height for weight layout", "0dp", height)
            assertTrue("$rowId should have layout_weight", weight.isNotEmpty())
        }
    }

    @Test
    fun testNumberPanelHasZeroKey() {
        val doc = parseXml()
        val panelNumber = findElementById(doc, "panel_number")
        assertNotNull("panel_number should exist", panelNumber)
        val num0 = findElementByIdInside(panelNumber!!, "num_0")
        assertNotNull("num_0 should be in panel_number", num0)
    }

    // === Visibility / layout regression tests ===

    @Test
    fun testMainT9DoesNotHaveZeroKey() {
        val doc = parseXml()
        val panelT9 = findElementById(doc, "panel_t9")
        assertNotNull("panel_t9 should exist", panelT9)
        val num0InT9 = findElementByIdInside(panelT9!!, "num_0")
        assertTrue("num_0 should NOT be in panel_t9", num0InT9 == null)
    }

    @Test
    fun testToggleEnglishInBottomRow() {
        val doc = parseXml()
        val rowT9_4 = findElementById(doc, "row_t9_4")
        assertNotNull("row_t9_4 should exist", rowT9_4)
        val toggleEnglish = findElementByIdInside(rowT9_4!!, "key_toggle_english")
        assertNotNull("key_toggle_english should be in row_t9_4 (bottom row)", toggleEnglish)
    }

    @Test
    fun testCandidateBarIsLinearLayout() {
        val doc = parseXml()
        val candidateBar = findElementById(doc, "candidate_bar")
        assertNotNull("candidate_bar should exist", candidateBar)
        assertEquals("candidate_bar should be a LinearLayout", "LinearLayout", candidateBar!!.tagName)
    }

    @Test
    fun testRootDirectChildrenAreCandidateBarAndKeyboardShell() {
        val doc = parseXml()
        val root = doc.documentElement
        val directChildren = mutableListOf<String>()
        for (i in 0 until root.childNodes.length) {
            val child = root.childNodes.item(i)
            if (child is org.w3c.dom.Element) {
                val id = child.getAttribute("android:id")
                if (id.isNotEmpty()) {
                    directChildren.add(id.removePrefix("@+id/"))
                }
            }
        }
        assertTrue("Root should have candidate_bar as direct child", directChildren.contains("candidate_bar"))
        assertTrue("Root should have keyboard_shell as direct child", directChildren.contains("keyboard_shell"))
        assertEquals("Root should only have 2 direct children with IDs", 2, directChildren.size)
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
}
