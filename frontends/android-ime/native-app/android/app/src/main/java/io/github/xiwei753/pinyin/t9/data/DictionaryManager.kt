package io.github.xiwei753.pinyin.t9.data

import android.content.Context
import kotlin.concurrent.thread

object DictionaryManager {
    @Volatile
    var instance: SQLiteDictionary? = null
        private set

    @Volatile
    var isLoading = false
        private set

    fun initAsync(context: Context) {
        if (instance != null || isLoading) return
        isLoading = true
        val appContext = context.applicationContext
        thread(start = true, name = "DictLoaderThread") {
            try {
                instance = SQLiteDictionary(appContext)
            } finally {
                isLoading = false
            }
        }
    }

    // Provider getter will block slightly if needed, but since it's now lightweight it shouldn't be long.
    // However, keeping the async nature, we'll return a proxy that returns empty until loaded.
    fun getProvider(context: Context): DictionaryProvider {
        initAsync(context)
        return object : DictionaryProvider {
            override fun getPinyinExactCandidates(pinyinSequence: String) =
                instance?.getPinyinExactCandidates(pinyinSequence) ?: emptyList()

            override fun getPinyinPrefixCandidates(pinyinPrefix: String) =
                instance?.getPinyinPrefixCandidates(pinyinPrefix) ?: emptyList()

            override fun getSingleSyllableCandidates(syllable: String) =
                instance?.getSingleSyllableCandidates(syllable) ?: emptyList()

            override fun getCandidates(code: String) =
                instance?.getCandidates(code) ?: emptyList()

            override fun getExactCandidates(code: String) =
                instance?.getExactCandidates(code) ?: emptyList()

            override fun getPrefixCandidates(code: String) =
                instance?.getPrefixCandidates(code) ?: emptyList()
        }
    }

    // For testing purposes
    fun reset() {
        instance?.close()
        instance = null
        isLoading = false
    }
}
