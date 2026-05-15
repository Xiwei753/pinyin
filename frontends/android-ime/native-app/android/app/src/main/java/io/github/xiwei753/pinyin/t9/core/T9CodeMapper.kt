package io.github.xiwei753.pinyin.t9.core

object T9CodeMapper {

    fun toCode(pinyin: String): String {
        val sb = java.lang.StringBuilder()
        for (char in pinyin.lowercase()) {
            when (char) {
                'a', 'b', 'c' -> sb.append("2")
                'd', 'e', 'f' -> sb.append("3")
                'g', 'h', 'i' -> sb.append("4")
                'j', 'k', 'l' -> sb.append("5")
                'm', 'n', 'o' -> sb.append("6")
                'p', 'q', 'r', 's' -> sb.append("7")
                't', 'u', 'v' -> sb.append("8")
                'w', 'x', 'y', 'z' -> sb.append("9")
                // Ignore spaces, quotes, hyphens, and other invalid characters
                else -> {}
            }
        }
        return sb.toString()
    }
}
