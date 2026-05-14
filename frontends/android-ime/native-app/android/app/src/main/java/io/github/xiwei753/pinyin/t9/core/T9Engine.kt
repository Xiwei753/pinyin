package io.github.xiwei753.pinyin.t9.core

class T9Engine {
    var buffer = ""
        private set

    private val dictionary = mapOf(
        "64426" to listOf("你好", "妮好"),
        "748732" to listOf("输入法"),
        "746946" to listOf("拼音"),
        "9466446" to listOf("中国"),
        "866428" to listOf("同步")
    )

    fun inputDigit(digit: String) {
        if (digit.matches(Regex("^[2-9]$"))) {
            buffer += digit
        }
    }

    fun backspace() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.substring(0, buffer.length - 1)
        }
    }

    fun clear() {
        buffer = ""
    }

    fun getCandidates(): List<String> {
        return dictionary[buffer] ?: emptyList()
    }

    fun selectCandidate(index: Int): String? {
        val candidates = getCandidates()
        if (index >= 0 && index < candidates.size) {
            val selected = candidates[index]
            clear()
            return selected
        }
        return null
    }
}
