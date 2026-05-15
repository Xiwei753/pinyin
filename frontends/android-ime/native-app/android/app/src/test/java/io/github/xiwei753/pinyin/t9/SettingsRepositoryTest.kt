package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class SettingsRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        // Setup editor mocks for method chaining
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)

        repository = SettingsRepository(mockContext)
    }

    @Test
    fun testHapticFeedbackDefault() {
        `when`(mockPrefs.getBoolean("haptic_feedback_enabled", true)).thenReturn(true)
        assertTrue(repository.isHapticFeedbackEnabled())
    }

    @Test
    fun testHapticFeedbackSet() {
        repository.setHapticFeedbackEnabled(false)
        verify(mockEditor).putBoolean("haptic_feedback_enabled", false)
        verify(mockEditor).apply()
    }

    @Test
    fun testCandidateCountDefault() {
        `when`(mockPrefs.getInt("candidate_count", 30)).thenReturn(30)
        assertEquals(30, repository.getCandidateCount())
    }

    @Test
    fun testCandidateCountSet() {
        repository.setCandidateCount(15)
        verify(mockEditor).putInt("candidate_count", 15)
        verify(mockEditor).apply()
    }

    @Test
    fun testThemeDefault() {
        `when`(mockPrefs.getString("theme", "system")).thenReturn("system")
        assertEquals("system", repository.getTheme())
    }

    @Test
    fun testThemeSet() {
        repository.setTheme("dark")
        verify(mockEditor).putString("theme", "dark")
        verify(mockEditor).apply()
    }

    @Test
    fun testKeyboardHeightDefault() {
        `when`(mockPrefs.getString("keyboard_height", "normal")).thenReturn("normal")
        assertEquals("normal", repository.getKeyboardHeight())
    }

    @Test
    fun testKeyboardHeightSet() {
        repository.setKeyboardHeight("high")
        verify(mockEditor).putString("keyboard_height", "high")
        verify(mockEditor).apply()
    }
}
