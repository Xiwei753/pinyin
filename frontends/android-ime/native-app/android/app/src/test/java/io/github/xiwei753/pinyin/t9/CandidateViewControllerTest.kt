package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyInt
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9Engine

class CandidateViewControllerTest {

    private lateinit var context: Context
    private lateinit var kv: KeyboardViews
    private lateinit var keyBinder: KeyboardKeyBinder
    private lateinit var themeController: KeyboardThemeController
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var controller: CandidateViewController
    private lateinit var handler: KeyboardActionHandler
    private lateinit var engine: T9Engine
    private lateinit var mockFloatingBar: View
    private lateinit var mockFloatingText: android.widget.TextView
    private lateinit var mockCandidateContainer: LinearLayout

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        mockFloatingBar = mock(View::class.java)
        mockFloatingText = mock(android.widget.TextView::class.java)
        mockCandidateContainer = mock(LinearLayout::class.java)
        keyBinder = mock(KeyboardKeyBinder::class.java)
        themeController = mock(KeyboardThemeController::class.java)
        settingsRepository = mock(SettingsRepository::class.java)
        val eng = mock(T9Engine::class.java)
        engine = eng

        kv = KeyboardViews(
            imeRoot = mock(),
            candidateBar = mock(),
            candidateContainer = mockCandidateContainer,
            pinyinFloatingBar = mockFloatingBar,
            pinyinFloatingText = mockFloatingText,
            keyboardShell = mock(),
            panelT9 = mock(),
            panelSymbol = mock(),
            panelNumber = mock(),
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
            keyToggleEnglish = mock(),
            enterContainer = mock(),
            symTabPunct = mock(), symTabMath = mock(), symTabBracket = mock(), symTabOther = mock(),
            symTextViews = emptyMap(),
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

        handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(eng) }

        controller = CandidateViewController(
            context = context,
            v = kv,
            keyBinder = keyBinder,
            themeController = themeController,
            settingsRepository = settingsRepository,
        )
    }

    @Test
    fun testPreeditHiddenWhenBufferEmpty() {
        `when`(engine.buffer).thenReturn("")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        verify(mockFloatingBar).visibility = View.GONE
    }

    @Test
    fun testPreeditShownWhenBufferNotEmpty() {
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        verify(mockFloatingBar).visibility = View.VISIBLE
        verify(mockFloatingText).text = "wo"
    }

    @Test
    fun testCandidateCountPassesToEngine() {
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(settingsRepository.getCandidateCount()).thenReturn(15)
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        verify(engine).getVisibleCandidates(15)
    }

    @Test
    fun testCandidatesReuseTextViews() {
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(settingsRepository.getCandidateCount()).thenReturn(30)
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(
            listOf(Candidate("我", "96", 900), Candidate("你", "96", 500))
        )
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())
        `when`(mockCandidateContainer.childCount).thenReturn(2)
        `when`(mockCandidateContainer.getChildAt(0)).thenReturn(mock(android.widget.TextView::class.java))
        `when`(mockCandidateContainer.getChildAt(1)).thenReturn(mock(android.widget.TextView::class.java))

        controller.refreshUi(handler)

        // Views are reused when childCount matches candidate count
        verify(mockCandidateContainer, never()).addView(
            any(android.view.View::class.java),
            any(android.view.ViewGroup.LayoutParams::class.java)
        )
    }

    @Test
    fun testCandidateClickCallsHandler() {
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(settingsRepository.getCandidateCount()).thenReturn(30)
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(
            listOf(Candidate("我", "96", 900))
        )
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())
        `when`(mockCandidateContainer.childCount).thenReturn(1)
        `when`(mockCandidateContainer.getChildAt(0)).thenReturn(mock(android.widget.TextView::class.java))

        controller.refreshUi(handler)

        // setupKey was called - we can verify this indirectly
        // by checking the child visibility was set to VISIBLE
        verify(mockCandidateContainer.getChildAt(0)).visibility = View.VISIBLE
    }

    @Test
    fun testResetUi() {
        controller.resetUi()

        verify(mockFloatingBar).visibility = View.GONE
        verify(mockCandidateContainer).removeAllViews()
    }

    @Test
    fun testPreeditHiddenOnSymbolMode() {
        handler.switchKeyboardMode(KeyboardMode.Symbol)
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        verify(mockFloatingBar).visibility = View.GONE
    }

    @Test
    fun testPreeditHiddenOnNumberMode() {
        handler.switchKeyboardMode(KeyboardMode.Number)
        `when`(engine.buffer).thenReturn("")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        verify(mockFloatingBar).visibility = View.GONE
    }
}
