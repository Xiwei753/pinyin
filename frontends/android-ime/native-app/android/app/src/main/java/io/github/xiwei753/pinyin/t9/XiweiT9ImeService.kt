package io.github.xiwei753.pinyin.t9

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.TextView

class XiweiT9ImeService : InputMethodService() {

    private lateinit var bufferText: TextView
    private lateinit var candidateBtn: Button

    private var buffer = ""

    private val dictionary = mapOf(
        "64426" to "你好",
        "748732" to "输入法",
        "746946" to "拼音",
        "9466446" to "中国",
        "866428" to "同步"
    )

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        bufferText = view.findViewById(R.id.buffer_text)
        candidateBtn = view.findViewById(R.id.candidate_btn)

        candidateBtn.setOnClickListener {
            onCandidateClicked()
        }

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
        buffer += digit
        updateUi()
    }

    private fun onDeletePressed() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.substring(0, buffer.length - 1)
            updateUi()
        } else {
            // If buffer is empty, send a backspace to the app
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
        }
    }

    private fun onZeroPressed() {
        if (buffer.isEmpty()) {
            currentInputConnection?.commitText(" ", 1)
        } else {
            // Can be used to cycle candidates later, ignoring for now
        }
    }

    private fun updateUi() {
        bufferText.text = buffer

        val candidate = dictionary[buffer]
        if (candidate != null) {
            candidateBtn.text = candidate
            candidateBtn.visibility = View.VISIBLE
        } else {
            candidateBtn.visibility = View.GONE
        }
    }

    private fun onCandidateClicked() {
        val candidate = candidateBtn.text.toString()
        if (candidate.isNotEmpty()) {
            currentInputConnection?.commitText(candidate, 1)
            buffer = ""
            updateUi()
        }
    }
}
