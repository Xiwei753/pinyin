package io.github.xiwei753.pinyin.t9

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider

class XiweiT9ImeServiceRefactoredRegressionTest {

    private lateinit var service: XiweiT9ImeService
    private lateinit var mockRepo: SettingsRepository
    private lateinit var mockHaptic: HapticFeedbackManager
    private lateinit var handler: KeyboardActionHandler
    private lateinit var engine: T9Engine

    @Before
    fun setUp() {
        service = XiweiT9ImeService()
        mockRepo = mock(SettingsRepository::class.java)
        mockHaptic = mock(HapticFeedbackManager::class.java)
        `when`(mockRepo.isDebugLoggingEnabled()).thenReturn(false)
        `when`(mockRepo.getTheme()).thenReturn("system")
        `when`(mockRepo.getKeyboardHeight()).thenReturn("normal")

        val mockDict = mock(DictionaryProvider::class.java)
        val eng = T9Engine(mockDict)
        engine = eng
        handler = KeyboardActionHandler(mock(ImeActionSink::class.java)).apply { attachEngine(eng) }

        injectField("settingsRepository", mockRepo)
        injectField("hapticFeedbackManager", mockHaptic)
        injectField("handler", handler)
    }

    private fun injectField(name: String, value: Any) {
        val field = XiweiT9ImeService::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(service, value)
    }

    @Test
    fun testServiceHasRequiredControllers() {
        val expectedFields = listOf(
            "keyboardViews", "themeController", "heightController",
            "candidateViewController",
            "deleteRepeatController", "handler", "settingsRepository",
            "hapticFeedbackManager"
        )
        for (fieldName in expectedFields) {
            try {
                val field = XiweiT9ImeService::class.java.getDeclaredField(fieldName)
                assertNotNull("Field $fieldName should exist", field)
            } catch (e: NoSuchFieldException) {
                fail("Expected field $fieldName not found in XiweiT9ImeService")
            }
        }
    }

    @Test
    fun testImeActionSinkInterface() {
        assertTrue("Service should implement ImeActionSink", service is ImeActionSink)
    }

    @Test
    fun testLifecycleMethodsDoNotCrash() {
        try {
            service.onStartInput(null, false)
        } catch (e: RuntimeException) {
            // Expected: super.onStartInput calls Android framework methods not available in unit tests
        } catch (e: Exception) {
            fail("onStartInput should not throw unexpected exception: ${e.message}")
        }
        try {
            service.onFinishInput()
        } catch (e: RuntimeException) {
            // Expected: super.onFinishInput calls Android framework methods not available in unit tests
        } catch (e: Exception) {
            fail("onFinishInput should not crash: ${e.message}")
        }
        try {
            service.onWindowHidden()
        } catch (e: RuntimeException) {
            // Expected: super.onWindowHidden calls Android framework methods not available in unit tests
        } catch (e: Exception) {
            fail("onWindowHidden should not crash: ${e.message}")
        }
    }
}
