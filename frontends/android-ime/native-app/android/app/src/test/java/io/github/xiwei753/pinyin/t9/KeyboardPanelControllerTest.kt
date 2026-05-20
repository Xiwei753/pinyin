package io.github.xiwei753.pinyin.t9

import android.view.View
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class KeyboardPanelControllerTest {

    private lateinit var kv: KeyboardViews
    private lateinit var controller: KeyboardPanelController
    private lateinit var mockPanelT9: View
    private lateinit var mockPanelSymbol: View
    private lateinit var mockPanelNumber: View
    private lateinit var mockFloatingBar: View
    private lateinit var mockToggleEnglish: TextView

    @Before
    fun setUp() {
        mockPanelT9 = mock(View::class.java)
        mockPanelSymbol = mock(View::class.java)
        mockPanelNumber = mock(View::class.java)
        mockFloatingBar = mock(View::class.java)
        mockToggleEnglish = mock(TextView::class.java)

        kv = KeyboardViews(
            imeRoot = mock(),
            candidateBar = mock(),
            candidateContainer = mock(),
            pinyinFloatingBar = mockFloatingBar,
            pinyinFloatingText = mock(),
            keyboardShell = mock(),
            panelT9 = mockPanelT9,
            panelSymbol = mockPanelSymbol,
            panelNumber = mockPanelNumber,
            readingTextViews = listOf(mock(), mock(), mock(), mock()),
            punctTextViews = listOf(mock(), mock(), mock(), mock()),
            symPagePunct = mock(),
            symPageMath = mock(),
            symPageBracket = mock(),
            symPageOther = mock(),
            symScrollContent = mock(),
            leftScrollRail = mock(),
            leftScrollContent = mock(),
            key1Text = mock(),
            key2 = mock(), key3 = mock(), key4 = mock(), key5 = mock(),
            key6 = mock(), key7 = mock(), key8 = mock(), key9 = mock(),
            key2Number = mock(), key3Number = mock(), key4Number = mock(), key5Number = mock(),
            key6Number = mock(), key7Number = mock(), key8Number = mock(), key9Number = mock(),
            key2Letters = mock(), key3Letters = mock(), key4Letters = mock(), key5Letters = mock(),
            key6Letters = mock(), key7Letters = mock(), key8Letters = mock(), key9Letters = mock(),
            keyDel = mock(), keyRetype = mock(), keyEnter = mock(), keySpace = mock(),
            keyToggleSymbol = mock(), keyToggleNumber = mock(),
            keyToggleEnglish = mockToggleEnglish,
            enterContainer = mock(),
            symTabPunct = mock(), symTabMath = mock(), symTabBracket = mock(), symTabOther = mock(),
            symTextViews = emptyMap(),
            symBack = mock(), symNumber = mock(), symDel = mock(), symEnter = mock(), symHide = mock(),
            num0 = mock(), num1 = mock(), num2 = mock(), num3 = mock(), num4 = mock(),
            num5 = mock(), num6 = mock(), num7 = mock(), num8 = mock(), num9 = mock(), numDot = mock(),
            numDel = mock(), numBack = mock(), numSymbol = mock(), numHide = mock(), numEnter = mock(),
            
            t9LeftColumn = mock(),
            t9LeftScrollFrame = mock(),
            t9SymbolButtonFrame = mock(),
            t9Key1Frame = mock(),
            t9Key2Frame = mock(),
            t9Key3Frame = mock(),
            t9Key4Frame = mock(),
            t9Key5Frame = mock(),
            t9Key6Frame = mock(),
            t9Key7Frame = mock(),
            t9Key8Frame = mock(),
            t9Key9Frame = mock(),
            t9DelFrame = mock(),
            t9RetypeFrame = mock(),
            t9NumberFrame = mock(),
            t9SpaceFrame = mock(),
            t9EnglishFrame = mock(),
        )
        controller = KeyboardPanelController(kv)
    }

    @Test
    fun testChineseT9ModePanelVisibility() {
        controller.updatePanel(KeyboardMode.ChineseT9)

        verify(mockPanelT9).visibility = View.VISIBLE
        verify(mockPanelSymbol).visibility = View.GONE
        verify(mockPanelNumber).visibility = View.GONE
    }

    @Test
    fun testEnglishT9ModePanelVisibility() {
        controller.updatePanel(KeyboardMode.EnglishT9)

        verify(mockPanelT9).visibility = View.VISIBLE
        verify(mockPanelSymbol).visibility = View.GONE
        verify(mockPanelNumber).visibility = View.GONE
    }

    @Test
    fun testSymbolModePanelVisibility() {
        controller.updatePanel(KeyboardMode.Symbol)

        verify(mockPanelT9).visibility = View.GONE
        verify(mockPanelSymbol).visibility = View.VISIBLE
        verify(mockPanelNumber).visibility = View.GONE
    }

    @Test
    fun testNumberModePanelVisibility() {
        controller.updatePanel(KeyboardMode.Number)

        verify(mockPanelT9).visibility = View.GONE
        verify(mockPanelSymbol).visibility = View.GONE
        verify(mockPanelNumber).visibility = View.VISIBLE
    }

    @Test
    fun testSymbolModeHidesPreedit() {
        controller.updatePanel(KeyboardMode.Symbol)

        verify(mockFloatingBar).visibility = View.GONE
    }

    @Test
    fun testNumberModeHidesPreedit() {
        controller.updatePanel(KeyboardMode.Number)

        verify(mockFloatingBar).visibility = View.GONE
    }

    @Test
    fun testToggleEnglishTextForChineseMode() {
        controller.updatePanel(KeyboardMode.ChineseT9)

        verify(mockToggleEnglish).text = "中/英"
    }

    @Test
    fun testToggleEnglishTextForEnglishMode() {
        controller.updatePanel(KeyboardMode.EnglishT9)

        verify(mockToggleEnglish).text = "英/中"
    }

    @Test
    fun testSymbolCategorySwitching() {
        val mockThemeCtrl = mock(KeyboardThemeController::class.java)
        val mockPalette = mock(ThemePalette::class.java)

        controller.setSymbolCategory("math", mockThemeCtrl, mockPalette)

        assertEquals("math", controller.currentSymCategory)
        verify(kv.symPagePunct).visibility = View.GONE
        verify(kv.symPageMath).visibility = View.VISIBLE
        verify(kv.symPageBracket).visibility = View.GONE
        verify(kv.symPageOther).visibility = View.GONE
    }
}
