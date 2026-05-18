package io.github.xiwei753.pinyin.t9

import android.util.Log

interface T9DebugLogger {
    fun log(tag: String, message: String)
}

class AndroidDebugLogger : T9DebugLogger {
    override fun log(tag: String, message: String) {
        Log.d(tag, message)
        T9DebugLogStore.append(tag, message)
    }
}

object T9DebugLogStore {
    private val buffer = StringBuilder()
    private const val MAX_CHARS = 200_000

    fun append(tag: String, message: String) {
        synchronized(buffer) {
            val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
                .format(java.util.Date())
            buffer.append("[$time] $tag: $message\n")
            if (buffer.length > MAX_CHARS) {
                val excess = buffer.length - MAX_CHARS + 10000
                buffer.delete(0, excess)
            }
        }
    }

    fun dump(): String = synchronized(buffer) { buffer.toString() }

    fun clear() {
        synchronized(buffer) { buffer.clear() }
    }

    fun isEmpty(): Boolean = synchronized(buffer) { buffer.isEmpty() }
}
