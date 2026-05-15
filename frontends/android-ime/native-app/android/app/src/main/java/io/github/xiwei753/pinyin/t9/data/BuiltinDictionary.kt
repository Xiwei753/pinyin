package io.github.xiwei753.pinyin.t9.data

import android.content.Context
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.T9CodeMapper
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader

class BuiltinDictionary : DictionaryProvider {
    private val prefixDictionary: Map<String, List<Candidate>>

    constructor(lines: List<String>) {
        prefixDictionary = parseLines(lines)
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
        prefixDictionary = parseLines(lines)
    }

    private fun parseLines(lines: List<String>): Map<String, List<Candidate>> {
        val map = mutableMapOf<String, MutableList<Candidate>>()
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
                        // Add to all prefixes
                        for (i in 1..code.length) {
                            val prefix = code.substring(0, i)
                            if (!map.containsKey(prefix)) {
                                map[prefix] = mutableListOf()
                            }
                            map[prefix]?.add(candidate)
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
                for (i in 1..candidate.code.length) {
                    val prefix = candidate.code.substring(0, i)
                    if (!map.containsKey(prefix)) {
                        map[prefix] = mutableListOf()
                    }
                    map[prefix]?.add(candidate)
                }
            }
        }

        val finalMap = mutableMapOf<String, List<Candidate>>()
        for ((prefix, candidates) in map) {
            // Deduplicate if multiple full codes share the same text and prefix
            // Sort by score descending and take top 100 to save memory and ensure fast UI
            val distinctSorted = candidates.distinctBy { it.text }
                .sortedByDescending { it.score }
                .take(100)
            finalMap[prefix] = distinctSorted
        }
        return finalMap
    }

    override fun getCandidates(code: String): List<Candidate> {
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
