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
            xiweiKeyboardView = mock(),
        )

        handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(eng) }

        controller = CandidateViewController(
            context = context,
            v = kv,
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
