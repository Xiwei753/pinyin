package io.github.xiwei753.pinyin.t9

import android.view.MotionEvent
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentCaptor

class KeyboardKeyBinderTest {

    private lateinit var haptic: HapticFeedbackManager
    private lateinit var binder: KeyboardKeyBinder
    private lateinit var views: KeyboardViews

    @Before
    fun setUp() {
        haptic = mock(HapticFeedbackManager::class.java)
        views = createMinimalMockKeyboardViews()
        binder = KeyboardKeyBinder(
            v = views,
            hapticFeedbackManager = haptic,
            panelController = mock(KeyboardPanelController::class.java),
            deleteRepeatController = mock(DeleteRepeatController::class.java),
            onModeChanged = {},
        )
    }

    private fun createMinimalMockKeyboardViews(): KeyboardViews {
        return KeyboardViews(
            imeRoot = mock(), candidateBar = mock(), candidateContainer = mock(),
            pinyinFloatingBar = mock(), pinyinFloatingText = mock(),
            keyboardShell = mock(),
            panelT9 = mock(), panelSymbol = mock(), panelNumber = mock(),
            readingTextViews = listOf(mock(), mock(), mock(), mock()),
            punctTextViews = listOf(mock(), mock(), mock(), mock()),
            symPagePunct = mock(), symPageMath = mock(), symPageBracket = mock(), symPageOther = mock(),
            symScrollContent = mock(),
            leftScrollRail = mock(), leftScrollContent = mock(),
            key1Text = mock(),
            key2 = mock(), key3 = mock(), key4 = mock(), key5 = mock(),
            key6 = mock(), key7 = mock(), key8 = mock(), key9 = mock(),
            key2Number = mock(), key3Number = mock(), key4Number = mock(), key5Number = mock(),
            key6Number = mock(), key7Number = mock(), key8Number = mock(), key9Number = mock(),
            key2Letters = mock(), key3Letters = mock(), key4Letters = mock(), key5Letters = mock(),
            key6Letters = mock(), key7Letters = mock(), key8Letters = mock(), key9Letters = mock(),
            keyDel = mock(), keyRetype = mock(), keyEnter = mock(), keySpace = mock(),
            keyToggleSymbol = mock(), keyToggleNumber = mock(),
            keyToggleEnglish = mock(),
            enterContainer = mock(),
            symTabPunct = mock(), symTabMath = mock(), symTabBracket = mock(), symTabOther = mock(),
            symCategoryTabs = mock(),
            generatedSymbolViews = mutableListOf(),
                        num0 = mock(), num1 = mock(), num2 = mock(), num3 = mock(), num4 = mock(),
            num5 = mock(), num6 = mock(), num7 = mock(), num8 = mock(), num9 = mock(), numDot = mock(),


                numKey1Frame = mock(),
                numKey2Frame = mock(),
                numKey3Frame = mock(),
                numKey4Frame = mock(),
                numKey5Frame = mock(),
                numKey6Frame = mock(),
                numKey7Frame = mock(),
                numKey8Frame = mock(),
                numKey9Frame = mock(),
                numDotFrame = mock(),
                num0Frame = mock(),
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
    }

    @Test
    fun testSetupKeyTapHapticOnActionDown() {
        val mockView = mock(View::class.java)
        binder.setupKey(mockView, isSpecial = false) { }

        val downEvent = mock(MotionEvent::class.java)
        `when`(downEvent.action).thenReturn(MotionEvent.ACTION_DOWN)

        val captor = ArgumentCaptor.forClass(View.OnTouchListener::class.java)
        verify(mockView).setOnTouchListener(captor.capture())
        captor.value.onTouch(mockView, downEvent)

        verify(haptic).performTap(mockView)
    }

    @Test
    fun testSetupKeySpecialHapticOnActionDown() {
        val mockView = mock(View::class.java)
        binder.setupKey(mockView, isSpecial = true) { }

        val downEvent = mock(MotionEvent::class.java)
        `when`(downEvent.action).thenReturn(MotionEvent.ACTION_DOWN)

        val captor = ArgumentCaptor.forClass(View.OnTouchListener::class.java)
        verify(mockView).setOnTouchListener(captor.capture())
        captor.value.onTouch(mockView, downEvent)

        verify(haptic).performSpecialKey(mockView)
    }

    @Test
    fun testSetupKeyClickTriggersAction() {
        val mockView = mock(View::class.java)
        val actions = mutableListOf<String>()
        binder.setupKey(mockView, isSpecial = false) { actions.add("clicked") }

        val clickCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)
        verify(mockView).setOnClickListener(clickCaptor.capture())
        clickCaptor.value.onClick(mockView)

        assertEquals(1, actions.size)
        assertEquals("clicked", actions[0])
    }

