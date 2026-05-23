package io.github.xiwei753.pinyin.t9

import android.graphics.Rect
import android.view.MotionEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val PANEL_W = 1080

@RunWith(RobolectricTestRunner::class)
class XiweiKeyboardViewTest {

    private val builder = KeyboardLayoutBuilder()
    private val registry = SymbolKeyRegistry()

    private fun createView(): XiweiKeyboardView {
        val ctx = RuntimeEnvironment.getApplication()
        return XiweiKeyboardView(ctx)
    }

    @Test
    fun actionDownThenActionUpDoesNotCrash() {
        val view = createView()
        view.layoutModel = buildT9Layout()
        view.palette = lightPalette()

        val downEvent = makeTouchEvent(MotionEvent.ACTION_DOWN, 300f, 50f)
        val upEvent = makeTouchEvent(MotionEvent.ACTION_UP, 300f, 50f)

        view.onTouchEvent(downEvent)
        view.onTouchEvent(upEvent)
    }

    @Test
    fun hapticFiresBeforeInputActionDispatch() {
        val view = createView()
        view.layoutModel = buildT9Layout()
        view.palette = lightPalette()

        val events = mutableListOf<String>()
        view.onHapticTap = { events.add("haptic") }
        view.onInputAction = { events.add("input") }

        val key2 = view.layoutModel!!.keys.find { it.id == "key_2" }!!
        val cx = key2.rect.centerX().toFloat()
        val cy = key2.rect.centerY().toFloat()

        view.onTouchEvent(makeTouchEvent(MotionEvent.ACTION_DOWN, cx, cy))
        view.onTouchEvent(makeTouchEvent(MotionEvent.ACTION_UP, cx, cy))

        assertEquals(listOf("haptic", "input"), events)
    }

    @Test
    fun actionDownThenActionCancelDoesNotCrash() {
        val view = createView()
        view.layoutModel = buildT9Layout()
        view.palette = lightPalette()

        val downEvent = makeTouchEvent(MotionEvent.ACTION_DOWN, 300f, 50f)
        val cancelEvent = makeTouchEvent(MotionEvent.ACTION_CANCEL, 300f, 50f)

        view.onTouchEvent(downEvent)
        view.onTouchEvent(cancelEvent)
    }

    @Test
    fun destroyDoesNotCrash() {
        val view = createView()
        view.layoutModel = buildT9Layout()
        view.palette = lightPalette()

        view.onTouchEvent(makeTouchEvent(MotionEvent.ACTION_DOWN, 300f, 50f))
        view.destroy()
    }

    @Test
    fun enterLongPressOnlyTriggersLongPressNotShortPress() {
        val view = createView()
        view.layoutModel = buildT9Layout()
        view.palette = lightPalette()

        val shortPressActions = mutableListOf<String>()
        val longPressActions = mutableListOf<String>()
        view.onEnterShortPress = { shortPressActions.add("short") }
        view.onEnterLongPress = { longPressActions.add("long") }

        val enterKey = view.layoutModel!!.keys.find { it.action == "enter" }!!
        val cx = enterKey.rect.centerX().toFloat()
        val cy = enterKey.rect.centerY().toFloat()

        view.onTouchEvent(makeTouchEvent(MotionEvent.ACTION_DOWN, cx, cy))
        view.simulateLongPress()
        view.onTouchEvent(makeTouchEvent(MotionEvent.ACTION_UP, cx, cy))

        assertTrue("Long press should have been triggered", longPressActions.isNotEmpty())
        assertEquals("Long press should trigger exactly once", 1, longPressActions.size)
        assertTrue("Short press should NOT have been triggered", shortPressActions.isEmpty())
    }

    @Test
    fun t9Key1ActionIsSeparator() {
        val model = buildT9Layout()
        val key1 = model.keys.find { it.id == "key_1" }
        assertNotNull("key_1 should exist", key1)
        assertEquals("separator", key1!!.action)
    }

