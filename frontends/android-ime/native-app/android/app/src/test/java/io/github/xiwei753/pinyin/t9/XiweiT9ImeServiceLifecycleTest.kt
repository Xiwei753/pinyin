package io.github.xiwei753.pinyin.t9

import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import android.content.Context
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary
import io.github.xiwei753.pinyin.t9.data.DictionaryManager
import java.lang.reflect.Method
import android.widget.TextView
import android.widget.LinearLayout
import android.view.View

class XiweiT9ImeServiceLifecycleTest {

    private fun setMockCore(service: XiweiT9ImeService) {
        val repoField = XiweiT9ImeService::class.java.getDeclaredField("settingsRepository")
        repoField.isAccessible = true
        repoField.set(service, mock(SettingsRepository::class.java))

        val hapticField = XiweiT9ImeService::class.java.getDeclaredField("hapticFeedbackManager")
        hapticField.isAccessible = true
        hapticField.set(service, mock(HapticFeedbackManager::class.java))

        val dictField = XiweiT9ImeService::class.java.getDeclaredField("dictionary")
        dictField.isAccessible = true
        dictField.set(service, mock(BuiltinDictionary::class.java))

        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("")
        val engineField = XiweiT9ImeService::class.java.getDeclaredField("engine")
        engineField.isAccessible = true
        engineField.set(service, engine)
    }

    @Test
    fun testOnStartInputBeforeOnCreateInputView() {
        val service = XiweiT9ImeService()
        setMockCore(service)
        try {
            // Android base class InputMethodService methods try to access Window/Display
            // in tests without robolectric. So it will throw RuntimeException from android.jar.
            // But we specifically want to verify that our `UninitializedPropertyAccessException`
            // is not thrown anymore.
            service.onStartInput(null, false)
            assertTrue(true)
        } catch (e: UninitializedPropertyAccessException) {
            fail("Should not throw UninitializedPropertyAccessException")
        } catch (e: Exception) {
            // ContextWrapper not mocked exception is expected from base class in JVM tests
            assertTrue(true)
        }
    }

    @Test
    fun testOnFinishInputBeforeOnCreateInputView() {
        val service = XiweiT9ImeService()
        setMockCore(service)
        try {
            service.onFinishInput()
            assertTrue(true)
        } catch (e: UninitializedPropertyAccessException) {
            fail("Should not throw UninitializedPropertyAccessException")
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    @Test
    fun testOnWindowHiddenBeforeOnCreateInputView() {
        val service = XiweiT9ImeService()
        setMockCore(service)
        try {
            service.onWindowHidden()
            assertTrue(true)
        } catch (e: UninitializedPropertyAccessException) {
            fail("Should not throw UninitializedPropertyAccessException")
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    private fun getResetMethod(): Method {
        val method = XiweiT9ImeService::class.java.getDeclaredMethod("resetCompositionState")
        method.isAccessible = true
        return method
    }

    @Test
    fun testResetCompositionState_UI_Not_Initialized_Does_Not_Crash() {
        val service = XiweiT9ImeService()
        setMockCore(service)

        try {
            getResetMethod().invoke(service)
            assertTrue(true)
        } catch (e: Exception) {
            e.printStackTrace()
            fail("Should not crash: ${e.cause}")
        }
    }

    @Test
    fun testResetCompositionState_UI_Initialized_Clears_Correctly() {
        val service = XiweiT9ImeService()
        setMockCore(service)

        val bufferTextMock = mock(TextView::class.java)
        val bufferField = XiweiT9ImeService::class.java.getDeclaredField("bufferText")
        bufferField.isAccessible = true
        bufferField.set(service, bufferTextMock)

        val containerMock = mock(LinearLayout::class.java)
        val containerField = XiweiT9ImeService::class.java.getDeclaredField("candidateContainer")
        containerField.isAccessible = true
        containerField.set(service, containerMock)

        getResetMethod().invoke(service)

        // Verify UI states are cleared
        verify(bufferTextMock).text = ""
        verify(containerMock).removeAllViews()
        verify(containerMock).visibility = View.GONE
    }
}
