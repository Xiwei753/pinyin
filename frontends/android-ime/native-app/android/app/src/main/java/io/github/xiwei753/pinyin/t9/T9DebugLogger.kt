package io.github.xiwei753.pinyin.t9

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val memoryBuffer = StringBuilder()
    private const val MAX_MEMORY_CHARS = 20_000
    private const val MAX_FILE_BYTES = 200_000L
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun initFileLogging(dir: File) {
        synchronized(memoryBuffer) {
            logFile = File(dir, "t9_debug.log")
        }
    }

    fun append(tag: String, message: String) {
        val time = dateFormat.format(Date())
        val line = "[$time] $tag: $message\n"
        synchronized(memoryBuffer) {
            memoryBuffer.append(line)
            if (memoryBuffer.length > MAX_MEMORY_CHARS) {
                val excess = memoryBuffer.length - MAX_MEMORY_CHARS + 2000
                memoryBuffer.delete(0, excess)
            }
        }
        val file = logFile ?: return
        try {
            file.parentFile?.mkdirs()
            file.appendText(line)
            if (file.length() > MAX_FILE_BYTES) {
                val content = file.readText()
                val cutPoint = (content.length * 0.3).toInt()
                file.writeText(content.substring(cutPoint))
            }
        } catch (_: Exception) {
        }
    }

    fun dumpFromFile(): String {
        val file = logFile
        if (file != null && file.exists()) {
            return try {
                file.readText()
            } catch (_: Exception) {
                dumpMemory()
            }
        }
        return dumpMemory()
    }

    fun dumpMemory(): String = synchronized(memoryBuffer) { memoryBuffer.toString() }

    fun dump(): String = dumpFromFile()

    fun clear() {
        synchronized(memoryBuffer) {
            memoryBuffer.clear()
        }
        val file = logFile
        if (file != null && file.exists()) {
            try {
                file.delete()
            } catch (_: Exception) {
            }
        }
    }

    fun isEmpty(): Boolean = synchronized(memoryBuffer) { memoryBuffer.isEmpty() }
}
