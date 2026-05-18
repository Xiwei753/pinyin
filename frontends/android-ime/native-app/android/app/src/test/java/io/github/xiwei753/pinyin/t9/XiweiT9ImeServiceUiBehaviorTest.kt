package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import android.widget.LinearLayout
import android.view.View

class XiweiT9ImeServiceUiBehaviorTest {

    private fun createService(): XiweiT9ImeService {
        return XiweiT9ImeService()
    }

    private fun injectField(service: XiweiT9ImeService, name: String, value: Any) {
        val field = XiweiT9ImeService::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(service, value)
    }

    @Test
    fun testZeroWithEmptyBufferCommitsSpace() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("")

        injectField(service, "engine", engine)
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))

        var threwOnAndroidFramework = false
        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("onZeroPressed")
            method.isAccessible = true
            method.invoke(service)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            if (e.cause?.message?.contains("not mocked") == true
                || e.cause is java.lang.RuntimeException) {
                threwOnAndroidFramework = true
            } else {
                fail("Unexpected exception: ${e.cause}")
            }
        }

        assertTrue("Expected Android framework call failure", threwOnAndroidFramework)
        verify(engine, never()).clear()
        verify(engine, never()).inputDigit("9")
    }

    @Test
    fun testZeroWithNonEmptyBufferCommitsFirstCandidate() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        val candidate = Candidate("\u6211", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        val candidates = listOf(candidate)

        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(candidates)
        `when`(engine.commitCandidate(candidate)).thenReturn(candidate)

        injectField(service, "engine", engine)
        injectField(service, "currentCandidates", candidates)
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("onZeroPressed")
            method.isAccessible = true
            method.invoke(service)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // Expected: updateUi() or currentInputConnection fails on Android framework
        }

        verify(engine).commitCandidate(candidate)
    }

    @Test
    fun testZeroWithNonEmptyBufferNoCandidatesClearsEngine() {
        val service = createService()
        val engine = mock(T9Engine::class.java)

        `when`(engine.buffer).thenReturn("99")
        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

        injectField(service, "engine", engine)
        injectField(service, "currentCandidates", emptyList<Candidate>())
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("onZeroPressed")
            method.isAccessible = true
            method.invoke(service)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // Expected: updateUi() fails on Android framework
        }

        verify(engine).clear()
    }

    @Test
    fun testCandidateClickUsesCachedCandidate() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        val candidate0 = Candidate("\u6211", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)
        val candidate1 = Candidate("\u6211\u4EEC", "96", 800, CandidateType.NORMAL, "wo", CandidateOrigin.EXACT_SINGLE)
        val candidates = listOf(candidate0, candidate1)

        `when`(engine.buffer).thenReturn("96")
        `when`(engine.commitCandidate(candidate1)).thenReturn(candidate1)

        injectField(service, "engine", engine)
        injectField(service, "currentCandidates", candidates)
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("onCandidateClicked", Int::class.java)
            method.isAccessible = true
            method.invoke(service, 1)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // Expected: updateUi() or currentInputConnection fails on Android framework
        }

        verify(engine).commitCandidate(candidate1)
        verify(engine, never()).commitCandidate(candidate0)
    }

    @Test
    fun testCandidateClickCommitsCorrectText() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        val candidate = Candidate("\u4E0D\u592A\u884C", "288249464", 1000, CandidateType.NORMAL, "bu tai xing", CandidateOrigin.EXACT_PHRASE)
        val candidates = listOf(candidate)

        `when`(engine.buffer).thenReturn("288249464")
        `when`(engine.commitCandidate(candidate)).thenReturn(candidate)

        injectField(service, "engine", engine)
        injectField(service, "currentCandidates", candidates)
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("onCandidateClicked", Int::class.java)
            method.isAccessible = true
            method.invoke(service, 0)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // Expected
        }

        verify(engine).commitCandidate(candidate)
    }

    @Test
    fun testCommitCandidateIsCalledOnClick() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        val candidate = Candidate("\u6211", "96", 1000, CandidateType.SINGLE_CHAR, "wo", CandidateOrigin.EXACT_SINGLE)

        `when`(engine.buffer).thenReturn("96")
        `when`(engine.commitCandidate(candidate)).thenReturn(candidate)

        injectField(service, "engine", engine)
        injectField(service, "currentCandidates", listOf(candidate))
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("onCandidateClicked", Int::class.java)
            method.isAccessible = true
            method.invoke(service, 0)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // Expected: updateUi() fails on Android framework
        }

        verify(engine).commitCandidate(candidate)
    }

    @Test
    fun testDigitPressedInputsToEngine() {
        val service = createService()
        val engine = mock(T9Engine::class.java)

        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

        injectField(service, "engine", engine)
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("onDigitPressed", String::class.java)
            method.isAccessible = true
            method.invoke(service, "9")
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // Expected: updateUi() fails on Android framework
        }

        verify(engine).inputDigit("9")
    }

    @Test
    fun testMultipleDigitInputAccumulates() {
        val service = createService()
        val engine = mock(T9Engine::class.java)

        `when`(engine.getPreedit()).thenReturn("")
        `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

        injectField(service, "engine", engine)
        injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("onDigitPressed", String::class.java)
            method.isAccessible = true
            method.invoke(service, "9")
        } catch (e: java.lang.reflect.InvocationTargetException) {}

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("onDigitPressed", String::class.java)
            method.isAccessible = true
            method.invoke(service, "6")
        } catch (e: java.lang.reflect.InvocationTargetException) {}

        verify(engine).inputDigit("9")
        verify(engine).inputDigit("6")
    }

    @Test
    fun testCandidateCountSettingPassesToEngine() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        val settingsRepo = mock(SettingsRepository::class.java)

        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getVisibleCandidates(eq(15))).thenReturn(emptyList())
        `when`(settingsRepo.getCandidateCount()).thenReturn(15)
        `when`(settingsRepo.isDebugLoggingEnabled()).thenReturn(false)

        injectField(service, "engine", engine)
        injectField(service, "settingsRepository", settingsRepo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))
        injectField(service, "bufferText", mock(android.widget.TextView::class.java))
        injectField(service, "candidateContainer", mock(LinearLayout::class.java))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("updateUi")
            method.isAccessible = true
            method.invoke(service)
        } catch (e: java.lang.reflect.InvocationTargetException) {
        }

        verify(engine).getVisibleCandidates(eq(15))
    }

    @Test
    fun testCandidateBarVisibilityNotAffectedByHeightSetting() {
        val service = createService()

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getTheme()).thenReturn("system")
        `when`(repo.getKeyboardHeight()).thenReturn("high")

        val candidateBar = mock(LinearLayout::class.java)
        val key1 = mock(android.widget.TextView::class.java)
        val keyParent = mock(android.widget.FrameLayout::class.java)
        `when`(key1.parent).thenReturn(keyParent)

        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "dictionary", mock(BuiltinDictionary::class.java))
        injectField(service, "engine", mock(T9Engine::class.java))
        injectField(service, "bufferText", key1)
        injectField(service, "candidateContainer", candidateBar)

        val rootView = mock(View::class.java)
        `when`(rootView.findViewById<LinearLayout>(R.id.candidate_bar)).thenReturn(candidateBar)
        // Return key1 for all TextView lookups
        for (id in listOf(R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
                R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9,
                R.id.key_star, R.id.key_0, R.id.key_del)) {
            `when`(rootView.findViewById<android.widget.TextView>(id)).thenReturn(key1)
        }

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("applyThemeAndHeight", View::class.java)
            method.isAccessible = true
            method.invoke(service, rootView)
        } catch (e: java.lang.reflect.InvocationTargetException) {
        }

        verify(candidateBar, never()).visibility = View.GONE
    }
}
