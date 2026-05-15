package io.github.xiwei753.pinyin.t9.data

import android.content.Context

object DictionaryManager {
    private var instance: BuiltinDictionary? = null

    fun getInstance(context: Context): BuiltinDictionary {
        if (instance == null) {
            instance = BuiltinDictionary.fromAssets(context.applicationContext)
        }
        return instance!!
    }

    // For testing purposes
    fun reset() {
        instance = null
    }
}
