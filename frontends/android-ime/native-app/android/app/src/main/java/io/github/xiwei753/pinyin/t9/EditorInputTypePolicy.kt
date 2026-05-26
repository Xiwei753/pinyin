package io.github.xiwei753.pinyin.t9

import android.view.inputmethod.EditorInfo
import android.text.InputType

enum class EditorEnterBehavior {
    SEND,
    SEARCH,
    GO,
    NEXT,
    DONE,
    NEWLINE,
}

data class EditorInputPolicy(
    val defaultKeyboardMode: KeyboardMode,
    val defaultLastTextMode: KeyboardMode,
    val enterBehavior: EditorEnterBehavior,
)

object EditorInputTypePolicy {
    fun resolve(editorInfo: EditorInfo?): EditorInputPolicy {
        val inputType = editorInfo?.inputType ?: 0
        val classMask = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION

        val isPhone = classMask == InputType.TYPE_CLASS_PHONE
        val isNumber = classMask == InputType.TYPE_CLASS_NUMBER
        val isPassword = classMask == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        ) || classMask == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        val isUrl = classMask == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_URI ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
        )
        val isEmail = classMask == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        )

        val defaultKeyboardMode = when {
            isPhone || isNumber -> KeyboardMode.Number
            isPassword || isUrl || isEmail -> KeyboardMode.EnglishQWERTY
            else -> KeyboardMode.ChineseT9
        }

        val defaultLastTextMode = when {
            isPhone || isNumber -> KeyboardMode.EnglishT9
            isPassword || isUrl || isEmail -> KeyboardMode.EnglishQWERTY
            else -> KeyboardMode.ChineseT9
        }

        val enterBehavior = when (editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE) {
            EditorInfo.IME_ACTION_SEND -> EditorEnterBehavior.SEND
            EditorInfo.IME_ACTION_SEARCH -> EditorEnterBehavior.SEARCH
            EditorInfo.IME_ACTION_GO -> EditorEnterBehavior.GO
            EditorInfo.IME_ACTION_NEXT -> EditorEnterBehavior.NEXT
            EditorInfo.IME_ACTION_DONE -> EditorEnterBehavior.DONE
            else -> EditorEnterBehavior.NEWLINE
        }

        return EditorInputPolicy(
            defaultKeyboardMode = defaultKeyboardMode,
            defaultLastTextMode = defaultLastTextMode,
            enterBehavior = enterBehavior,
        )
    }
}
