package io.github.xiwei753.pinyin.t9

import android.os.Handler
import android.os.Looper

class DeleteRepeatController(
    private val onDelete: () -> Unit
) {
    private val deleteHandler = Handler(Looper.getMainLooper())
    private var deleteLongPressRunning = false
    private val deleteRepeatDelay = 80L

    fun start() {
        deleteLongPressRunning = true
        deleteHandler.post(object : Runnable {
            override fun run() {
                if (!deleteLongPressRunning) return
                onDelete()
                deleteHandler.postDelayed(this, deleteRepeatDelay)
            }
        })
    }

    fun stop() {
        deleteLongPressRunning = false
        deleteHandler.removeCallbacksAndMessages(null)
    }

    fun destroy() {
        stop()
    }
}
