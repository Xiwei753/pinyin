package io.github.xiwei753.pinyin.t9.data

import android.content.Context
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9CodeMapper
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader

class BuiltinDictionary : DictionaryProvider {
    private val prefixDictionary: Map<String, List<Candidate>>
    private val exactDictionary: Map<String, List<Candidate>>

    constructor(lines: List<String>) {
        val result = parseLines(lines)
        prefixDictionary = result.first
        exactDictionary = result.second
    }

    constructor(inputStream: InputStream) {
        val lines = mutableListOf<String>()
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    lines.add(line)
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            // Ignore error
        }
        val result = parseLines(lines)
        prefixDictionary = result.first
        exactDictionary = result.second
    }

    private fun parseLines(lines: List<String>): Pair<Map<String, List<Candidate>>, Map<String, List<Candidate>>> {
        val prefixMap = mutableMapOf<String, MutableList<Candidate>>()
        val exactMap = mutableMapOf<String, MutableList<Candidate>>()
        var hasValidEntries = false

        try {
            for (line in lines) {
                val parts = line.split("\t")
                if (parts.size >= 3) {
                    val text = parts[0]
                    val pinyin = parts[1]
                    val score = parts[2].toIntOrNull() ?: 0
                    val code = T9CodeMapper.toCode(pinyin)

                    if (code.isNotEmpty()) {
                        hasValidEntries = true
                        val candidate = Candidate(text, code, score)
                        // Add to exact map
                        if (!exactMap.containsKey(code)) {
                            exactMap[code] = mutableListOf()
                        }
                        exactMap[code]?.add(candidate)

                        // Add to all prefixes
                        for (i in 1..code.length) {
                            val prefix = code.substring(0, i)
                            if (!prefixMap.containsKey(prefix)) {
                                prefixMap[prefix] = mutableListOf()
                            }
                            prefixMap[prefix]?.add(candidate)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Error during parsing
        }

        // If map is empty or we had an error, use fallback
        if (!hasValidEntries) {
            val fallbackCandidates = listOf(
                Candidate("你好", "64426", 100000),
                Candidate("输入法", "7487832", 90000)
            )
            for (candidate in fallbackCandidates) {
                if (!exactMap.containsKey(candidate.code)) {
                    exactMap[candidate.code] = mutableListOf()
                }
                exactMap[candidate.code]?.add(candidate)

                for (i in 1..candidate.code.length) {
                    val prefix = candidate.code.substring(0, i)
                    if (!prefixMap.containsKey(prefix)) {
                        prefixMap[prefix] = mutableListOf()
                    }
                    prefixMap[prefix]?.add(candidate)
                }
            }
        }

        val finalPrefixMap = mutableMapOf<String, List<Candidate>>()
        for ((prefix, candidates) in prefixMap) {
            val distinctSorted = candidates.distinctBy { it.text }
                .sortedByDescending { it.score }
                .take(100)
            finalPrefixMap[prefix] = distinctSorted
        }

        val finalExactMap = mutableMapOf<String, List<Candidate>>()
        for ((code, candidates) in exactMap) {
            val distinctSorted = candidates.distinctBy { it.text }
                .sortedByDescending { it.score }
                .take(100)
            finalExactMap[code] = distinctSorted
        }

        return Pair(finalPrefixMap, finalExactMap)
    }

    override fun getCandidates(code: String): List<Candidate> {
        return getPrefixCandidates(code)
    }

    override fun getExactCandidates(code: String): List<Candidate> {
        if (code.isEmpty()) return emptyList()
        return exactDictionary[code] ?: emptyList()
    }

    override fun getPrefixCandidates(code: String): List<Candidate> {
        if (code.isEmpty()) return emptyList()
        return prefixDictionary[code] ?: emptyList()
    }

    companion object {
        fun fromAssets(context: Context): BuiltinDictionary {
            return try {
                val inputStream = context.assets.open("t9_source_dict.tsv")
                BuiltinDictionary(inputStream)
            } catch (e: Exception) {
                BuiltinDictionary(emptyList()) // fallback
            }
        }
    }
}
