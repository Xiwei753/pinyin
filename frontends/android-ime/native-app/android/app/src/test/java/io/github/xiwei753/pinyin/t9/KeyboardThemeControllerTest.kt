package io.github.xiwei753.pinyin.t9

import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
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
}
