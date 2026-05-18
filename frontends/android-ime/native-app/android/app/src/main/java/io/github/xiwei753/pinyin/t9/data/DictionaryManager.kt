package io.github.xiwei753.pinyin.t9.data

import android.content.Context

object DictionaryManager {
    @Volatile
    var instance: SQLiteDictionary? = null
        private set

    @Synchronized
    fun getProvider(context: Context): DictionaryProvider {
        if (instance == null) {
            instance = SQLiteDictionary(context.applicationContext)
        }
        return instance!!
    }

    // For testing purposes
    fun reset() {
        instance?.close()
        instance = null
    }
}
