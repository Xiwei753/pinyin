package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.imecore.CandidateStripState
import io.github.xiwei753.pinyin.imecore.ImeInputAction
import io.github.xiwei753.pinyin.imecore.PreeditState
import io.github.xiwei753.pinyin.t9.core.Candidate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CandidateViewControllerTest {

    private lateinit var context: Context
    private lateinit var controller: CandidateViewController
    private lateinit var mockFloatingBar: View
    private lateinit var mockFloatingText: TextView
    private lateinit var candidateContainer: LinearLayout

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockFloatingBar = View(context)
        mockFloatingText = TextView(context)
        candidateContainer = LinearLayout(context)

        val kv = KeyboardViews(
            imeRoot = mock(),
            candidateBar = mock(),
            candidateContainer = candidateContainer,
            pinyinFloatingBar = mockFloatingBar,
            pinyinFloatingText = mockFloatingText,
            xiweiKeyboardView = mock(),
        )

        controller = CandidateViewController(
            context = context,
            v = kv,
            themeController = mock(KeyboardThemeController::class.java),
            settingsRepository = mock(SettingsRepository::class.java),
        )
    }

    @Test
    fun testPreeditHiddenWhenStateHidden() {
        controller.refreshFromState(state(preeditVisible = false, preedit = ""))

        assertEquals(View.GONE, mockFloatingBar.visibility)
    }

    @Test
    fun testPreeditShownFromState() {
        controller.refreshFromState(state(preeditVisible = true, preedit = "wo"))

        assertEquals(View.VISIBLE, mockFloatingBar.visibility)
        assertEquals("wo", mockFloatingText.text.toString())
    }

    @Test
    fun testCandidatesLayoutRendering() {
        controller.refreshFromState(state(candidates = listOf(Candidate("我", "96", 900), Candidate("你", "96", 500))))

        assertEquals(2, candidateContainer.childCount)
        val tv1 = candidateContainer.getChildAt(0) as TextView
        val tv2 = candidateContainer.getChildAt(1) as TextView

        assertEquals("我", tv1.text.toString())
        assertEquals("你", tv2.text.toString())
    }

    @Test
    fun testReadingsAreNOTInCandidateBar() {
        controller.refreshFromState(state(candidates = listOf(Candidate("你", "64", 900)), readings = listOf("ni", "mi")))

        assertEquals(1, candidateContainer.childCount)
        val tvCandidate = candidateContainer.getChildAt(0) as TextView
        assertEquals("你", tvCandidate.text.toString())
    }

    @Test
    fun testCandidateClickEmitsInputActionIndex() {
        var clickedAction: ImeInputAction? = null
        controller.onInputAction = { clickedAction = it }
        controller.refreshFromState(state(candidates = listOf(Candidate("我", "96", 900))))

        val tvCandidate = candidateContainer.getChildAt(0) as TextView
        tvCandidate.performClick()

        assertEquals(ImeInputAction.CandidateSelected(0), clickedAction)
    }

    @Test
    fun testPreeditIsNotClickableAndDoesNotEmitCandidateAction() {
        var clickedAction: ImeInputAction? = null
        controller.onInputAction = { clickedAction = it }
        controller.refreshFromState(state(preeditVisible = true, preedit = "wo", candidates = listOf(Candidate("我", "96", 900))))

        mockFloatingBar.performClick()
        mockFloatingText.performClick()

        assertEquals(null, clickedAction)
    }

    @Test
    fun testPreeditDisplayDoesNotShiftCandidateIndex() {
        var clickedAction: ImeInputAction? = null
        controller.onInputAction = { clickedAction = it }
        controller.refreshFromState(
            state(
                preeditVisible = true,
                preedit = "wo",
                candidates = listOf(Candidate("我", "96", 900), Candidate("喔", "96", 800)),
            )
        )

        assertEquals(View.VISIBLE, mockFloatingBar.visibility)
        assertEquals(2, candidateContainer.childCount)

        val tvCandidate0 = candidateContainer.getChildAt(0) as TextView
        val tvCandidate1 = candidateContainer.getChildAt(1) as TextView

        tvCandidate0.performClick()
        assertEquals(ImeInputAction.CandidateSelected(0), clickedAction)

        tvCandidate1.performClick()
        assertEquals(ImeInputAction.CandidateSelected(1), clickedAction)
    }

    @Test
    fun testRefreshFromStateHasNoHandlerDependency() {
        controller.refreshFromState(state(candidates = listOf(Candidate("我", "96", 900))))

        val renderMethod = CandidateViewController::class.java.methods.single { it.name == "refreshFromState" }
        assertEquals(1, renderMethod.parameterTypes.size)
        assertEquals(KeyboardUiState::class.java, renderMethod.parameterTypes[0])
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
        controller.refreshFromState(state(mode = KeyboardMode.Symbol, preeditVisible = false, preedit = "wo"))

        assertEquals(View.GONE, mockFloatingBar.visibility)
    }

    @Test
    fun testPreeditHiddenOnNumberMode() {
        controller.refreshFromState(state(mode = KeyboardMode.Number, preeditVisible = false, preedit = ""))

        assertEquals(View.GONE, mockFloatingBar.visibility)
    }

    private fun state(
        mode: KeyboardMode = KeyboardMode.ChineseT9,
        preeditVisible: Boolean = false,
        preedit: String = "",
        readings: List<String> = emptyList(),
        candidates: List<Candidate> = emptyList(),
    ): KeyboardUiState = KeyboardUiState(
        keyboardMode = mode,
        lastTextMode = KeyboardMode.ChineseT9,
        rawBuffer = if (preedit.isNotEmpty()) "96" else "",
        preedit = preedit,
        readings = readings,
        activeReading = readings.firstOrNull(),
        candidatesSnapshot = candidates,
        currentSymCategory = "punct",
        isComposing = preedit.isNotEmpty(),
        themePalette = ThemePalette(
            bgColor = 0,
            candidateBarColor = 0,
            textColor = 0,
            subColor = 0,
            preeditBgColor = 0,
            symTabActiveBg = 0,
            symTabInactiveBg = 0,
            symTabActiveText = 0,
            symTabInactiveText = 0,
            isDark = false,
            keyBgColor = 0,
            specialKeyBgColor = 0,
            keyPressedBgColor = 0,
            specialKeyPressedBgColor = 0,
        ),
        candidateStripState = CandidateStripState(candidates.isNotEmpty(), candidates),
        preeditState = PreeditState(preeditVisible, preedit),
    )
}