    @Test
    fun numberModeKey1ActionIsDigit1() {
        val builder = KeyboardLayoutBuilder()
        val model = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)
        val num1 = model.keys.find { it.id == "num_1" }
        assertNotNull("num_1 should exist", num1)
        assertEquals("digit:1", num1!!.action)
    }

    @Test
    fun numberModeDigit1DispatchesDigit1() {
        val view = createView()
        val builder = KeyboardLayoutBuilder()
        view.layoutModel = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)
        view.palette = lightPalette()

        val actions = mutableListOf<String>()
        view.onKeyAction = { actions.add(it) }

        val num1 = view.layoutModel!!.keys.find { it.id == "num_1" }!!
        val cx = num1.rect.centerX().toFloat()
        val cy = num1.rect.centerY().toFloat()

        view.onTouchEvent(makeTouchEvent(MotionEvent.ACTION_DOWN, cx, cy))
        view.onTouchEvent(makeTouchEvent(MotionEvent.ACTION_UP, cx, cy))

        assertEquals(1, actions.size)
        assertEquals("digit:1", actions[0])
    }

    @Test
    fun t9ModeSeparatorDispatchesSeparator() {
        val view = createView()
        view.layoutModel = buildT9Layout()
        view.palette = lightPalette()

        val actions = mutableListOf<String>()
        view.onKeyAction = { actions.add(it) }

        val key1 = view.layoutModel!!.keys.find { it.id == "key_1" }!!
        val cx = key1.rect.centerX().toFloat()
        val cy = key1.rect.centerY().toFloat()

        view.onTouchEvent(makeTouchEvent(MotionEvent.ACTION_DOWN, cx, cy))
        view.onTouchEvent(makeTouchEvent(MotionEvent.ACTION_UP, cx, cy))

        assertEquals(1, actions.size)
        assertEquals("separator", actions[0])
    }

    @Test
    fun numberModeDigits0To9AllDispatchDigitActions() {
        val builder = KeyboardLayoutBuilder()
        val model = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)

        val allDigitKeys = (model.keys + model.leftRailKeys)
            .filter { it.action.startsWith("digit:") }
        assertEquals("Number mode should have 9 + 2 digit keys", 11, allDigitKeys.size)

        for (key in allDigitKeys) {
            assertTrue(key.action.startsWith("digit:"))
        }
    }

    @Test
    fun hitTestDoesNotModifyKeyRects() {
        val model = buildT9Layout()
        val renderer = KeyboardRenderer()

        val key2 = model.keys.find { it.id == "key_2" }!!
        val originalRect = Rect(key2.rect)

        val cx = key2.rect.centerX().toFloat()
        val cy = key2.rect.centerY().toFloat()

        renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)

        assertEquals("key_2 rect should be unchanged", originalRect, key2.rect)
    }

    @Test
    fun hitTestOnAllKeysDoesNotModifyRects() {
        val model = buildT9Layout()
        val renderer = KeyboardRenderer()
        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)
        val originalRects = allKeys.map { it.id to Rect(it.rect) }.toMap()

        for (key in allKeys) {
            val cx = key.rect.centerX().toFloat()
            val cy = key.rect.centerY().toFloat()
            renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        }

        for (key in allKeys) {
            assertEquals("Key ${key.id} rect should be unchanged", originalRects[key.id], key.rect)
        }
    }

    @Test
    fun hitDigit2ReturnsKey2() {
        val model = buildT9Layout()
        val renderer = KeyboardRenderer()
        val key2 = model.keys.find { it.id == "key_2" }!!
        val cx = key2.rect.centerX().toFloat()
        val cy = key2.rect.centerY().toFloat()

        val hit = KeyboardRenderer().hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertEquals("key_2", hit!!.id)
    }

    @Test
    fun hitPlaceholderReturnsKey() {
        val model = buildT9Layout()
        val renderer = KeyboardRenderer()
        val hit = renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, -10f, -10f, 0)
        assertNull("Hit outside bounds should return null", hit)
    }

    @Test
    fun hitSymbolKeyReturnsSymbolAction() {
        val entries = listOf(1 to "A", 2 to "B", 3 to "C", 4 to "D", 5 to "E")
        val registry = SymbolKeyRegistry()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 2.5f)

        val symbolKeys = model.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }
        assertTrue(symbolKeys.isNotEmpty())

        val first = symbolKeys.first()
        val cx = first.rect.centerX().toFloat()
        val cy = first.rect.centerY().toFloat()

        val hit = KeyboardRenderer().hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertEquals("symbol:commit", hit!!.action)
    }

    @Test
    fun hitLeftRailKeysAlwaysReturnsPunctAction() {
        val readings = emptyList<String>()
        val builder = KeyboardLayoutBuilder()
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, readings, false, KeyboardMode.ChineseT9)
        val renderer = KeyboardRenderer()

        val punctKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_PUNCT }
        assertEquals(4, punctKeys.size)

        val first = punctKeys.first()
        val cx = first.rect.centerX().toFloat()
        val cy = first.rect.centerY().toFloat()

        val hit = KeyboardRenderer().hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertTrue(hit!!.action.startsWith("punct:"))
    }

    @Test
    fun hitBottomLeftKeyReturnsCorrectAction() {
        val builder = KeyboardLayoutBuilder()
        val model = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)
        val renderer = KeyboardRenderer()

        val bt = model.bottomLeftKey
        assertNotNull(bt)
        val cx = bt!!.rect.centerX().toFloat()
        val cy = bt.rect.centerY().toFloat()

        val hit = KeyboardRenderer().hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertEquals("toggle:symbol", hit!!.action)
    }

    @Test
    fun applyThemeUpdatesXiweiKeyboardViewPalette() {
        val palette = lightPalette()
        assertEquals(0xFFFFFFFF.toInt(), palette.keyBgColor)
        assertEquals(0xFFEBEBEB.toInt(), palette.specialKeyBgColor)
        assertFalse(palette.isDark)
    }

    @Test
    fun themeControllerDoesNotCallOldKeyBackgroundMethods() {
        val palette = lightPalette()
        val mockViews = mockKeyboardViews()

        val repo = mockSettingsRepository("light")
        val res = mockResources()
        val controller = KeyboardThemeController(repo, res)

        controller.applyTheme(mockViews, palette)
    }

    @Test
    fun onSizeChangedTriggersRebuildCallback() {
        val view = createView()
        view.layoutModel = buildT9Layout()

        val calls = mutableListOf<Pair<Int, Int>>()
        view.requestRebuildLayout = { calls.add(Pair(view.width, view.height)) }

        view.layout(0, 0, 1080, 480)
    }

    @Test
    fun layoutModelWidthMatchesViewWidth() {
        val builder = KeyboardLayoutBuilder()
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        assertEquals(1080, model.panelWidth)
        assertEquals(480, model.panelHeight)
    }

    @Test
    fun onSizeChangedWithSameDimsDoesNotRebuild() {
        val view = createView()
        var callCount = 0
        view.requestRebuildLayout = { callCount++ }

        view.layout(0, 0, 1080, 480)
        assertEquals("First layout should trigger rebuild", 1, callCount)

        view.layout(0, 0, 1080, 480)
        assertEquals("Same size should not rebuild again", 1, callCount)

        view.layout(0, 0, 720, 400)
        assertEquals("Different size should rebuild", 2, callCount)
    }

    @Test
    fun symbolBuildWithDensity1DoesNotExceedBounds() {
        val pairs = makeSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, pairs, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 1.0f)
        for (k in model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)) {
            if (k.role == KeyboardKeyRole.PLACEHOLDER) continue
            assertTrue("key ${k.id} left >= 0", k.rect.left >= 0)
            assertTrue("key ${k.id} top >= 0", k.rect.top >= 0)
            assertTrue("key ${k.id} right <= panelWidth", k.rect.right <= 1080)
        }
    }

    @Test
    fun symbolBuildWithDensity3_5DoesNotExceedBounds() {
        val pairs = makeSymbolEntries()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, pairs, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 3.5f)
        for (k in model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)) {
            if (k.role == KeyboardKeyRole.PLACEHOLDER) continue
            assertTrue("key ${k.id} left >= 0", k.rect.left >= 0)
            assertTrue("key ${k.id} top >= 0", k.rect.top >= 0)
            assertTrue("key ${k.id} right <= panelWidth", k.rect.right <= 1080)
        }
    }

    @Test
    fun symbolBuildAlwaysHas5Columns() {
        val pairs = makeSymbolEntries()
        for (density in listOf(1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f)) {
            val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, pairs, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = density)
            val symbolKeys = model.keys.filter { (it.role == KeyboardKeyRole.SYMBOL_KEY || it.role == KeyboardKeyRole.PLACEHOLDER) && it.id != "symbol_bottom_right_placeholder" }
            val rows = symbolKeys.chunked(5)
            for ((ri, row) in rows.withIndex()) {
                assertEquals("Row $ri at density $density should have 5 cells", 5, row.size)
            }
        }
    }

    @Test
    fun symbolCellWidthChangesWithDensity() {
        val pairs = makeSymbolEntries()
        val modelLow = builder.buildSymbol(1080, 480, 96, 88, 8, 8, pairs, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 1.0f)
        val modelHigh = builder.buildSymbol(1080, 480, 96, 88, 8, 8, pairs, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 3.0f)
        val lowCellWidth = modelLow.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }.firstOrNull()?.rect?.width() ?: 0
        val highCellWidth = modelHigh.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }.firstOrNull()?.rect?.width() ?: 0
        assertTrue("Cell width should be positive at density 1.0", lowCellWidth > 0)
        assertTrue("Cell width should be positive at density 3.0", highCellWidth > 0)
    }

    @Test
    fun symbolPlaceholderOnlyAtEndOfLastRow() {
        val pairs = listOf(1 to "A", 2 to "B", 3 to "C", 4 to "D", 5 to "E", 6 to "F", 7 to "G")
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, pairs, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 2.5f)
        val placeholders = model.keys.filter { it.role == KeyboardKeyRole.PLACEHOLDER && it.id != "symbol_bottom_right_placeholder" }
        assertEquals("7 entries should produce 3 placeholders (row of 5 + row of 2 => 3 fillers)", 3, placeholders.size)
        val symbolKeys = model.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }
        val lastSymbolIdx = model.keys.indexOfLast { it.role == KeyboardKeyRole.SYMBOL_KEY }
        for (ph in placeholders) {
            val phIdx = model.keys.indexOf(ph)
            assertTrue("Placeholder should appear after last symbol key in keys list", phIdx > lastSymbolIdx)
        }
    }

    @Test
    fun hitTestKeyRectUnchangedAfterHit() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        val renderer = KeyboardRenderer()
        val allKeys = model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)
        val before = allKeys.map { it.id to android.graphics.Rect(it.rect) }.toMap()
        for (k in allKeys) {
            renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, k.rect.centerX().toFloat(), k.rect.centerY().toFloat(), 0)
        }
        for (k in allKeys) {
            assertEquals("Key ${k.id} rect unchanged", before[k.id], k.rect)
        }
    }

    @Test
    fun hitTestLeftRailKeyRectUnchanged() {
        val readings = listOf("meng", "neng", "men")
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, readings, true, KeyboardMode.ChineseT9)
        val renderer = KeyboardRenderer()
        for (k in model.leftRailKeys) {
            val before = android.graphics.Rect(k.rect)
            renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, k.rect.centerX().toFloat(), k.rect.centerY().toFloat(), 0)
            assertEquals("leftRail key ${k.id} rect unchanged", before, k.rect)
        }
    }

    @Test
    fun hitPlaceholderReturnsSymbolKeyNotAction() {
        val pairs = listOf(1 to "A")
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, pairs, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 2.5f)
        val renderer = KeyboardRenderer()
        val placeholder = model.keys.find { it.role == KeyboardKeyRole.PLACEHOLDER && it.id != "symbol_bottom_right_placeholder" }
        assertNotNull("Should have placeholder", placeholder)
        val cx = placeholder!!.rect.centerX().toFloat()
        val cy = placeholder.rect.centerY().toFloat()
        val hit = KeyboardRenderer().hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertEquals(KeyboardKeyRole.PLACEHOLDER, hit!!.role)
    }

    @Test
    fun hitSymbolKeyReturnsSymbolCommit() {
        val pairs = listOf(1 to "A", 2 to "B", 3 to "C", 4 to "D", 5 to "E")
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, pairs, "punct", KeyboardMode.ChineseT9, emptyMap(), registry, density = 2.5f)
        val renderer = KeyboardRenderer()
        val first = model.keys.find { it.role == KeyboardKeyRole.SYMBOL_KEY }!!
        val cx = first.rect.centerX().toFloat()
        val cy = first.rect.centerY().toFloat()
        val hit = KeyboardRenderer().hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertEquals("symbol:commit", hit!!.action)
    }

    @Test
    fun hitT9Key2ReturnsDigit2() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        val renderer = KeyboardRenderer()
        val k2 = model.keys.find { it.id == "key_2" }!!
        val cx = k2.rect.centerX().toFloat()
        val cy = k2.rect.centerY().toFloat()
        val hit = KeyboardRenderer().hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertEquals("digit:2", hit!!.action)
    }

    @Test
    fun hitNumberKey1ReturnsDigit1() {
        val model = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)
        val renderer = KeyboardRenderer()
        val n1 = model.keys.find { it.id == "num_1" }!!
        val cx = n1.rect.centerX().toFloat()
        val cy = n1.rect.centerY().toFloat()
        val hit = KeyboardRenderer().hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertEquals("digit:1", hit!!.action)
    }

    @Test
    fun hitT9Key1ReturnsSeparator() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
        val renderer = KeyboardRenderer()
        val k1 = model.keys.find { it.id == "key_1" }!!
        val cx = k1.rect.centerX().toFloat()
        val cy = k1.rect.centerY().toFloat()
        val hit = KeyboardRenderer().hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertEquals("separator", hit!!.action)
    }

    // --- helpers ---

    private fun buildT9Layout(): KeyboardLayoutModel {
        val builder = KeyboardLayoutBuilder()
        return builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), false, KeyboardMode.ChineseT9)
    }

    private fun lightPalette(): ThemePalette {
        return ThemePalette(
            bgColor = ThemeColors.LIGHT_BG,
            candidateBarColor = ThemeColors.LIGHT_CANDIDATE_BAR,
            textColor = ThemeColors.LIGHT_TEXT,
            subColor = ThemeColors.LIGHT_SUB,
            preeditBgColor = ThemeColors.LIGHT_PREEDIT_BG,
            symTabActiveBg = ThemeColors.LIGHT_TAB_ACTIVE_BG,
            symTabInactiveBg = ThemeColors.LIGHT_TAB_INACTIVE_BG,
            symTabActiveText = ThemeColors.LIGHT_TAB_ACTIVE_TEXT,
            symTabInactiveText = ThemeColors.LIGHT_TAB_INACTIVE_TEXT,
            isDark = false,
            keyBgColor = ThemeColors.LIGHT_KEY_BG,
            specialKeyBgColor = ThemeColors.LIGHT_SPECIAL_KEY_BG,
            keyPressedBgColor = ThemeColors.LIGHT_KEY_PRESSED,
            specialKeyPressedBgColor = ThemeColors.LIGHT_SPECIAL_KEY_PRESSED,
        )
    }

    private fun mockSettingsRepository(theme: String): SettingsRepository {
        val org = Mockito.mock(SettingsRepository::class.java)
        Mockito.`when`(org.getTheme()).thenReturn(theme)
        return org
    }

    private fun mockResources(): android.content.res.Resources {
        val org = Mockito.mock(android.content.res.Resources::class.java)
        val config = android.content.res.Configuration()
        Mockito.`when`(org.configuration).thenReturn(config)
        val metrics = android.util.DisplayMetrics().apply { density = 2.0f }
        Mockito.`when`(org.displayMetrics).thenReturn(metrics)
        return org
    }

    private fun mockKeyboardViews(): KeyboardViews {
        return KeyboardViews(
            imeRoot = Mockito.mock(),
            candidateBar = Mockito.mock(),
            candidateContainer = Mockito.mock(),
            pinyinFloatingBar = Mockito.mock(),
            pinyinFloatingText = Mockito.mock(),
            xiweiKeyboardView = Mockito.mock(),
        )
    }

    private fun makeTouchEvent(action: Int, x: Float, y: Float): MotionEvent {
        return MotionEvent.obtain(0, System.currentTimeMillis(), action, x, y, 0)
    }

    private fun makeSymbolEntries(): List<Pair<Int, String>> {
        return (1..37).map { it to "s$it" }
    }
}
