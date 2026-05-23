package io.github.xiwei753.pinyin.t9.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.system.measureTimeMillis

class SQLiteDictionary private constructor(
    private var db: SQLiteDatabase?,
    val isFallback: Boolean,
    val loadedWordCount: Int
) : DictionaryProvider {

    companion object {
        private const val DB_NAME = "t9_dict.db"

        fun prepareAndOpen(context: Context): SQLiteDictionary {
            val dbFile = context.getDatabasePath(DB_NAME)

            try {
                // Very simple versioning: check if asset length matches local file length
                val assetStream = context.assets.open(DB_NAME)
                val assetSize = assetStream.available()
                assetStream.close()

                if (!dbFile.exists() || dbFile.length() != assetSize.toLong()) {
                    Log.d("SQLiteDictionary", "Copying database from assets. Old size: ${if(dbFile.exists()) dbFile.length() else 0}, New size: $assetSize")
                    dbFile.parentFile?.mkdirs()

                    val tmpFile = File(dbFile.absolutePath + ".tmp")
                    copyDatabase(context, tmpFile)

                    // Atomic rename to prevent partial corruption
                    if (tmpFile.renameTo(dbFile)) {
                        Log.d("SQLiteDictionary", "Successfully replaced db file.")
                    } else {
                        Log.e("SQLiteDictionary", "Failed to rename tmp file to db file.")
                        throw RuntimeException("Failed to rename temporary database file.")
                    }
                }

                val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

                var loadedWordCount = 0
                var isFallback = false
                val cursor = db.rawQuery("SELECT COUNT(*) FROM entries", null)
                if (cursor != null && cursor.moveToFirst()) {
                    loadedWordCount = cursor.getInt(0)
                    cursor.close()
                }
                Log.d("SQLiteDictionary", "Successfully opened db with $loadedWordCount entries")
                return SQLiteDictionary(db, isFallback, loadedWordCount)
            } catch (e: Exception) {
                Log.e("SQLiteDictionary", "Error opening database", e)
                return SQLiteDictionary(null, true, 2) // Fallback mode word count
            }
        }

        private fun copyDatabase(context: Context, dbFile: File) {
            val inputStream: InputStream = context.assets.open(DB_NAME)
            val outputStream = FileOutputStream(dbFile)
            val buffer = ByteArray(1024 * 32)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        }
    }

    private fun cursorToCandidates(cursor: android.database.Cursor, forcePrefixOrigin: Boolean = false): List<Candidate> {
        val candidates = mutableListOf<Candidate>()
        if (cursor.moveToFirst()) {
            val textIdx = cursor.getColumnIndex("text")
            val pinyinIdx = cursor.getColumnIndex("pinyin")
            val codeIdx = cursor.getColumnIndex("code")
            val freqIdx = cursor.getColumnIndex("freq")

            val originIdx = cursor.getColumnIndex("origin")

            do {
                val text = cursor.getString(textIdx)
                val pinyin = cursor.getString(pinyinIdx)
                val code = cursor.getString(codeIdx)
                val freq = cursor.getInt(freqIdx)

                val originStr = cursor.getString(originIdx)

                val type = CandidateType.NORMAL
                val origin = if (forcePrefixOrigin) {
                    CandidateOrigin.PREFIX_COMPLETION
                } else {
                    try { CandidateOrigin.valueOf(originStr) } catch (e: Exception) { CandidateOrigin.UNKNOWN }
                }

                candidates.add(Candidate(text, code, freq, CandidateType.NORMAL, "", origin))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return candidates.distinctBy { it.text }
    }

    override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> {
        if (isFallback) {
            if (pinyinSequence == "ni hao") return getFallbackCandidates().filter { it.text == "你好" }
            if (pinyinSequence == "shu ru fa") return getFallbackCandidates().filter { it.text == "输入法" }
            return emptyList()
        }
        val db = db ?: return emptyList()
        val cursor = db.rawQuery(
            "SELECT text, pinyin, code, freq,  origin FROM entries WHERE pinyin = ? ORDER BY freq DESC LIMIT 100",
            arrayOf(pinyinSequence)
        )
        return cursorToCandidates(cursor)
    }

    override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
        if (isFallback) {
            if ("ni hao".startsWith(pinyinPrefix)) return getFallbackCandidates().filter { it.text == "你好" }.map { it.copy(origin = CandidateOrigin.PREFIX_COMPLETION) }
            if ("shu ru fa".startsWith(pinyinPrefix)) return getFallbackCandidates().filter { it.text == "输入法" }.map { it.copy(origin = CandidateOrigin.PREFIX_COMPLETION) }
            return emptyList()
        }
        val db = db ?: return emptyList()
        val cursor = db.rawQuery(
            "SELECT text, pinyin, code, freq,  origin FROM entries WHERE pinyin LIKE ? ORDER BY freq DESC LIMIT 100",
            arrayOf("$pinyinPrefix %")
        )
        val spaceMatches = cursorToCandidates(cursor, forcePrefixOrigin = true)

        val cursor2 = db.rawQuery(
            "SELECT text, pinyin, code, freq,  origin FROM entries WHERE pinyin = ? ORDER BY freq DESC LIMIT 100",
            arrayOf(pinyinPrefix)
        )
        val exactMatches = cursorToCandidates(cursor2) // keep exact origins

        return (exactMatches + spaceMatches).distinctBy { it.text }.sortedByDescending { it.score }.take(100)
    }

    override fun getSingleSyllableCandidates(syllable: String): List<Candidate> {
        if (isFallback) return emptyList()
        val db = db ?: return emptyList()
        val cursor = db.rawQuery(
            "SELECT text, pinyin, code, freq,  origin FROM entries WHERE pinyin = ? AND syllable_count = 1 ORDER BY freq DESC LIMIT 100",
            arrayOf(syllable)
        )
        return cursorToCandidates(cursor)
    }

    override fun getCandidates(code: String): List<Candidate> {
        return getPrefixCandidates(code)
    }

    override fun getExactCandidates(code: String): List<Candidate> {
        if (isFallback) {
            if (code == "64426") return getFallbackCandidates().filter { it.text == "你好" }
            if (code == "7487832") return getFallbackCandidates().filter { it.text == "输入法" }
            return emptyList()
        }
        val db = db ?: return emptyList()
        val cursor = db.rawQuery(
            "SELECT text, pinyin, code, freq,  origin FROM entries WHERE code = ? ORDER BY freq DESC LIMIT 100",
            arrayOf(code)
        )
        return cursorToCandidates(cursor)
    }

    override fun getPrefixCandidates(code: String): List<Candidate> {
        if (isFallback) {
            val res = mutableListOf<Candidate>()
            if ("64426".startsWith(code)) res.addAll(getFallbackCandidates().filter { it.text == "你好" }.map { it.copy(origin = CandidateOrigin.PREFIX_COMPLETION) })
            if ("7487832".startsWith(code)) res.addAll(getFallbackCandidates().filter { it.text == "输入法" }.map { it.copy(origin = CandidateOrigin.PREFIX_COMPLETION) })
            return res
        }
        val db = db ?: return emptyList()
        val cursor = db.rawQuery(
            "SELECT text, pinyin, code, freq,  origin FROM entries WHERE code LIKE ? ORDER BY freq DESC LIMIT 100",
            arrayOf("$code%")
        )
        return cursorToCandidates(cursor, forcePrefixOrigin = true)
    }

    private fun getFallbackCandidates(): List<Candidate> {
        return listOf(
            Candidate("你好", "64426", 100000, CandidateType.NORMAL, "", CandidateOrigin.EXACT_PHRASE),
            Candidate("输入法", "7487832", 90000, CandidateType.NORMAL, "", CandidateOrigin.EXACT_PHRASE)
        )
    }

    fun close() {
        db?.close()
        db = null
    }
}
