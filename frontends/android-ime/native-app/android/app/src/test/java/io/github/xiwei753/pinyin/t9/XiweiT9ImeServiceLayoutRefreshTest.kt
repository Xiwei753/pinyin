package io.github.xiwei753.pinyin.t9

import android.content.Context
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.core.PinyinComposition
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class XiweiT9ImeServiceLayoutRefreshTest {

    private lateinit var service: XiweiT9ImeService
    private lateinit var mockXiweiKeyboardView: XiweiKeyboardView
    private lateinit var handler: KeyboardActionHandler
    private lateinit var engine: T9Engine

    private lateinit var logMock: MockedStatic<Log>
    private lateinit var looperMock: MockedStatic<Looper>

    @Before
    fun setup() {
        service = spy(XiweiT9ImeService())
        doReturn(java.io.File("/tmp")).`when`(service).filesDir
        doReturn(service).`when`(service).applicationContext

        logMock = mockStatic(Log::class.java)
        logMock.`when`<Int> { Log.i(anyString(), anyString()) }.thenReturn(0)
        logMock.`when`<Int> { Log.e(anyString(), anyString(), any()) }.thenReturn(0)

        looperMock = mockStatic(Looper::class.java)
        looperMock.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mock(Looper::class.java))

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getTheme()).thenReturn("system")
        `when`(repo.getKeyboardHeight()).thenReturn("normal")
        val fieldRepo = XiweiT9ImeService::class.java.getDeclaredField("settingsRepository")
        fieldRepo.isAccessible = true
        fieldRepo.set(service, repo)

        val resources = mock(android.content.res.Resources::class.java)
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 1920
        metrics.density = 2.0f
        `when`(resources.displayMetrics).thenReturn(metrics)
        `when`(resources.configuration).thenReturn(android.content.res.Configuration())
        doReturn(resources).`when`(service).resources

        val mockInflater = mock(LayoutInflater::class.java)
        doReturn(mockInflater).`when`(service).layoutInflater

        val mockView = mock(View::class.java)
        `when`(mockInflater.inflate(eq(R.layout.keyboard_view), isNull())).thenReturn(mockView)

        mockXiweiKeyboardView = mock(XiweiKeyboardView::class.java)
        `when`(mockView.findViewById<View>(R.id.ime_root)).thenReturn(mock(View::class.java))
        `when`(mockView.findViewById<View>(R.id.candidate_bar)).thenReturn(mock(LinearLayout::class.java))
        `when`(mockView.findViewById<View>(R.id.candidate_container)).thenReturn(mock(LinearLayout::class.java))
        `when`(mockView.findViewById<View>(R.id.pinyin_floating_bar)).thenReturn(mock(View::class.java))
        `when`(mockView.findViewById<View>(R.id.pinyin_floating_text)).thenReturn(mock(TextView::class.java))
        `when`(mockView.findViewById<XiweiKeyboardView>(R.id.xiwei_keyboard_view)).thenReturn(mockXiweiKeyboardView)

        service.onCreateInputView()

        val mockController = mock(CandidateViewController::class.java)
        val fieldController = XiweiT9ImeService::class.java.getDeclaredField("candidateViewController")
        fieldController.isAccessible = true
        fieldController.set(service, mockController)
        
        val fieldHandler = XiweiT9ImeService::class.java.getDeclaredField("handler")
        fieldHandler.isAccessible = true
        handler = fieldHandler.get(service) as KeyboardActionHandler
        
        engine = mock(T9Engine::class.java)
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getPreeditHint()).thenReturn("")
        `when`(engine.buffer).thenReturn("")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())
        `when`(engine.createDetachedCandidateEngine(anyString(), anyList())).thenReturn(engine)
        handler.attachEngine(engine)

        // Let's ensure the initial view bindings triggered layout update, and clear the invocations
        verify(mockXiweiKeyboardView, atLeastOnce()).layoutModel = any()
        reset(mockXiweiKeyboardView)
    }

    @After
    fun teardown() {
        logMock.close()
        looperMock.close()
    }

    private fun captureLayoutModel(): KeyboardLayoutModel {
        val captor = ArgumentCaptor.forClass(KeyboardLayoutModel::class.java)
        verify(mockXiweiKeyboardView, atLeastOnce()).layoutModel = captor.capture()
        return captor.allValues.last()
    }

    @Test
    fun testRawBufferEmptyShowsPunct() {
        `when`(engine.buffer).thenReturn("")
        `when`(engine.readings).thenReturn(emptyList())

        service.refreshUi()

        val model = captureLayoutModel()
        val punctKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_PUNCT }
        val readingKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_READING }
        
        assertTrue("Should show punctuation when buffer is empty", punctKeys.isNotEmpty())
        assertTrue("Should not show readings when buffer is empty", readingKeys.isEmpty())
    }

    @Test
    fun testInputDigitShowsReadingsAndTriggersRefresh() {
        `when`(engine.buffer).thenReturn("64")
        `when`(engine.getPreedit()).thenReturn("ni")
        `when`(engine.getPreeditHint()).thenReturn("ni")
        `when`(engine.readings).thenReturn(listOf("ni"))

        // Call onDigitPressed which should trigger refreshUi and rebuildLayoutModel
        handler.onDigitPressed("6")
        
        val model = captureLayoutModel()
        val readingKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_READING }
        val punctKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_PUNCT }

        assertTrue("Should contain reading keys in left rail", readingKeys.isNotEmpty())
        assertTrue("Should not show punctuation when buffer is non-empty", punctKeys.isEmpty())
    }

    @Test
    fun testOnDeleteClearsBufferAndReturnsToPunct() {
        `when`(engine.buffer).thenReturn("6").thenReturn("")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getPreeditHint()).thenReturn("")
        `when`(engine.readings).thenReturn(emptyList())

        handler.onDelete()

        val model = captureLayoutModel()
        val punctKeys = model.leftRailKeys.filter { it.role == KeyboardKeyRole.RAIL_PUNCT }
        assertTrue("Should return to punctuation when buffer is cleared", punctKeys.isNotEmpty())
    }

    @Test
    fun testSetActiveReadingTriggersRefresh() {
        `when`(engine.buffer).thenReturn("64")
        `when`(engine.getPreedit()).thenReturn("ni")
        `when`(engine.getPreeditHint()).thenReturn("ni")
        `when`(engine.readings).thenReturn(listOf("ni", "mi"))
        `when`(engine.setActiveReading(anyString())).thenReturn(true)

        handler.setActiveReading("mi")

        val model = captureLayoutModel()
        assertNotNull(model)
    }

    @Test
    fun testOnCandidateClickTriggersRefresh() {
        // mock candidate click when not empty
        `when`(engine.buffer).thenReturn("64")
        `when`(engine.getPreedit()).thenReturn("ni")
        `when`(engine.getPreeditHint()).thenReturn("ni")
        `when`(engine.readings).thenReturn(listOf("ni"))
        
        val dummyCandidate = io.github.xiwei753.pinyin.t9.core.Candidate("ni", "你", 100)
        `when`(engine.getVisibleCandidates(10)).thenReturn(listOf(dummyCandidate))
        `when`(engine.commitCandidate(dummyCandidate)).thenReturn(dummyCandidate)

        // Read candidates to populate the handler's cache
        handler.refreshCandidates(10)

        handler.onCandidateClick(0)

        val model = captureLayoutModel()
        assertNotNull(model)
    }
}
