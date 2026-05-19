package io.github.xiwei753.pinyin.t9

import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import io.github.xiwei753.pinyin.t9.core.T9Engine
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

        val engine = mock(T9Engine::class.java)
        `when`(engine.buffer).thenReturn("")
        val handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(engine) }
        val ctrlField = XiweiT9ImeService::class.java.getDeclaredField("handler")
        ctrlField.isAccessible = true
        ctrlField.set(service, handler)
    }

    @Test
    fun testOnStartInputBeforeOnCreateInputView() {
        val service = XiweiT9ImeService()
        setMockCore(service)
        try {
            service.onStartInput(null, false)
            assertTrue(true)
        } catch (e: UninitializedPropertyAccessException) {
            fail("Should not throw UninitializedPropertyAccessException")
        } catch (e: Exception) {
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



}
