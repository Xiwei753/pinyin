package io.github.xiwei753.pinyin.t9

import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.*

class KeyboardThemeControllerTest {

    private lateinit var mockRepo: SettingsRepository
    private lateinit var mockResources: Resources
    private lateinit var controller: KeyboardThemeController
    private lateinit var mockConfig: Configuration

    @Before
    fun setUp() {
        mockRepo = mock(SettingsRepository::class.java)
        mockResources = mock(Resources::class.java)
        mockConfig = Configuration()
        `when`(mockResources.configuration).thenReturn(mockConfig)
        val metrics = DisplayMetrics().apply { density = 1.0f }
        `when`(mockResources.displayMetrics).thenReturn(metrics)
        controller = KeyboardThemeController(mockRepo, mockResources)
    }

    @Test
    fun testLightThemePalette() {
        `when`(mockRepo.getTheme()).thenReturn("light")
        val palette = controller.getThemePalette()

        assertFalse("Light theme should not be dark", palette.isDark)
        assertEquals(ThemeColors.LIGHT_BG, palette.bgColor)
        assertEquals(ThemeColors.LIGHT_CANDIDATE_BAR, palette.candidateBarColor)
        assertEquals(ThemeColors.LIGHT_TEXT, palette.textColor)
        assertEquals(ThemeColors.LIGHT_SUB, palette.subColor)
        assertEquals(ThemeColors.LIGHT_PREEDIT_BG, palette.preeditBgColor)
    }

    @Test
    fun testDarkThemePalette() {
        `when`(mockRepo.getTheme()).thenReturn("dark")
        val palette = controller.getThemePalette()

        assertTrue("Dark theme should be dark", palette.isDark)
        assertEquals(ThemeColors.DARK_BG, palette.bgColor)
        assertEquals(ThemeColors.DARK_CANDIDATE_BAR, palette.candidateBarColor)
        assertEquals(ThemeColors.DARK_TEXT, palette.textColor)
        assertEquals(ThemeColors.DARK_SUB, palette.subColor)
        assertEquals(ThemeColors.DARK_PREEDIT_BG, palette.preeditBgColor)
    }

    @Test
    fun testSystemThemeNight() {
        `when`(mockRepo.getTheme()).thenReturn("system")
        mockConfig.uiMode = Configuration.UI_MODE_NIGHT_YES

        val palette = controller.getThemePalette()

        assertTrue("System theme with night mode should be dark", palette.isDark)
    }

    @Test
    fun testSystemThemeDay() {
        `when`(mockRepo.getTheme()).thenReturn("system")
        mockConfig.uiMode = Configuration.UI_MODE_NIGHT_NO

        val palette = controller.getThemePalette()

        assertFalse("System theme with day mode should be light", palette.isDark)
    }

    @Test
    fun testGetThemePaletteDoesNotCrash() {
        `when`(mockRepo.getTheme()).thenReturn("light")
        val palette = controller.getThemePalette()
        assertNotNull("Theme palette should not be null", palette)
    }

    @Test
    fun testApplySymbolTabColorsPreservesRoundedCorners() {
        `when`(mockRepo.getTheme()).thenReturn("light")
        val palette = controller.getThemePalette()
        val mockViews = mock(KeyboardViews::class.java)
        val punctTab = mock(TextView::class.java)
        val mathTab = mock(TextView::class.java)
        val bracketTab = mock(TextView::class.java)
        val otherTab = mock(TextView::class.java)

        `when`(mockViews.symTabPunct).thenReturn(punctTab)
        `when`(mockViews.symTabMath).thenReturn(mathTab)
        `when`(mockViews.symTabBracket).thenReturn(bracketTab)
        `when`(mockViews.symTabOther).thenReturn(otherTab)

        controller.applySymbolTabColors(mockViews, palette, "punct")

        // Must use setBackgroundResource (preserves rounded corners) instead of setBackgroundColor
        verify(punctTab).setBackgroundResource(io.github.xiwei753.pinyin.t9.R.drawable.key_bg)
        verify(mathTab).setBackgroundResource(io.github.xiwei753.pinyin.t9.R.drawable.key_bg_special)
        verify(bracketTab).setBackgroundResource(io.github.xiwei753.pinyin.t9.R.drawable.key_bg_special)
        verify(otherTab).setBackgroundResource(io.github.xiwei753.pinyin.t9.R.drawable.key_bg_special)
        // Must NOT call setBackgroundColor
        verify(punctTab, never()).setBackgroundColor(anyInt())
        // Must set text colors
        verify(punctTab).setTextColor(palette.symTabActiveText)
        verify(mathTab).setTextColor(palette.symTabInactiveText)
    }

    @Test
    fun testApplySymbolTabColorsActiveInactiveCorrect() {
        `when`(mockRepo.getTheme()).thenReturn("light")
        val palette = controller.getThemePalette()
        val mockViews = mock(KeyboardViews::class.java)
        val punctTab = mock(TextView::class.java)
        val mathTab = mock(TextView::class.java)
        val bracketTab = mock(TextView::class.java)
        val otherTab = mock(TextView::class.java)

        `when`(mockViews.symTabPunct).thenReturn(punctTab)
        `when`(mockViews.symTabMath).thenReturn(mathTab)
        `when`(mockViews.symTabBracket).thenReturn(bracketTab)
        `when`(mockViews.symTabOther).thenReturn(otherTab)

        controller.applySymbolTabColors(mockViews, palette, "punct")
        verify(punctTab).setTextColor(palette.symTabActiveText)
        verify(mathTab).setTextColor(palette.symTabInactiveText)
        verify(bracketTab).setTextColor(palette.symTabInactiveText)
        verify(otherTab).setTextColor(palette.symTabInactiveText)

        controller.applySymbolTabColors(mockViews, palette, "math")
        verify(punctTab).setTextColor(palette.symTabInactiveText)
        verify(mathTab).setTextColor(palette.symTabActiveText)
    }

    @Test
    fun testApplySymbolTabColorsMultipleSwitchesKeepRoundedCorners() {
        `when`(mockRepo.getTheme()).thenReturn("light")
        val palette = controller.getThemePalette()
        val mockViews = mock(KeyboardViews::class.java)
        val tabs = listOf(
            mock(TextView::class.java),
            mock(TextView::class.java),
            mock(TextView::class.java),
            mock(TextView::class.java),
        )
        `when`(mockViews.symTabPunct).thenReturn(tabs[0])
        `when`(mockViews.symTabMath).thenReturn(tabs[1])
        `when`(mockViews.symTabBracket).thenReturn(tabs[2])
        `when`(mockViews.symTabOther).thenReturn(tabs[3])

        for (cat in listOf("punct", "math", "bracket", "other", "punct", "math")) {
            controller.applySymbolTabColors(mockViews, palette, cat)
        }

        // After multiple switches, setBackgroundResource was used (not setBackgroundColor)
        for (tab in tabs) {
            verify(tab, never()).setBackgroundColor(anyInt())
        }
        tabs.forEachIndexed { i, tab ->
            verify(tab, atLeast(1)).setBackgroundResource(anyInt())
        }
    }
}
