package io.github.xiwei753.pinyin.t9.testutil

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateOrigin
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import java.io.File
import java.sql.DriverManager
import java.sql.Connection
import java.sql.ResultSet

class TestSQLiteDictionary(dbPath: String) : DictionaryProvider {
    private var connection: Connection? = null

    init {
        // We use sqlite-jdbc to read the db since android.database.sqlite.SQLiteDatabase is not mockable easily without robolectric
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resultSetToCandidates(rs: ResultSet, forcePrefixOrigin: Boolean = false): List<Candidate> {
        val candidates = mutableListOf<Candidate>()
        while (rs.next()) {
            val text = rs.getString("text")
            val pinyin = rs.getString("pinyin")
            val code = rs.getString("code")
            val score = rs.getInt("freq")
            val typeStr = "NORMAL"
            val originStr = rs.getString("origin")

            val type = try { CandidateType.valueOf(typeStr) } catch (e: Exception) { CandidateType.NORMAL }
            val origin = if (forcePrefixOrigin) {
                CandidateOrigin.PREFIX_COMPLETION
            } else {
                try { CandidateOrigin.valueOf(originStr) } catch (e: Exception) { CandidateOrigin.UNKNOWN }
            }

            candidates.add(Candidate(text, code, score, type, "", origin))
        }
        return candidates.distinctBy { it.text }
    }

    override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> {
        val conn = connection ?: return emptyList()
        val stmt = conn.prepareStatement("SELECT text, pinyin, code, freq, origin FROM entries WHERE pinyin = ? ORDER BY freq DESC LIMIT 100")
        stmt.setString(1, pinyinSequence)
        val rs = stmt.executeQuery()
        val res = resultSetToCandidates(rs)
        rs.close()
        stmt.close()
        return res
    }

    override fun getPinyinExactCandidatesMultiple(pinyinSequences: List<String>): Map<String, List<Candidate>> {
        val resultMap = mutableMapOf<String, MutableList<Candidate>>()
        val uniqueSequences = pinyinSequences.distinct()
        if (uniqueSequences.isEmpty()) return emptyMap()

        val conn = connection ?: return emptyMap()
        val chunks = uniqueSequences.chunked(100)

        for (chunk in chunks) {
            val placeholders = chunk.joinToString(",") { "?" }
            val query = "SELECT text, pinyin, code, freq, origin FROM entries WHERE pinyin IN ($placeholders) ORDER BY freq DESC LIMIT ${chunk.size * 50}"
            val stmt = conn.prepareStatement(query)
            for (i in chunk.indices) {
                stmt.setString(i + 1, chunk[i])
            }
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val text = rs.getString("text")
                val pinyin = rs.getString("pinyin")
                val code = rs.getString("code")
                val score = rs.getInt("freq")
                val originStr = rs.getString("origin")

                val origin = try { CandidateOrigin.valueOf(originStr) } catch (e: Exception) { CandidateOrigin.UNKNOWN }

                val candidate = Candidate(text, code, score, CandidateType.NORMAL, "", origin)

                if (!resultMap.containsKey(pinyin)) {
                    resultMap[pinyin] = mutableListOf()
                }
                resultMap[pinyin]!!.add(candidate)
            }
            rs.close()
            stmt.close()
        }

        val finalMap = mutableMapOf<String, List<Candidate>>()
        for ((pinyin, list) in resultMap) {
            finalMap[pinyin] = list.distinctBy { it.text }.take(100)
        }

        return finalMap
    }

    override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
        val conn = connection ?: return emptyList()
        val stmt = conn.prepareStatement("SELECT text, pinyin, code, freq, origin FROM entries WHERE pinyin LIKE ? ORDER BY freq DESC LIMIT 100")
        stmt.setString(1, "$pinyinPrefix %")
        val rs = stmt.executeQuery()
        val spaceMatches = resultSetToCandidates(rs, forcePrefixOrigin = true)
        rs.close()
        stmt.close()

        val stmt2 = conn.prepareStatement("SELECT text, pinyin, code, freq, origin FROM entries WHERE pinyin = ? ORDER BY freq DESC LIMIT 100")
        stmt2.setString(1, pinyinPrefix)
        val rs2 = stmt2.executeQuery()
        val exactMatches = resultSetToCandidates(rs2)
        rs2.close()
        stmt2.close()

        return (exactMatches + spaceMatches).distinctBy { it.text }.sortedByDescending { it.score }.take(100)
    }

    override fun getSingleSyllableCandidates(syllable: String): List<Candidate> {
        val conn = connection ?: return emptyList()
        val stmt = conn.prepareStatement("SELECT text, pinyin, code, freq, origin FROM entries WHERE pinyin = ? AND syllable_count = 1 ORDER BY freq DESC LIMIT 100")
        stmt.setString(1, syllable)
        val rs = stmt.executeQuery()
        val res = resultSetToCandidates(rs)
        rs.close()
        stmt.close()
        return res
    }

    override fun getCandidates(code: String): List<Candidate> {
        return getPrefixCandidates(code)
    }

    override fun getExactCandidates(code: String): List<Candidate> {
        val conn = connection ?: return emptyList()
        val stmt = conn.prepareStatement("SELECT text, pinyin, code, freq, origin FROM entries WHERE code = ? ORDER BY freq DESC LIMIT 100")
        stmt.setString(1, code)
        val rs = stmt.executeQuery()
        val res = resultSetToCandidates(rs)
        rs.close()
        stmt.close()
        return res
    }

    override fun getPrefixCandidates(code: String): List<Candidate> {
        val conn = connection ?: return emptyList()
        val stmt = conn.prepareStatement("SELECT text, pinyin, code, freq, origin FROM entries WHERE code LIKE ? ORDER BY freq DESC LIMIT 100")
        stmt.setString(1, "$code%")
        val rs = stmt.executeQuery()
        val res = resultSetToCandidates(rs, forcePrefixOrigin = true)
        rs.close()
        stmt.close()
        return res
    }

    fun getLoadedWordCount(): Int {
        val conn = connection ?: return 0
        val stmt = conn.createStatement()
        val rs = stmt.executeQuery("SELECT COUNT(*) FROM entries")
        val count = if (rs.next()) rs.getInt(1) else 0
        rs.close()
        stmt.close()
        return count
    }

    fun close() {
        connection?.close()
    }
}
