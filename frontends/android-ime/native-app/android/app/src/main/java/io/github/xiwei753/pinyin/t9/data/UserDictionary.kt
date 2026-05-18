package io.github.xiwei753.pinyin.t9.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType

open class UserDictionary private constructor(
    private var db: SQLiteDatabase?
) {
    companion object {
        private const val DB_NAME = "user_dict.db"

        @Volatile
        private var instance: UserDictionary? = null

        fun getInstance(context: Context): UserDictionary {
            return instance ?: synchronized(this) {
                instance ?: init(context).also { instance = it }
            }
        }

        private fun init(context: Context): UserDictionary {
            val dbFile = context.getDatabasePath(DB_NAME)
            dbFile.parentFile?.mkdirs()

            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS user_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL,
                    pinyin TEXT NOT NULL,
                    count INTEGER DEFAULT 1,
                    last_used INTEGER DEFAULT 0
                )
            """)

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_pinyin ON user_entries (pinyin)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_text_pinyin ON user_entries (text, pinyin)")

            Log.d("UserDictionary", "Initialized user dictionary")
            return UserDictionary(db)
        }
    }

    open fun recordSelection(text: String, pinyin: String) {
        if (db == null) return

        if (!isValidLearning(text, pinyin)) {
            return
        }

        val now = System.currentTimeMillis()
        try {
            db?.execSQL("""
                INSERT INTO user_entries (text, pinyin, count, last_used)
                VALUES (?, ?, 1, ?)
                ON CONFLICT(text, pinyin) DO UPDATE SET
                    count = count + 1,
                    last_used = ?
            """, arrayOf(text, pinyin, now, now))
        } catch (e: Exception) {
            Log.e("UserDictionary", "Error recording selection", e)
        }
    }

    open fun getUserCandidates(pinyin: String): List<Candidate> {
        if (db == null) return emptyList()

        val cursor = db?.rawQuery("""
            SELECT text, pinyin, count, last_used
            FROM user_entries
            WHERE pinyin = ?
            ORDER BY count DESC, last_used DESC
            LIMIT 20
        """, arrayOf(pinyin))

        val candidates = mutableListOf<Candidate>()
        if (cursor?.moveToFirst() == true) {
            val textIdx = cursor.getColumnIndex("text")
            val pinyinIdx = cursor.getColumnIndex("pinyin")
            val countIdx = cursor.getColumnIndex("count")

            do {
                val text = cursor.getString(textIdx)
                val pinyin = cursor.getString(pinyinIdx)
                val count = cursor.getInt(countIdx)

                val score = 200000 + count * 10000
                val type = if (text.length == 1) CandidateType.SINGLE_CHAR else CandidateType.NORMAL

                candidates.add(
                    Candidate(text, pinyin, score, type, pinyin, CandidateOrigin.USER_HISTORY)
                )
            } while (cursor.moveToNext())
            cursor.close()
        }
        return candidates
    }

    fun getUserBoost(pinyin: String, text: String): Int {
        if (db == null) return 0

        val cursor = db?.rawQuery("""
            SELECT count FROM user_entries
            WHERE pinyin = ? AND text = ?
            LIMIT 1
        """, arrayOf(pinyin, text))

        val boost = if (cursor?.moveToFirst() == true) {
            val count = cursor.getInt(0)
            cursor.close()
            count * 15000
        } else {
            0
        }
        return boost
    }

    open fun clearUserDictionary() {
        db?.execSQL("DELETE FROM user_entries")
        Log.d("UserDictionary", "Cleared user dictionary")
    }

    fun close() {
        db?.close()
        db = null
        instance = null
    }

    private fun isValidLearning(text: String, pinyin: String): Boolean {
        if (text.isEmpty() || pinyin.isEmpty()) return false
        if (text.matches(Regex("^[0-9]+$"))) return false
        if (text.matches(Regex("^[a-zA-Z\\s]+$"))) return false
        if (text.contains(" ")) return false
        return true
    }
}