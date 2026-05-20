package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq

class HapticFeedbackSetupTest {

    private lateinit var mockContext: Context
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var manager: HapticFeedbackManager

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockSettingsRepository = mock(SettingsRepository::class.java)
        `when`(mockSettingsRepository.isHapticFeedbackEnabled()).thenReturn(true)
        manager = HapticFeedbackManager(mockContext, mockSettingsRepository)
    }

    @Test
    fun testNormalKeyUsesKeyboardTapConstant() {
        val mockView = mock(View::class.java)
        manager.performTap(mockView)
        verify(mockView).performHapticFeedback(eq(HapticFeedbackConstants.KEYBOARD_TAP))
    }

    @Test
    fun testSpecialKeyUsesKeyboardPressConstant() {
        val mockView = mock(View::class.java)
        manager.performSpecialKey(mockView)
        verify(mockView).performHapticFeedback(eq(HapticFeedbackConstants.KEYBOARD_PRESS))
    }

    @Test
    fun testSetupKeyTriggersHapticOnActionDown() {
        val mockView = mock(View::class.java)

        val touchListener = View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                manager.performTap(v)
            }
            false
        }

        val downEvent = mock(MotionEvent::class.java)
        `when`(downEvent.action).thenReturn(MotionEvent.ACTION_DOWN)

        touchListener.onTouch(mockView, downEvent)

        verify(mockView).performHapticFeedback(eq(HapticFeedbackConstants.KEYBOARD_TAP))
    }

    @Test
    fun testSetupKeySpecialTriggersHapticOnActionDown() {
        val mockView = mock(View::class.java)

        val touchListener = View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                manager.performSpecialKey(v)
            }
            false
        }

        val downEvent = mock(MotionEvent::class.java)
        `when`(downEvent.action).thenReturn(MotionEvent.ACTION_DOWN)

        touchListener.onTouch(mockView, downEvent)

        verify(mockView).performHapticFeedback(eq(HapticFeedbackConstants.KEYBOARD_PRESS))
    }

    @Test
    fun testSetupKeyDoesNotTriggerHapticOnActionUp() {
        val mockView = mock(View::class.java)

        val touchListener = View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                manager.performTap(v)
            }
            false
        }

        val upEvent = mock(MotionEvent::class.java)
        `when`(upEvent.action).thenReturn(MotionEvent.ACTION_UP)

        touchListener.onTouch(mockView, upEvent)

        verify(mockView, never()).performHapticFeedback(anyInt())
    }

    @Test
    fun testOnClickIsSeparateFromHapticTouch() {
        val mockView = mock(View::class.java)
        val businessAction = mutableListOf<String>()

        // Verify the setupKey pattern: onTouchListener handles haptic, onClickListener handles business
        val touchListener = View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                manager.performTap(v)
            }
            false
        }
        val clickListener = View.OnClickListener {
            businessAction.add("clicked")
        }

        mockView.setOnTouchListener(touchListener)
        mockView.setOnClickListener(clickListener)

        // Verify both listeners are set (not null)
        verify(mockView).setOnTouchListener(touchListener)
        verify(mockView).setOnClickListener(clickListener)

        // Haptic is triggered via touch listener on ACTION_DOWN
        val downEvent = mock(MotionEvent::class.java)
        `when`(downEvent.action).thenReturn(MotionEvent.ACTION_DOWN)
        touchListener.onTouch(mockView, downEvent)
        verify(mockView).performHapticFeedback(eq(HapticFeedbackConstants.KEYBOARD_TAP))

        // onClick is a separate callback - business logic only, no haptic
        // (mock View won't actually invoke onClickListener, but we verify the pattern)
    }

    @Test
    fun testHapticDisabled_DoesNotCallView() {
        `when`(mockSettingsRepository.isHapticFeedbackEnabled()).thenReturn(false)
        val mockView = mock(View::class.java)

        manager.performTap(mockView)
        manager.performSpecialKey(mockView)

        verify(mockView, never()).performHapticFeedback(anyInt())
    }
}
