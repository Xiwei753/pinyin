package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyInt
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9Engine

@RunWith(RobolectricTestRunner::class)
class CandidateViewControllerTest {

    private lateinit var context: Context
    private lateinit var kv: KeyboardViews
    private lateinit var themeController: KeyboardThemeController
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var controller: CandidateViewController
    private lateinit var handler: KeyboardActionHandler
    private lateinit var engine: T9Engine
    private lateinit var mockFloatingBar: View
    private lateinit var mockFloatingText: TextView
    private lateinit var candidateContainer: LinearLayout

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockFloatingBar = View(context)
        mockFloatingText = TextView(context)
        candidateContainer = LinearLayout(context)
        themeController = mock(KeyboardThemeController::class.java)
        settingsRepository = mock(SettingsRepository::class.java)
        val eng = mock(T9Engine::class.java)
        engine = eng

        kv = KeyboardViews(
            imeRoot = mock(),
            candidateBar = mock(),
            candidateContainer = candidateContainer,
            pinyinFloatingBar = mockFloatingBar,
            pinyinFloatingText = mockFloatingText,
            xiweiKeyboardView = mock(),
        )

        handler = spy(KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(eng) })

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

        assertEquals(View.GONE, mockFloatingBar.visibility)
    }

    @Test
    fun testPreeditShownWhenBufferNotEmpty() {
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        assertEquals(View.VISIBLE, mockFloatingBar.visibility)
        assertEquals("wo", mockFloatingText.text.toString())
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
    fun testCandidatesLayoutRendering() {
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(settingsRepository.getCandidateCount()).thenReturn(30)
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(
            listOf(Candidate("我", "96", 900), Candidate("你", "96", 500))
        )
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        // Only candidates should be in the container, NOT preedit or reading
        assertEquals(2, candidateContainer.childCount)
        val tv1 = candidateContainer.getChildAt(0) as TextView
        val tv2 = candidateContainer.getChildAt(1) as TextView

        assertEquals("我", tv1.text.toString())
        assertEquals("你", tv2.text.toString())
    }

    @Test
    fun testReadingsAreNOTInCandidateBar() {
        `when`(engine.buffer).thenReturn("64")
        `when`(engine.getPreedit()).thenReturn("ni")
        `when`(engine.readings).thenReturn(listOf("ni", "mi"))
        `when`(engine.activeReading).thenReturn("ni")
        `when`(settingsRepository.getCandidateCount()).thenReturn(30)
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(
            listOf(Candidate("你", "64", 900))
        )
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        // Only Chinese candidates should be here
        assertEquals(1, candidateContainer.childCount)
        
        val tvCandidate = candidateContainer.getChildAt(0) as TextView
        assertEquals("你", tvCandidate.text.toString())
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

        controller.refreshUi(handler)

        assertEquals(1, candidateContainer.childCount)
        val tvCandidate = candidateContainer.getChildAt(0) as TextView
        assertEquals("我", tvCandidate.text.toString())

        tvCandidate.performClick()

        verify(handler).onCandidateClick(0)
    }

    @Test
    fun testPreeditIsNotClickableAndDoesNotCallHandler() {
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(settingsRepository.getCandidateCount()).thenReturn(30)
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(
            listOf(Candidate("我", "96", 900))
        )
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        assertEquals(View.VISIBLE, mockFloatingBar.visibility)

        mockFloatingBar.performClick()
        mockFloatingText.performClick()

        verify(handler, never()).onCandidateClick(anyInt())
    }

    @Test
    fun testPreeditDisplayDoesNotShiftCandidateIndex() {
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(settingsRepository.getCandidateCount()).thenReturn(30)
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(
            listOf(Candidate("我", "96", 900), Candidate("喔", "96", 800))
        )
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        controller.refreshUi(handler)

        assertEquals(View.VISIBLE, mockFloatingBar.visibility)
        assertEquals(2, candidateContainer.childCount)

        val tvCandidate0 = candidateContainer.getChildAt(0) as TextView
        val tvCandidate1 = candidateContainer.getChildAt(1) as TextView

        assertEquals("我", tvCandidate0.text.toString())
        assertEquals("喔", tvCandidate1.text.toString())

        tvCandidate0.performClick()
        verify(handler).onCandidateClick(0)

        tvCandidate1.performClick()
        verify(handler).onCandidateClick(1)
    }

    @Test
    fun testResetUi() {
        controller.resetUi()

        assertEquals(View.GONE, mockFloatingBar.visibility)
        assertEquals(0, candidateContainer.childCount)
        assertEquals(View.GONE, candidateContainer.visibility)
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

        assertEquals(View.GONE, mockFloatingBar.visibility)
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

        assertEquals(View.GONE, mockFloatingBar.visibility)
    }
}
