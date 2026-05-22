package io.github.xiwei753.pinyin.t9

import android.view.inputmethod.EditorInfo
import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorInputTypePolicyTest {

    @Test
    fun normalTextUsesChineseT9() {
        val info = editorInfo(inputType = InputType.TYPE_CLASS_TEXT)
        val policy = EditorInputTypePolicy.resolve(info)

        assertEquals(KeyboardMode.ChineseT9, policy.defaultKeyboardMode)
        assertEquals(EditorEnterBehavior.NEWLINE, policy.enterBehavior)
        assertTrue(policy.allowChineseCandidates)
    }

    @Test
    fun passwordUsesEnglishOrNumberAndDisablesChinese() {
        val textPassword = editorInfo(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val numberPassword = editorInfo(inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)

        val textPolicy = EditorInputTypePolicy.resolve(textPassword)
        val numberPolicy = EditorInputTypePolicy.resolve(numberPassword)

        assertEquals(KeyboardMode.EnglishT9, textPolicy.defaultKeyboardMode)
        assertEquals(KeyboardMode.Number, numberPolicy.defaultKeyboardMode)
        assertFalse(textPolicy.allowChineseCandidates)
        assertFalse(numberPolicy.allowChineseCandidates)
    }

    @Test
    fun numberAndPhoneUseNumberMode() {
        val numberPolicy = EditorInputTypePolicy.resolve(editorInfo(inputType = InputType.TYPE_CLASS_NUMBER))
        val phonePolicy = EditorInputTypePolicy.resolve(editorInfo(inputType = InputType.TYPE_CLASS_PHONE))

        assertEquals(KeyboardMode.Number, numberPolicy.defaultKeyboardMode)
        assertEquals(KeyboardMode.Number, phonePolicy.defaultKeyboardMode)
        assertFalse(numberPolicy.allowChineseCandidates)
        assertFalse(phonePolicy.allowChineseCandidates)
    }

    @Test
    fun urlAndEmailUseEnglishT9() {
        val urlPolicy = EditorInputTypePolicy.resolve(editorInfo(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
        val emailPolicy = EditorInputTypePolicy.resolve(editorInfo(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))

        assertEquals(KeyboardMode.EnglishT9, urlPolicy.defaultKeyboardMode)
        assertEquals(KeyboardMode.EnglishT9, emailPolicy.defaultKeyboardMode)
        assertFalse(urlPolicy.allowChineseCandidates)
        assertFalse(emailPolicy.allowChineseCandidates)
    }

    @Test
    fun multilineTextKeepsChineseAndNewlinePolicy() {
        val policy = EditorInputTypePolicy.resolve(
            editorInfo(
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                imeOptions = EditorInfo.IME_ACTION_NONE,
            )
        )

        assertEquals(KeyboardMode.ChineseT9, policy.defaultKeyboardMode)
        assertEquals(EditorEnterBehavior.NEWLINE, policy.enterBehavior)
    }

    @Test
    fun sendSearchGoNextDoneMapToEnterBehaviors() {
        assertEquals(EditorEnterBehavior.SEND, EditorInputTypePolicy.resolve(editorInfo(imeOptions = EditorInfo.IME_ACTION_SEND)).enterBehavior)
        assertEquals(EditorEnterBehavior.SEARCH, EditorInputTypePolicy.resolve(editorInfo(imeOptions = EditorInfo.IME_ACTION_SEARCH)).enterBehavior)
        assertEquals(EditorEnterBehavior.GO, EditorInputTypePolicy.resolve(editorInfo(imeOptions = EditorInfo.IME_ACTION_GO)).enterBehavior)
        assertEquals(EditorEnterBehavior.NEXT, EditorInputTypePolicy.resolve(editorInfo(imeOptions = EditorInfo.IME_ACTION_NEXT)).enterBehavior)
        assertEquals(EditorEnterBehavior.DONE, EditorInputTypePolicy.resolve(editorInfo(imeOptions = EditorInfo.IME_ACTION_DONE)).enterBehavior)
    }

    private fun editorInfo(inputType: Int = InputType.TYPE_CLASS_TEXT, imeOptions: Int = EditorInfo.IME_ACTION_UNSPECIFIED): EditorInfo {
        return EditorInfo().apply {
            this.inputType = inputType
            this.imeOptions = imeOptions
        }
    }
}
