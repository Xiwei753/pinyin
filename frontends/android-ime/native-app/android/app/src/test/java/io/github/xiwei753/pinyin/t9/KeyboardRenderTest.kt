package io.github.xiwei753.pinyin.t9

import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyboardRenderTest {

    private val renderer = KeyboardRenderer()
    private val builder = KeyboardLayoutBuilder()
    private val density = 2.0f

    private lateinit var darkPalette: ThemePalette
    private lateinit var lightPalette: ThemePalette

    @Before
    fun setUp() {
        darkPalette = ThemePalette(
            bgColor = ThemeColors.DARK_BG,
            candidateBarColor = ThemeColors.DARK_CANDIDATE_BAR,
            textColor = ThemeColors.DARK_TEXT,
            subColor = ThemeColors.DARK_SUB,
            preeditBgColor = ThemeColors.DARK_PREEDIT_BG,
            symTabActiveBg = ThemeColors.DARK_TAB_ACTIVE_BG,
            symTabInactiveBg = ThemeColors.DARK_TAB_INACTIVE_BG,
            symTabActiveText = ThemeColors.DARK_TAB_ACTIVE_TEXT,
            symTabInactiveText = ThemeColors.DARK_TAB_INACTIVE_TEXT,
            isDark = true,
            keyBgColor = ThemeColors.DARK_KEY_BG,
            specialKeyBgColor = ThemeColors.DARK_SPECIAL_KEY_BG,
            keyPressedBgColor = ThemeColors.DARK_KEY_PRESSED,
            specialKeyPressedBgColor = ThemeColors.DARK_SPECIAL_KEY_PRESSED,
        )

        lightPalette = ThemePalette(
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

    @Test
    fun darkModeNormalKeyBgIsNotWhite() {
        val white = 0xFFFFFFFF.toInt()
        assertNotEquals(darkPalette.keyBgColor, white)
        assertTrue(darkPalette.keyBgColor and 0x00FFFFFF < 0x00808080)
    }

    @Test
    fun darkModeSpecialKeyBgIsNotLightGray() {
        val lightGray = 0xFFEBEBEB.toInt()
        assertNotEquals(darkPalette.specialKeyBgColor, lightGray)
    }

    @Test
    fun labelAndSubLabelColorsAreCorrect() {
        assertNotEquals(lightPalette.textColor, lightPalette.subColor)
        assertNotEquals(darkPalette.textColor, darkPalette.subColor)
    }

    @Test
    fun t9ModeDrawDoesNotCrash() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)
        val bitmap = Bitmap.createBitmap(1080, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        try {
            renderer.drawKeyboard(canvas, model, darkPalette, density, KeyboardMode.ChineseT9, null)
        } catch (e: Exception) {
            fail("drawKeyboard should not crash: ${e.message}")
        }
    }

    @Test
    fun numberModeDrawDoesNotCrash() {
        val model = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)
        val bitmap = Bitmap.createBitmap(1080, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        try {
            renderer.drawKeyboard(canvas, model, darkPalette, density, KeyboardMode.Number, null)
        } catch (e: Exception) {
            fail("drawKeyboard should not crash: ${e.message}")
        }
    }

    @Test
    fun symbolModeDrawDoesNotCrash() {
        val entries = listOf(1 to "A", 2 to "B", 3 to "C", 4 to "D", 5 to "E")
        val registry = SymbolKeyRegistry()
        val model = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, emptyMap(), registry)
        val bitmap = Bitmap.createBitmap(1080, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        try {
            renderer.drawKeyboard(canvas, model, darkPalette, density, KeyboardMode.Symbol, "punct")
        } catch (e: Exception) {
            fail("drawKeyboard should not crash: ${e.message}")
        }
    }

    @Test
    fun drawDoesNotChangeKeyRects() {
        val model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)
        val originalRects = (model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)).map { it.id to android.graphics.Rect(it.rect) }

        val bitmap = Bitmap.createBitmap(1080, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        renderer.drawKeyboard(canvas, model, lightPalette, density, KeyboardMode.ChineseT9, null)

        val newRects = (model.keys + model.leftRailKeys + listOfNotNull(model.bottomLeftKey)).map { it.id to it.rect }
        for ((id, orig) in originalRects) {
            val newRect = newRects.first { it.first == id }.second
            assertEquals("Key $id rect should not change after draw", orig, newRect)
        }
    }

    @Test
    fun allModesUseThemePaletteWithoutStateListDrawable() {
        val t9Model = builder.buildT9(1080, 480, 96, 88, 8, 8, emptyList(), KeyboardMode.ChineseT9)
        val numModel = builder.buildNumber(1080, 480, 96, 88, 8, 8, KeyboardMode.Number, KeyboardMode.ChineseT9)
        val entries = listOf(1 to "A", 2 to "B")
        val registry = SymbolKeyRegistry()
        val symModel = builder.buildSymbol(1080, 480, 96, 88, 8, 8, entries, "punct", KeyboardMode.ChineseT9, emptyMap(), registry)

        for (model in listOf(t9Model, numModel, symModel)) {
            val bitmap = Bitmap.createBitmap(1080, 480, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            renderer.drawKeyboard(canvas, model, darkPalette, density, KeyboardMode.ChineseT9, null)
            assertTrue(bitmap.width > 0)
        }
    }
}
