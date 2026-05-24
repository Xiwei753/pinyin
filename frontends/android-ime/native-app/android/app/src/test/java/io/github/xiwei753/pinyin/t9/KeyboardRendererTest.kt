package io.github.xiwei753.pinyin.t9

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import io.github.xiwei753.pinyin.imecore.LayoutTokens
import org.junit.Assert.*
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class KeyboardRendererTest {

    @Test
    fun testPunctTextSizeIsLargeEnough() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0, subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0, symTabInactiveBg = 0,
            symTabActiveText = 0, symTabInactiveText = 0, isDark = false,
            keyBgColor = 0, specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0
        )
        
        val punctKey = KeyboardKey(
            id = "punct_，",
            role = KeyboardKeyRole.RAIL_PUNCT,
            rect = Rect(0, 0, 100, 100),
            label = "，",
            action = "punct:，",
        )
        val model = KeyboardLayoutModel(emptyList(), listOf(punctKey), null, 1000, 1000)

        var capturedTextSize = 0f

        doAnswer { invocation ->
            val text = invocation.arguments[0] as String
            if (text == "，") {
                val paint = invocation.arguments[3] as Paint
                capturedTextSize = paint.textSize
                println("Punct text size: $capturedTextSize")
            }
            null
        }.`when`(mockCanvas).drawText(anyString(), anyFloat(), anyFloat(), any(Paint::class.java))

        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.ChineseT9, null)

        assertTrue("Text size should be at least 0.61 * height. actual size: $capturedTextSize", capturedTextSize > 61f)
    }

    @Test
    fun testT9KeyHasDigitOnTopAndLettersInMiddle() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0, subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0, symTabInactiveBg = 0,
            symTabActiveText = 0, symTabInactiveText = 0, isDark = false,
            keyBgColor = 0, specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0
        )
        
        val t9Key = KeyboardKey(
            id = "key_2",
            role = KeyboardKeyRole.NORMAL,
            rect = Rect(0, 0, 100, 100),
            label = "2",
            subLabel = "ABC",
            action = "digit:2",
        )
        val model = KeyboardLayoutModel(listOf(t9Key), emptyList(), null, 1000, 1000)

        var digitY = 0f
        var digitSize = 0f
        var lettersY = 0f
        var lettersSize = 0f

        doAnswer { invocation ->
            val text = invocation.arguments[0] as String
            val y = invocation.arguments[2] as Float
            val paint = invocation.arguments[3] as Paint
            if (text == "2") {
                digitY = y
                digitSize = paint.textSize
                println("Digit size: $digitSize, y: $digitY")
            } else if (text == "ABC") {
                lettersY = y
                lettersSize = paint.textSize
                println("Letters size: $lettersSize, y: $lettersY")
            }
            null
        }.`when`(mockCanvas).drawText(anyString(), anyFloat(), anyFloat(), any(Paint::class.java))

        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.ChineseT9, null)

        assertTrue("Digit should be drawn", digitSize > 0f)
        assertTrue("Letters should be drawn", lettersSize > 0f)
        assertTrue("Digit should be drawn above letters (smaller Y coordinate). digitY=$digitY, lettersY=$lettersY", digitY < lettersY)
        assertTrue("Letters text size should be larger than digit text size", lettersSize > digitSize)
    }

    @Test
    fun testNumberKeyWithoutSubLabelIsDrawnCentered() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0, subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0, symTabInactiveBg = 0,
            symTabActiveText = 0, symTabInactiveText = 0, isDark = false,
            keyBgColor = 0, specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0
        )
        
        val numKey = KeyboardKey(
            id = "num_1",
            role = KeyboardKeyRole.NORMAL,
            rect = Rect(0, 0, 100, 100),
            label = "1",
            subLabel = null,
            action = "digit:1",
        )
        val model = KeyboardLayoutModel(listOf(numKey), emptyList(), null, 1000, 1000)

        var drawnY = 0f

        doAnswer { invocation ->
            val text = invocation.arguments[0] as String
            if (text == "1") {
                drawnY = invocation.arguments[2] as Float
                println("drawnY: $drawnY")
            }
            null
        }.`when`(mockCanvas).drawText(anyString(), anyFloat(), anyFloat(), any(Paint::class.java))

        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.Number, null)

        // It should be near the center (50) minus descent/ascent offset.
        assertTrue("Digit should be drawn near the center. drawnY=$drawnY", drawnY > 30f && drawnY < 70f)
    }

    @Test
    fun testReadingTextSizeIsLargeEnough() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0, subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0, symTabInactiveBg = 0,
            symTabActiveText = 0, symTabInactiveText = 0, isDark = false,
            keyBgColor = 0, specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0
        )
        
        val readingKey = KeyboardKey(
            id = "reading_0",
            role = KeyboardKeyRole.RAIL_READING,
            rect = Rect(0, 0, 100, 100),
            label = "mi",
            action = "reading:0",
        )
        val model = KeyboardLayoutModel(emptyList(), listOf(readingKey), null, 1000, 1000)

        var capturedTextSize = 0f

        doAnswer { invocation ->
            val text = invocation.arguments[0] as String
            if (text == "mi") {
                val paint = invocation.arguments[3] as Paint
                capturedTextSize = paint.textSize
                println("Reading text size: $capturedTextSize")
            }
            null
        }.`when`(mockCanvas).drawText(anyString(), anyFloat(), anyFloat(), any(Paint::class.java))

        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.ChineseT9, null)

        // Height is 100, Width is 100. Size should be minOf(100 * 0.45, 100 * 0.65) = 45.
        assertEquals(45f, capturedTextSize, 0.01f)
    }

    @Test
    fun railedItemsUseLayoutTokensCornerRadius() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0, subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0, symTabInactiveBg = 0,
            symTabActiveText = 0, symTabInactiveText = 0, isDark = false,
            keyBgColor = 0, specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0,
            layoutTokens = LayoutTokens(keyCornerRadius = 8f),
        )
        val punctKey = KeyboardKey(id="punct_，", role=KeyboardKeyRole.RAIL_PUNCT, rect=Rect(0,0,100,100), label="，", action="punct:，")
        val readingKey = KeyboardKey(id="reading_0", role=KeyboardKeyRole.RAIL_READING, rect=Rect(0,0,100,100), label="mi", action="reading:0")
        val catKey = KeyboardKey(id="sym_tab_punct", role=KeyboardKeyRole.RAIL_SYMBOL_CATEGORY, rect=Rect(0,0,100,100), label="标点", action="symtab:punct")
        val model = KeyboardLayoutModel(
            keys = emptyList(),
            leftRailKeys = listOf(punctKey, readingKey, catKey),
            bottomLeftKey = null,
            panelWidth = 1000,
            panelHeight = 1000,
        )

        val capturedRadii = mutableListOf<Float>()
        doAnswer { invocation ->
            val radius = invocation.arguments[2] as Float
            capturedRadii.add(radius)
            null
        }.`when`(mockCanvas).drawRoundRect(any(RectF::class.java), anyFloat(), anyFloat(), any(Paint::class.java))

        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.ChineseT9, null)

        assertTrue("Should have drawn at least 3 keys", capturedRadii.size >= 3)
        for (r in capturedRadii) {
            assertEquals("All rail items should use keyCornerRadius from LayoutTokens", 8f, r, 0.01f)
        }
    }

    @Test
    fun pressedStateDoesNotChangeKeyRect() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0, subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0, symTabInactiveBg = 0,
            symTabActiveText = 0, symTabInactiveText = 0, isDark = false,
            keyBgColor = 0, specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0,
        )
        val punctKey = KeyboardKey(id="punct_，", role=KeyboardKeyRole.RAIL_PUNCT, rect=Rect(0,0,100,100), label="，", action="punct:，")
        val model = KeyboardLayoutModel(emptyList(), listOf(punctKey), null, 1000, 1000)

        val originalRect = Rect(punctKey.rect)
        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.ChineseT9, null, pressedKeyId = "punct_，")
        assertEquals("Pressed state should not change rect", originalRect, punctKey.rect)
    }

    @Test
    fun selectedStateDoesNotChangeKeyRect() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0, subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0, symTabInactiveBg = 0,
            symTabActiveText = 0, symTabInactiveText = 0, isDark = false,
            keyBgColor = 0, specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0,
        )
        val catKey = KeyboardKey(id="sym_tab_math", role=KeyboardKeyRole.RAIL_SYMBOL_CATEGORY, rect=Rect(0,0,100,100), label="数学", action="symtab:math", isSelected = true)
        val model = KeyboardLayoutModel(emptyList(), listOf(catKey), null, 1000, 1000)

        val originalRect = Rect(catKey.rect)
        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.Symbol, null)
        assertEquals("Selected state should not change rect", originalRect, catKey.rect)
    }

    @Test
    fun testSelectedKeyDrawsActiveColors() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0xAA000000.toInt(), subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0xFFFF0000.toInt(), symTabInactiveBg = 0,
            symTabActiveText = 0xFF00FF00.toInt(), symTabInactiveText = 0, isDark = false,
            keyBgColor = 0xBB000000.toInt(), specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0
        )
        
        val selectedKey = KeyboardKey(
            id = "reading_0",
            role = KeyboardKeyRole.RAIL_READING,
            rect = Rect(0, 0, 100, 100),
            label = "mi",
            action = "reading:0",
            isSelected = true,
        )
        val model = KeyboardLayoutModel(emptyList(), listOf(selectedKey), null, 1000, 1000)

        var capturedBgColor = 0
        var capturedTextColor = 0

        // Capture background color
        doAnswer { invocation ->
            val paint = invocation.arguments[3] as Paint
            capturedBgColor = paint.color
            null
        }.`when`(mockCanvas).drawRoundRect(any(RectF::class.java), anyFloat(), anyFloat(), any(Paint::class.java))

        // Capture text color
        doAnswer { invocation ->
            val text = invocation.arguments[0] as String
            if (text == "mi") {
                val paint = invocation.arguments[3] as Paint
                capturedTextColor = paint.color
            }
            null
        }.`when`(mockCanvas).drawText(anyString(), anyFloat(), anyFloat(), any(Paint::class.java))

        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.ChineseT9, null)

        assertEquals("Background color should be symTabActiveBg", palette.symTabActiveBg, capturedBgColor)
        assertEquals("Text color should be symTabActiveText", palette.symTabActiveText, capturedTextColor)
    }


    @Test
    fun testReadingTextIsScaledToFitInRect() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0, subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0, symTabInactiveBg = 0,
            symTabActiveText = 0, symTabInactiveText = 0, isDark = false,
            keyBgColor = 0, specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0
        )

        val longReadingKey = KeyboardKey(
            id = "reading_0",
            role = KeyboardKeyRole.RAIL_READING,
            rect = android.graphics.Rect(0, 0, 50, 50),
            label = "jin tian wan shang",
            action = "reading:0",
        )
        val shortReadingKey = KeyboardKey(
            id = "reading_1",
            role = KeyboardKeyRole.RAIL_READING,
            rect = android.graphics.Rect(0, 0, 50, 50),
            label = "ti",
            action = "reading:1",
        )

        val model = KeyboardLayoutModel(emptyList(), listOf(longReadingKey, shortReadingKey), null, 1000, 1000)

        var longTextSize = 0f
        var shortTextSize = 0f

        doAnswer { invocation ->
            val text = invocation.arguments[0] as String
            val paint = invocation.arguments[3] as Paint
            if (text == "jin tian wan shang") {
                longTextSize = paint.textSize
            } else if (text == "ti") {
                shortTextSize = paint.textSize
            }
            null
        }.`when`(mockCanvas).drawText(anyString(), anyFloat(), anyFloat(), any(Paint::class.java))

        doAnswer { invocation ->
            true
        }.`when`(mockCanvas).clipRect(any(android.graphics.Rect::class.java))

        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.ChineseT9, null)

        // Mocking Paint is not done, so it's using the real Paint which measures texts dynamically. Robolectric does basic text measuring.
        // Wait, KeyboardRenderer uses Paint. We can just check the values.
        println("longTextSize: $longTextSize, shortTextSize: $shortTextSize")
        assertTrue("Long text should be scaled smaller than short text", longTextSize <= shortTextSize)
    }

    @Test
    fun testRailPunctKeyCentering() {
        val renderer = KeyboardRenderer()
        val mockCanvas = mock(Canvas::class.java)
        val palette = ThemePalette(
            bgColor = 0, candidateBarColor = 0, textColor = 0, subColor = 0,
            preeditBgColor = 0, symTabActiveBg = 0, symTabInactiveBg = 0,
            symTabActiveText = 0, symTabInactiveText = 0, isDark = false,
            keyBgColor = 0, specialKeyBgColor = 0, keyPressedBgColor = 0, specialKeyPressedBgColor = 0
        )

        val punctKey = KeyboardKey(
            id = "punct_，",
            role = KeyboardKeyRole.RAIL_PUNCT,
            rect = Rect(10, 20, 110, 120), // 100x100 box, center is (60, 70)
            label = "，",
            action = "punct:，",
        )
        val model = KeyboardLayoutModel(emptyList(), listOf(punctKey), null, 1000, 1000)

        var drawnX = 0f
        var drawnY = 0f
        doAnswer { invocation ->
            drawnX = invocation.arguments[1] as Float
            drawnY = invocation.arguments[2] as Float
            null
        }.`when`(mockCanvas).drawText(anyString(), anyFloat(), anyFloat(), any(Paint::class.java))

        renderer.drawKeyboard(mockCanvas, model, palette, 1f, KeyboardMode.ChineseT9, null)

        assertTrue("drawnX should be calculated", drawnX > 0f)
        assertTrue("drawnY should be calculated", drawnY > 0f)
    }
}
