package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class HapticFeedbackManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var mockView: View
    private lateinit var manager: HapticFeedbackManager

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockSettingsRepository = mock(SettingsRepository::class.java)
        mockView = mock(View::class.java)

        // Default to enabled
        `when`(mockSettingsRepository.isHapticFeedbackEnabled()).thenReturn(true)

        manager = HapticFeedbackManager(mockContext, mockSettingsRepository)
    }

    @Test
    fun testPerformTap_NoExceptionWhenSecurityExceptionThrown() {
        // Mock view to throw SecurityException
        `when`(mockView.performHapticFeedback(anyInt())).thenThrow(SecurityException("Permission denied"))

        // Should not throw
        manager.performTap(mockView)

        // Verify it was called
        verify(mockView).performHapticFeedback(anyInt())
    }

    @Test
    fun testPerformTap_NoExceptionWhenRuntimeExceptionThrown() {
        // Mock view to throw generic RuntimeException
        `when`(mockView.performHapticFeedback(anyInt())).thenThrow(RuntimeException("System service dead"))

        // Should not throw
        manager.performTap(mockView)

        // Verify it was called
        verify(mockView).performHapticFeedback(anyInt())
    }

    @Test
    fun testPerformSpecialKey_NoExceptionThrown() {
        `when`(mockView.performHapticFeedback(anyInt())).thenThrow(SecurityException("Permission denied"))

        // Should not throw
        manager.performSpecialKey(mockView)
    }

    @Test
    fun testPerformLongPress_NoExceptionThrown() {
        `when`(mockView.performHapticFeedback(anyInt())).thenThrow(SecurityException("Permission denied"))

        // Should not throw
        manager.performLongPress(mockView)
    }

    @Test
    fun testHapticFeedbackDisabled_DoesNotCallView() {
        `when`(mockSettingsRepository.isHapticFeedbackEnabled()).thenReturn(false)

        manager.performTap(mockView)
        manager.performSpecialKey(mockView)
        manager.performLongPress(mockView)

        // Verify view is never touched when disabled
        verify(mockView, never()).performHapticFeedback(anyInt())
    }
}
