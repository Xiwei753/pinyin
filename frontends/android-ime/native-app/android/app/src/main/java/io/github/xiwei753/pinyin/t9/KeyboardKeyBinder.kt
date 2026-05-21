package io.github.xiwei753.pinyin.t9

import android.view.MotionEvent
import android.view.View

class KeyboardKeyBinder(
    private val v: KeyboardViews,
    private val hapticFeedbackManager: HapticFeedbackManager,
    private val panelController: KeyboardPanelController,
    private val deleteRepeatController: DeleteRepeatController,
    private val onModeChanged: () -> Unit,
) {
    private var symbolKeyClick: (String) -> Unit = {}
    private var onRefreshUi: () -> Unit = {}

    fun setSymbolKeyClickHandler(handler: (String) -> Unit) {
        symbolKeyClick = handler
    }

    fun setOnRefreshUi(handler: () -> Unit) {
        onRefreshUi = handler
    }

    fun setupKey(view: View?, isSpecial: Boolean, action: () -> Unit) {
        if (view == null) return
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (isSpecial) hapticFeedbackManager.performSpecialKey(v)
                else hapticFeedbackManager.performTap(v)
            }
            false
        }
        view.setOnClickListener { action() }
    }

    fun setupAllKeys(handler: KeyboardActionHandler) {
        setupT9DigitKeys(handler)
        setupNumberPadKeys(handler)
        setupSeparatorKey(handler)
        setupDeleteKey(handler)
        setupRetypeKey(handler)
        setupPunctKeys(handler)
        setupReadingKeys(handler)
        setupModeToggleKeys(handler)
        setupSpaceKey(handler)
        setupEnterKey(handler)
        setupSymbolGridKeys(handler)
    }

    private fun setupNumberPadKeys(handler: KeyboardActionHandler) {
        val numKeys = listOf(
            v.num1 to "1", v.num2 to "2", v.num3 to "3",
            v.num4 to "4", v.num5 to "5", v.num6 to "6",
            v.num7 to "7", v.num8 to "8", v.num9 to "9",
            v.num0 to "0", v.numDot to ".",
        )
        for ((keyView, text) in numKeys) {
            setupKey(keyView, false) {
                handler.onDigitPressed(text)
            }
        }
    }

    private fun setupT9DigitKeys(handler: KeyboardActionHandler) {
        val numberKeys = mapOf(
            v.key2 to "2", v.key3 to "3", v.key4 to "4",
            v.key5 to "5", v.key6 to "6", v.key7 to "7",
            v.key8 to "8", v.key9 to "9",
        )
        for ((keyView, digit) in numberKeys) {
            setupKey(keyView, false) {
                handler.onDigitPressed(digit)
            }
        }
    }

    private fun setupSeparatorKey(handler: KeyboardActionHandler) {
        setupKey(v.key1Text, false) {
            handler.onSeparator()
        }
    }

    private fun setupDeleteKey(handler: KeyboardActionHandler) {
        v.keyDel.setOnClickListener {
            handler.onDelete()
        }
        v.keyDel.setOnLongClickListener {
            deleteRepeatController.start()
            true
        }
        v.keyDel.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hapticFeedbackManager.performSpecialKey(view)
            }
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                deleteRepeatController.stop()
            }
            false
        }
    }

    private fun setupRetypeKey(handler: KeyboardActionHandler) {
        setupKey(v.keyRetype, true) {
            handler.onClearComposingForRetype()
        }
    }

    private fun setupPunctKeys(handler: KeyboardActionHandler) {
        val punctKeys = mapOf(
            v.punctTextViews[0] to "，",
            v.punctTextViews[1] to "。",
            v.punctTextViews[2] to "？",
            v.punctTextViews[3] to "！",
        )
        for ((keyView, text) in punctKeys) {
            setupKey(keyView, false) {
                handler.onPunctCommit(text)
            }
        }
    }

    private fun setupReadingKeys(handler: KeyboardActionHandler) {
        for (ridView in v.readingTextViews) {
            setupKey(ridView, false) {
                val reading = ridView.text.toString()
                if (reading.isNotEmpty()) {
                    handler.setActiveReading(reading)
                    onRefreshUi()
                }
            }
        }
    }

    private fun setupModeToggleKeys(handler: KeyboardActionHandler) {
        setupKey(v.keyToggleSymbol, false) {
            handler.toggleSymbolKey()
            onModeChanged()
        }
        setupKey(v.keyToggleEnglish, false) {
            if (handler.keyboardMode == KeyboardMode.EnglishT9) {
                handler.switchKeyboardMode(KeyboardMode.ChineseT9)
            } else if (handler.keyboardMode == KeyboardMode.ChineseT9) {
                handler.switchKeyboardMode(KeyboardMode.EnglishT9)
            } else {
                handler.switchKeyboardMode(KeyboardMode.ChineseT9)
            }
            onModeChanged()
        }
        setupKey(v.keyToggleNumber, false) {
            handler.toggleNumberKey()
            onModeChanged()
        }
    }

    private fun setupSpaceKey(handler: KeyboardActionHandler) {
        setupKey(v.keySpace, false) {
            handler.onSpace()
        }
    }

    private fun setupEnterKey(handler: KeyboardActionHandler) {
        setupKey(v.keyEnter, true) {
            handler.onEnter()
        }
    }

    private fun setupSymbolGridKeys(handler: KeyboardActionHandler) {
        for (symView in v.generatedSymbolViews) {
            setupKey(symView, false) {
                val text = (symView as? android.widget.TextView)?.text?.toString() ?: return@setupKey
                handler.onDigitPressed(text)
                handler.switchKeyboardMode(handler.lastTextMode)
                onModeChanged()
            }
        }
    }
}
