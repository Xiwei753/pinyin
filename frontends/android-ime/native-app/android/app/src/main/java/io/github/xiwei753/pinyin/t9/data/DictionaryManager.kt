package io.github.xiwei753.pinyin.t9.data

import android.content.Context

sealed class DictionaryState {
    object NotStarted : DictionaryState()
    object Preparing : DictionaryState()
    data class Ready(val dictionary: SQLiteDictionary) : DictionaryState()
    data class Fallback(val error: Throwable?) : DictionaryState()
}

interface DictionaryStateListener {
    fun onStateChanged(state: DictionaryState)
}

object DictionaryManager {
    @Volatile
    var state: DictionaryState = DictionaryState.NotStarted
        private set

    private val listeners = mutableListOf<DictionaryStateListener>()

    // For legacy usages that were casting to SQLiteDictionary. They should be updated to observe state,
    // but in cases they haven't been fully migrated, we can return the dictionary if Ready, or a fallback.
    // However, the issue explicitly prohibits fake empty proxies.

    fun registerListener(listener: DictionaryStateListener) {
        synchronized(listeners) {
            listeners.add(listener)
            listener.onStateChanged(state)
        }
    }

    fun unregisterListener(listener: DictionaryStateListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    private fun notifyStateChanged(newState: DictionaryState) {
        synchronized(listeners) {
            state = newState
            for (listener in listeners) {
                listener.onStateChanged(newState)
            }
        }
    }

    @Synchronized
    fun prepareAsync(context: Context) {
        if (state is DictionaryState.Ready || state is DictionaryState.Preparing) {
            return
        }

        notifyStateChanged(DictionaryState.Preparing)

        val appContext = context.applicationContext
        Thread {
            try {
                val dict = SQLiteDictionary.prepareAndOpen(appContext!!)
                if (dict.isFallback) {
                    notifyStateChanged(DictionaryState.Fallback(null))
                } else {
                    notifyStateChanged(DictionaryState.Ready(dict))
                }
            } catch (e: Exception) {
                 // it will return fallback mode dictionary on failure internally
                notifyStateChanged(DictionaryState.Fallback(e))
            }
        }.start()
    }

    fun getReadyProviderOrNull(): SQLiteDictionary? {
        val s = state
        return if (s is DictionaryState.Ready) s.dictionary else null
    }

    fun getProviderBlocking(context: Context): DictionaryProvider {
        val isMainThread = android.os.Looper.myLooper() == android.os.Looper.getMainLooper()
        if (isMainThread) {
            throw IllegalStateException("getProviderBlocking must not be called on the main thread")
        }

        synchronized(this) {
            if (state is DictionaryState.NotStarted) {
                val dict = SQLiteDictionary.prepareAndOpen(context.applicationContext)
                if (dict.isFallback) {
                    notifyStateChanged(DictionaryState.Fallback(null))
                } else {
                    notifyStateChanged(DictionaryState.Ready(dict))
                }
            }
        }

        // If it's preparing, we might have to wait... For tests we can wait or just initialize directly.
        // If preparing was started by another thread, block until ready.
        var s = state
        while (s is DictionaryState.Preparing) {
            Thread.sleep(50)
            s = state
        }

        return when (s) {
            is DictionaryState.Ready -> s.dictionary
            is DictionaryState.Fallback -> SQLiteDictionary.prepareAndOpen(context.applicationContext) // return fallback proxy
            else -> throw IllegalStateException("Unexpected state $s")
        }
    }

    // For testing purposes
    fun reset() {
        val s = state
        if (s is DictionaryState.Ready) {
            s.dictionary.close()
        } else if (s is DictionaryState.Fallback) {
            // Nothing to close since it doesn't hold DB if it's purely fallback error state.
            // But if it holds fallback dict, we could close it if we stored it.
        }
        synchronized(listeners) {
            state = DictionaryState.NotStarted
        }
    }

    // Legacy getProvider for smooth migration, must be updated to not block or not be called on main
    // The issue says: "getProvider(context) 会同步创建 SQLiteDictionary... 可能重新卡顿... 正确做法是：要把“DB 准备”与“候选查询”分开"
    // And "getProviderBlocking() 如果必须保留，只能在非主线程调用"
    @Synchronized
    fun getProvider(context: Context): DictionaryProvider {
        // According to requirements: "getProviderBlocking() 如果必须保留，只能在非主线程调用；主线程调用要有保护或明确禁止。"
        // But MainActivity calls it: `val dict = DictionaryManager.getProvider(this) as SQLiteDictionary`
        // So we will modify MainActivity and remove this legacy getProvider.
        throw UnsupportedOperationException("Use prepareAsync and observe state instead, or getProviderBlocking on a background thread.")
    }
}
