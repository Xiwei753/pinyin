package io.github.xiwei753.pinyin.t9

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.lang.reflect.Method
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class XiweiT9ImeServiceLoggingTest {

    private lateinit var service: XiweiT9ImeService
    private lateinit var fakeLogger: T9DebugLoggerTest.FakeDebugLogger
    private lateinit var mockRepo: SettingsRepository

    @Before
    fun setUp() {
        // Create an instance of the service.
        service = XiweiT9ImeService()

        // Inject the fake logger.
        fakeLogger = T9DebugLoggerTest.FakeDebugLogger()
        service.debugLogger = fakeLogger

        // Mock SettingsRepository.
        val mockContext = mock(Context::class.java)
        val mockPrefs = mock(android.content.SharedPreferences::class.java)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        mockRepo = mock(SettingsRepository::class.java)

        // Use reflection to set settingsRepository
        val repoField = XiweiT9ImeService::class.java.getDeclaredField("settingsRepository")
        repoField.isAccessible = true
        repoField.set(service, mockRepo)

        // Mock DictionaryProvider and inject T9Engine to prevent null pointers
        val mockDict = mock(DictionaryProvider::class.java)
        val engineField = XiweiT9ImeService::class.java.getDeclaredField("engine")
        engineField.isAccessible = true
        engineField.set(service, T9Engine(mockDict))

        // Set candidateContainer visibility to prevent null pointers during logging
        val containerField = XiweiT9ImeService::class.java.getDeclaredField("candidateContainer")
        containerField.isAccessible = true
        val mockContainer = mock(android.widget.LinearLayout::class.java)
        containerField.set(service, mockContainer)
    }

    private fun triggerLogDebugInfo() {
        val method = XiweiT9ImeService::class.java.getDeclaredMethod("logDebugInfo")
        method.isAccessible = true
        method.invoke(service)
    }

    @Test
    fun testLoggerNotCalledWhenDisabled() {
        `when`(mockRepo.isDebugLoggingEnabled()).thenReturn(false)

        triggerLogDebugInfo()

        assertTrue("Logger should not be called when disabled", fakeLogger.logs.isEmpty())
    }

    @Test
    fun testLoggerCalledWhenEnabled() {
        `when`(mockRepo.isDebugLoggingEnabled()).thenReturn(true)

        triggerLogDebugInfo()

        assertTrue("Logger should be called when enabled", fakeLogger.logs.isNotEmpty())
        assertEquals("XiweiT9Debug", fakeLogger.logs[0].first)
        assertTrue(fakeLogger.logs.any { it.second.contains("================== T9 Debug Info ==================") })
    }
}
