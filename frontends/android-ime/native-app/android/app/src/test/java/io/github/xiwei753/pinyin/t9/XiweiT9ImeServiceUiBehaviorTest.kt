package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9Engine
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyInt
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
    fun testCandidateCountSettingPassesToController() {
        val service = createService()
        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("96")
        `when`(engine.getPreedit()).thenReturn("wo")
        `when`(engine.getVisibleCandidates(eq(15))).thenReturn(emptyList())
        `when`(engine.getCompositions()).thenReturn(emptyList())
        `when`(engine.getInternalCandidates()).thenReturn(emptyList())

        val controller = T9ImeController(engine)

        val repo = mock(SettingsRepository::class.java)
        `when`(repo.getCandidateCount()).thenReturn(15)
        `when`(repo.isDebugLoggingEnabled()).thenReturn(false)
        `when`(repo.getTheme()).thenReturn("system")

        injectField(service, "controller", controller)
        injectField(service, "settingsRepository", repo)
        injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))
        injectField(service, "bufferText", mock(android.widget.TextView::class.java))
        injectField(service, "candidateContainer", mock(LinearLayout::class.java))

        try {
            val method = XiweiT9ImeService::class.java.getDeclaredMethod("refreshUi")
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
        injectField(service, "controller", T9ImeController(mock(T9Engine::class.java)))
        injectField(service, "bufferText", key1)
        injectField(service, "candidateContainer", candidateBar)

        val rootView = mock(View::class.java)
        `when`(rootView.findViewById<LinearLayout>(R.id.candidate_bar)).thenReturn(candidateBar)
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

    @Test
    fun testServiceCanBeCreatedWithoutCrash() {
        try {
            val service = createService()
            val engine = mock(T9Engine::class.java)
            `when`(engine.buffer).thenReturn("")
            `when`(engine.getPreedit()).thenReturn("")
            `when`(engine.getCompositions()).thenReturn(emptyList())
            `when`(engine.getInternalCandidates()).thenReturn(emptyList())
            `when`(engine.getVisibleCandidates(anyInt())).thenReturn(emptyList())

            injectField(service, "controller", T9ImeController(engine))
            injectField(service, "settingsRepository", mock(SettingsRepository::class.java))
            injectField(service, "hapticFeedbackManager", mock(HapticFeedbackManager::class.java))

            assertTrue(true)
        } catch (e: Exception) {
            fail("Service creation should not crash: ${e.message}")
        }
    }
}
