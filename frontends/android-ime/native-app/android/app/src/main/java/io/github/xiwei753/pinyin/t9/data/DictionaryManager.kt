package io.github.xiwei753.pinyin.t9.data

import android.content.Context
import kotlin.concurrent.thread
import io.github.xiwei753.pinyin.t9.core.Candidate

object DictionaryManager {
    @Volatile
    var instance: BuiltinDictionary? = null
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
                instance = BuiltinDictionary.fromAssets(appContext)
            } finally {
                isLoading = false
            }
        }
    }

    // Proxy provider to avoid blocking or null checks everywhere
    fun getProvider(context: Context): DictionaryProvider {
        initAsync(context)
        return AsyncDictionaryProxy
    }

    // For testing purposes
    fun reset() {
        instance = null
        isLoading = false
    }
}

object AsyncDictionaryProxy : DictionaryProvider {
    override fun getPinyinExactCandidates(pinyinSequence: String) =
        DictionaryManager.instance?.getPinyinExactCandidates(pinyinSequence) ?: emptyList()

    override fun getPinyinPrefixCandidates(pinyinPrefix: String) =
        DictionaryManager.instance?.getPinyinPrefixCandidates(pinyinPrefix) ?: emptyList()

    override fun getSingleSyllableCandidates(syllable: String) =
        DictionaryManager.instance?.getSingleSyllableCandidates(syllable) ?: emptyList()

    override fun getCandidates(code: String) =
        DictionaryManager.instance?.getCandidates(code) ?: emptyList()

    override fun getExactCandidates(code: String) =
        DictionaryManager.instance?.getExactCandidates(code) ?: emptyList()

    override fun getPrefixCandidates(code: String) =
        DictionaryManager.instance?.getPrefixCandidates(code) ?: emptyList()
}