    @Test
    fun testSetupKeyDoesNotTriggerHapticOnActionUp() {
        val mockView = mock(View::class.java)
        binder.setupKey(mockView, isSpecial = false) { }

        val upEvent = mock(MotionEvent::class.java)
        `when`(upEvent.action).thenReturn(MotionEvent.ACTION_UP)

        val captor = ArgumentCaptor.forClass(View.OnTouchListener::class.java)
        verify(mockView).setOnTouchListener(captor.capture())
        captor.value.onTouch(mockView, upEvent)

        verifyNoInteractions(haptic)
    }

    @Test
    fun testNullViewDoesNotCrash() {
        binder.setupKey(null, isSpecial = false) { }
    }

    @Test
    fun testEnterKeyShortPressTriggersTapHaptic() {
        val mockHandler = mock(KeyboardActionHandler::class.java)
        binder.setupAllKeys(mockHandler)

        val clickCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)
        verify(views.keyEnter).setOnClickListener(clickCaptor.capture())
        clickCaptor.value.onClick(views.keyEnter)

        verify(haptic).performTap(views.keyEnter)
        verify(mockHandler).onEnterShortPress()
    }

    @Test
    fun testEnterKeyLongPressTriggersLongPressHaptic() {
        val mockHandler = mock(KeyboardActionHandler::class.java)
        binder.setupAllKeys(mockHandler)

        val longClickCaptor = ArgumentCaptor.forClass(View.OnLongClickListener::class.java)
        verify(views.keyEnter).setOnLongClickListener(longClickCaptor.capture())
        val result = longClickCaptor.value.onLongClick(views.keyEnter)

        assertTrue("Long click should return true to consume event", result)
        verify(haptic).performLongPress(views.keyEnter)
        verify(mockHandler).onEnterLongPress()
    }

    @Test
    fun testEnterKeyNoActionDownTouchListener() {
        val mockHandler = mock(KeyboardActionHandler::class.java)
        binder.setupAllKeys(mockHandler)

        // Should NOT set an onTouchListener on enter key
        verify(views.keyEnter, never()).setOnTouchListener(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun testEnterKeyShortPressDoesNotTriggerSpecialKeyHaptic() {
        val mockHandler = mock(KeyboardActionHandler::class.java)
        binder.setupAllKeys(mockHandler)

        val clickCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)
        verify(views.keyEnter).setOnClickListener(clickCaptor.capture())
        clickCaptor.value.onClick(views.keyEnter)

        verify(haptic, never()).performSpecialKey(views.keyEnter)
    }

    @Test
    fun testEnterKeyLongPressDoesNotTriggerSpecialKeyHaptic() {
        val mockHandler = mock(KeyboardActionHandler::class.java)
        binder.setupAllKeys(mockHandler)

        val longClickCaptor = ArgumentCaptor.forClass(View.OnLongClickListener::class.java)
        verify(views.keyEnter).setOnLongClickListener(longClickCaptor.capture())
        longClickCaptor.value.onLongClick(views.keyEnter)

        verify(haptic, never()).performSpecialKey(views.keyEnter)
    }
}
