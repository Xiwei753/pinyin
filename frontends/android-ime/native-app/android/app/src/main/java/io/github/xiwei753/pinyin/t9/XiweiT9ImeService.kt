package io.github.xiwei753.pinyin.t9

import android.inputmethodservice.InputMethodService
import android.view.View

import android.widget.LinearLayout
import android.view.LayoutInflater
import android.widget.TextView
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary

class XiweiT9ImeService : InputMethodService() {

    private lateinit var bufferText: TextView
    private lateinit var candidateContainer: LinearLayout

    private lateinit var dictionary: BuiltinDictionary
    private lateinit var engine: T9Engine
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var hapticFeedbackManager: HapticFeedbackManager

    override fun onCreateInputView(): View {
        settingsRepository = SettingsRepository(this)
        hapticFeedbackManager = HapticFeedbackManager(this, settingsRepository)
        dictionary = BuiltinDictionary.fromAssets(this)
        engine = T9Engine(dictionary)

        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        bufferText = view.findViewById(R.id.buffer_text)
        candidateContainer = view.findViewById(R.id.candidate_container)

        setupKeys(view)

        return view
    }

    private fun setupKeys(view: View) {
        val numberKeys = mapOf(
            R.id.key_2 to "2",
            R.id.key_3 to "3",
            R.id.key_4 to "4",
            R.id.key_5 to "5",
            R.id.key_6 to "6",
            R.id.key_7 to "7",
            R.id.key_8 to "8",
            R.id.key_9 to "9"
        )

        for ((id, digit) in numberKeys) {
            view.findViewById<TextView>(id).setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                onDigitPressed(digit)
            }
        }

        view.findViewById<TextView>(R.id.key_del).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            onDeletePressed()
        }

        view.findViewById<TextView>(R.id.key_0).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            onZeroPressed()
        }

        view.findViewById<TextView>(R.id.key_1).setOnClickListener { v ->
            hapticFeedbackManager.performTap(v)
            // Do nothing for now
        }

        view.findViewById<TextView>(R.id.key_star).setOnClickListener { v ->
            hapticFeedbackManager.performSpecialKey(v)
            // Do nothing for now
        }
    }

    private fun onDigitPressed(digit: String) {
        engine.inputDigit(digit)
        updateUi()
    }

    private fun onDeletePressed() {
        if (engine.buffer.isNotEmpty()) {
            engine.backspace()
            updateUi()
        } else {
            // If buffer is empty, send a backspace to the app
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
        }
    }

    private fun onZeroPressed() {
        if (engine.buffer.isEmpty()) {
            currentInputConnection?.commitText(" ", 1)
        } else {
            val candidates = engine.getCandidates()
            if (candidates.isNotEmpty()) {
                val candidate = engine.selectCandidate(0)
                if (candidate != null) {
                    currentInputConnection?.commitText(candidate.text, 1)
                }
            } else {
                currentInputConnection?.commitText(engine.buffer, 1)
                engine.clear()
            }
            updateUi()
        }
    }

    private fun updateUi() {
        bufferText.text = engine.buffer

        candidateContainer.removeAllViews()

        val limit = settingsRepository.getCandidateCount()
        val candidates = engine.getCandidates(limit)

        // Use a primitive view pool for candidate TextViews
        val inflater = LayoutInflater.from(this)

        for ((index, candidate) in candidates.withIndex()) {
            val btn: TextView = if (index < candidateContainer.childCount) {
                candidateContainer.getChildAt(index) as TextView
            } else {
                val newBtn = TextView(this).apply {
                    textSize = 18f
                    setTextColor(android.graphics.Color.parseColor("#333333"))
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    background = getDrawable(R.drawable.candidate_bg)
                    isClickable = true
                    isFocusable = true
                }
                candidateContainer.addView(newBtn)
                newBtn
            }

            btn.visibility = View.VISIBLE
            btn.text = candidate.text
            btn.setOnClickListener { v ->
                hapticFeedbackManager.performTap(v)
                onCandidateClicked(index)
            }
        }

        // Hide unused views
        for (i in candidates.size until candidateContainer.childCount) {
            candidateContainer.getChildAt(i).visibility = View.GONE
        }
    }

    private fun onCandidateClicked(index: Int) {
        val candidate = engine.selectCandidate(index)
        if (candidate != null) {
            currentInputConnection?.commitText(candidate.text, 1)
            updateUi()
        }
    }
}
