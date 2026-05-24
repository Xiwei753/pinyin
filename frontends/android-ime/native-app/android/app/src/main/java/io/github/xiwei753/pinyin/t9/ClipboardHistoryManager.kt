package io.github.xiwei753.pinyin.t9

import android.content.Context
import org.json.JSONArray

object ClipboardHistoryManager {
    private const val PREFS_NAME = "xiwei_clipboard_history"
    private const val KEY_HISTORY = "history"
    private const val MAX_ITEMS = 20

    fun getHistory(context: Context): List<String> {
        val list = mutableListOf<String>()
        try {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = sharedPrefs?.getString(KEY_HISTORY, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Throwable) {}
        return list
    }

    fun addText(context: Context, text: String) {
        if (text.isEmpty()) return
        val list = getHistory(context).toMutableList()
        list.remove(text)
        list.add(0, text)
        if (list.size > MAX_ITEMS) {
            list.removeAt(list.size - 1)
        }
        saveHistory(context, list)
    }

    private fun saveHistory(context: Context, list: List<String>) {
        try {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonArray = JSONArray(list)
            sharedPrefs?.edit()?.putString(KEY_HISTORY, jsonArray.toString())?.apply()
        } catch (e: Throwable) {}
    }
}
