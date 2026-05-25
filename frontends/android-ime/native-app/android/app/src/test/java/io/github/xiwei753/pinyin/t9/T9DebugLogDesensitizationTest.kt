package io.github.xiwei753.pinyin.t9

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.github.xiwei753.pinyin.t9.core.T9Engine
import org.robolectric.util.ReflectionHelpers
import android.view.View

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class T9DebugLogDesensitizationTest {

    class FakeDebugLogger : T9DebugLogger {
        val logs = mutableListOf<String>()
        override fun log(tag: String, message: String) {
            logs.add("[$tag] $message")
        }
    }

    @Test
    fun testLogDesensitization() {
        val logger = FakeDebugLogger()
        val controller = Robolectric.buildService(XiweiT9ImeService::class.java)
        val service = controller.create().get()

        val mockRepo = mock(SettingsRepository::class.java)
        `when`(mockRepo.isDebugLoggingEnabled()).thenReturn(true)
        service.settingsRepository = mockRepo
        service.debugLogger = logger

        // Test logAction for commitText
        service.commitText("我/不太行/abc")

        // Ensure sensitive text is NOT in the logs
        val joinedLogs = logger.logs.joinToString("\n")
        assertFalse("Logs should not contain actual input text", joinedLogs.contains("我/不太行/abc"))

        // Ensure we logged length instead
        assertTrue("Logs should contain the text length", joinedLogs.contains("text_len=9"))

        // Test logDebugInfo via refreshUi
        val mockEngine = mock(T9Engine::class.java)
        `when`(mockEngine.buffer).thenReturn("1234")
        `when`(mockEngine.getPreedit()).thenReturn("ab")

        service.handler = KeyboardActionHandler(
            actionSink = service
        )
        // We need to inject dummy objects to refreshUi does not crash
        ReflectionHelpers.setField(service, "keyboardViews", mock(KeyboardViews::class.java))
        ReflectionHelpers.setField(service, "candidateViewController", mock(CandidateViewController::class.java))

        service.handler.attachEngine(mockEngine)

        // Reset logs
        logger.logs.clear()

        // Ensure that engine buffer and preedit strings are not leaked
        // Calling logDebugInfo internally in refreshUi
        service.refreshUi()

        val updatedLogs = logger.logs.joinToString("\n")
        assertFalse("Logs should not contain engine buffer", updatedLogs.contains("1234"))
        assertFalse("Logs should not contain preedit", updatedLogs.contains("ab"))

        assertTrue("Logs should contain buffer length info", updatedLogs.contains("raw_len=4"))
        assertTrue("Logs should contain preedit length info", updatedLogs.contains("preedit_len=2"))
    }
}
