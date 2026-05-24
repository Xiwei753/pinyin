package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.imecore.CandidateStripState
import io.github.xiwei753.pinyin.imecore.CandidateSnapshotItem
import io.github.xiwei753.pinyin.imecore.ImeInputAction
import io.github.xiwei753.pinyin.imecore.PreeditState
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
        controller.refreshFromState(state(preeditVisible = false, preedit = "", rawBuffer = "96"))

        assertEquals(View.GONE, mockFloatingBar.visibility)
        assertEquals(View.GONE, candidateContainer.visibility)
    }

    @Test
    fun testPreeditShownInCandidateContainer() {
        controller.refreshFromState(state(preeditVisible = true, preedit = "wo"))

        assertEquals(View.GONE, mockFloatingBar.visibility)
        assertEquals(View.VISIBLE, candidateContainer.visibility)
        assertEquals(1, candidateContainer.childCount)
        val chip = candidateContainer.getChildAt(0) as TextView
        assertEquals("wo", chip.text.toString())
    }

    @Test
    fun testCandidatesLayoutRendering() {
        controller.refreshFromState(state(candidates = listOf(candidate("我", "96", 900), candidate("你", "96", 500))))

        assertEquals(2, candidateContainer.childCount)
        val tv1 = candidateContainer.getChildAt(0) as TextView
        val tv2 = candidateContainer.getChildAt(1) as TextView

        assertEquals("我", tv1.text.toString())
        assertEquals("你", tv2.text.toString())
    }

    @Test
    fun testPreeditAsFirstChipBeforeCandidates() {
        controller.refreshFromState(
            state(
                preeditVisible = true,
                preedit = "wo",
                candidates = listOf(candidate("我", "96", 900), candidate("喔", "96", 800)),
            )
        )

        assertEquals(View.GONE, mockFloatingBar.visibility)
        assertEquals(View.VISIBLE, candidateContainer.visibility)
        assertEquals(3, candidateContainer.childCount)
        val preeditChip = candidateContainer.getChildAt(0) as TextView
        assertEquals("wo", preeditChip.text.toString())
        val tv1 = candidateContainer.getChildAt(1) as TextView
        assertEquals("我", tv1.text.toString())
        val tv2 = candidateContainer.getChildAt(2) as TextView
        assertEquals("喔", tv2.text.toString())
    }

    @Test
    fun testReadingsAreNOTInCandidateBar() {
        controller.refreshFromState(state(candidates = listOf(candidate("你", "64", 900)), readings = listOf("ni", "mi")))

        assertEquals(1, candidateContainer.childCount)
        val tvCandidate = candidateContainer.getChildAt(0) as TextView
        assertEquals("你", tvCandidate.text.toString())
    }

    @Test
    fun testCandidateClickEmitsInputActionIndex() {
        var clickedAction: ImeInputAction? = null
        controller.onInputAction = { clickedAction = it }
        controller.refreshFromState(state(candidates = listOf(candidate("我", "96", 900))))

        val tvCandidate = candidateContainer.getChildAt(0) as TextView
        tvCandidate.performClick()

        assertEquals(ImeInputAction.CandidateSelected(0), clickedAction)
    }

    @Test
    fun testPreeditChipIsNotClickableAndDoesNotEmitCandidateAction() {
        var clickedAction: ImeInputAction? = null
        controller.onInputAction = { clickedAction = it }
        controller.refreshFromState(
            state(
                preeditVisible = true,
                preedit = "wo",
                candidates = listOf(candidate("我", "96", 900)),
            )
        )

        val preeditChip = candidateContainer.getChildAt(0) as TextView
        preeditChip.performClick()

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
                candidates = listOf(candidate("我", "96", 900), candidate("喔", "96", 800)),
            )
        )

        assertEquals("preedit chip at index 0", "wo", (candidateContainer.getChildAt(0) as TextView).text)
        assertEquals("first candidate at index 1", "我", (candidateContainer.getChildAt(1) as TextView).text)
        assertEquals("second candidate at index 2", "喔", (candidateContainer.getChildAt(2) as TextView).text)

        val tvCandidate0 = candidateContainer.getChildAt(1) as TextView
        tvCandidate0.performClick()
        assertEquals(ImeInputAction.CandidateSelected(0), clickedAction)

        val tvCandidate1 = candidateContainer.getChildAt(2) as TextView
        tvCandidate1.performClick()
        assertEquals(ImeInputAction.CandidateSelected(1), clickedAction)
    }

    @Test
    fun testRefreshFromStateHasNoHandlerDependency() {
        controller.refreshFromState(state(candidates = listOf(candidate("我", "96", 900))))

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

    @Test
    fun testNoPhantomPinyinCandidateInCandidateList() {
        controller.refreshFromState(
            state(
                preeditVisible = true,
                preedit = "bu tai xing",
                candidates = listOf(candidate("不太行", "288249464", 9000)),
            )
        )

        assertEquals(2, candidateContainer.childCount)
        val preeditChip = candidateContainer.getChildAt(0) as TextView
        assertEquals("bu tai xing", preeditChip.text.toString())
        val realCand = candidateContainer.getChildAt(1) as TextView
        assertEquals("不太行", realCand.text.toString())

        var clickedAction: ImeInputAction? = null
        controller.onInputAction = { clickedAction = it }
        preeditChip.performClick()
        assertEquals("preedit chip must not emit CandidateSelected", null, clickedAction)

        realCand.performClick()
        assertEquals("clicking first real candidate must emit CandidateSelected(0)", ImeInputAction.CandidateSelected(0), clickedAction)
    }

    @Test
    fun testCandidateBarHeightStableWithAndWithoutPreedit() {
        controller.refreshFromState(state(preeditVisible = true, preedit = "wo"))
        val heightWithPreedit = candidateContainer.height
        candidateContainer.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.AT_MOST),
            android.view.View.MeasureSpec.makeMeasureSpec(48, android.view.View.MeasureSpec.AT_MOST),
        )
        val measuredHeightWithPreedit = candidateContainer.measuredHeight

        controller.refreshFromState(state(candidates = listOf(candidate("我", "96", 900))))
        candidateContainer.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.AT_MOST),
            android.view.View.MeasureSpec.makeMeasureSpec(48, android.view.View.MeasureSpec.AT_MOST),
        )
        val measuredHeightWithCandidates = candidateContainer.measuredHeight

        assertEquals(
            "candidate bar height should be consistent",
            measuredHeightWithPreedit, measuredHeightWithCandidates,
        )
    }

    @Test
    fun testEmptyStateShowsFunctionalChips() {
        controller.refreshFromState(state(preeditVisible = false, preedit = "", rawBuffer = ""))

        assertEquals(View.VISIBLE, candidateContainer.visibility)
        assertEquals(3, candidateContainer.childCount)
        val chips = (0 until 3).map { (candidateContainer.getChildAt(it) as TextView).text.toString() }
        assertEquals(listOf("📋", "⚙", "↔"), chips)
    }

    @Test
    fun testFunctionalChipsDoNotPolluteCandidatesAndDoNotEmitCandidateSelected() {
        controller.refreshFromState(state(preeditVisible = false, preedit = "", rawBuffer = ""))

        var clickedAction: ImeInputAction? = null
        controller.onInputAction = { clickedAction = it }

        // Click "📋" (index 0)
        val clipChip = candidateContainer.getChildAt(0) as TextView
        clipChip.performClick()
        assert(clickedAction !is ImeInputAction.CandidateSelected)
    }

    @Test
    fun testClipboardHistoryDeduplicationAndSaving() {
        // Clear history first
        val sharedPrefs = context.getSharedPreferences("xiwei_clipboard_history", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()

        // Mock system clipboard manager to have new text
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("text", "hello world")
        clipboardManager.setPrimaryClip(clip)

        // Trigger refresh which updates history
        controller.refreshFromState(state(preeditVisible = false, preedit = "", rawBuffer = ""))

        // Click clipboard button to view history
        val clipChip = candidateContainer.getChildAt(0) as TextView
        clipChip.performClick()

        // Candidate bar should now show close and the list of history
        assertEquals(2, candidateContainer.childCount)
        assertEquals("关闭", (candidateContainer.getChildAt(0) as TextView).text.toString())
        assertEquals("hello world", (candidateContainer.getChildAt(1) as TextView).text.toString())

        // Test deduplication and 20 limit
        for (i in 1..25) {
            val c = android.content.ClipData.newPlainText("text", "text_$i")
            clipboardManager.setPrimaryClip(c)
            controller.refreshFromState(state(preeditVisible = false, preedit = "", rawBuffer = ""))
        }

        // Open clipboard again
        clipChip.performClick()
        // Child count should be 1 (close button) + 20 (history limit) = 21
        assertEquals(21, candidateContainer.childCount)
        assertEquals("text_25", (candidateContainer.getChildAt(1) as TextView).text.toString())

        // Click a history item, it should commit full text and close
        var clickedAction: ImeInputAction? = null
        controller.onInputAction = { clickedAction = it }
        val itemChip = candidateContainer.getChildAt(1) as TextView
        itemChip.performClick()

        assertEquals(ImeInputAction.SymbolCommitted("text_25"), clickedAction)
        
        // It should return to empty state
        assertEquals(3, candidateContainer.childCount)
        assertEquals("📋", (candidateContainer.getChildAt(0) as TextView).text.toString())
    }

    @Test
    fun testClipboardEmptyState() {
        val sharedPrefs = context.getSharedPreferences("xiwei_clipboard_history", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()

        controller.refreshFromState(state(preeditVisible = false, preedit = "", rawBuffer = ""))

        // Click clipboard
        val clipChip = candidateContainer.getChildAt(0) as TextView
        clipChip.performClick()

        assertEquals(2, candidateContainer.childCount)
        assertEquals("关闭", (candidateContainer.getChildAt(0) as TextView).text.toString())
        assertEquals("剪贴板为空", (candidateContainer.getChildAt(1) as TextView).text.toString())
    }

    @Test
    fun testSelectionPanelOperations() {
        controller.refreshFromState(state(preeditVisible = false, preedit = "", rawBuffer = ""))

        // Click "↔" (index 2)
        val selectChip = candidateContainer.getChildAt(2) as TextView
        selectChip.performClick()

        // Checks child count: ←, →, 全选, 复制, 剪切, 粘贴, 关闭 (total 7)
        assertEquals(7, candidateContainer.childCount)
        val chips = (0 until 7).map { (candidateContainer.getChildAt(it) as TextView).text.toString() }
        assertEquals(listOf("←", "→", "全选", "复制", "剪切", "粘贴", "关闭"), chips)

        var moveLeft: Boolean? = null
        controller.onMoveCursor = { moveLeft = it }
        candidateContainer.getChildAt(0).performClick()
        assertEquals(false, moveLeft) // Moves left

        var moveRight: Boolean? = null
        controller.onMoveCursor = { moveRight = it }
        candidateContainer.getChildAt(1).performClick()
        assertEquals(true, moveRight) // Moves right

        var editAction: Int? = null
        controller.onEditorAction = { editAction = it }
        candidateContainer.getChildAt(2).performClick() // 全选
        assertEquals(android.R.id.selectAll, editAction)

        // Close should return to empty state
        candidateContainer.getChildAt(6).performClick()
        assertEquals(3, candidateContainer.childCount)
    }

    @Test
    fun testRegressionNormalTypingOverridesEmptyState() {
        // First switch to Selection panel
        controller.refreshFromState(state(preeditVisible = false, preedit = "", rawBuffer = ""))
        candidateContainer.getChildAt(2).performClick() // Open selection
        assertEquals(7, candidateContainer.childCount)

        // Now type a digit (non-empty rawBuffer and candidates)
        controller.refreshFromState(
            state(
                preeditVisible = true,
                preedit = "a",
                candidates = listOf(candidate("啊", "2", 900)),
                rawBuffer = "2"
            )
        )

        // Should render candidates, not selection panel!
        assertEquals(2, candidateContainer.childCount)
        assertEquals("a", (candidateContainer.getChildAt(0) as TextView).text.toString())
        assertEquals("啊", (candidateContainer.getChildAt(1) as TextView).text.toString())
    }

    @Test
    fun testKeyboardModeClipboardAndSelectionPanelPlaceholder() {
        val modes = KeyboardMode.values()
        assert(modes.contains(KeyboardMode.ClipboardPanel))
        assert(modes.contains(KeyboardMode.SelectionPanel))
    }

    private fun state(
        mode: KeyboardMode = KeyboardMode.ChineseT9,
        preeditVisible: Boolean = false,
        preedit: String = "",
        readings: List<String> = emptyList(),
        candidates: List<CandidateSnapshotItem> = emptyList(),
        rawBuffer: String = if (preedit.isNotEmpty()) "96" else "",
    ): KeyboardUiState = KeyboardUiState(
        keyboardMode = mode,
        lastTextMode = KeyboardMode.ChineseT9,
        rawBuffer = rawBuffer,
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

    private fun candidate(text: String, code: String, score: Int) = CandidateSnapshotItem(
        text = text,
        code = code,
        sourcePinyin = code,
        score = score,
        origin = "TEST",
    )
}
