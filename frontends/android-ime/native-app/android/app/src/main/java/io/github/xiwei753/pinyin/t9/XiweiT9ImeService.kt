package io.github.xiwei753.pinyin.t9

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary

class XiweiT9ImeService : InputMethodService() {

    private lateinit var bufferText: TextView
    private lateinit var candidateContainer: LinearLayout

    private lateinit var dictionary: BuiltinDictionary
    private lateinit var engine: T9Engine

    override fun onCreateInputView(): View {
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
            view.findViewById<Button>(id).setOnClickListener {
                onDigitPressed(digit)
            }
        }

        view.findViewById<Button>(R.id.key_del).setOnClickListener {
            onDeletePressed()
        }

        view.findViewById<Button>(R.id.key_0).setOnClickListener {
            onZeroPressed()
        }

        view.findViewById<Button>(R.id.key_1).setOnClickListener {
            // Do nothing for now
        }

        view.findViewById<Button>(R.id.key_star).setOnClickListener {
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
            // Can be used to cycle candidates later, ignoring for now
        }
    }

    private fun updateUi() {
        bufferText.text = engine.buffer

        candidateContainer.removeAllViews()

        val candidates = engine.getCandidates()
        for ((index, candidate) in candidates.withIndex()) {
            val btn = Button(this).apply {
                text = candidate.text
                textSize = 16f
                minHeight = 0
                minimumHeight = 0
                setPadding(32, 16, 32, 16)
                setOnClickListener {
                    onCandidateClicked(index)
                }
            }
            candidateContainer.addView(btn)
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
