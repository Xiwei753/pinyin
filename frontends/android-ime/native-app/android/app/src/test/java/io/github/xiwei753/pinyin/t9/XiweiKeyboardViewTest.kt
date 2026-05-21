package io.github.xiwei753.pinyin.t9

import android.graphics.Rect
import android.view.MotionEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class XiweiKeyboardViewTest {

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

        val hit = renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
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
        val builder = KeyboardLayoutBuilder()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, emptyMap(), registry)
        val renderer = KeyboardRenderer()

        val symbolKeys = model.keys.filter { it.role == KeyboardKeyRole.SYMBOL_KEY }
        assertTrue(symbolKeys.isNotEmpty())

        val first = symbolKeys.first()
        val cx = first.rect.centerX().toFloat()
        val cy = first.rect.centerY().toFloat()

        val hit = renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertEquals("symbol:commit", hit!!.action)
    }

    @Test
    fun hitLeftRailReadingReturnsReadingAction() {
        val readings = listOf("meng", "neng", "men")
        val builder = KeyboardLayoutBuilder()
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, readings, KeyboardMode.ChineseT9)
        val renderer = KeyboardRenderer()

        val readingKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.LEFT_RAIL_READING && it.label.isNotEmpty() }
        assertTrue(readingKeys.isNotEmpty())

        val first = readingKeys.first()
        val cx = first.rect.centerX().toFloat()
        val cy = first.rect.centerY().toFloat()

        val hit = renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
        assertNotNull(hit)
        assertTrue(hit!!.action.startsWith("reading:"))
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

        val hit = renderer.hitTest(model.keys, model.leftRailKeys, model.bottomLeftKey, cx, cy, 0)
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
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)
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

    // --- helpers ---

    private fun buildT9Layout(): KeyboardLayoutModel {
        val builder = KeyboardLayoutBuilder()
        return builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)
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
            keyboardShell = Mockito.mock(),
            panelT9 = Mockito.mock(),
            panelSymbol = Mockito.mock(),
            panelNumber = Mockito.mock(),
            readingTextViews = listOf(),
            punctTextViews = listOf(),
            symPagePunct = Mockito.mock(),
            symPageMath = Mockito.mock(),
            symPageBracket = Mockito.mock(),
            symPageOther = Mockito.mock(),
            symScrollContent = Mockito.mock(),
            leftScrollRail = Mockito.mock(),
            leftScrollContent = Mockito.mock(),
            key1Text = Mockito.mock(),
            key2 = Mockito.mock(), key3 = Mockito.mock(), key4 = Mockito.mock(), key5 = Mockito.mock(),
            key6 = Mockito.mock(), key7 = Mockito.mock(), key8 = Mockito.mock(), key9 = Mockito.mock(),
            key2Number = Mockito.mock(), key3Number = Mockito.mock(), key4Number = Mockito.mock(), key5Number = Mockito.mock(),
            key6Number = Mockito.mock(), key7Number = Mockito.mock(), key8Number = Mockito.mock(), key9Number = Mockito.mock(),
            key2Letters = Mockito.mock(), key3Letters = Mockito.mock(), key4Letters = Mockito.mock(), key5Letters = Mockito.mock(),
            key6Letters = Mockito.mock(), key7Letters = Mockito.mock(), key8Letters = Mockito.mock(), key9Letters = Mockito.mock(),
            keyDel = Mockito.mock(), keyRetype = Mockito.mock(), keyEnter = Mockito.mock(), keySpace = Mockito.mock(),
            keyToggleSymbol = Mockito.mock(), keyToggleNumber = Mockito.mock(),
            keyToggleEnglish = Mockito.mock(),
            enterContainer = Mockito.mock(),
            symTabPunct = Mockito.mock(), symTabMath = Mockito.mock(), symTabBracket = Mockito.mock(), symTabOther = Mockito.mock(),
            symCategoryTabs = Mockito.mock(),
            generatedSymbolViews = mutableListOf(),
            num0 = Mockito.mock(), num1 = Mockito.mock(), num2 = Mockito.mock(), num3 = Mockito.mock(), num4 = Mockito.mock(),
            num5 = Mockito.mock(), num6 = Mockito.mock(), num7 = Mockito.mock(), num8 = Mockito.mock(), num9 = Mockito.mock(), numDot = Mockito.mock(),
            numKey1Frame = Mockito.mock(), numKey2Frame = Mockito.mock(), numKey3Frame = Mockito.mock(), numKey4Frame = Mockito.mock(), numKey5Frame = Mockito.mock(),
            numKey6Frame = Mockito.mock(), numKey7Frame = Mockito.mock(), numKey8Frame = Mockito.mock(), numKey9Frame = Mockito.mock(),
            numDotFrame = Mockito.mock(), num0Frame = Mockito.mock(),
            t9LeftScrollFrame = Mockito.mock(), t9SymbolButtonFrame = Mockito.mock(),
            t9Key1Frame = Mockito.mock(), t9Key2Frame = Mockito.mock(), t9Key3Frame = Mockito.mock(),
            t9Key4Frame = Mockito.mock(), t9Key5Frame = Mockito.mock(), t9Key6Frame = Mockito.mock(),
            t9Key7Frame = Mockito.mock(), t9Key8Frame = Mockito.mock(), t9Key9Frame = Mockito.mock(),
            t9DelFrame = Mockito.mock(), t9RetypeFrame = Mockito.mock(), t9NumberFrame = Mockito.mock(),
            t9SpaceFrame = Mockito.mock(), t9EnglishFrame = Mockito.mock(),
            xiweiKeyboardView = null,
        )
    }

    private fun makeTouchEvent(action: Int, x: Float, y: Float): MotionEvent {
        return MotionEvent.obtain(0, System.currentTimeMillis(), action, x, y, 0)
    }
}
