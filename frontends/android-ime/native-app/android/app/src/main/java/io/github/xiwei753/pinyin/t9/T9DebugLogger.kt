package io.github.xiwei753.pinyin.t9

import android.util.Log

interface T9DebugLogger {
    fun log(tag: String, message: String)
}

class AndroidDebugLogger : T9DebugLogger {
    override fun log(tag: String, message: String) {
        Log.d(tag, message)
    }
}
