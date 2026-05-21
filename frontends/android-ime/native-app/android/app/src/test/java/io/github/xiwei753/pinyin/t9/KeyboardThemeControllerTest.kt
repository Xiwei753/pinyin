package io.github.xiwei753.pinyin.t9

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
    fun darkThemeNormalKeyBgIsNotWhite() {
        `when`(mockRepo.getTheme()).thenReturn("dark")
        val palette = controller.getThemePalette()
        val white = 0xFFFFFFFF.toInt()
        assertNotEquals("Dark mode normal key bg should not be pure white", white, palette.keyBgColor)
        assertTrue("Dark mode normal key bg should be dark", palette.keyBgColor and 0x00FFFFFF < 0x00808080)
    }

    @Test
    fun darkThemeSpecialKeyBgIsNotLightGray() {
        `when`(mockRepo.getTheme()).thenReturn("dark")
        val palette = controller.getThemePalette()
        val lightGray = 0xFFEBEBEB.toInt()
        assertNotEquals("Dark mode special key bg should not be light gray", lightGray, palette.specialKeyBgColor)
    }

    @Test
    fun darkThemeKeyBgContrastWithText() {
        `when`(mockRepo.getTheme()).thenReturn("dark")
        val palette = controller.getThemePalette()
        val contrast = kotlin.math.abs(palette.textColor - palette.keyBgColor)
        assertTrue("Dark mode text should be clearly visible on key bg (contrast > 0x808080)",
            contrast > 0x808080)
    }

    @Test
    fun lightThemeKeyBgIsWhite() {
        `when`(mockRepo.getTheme()).thenReturn("light")
        val palette = controller.getThemePalette()
        assertEquals("Light mode normal key bg should be white", 0xFFFFFFFF.toInt(), palette.keyBgColor)
    }

    @Test
    fun lightThemeSpecialKeyBgIsLightGray() {
        `when`(mockRepo.getTheme()).thenReturn("light")
        val palette = controller.getThemePalette()
        assertEquals("Light mode special key bg should be light gray", 0xFFEBEBEB.toInt(), palette.specialKeyBgColor)
    }

    @Test
    fun darkThemeKeyBgColorsMatchExpected() {
        `when`(mockRepo.getTheme()).thenReturn("dark")
        val palette = controller.getThemePalette()
        assertEquals(ThemeColors.DARK_KEY_BG, palette.keyBgColor)
        assertEquals(ThemeColors.DARK_SPECIAL_KEY_BG, palette.specialKeyBgColor)
        assertEquals(ThemeColors.DARK_KEY_PRESSED, palette.keyPressedBgColor)
        assertEquals(ThemeColors.DARK_SPECIAL_KEY_PRESSED, palette.specialKeyPressedBgColor)
    }

    @Test
    fun lightThemeKeyBgColorsMatchExpected() {
        `when`(mockRepo.getTheme()).thenReturn("light")
        val palette = controller.getThemePalette()
        assertEquals(ThemeColors.LIGHT_KEY_BG, palette.keyBgColor)
        assertEquals(ThemeColors.LIGHT_SPECIAL_KEY_BG, palette.specialKeyBgColor)
        assertEquals(ThemeColors.LIGHT_KEY_PRESSED, palette.keyPressedBgColor)
        assertEquals(ThemeColors.LIGHT_SPECIAL_KEY_PRESSED, palette.specialKeyPressedBgColor)
    }

}
