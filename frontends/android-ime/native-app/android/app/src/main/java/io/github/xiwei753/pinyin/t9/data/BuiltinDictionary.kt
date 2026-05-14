package io.github.xiwei753.pinyin.t9.data

import android.content.Context
import io.github.xiwei753.pinyin.t9.core.Candidate
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader

class BuiltinDictionary : DictionaryProvider {
    private val dictionary: Map<String, List<Candidate>>

    constructor(lines: List<String>) {
        dictionary = parseLines(lines)
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
        dictionary = parseLines(lines)
    }

    private fun parseLines(lines: List<String>): Map<String, List<Candidate>> {
        val map = mutableMapOf<String, MutableList<Candidate>>()
        try {
            for (line in lines) {
                val parts = line.split("\t")
                if (parts.size >= 3) {
                    val code = parts[0]
                    val text = parts[1]
                    val score = parts[2].toIntOrNull() ?: 0

                    if (!map.containsKey(code)) {
                        map[code] = mutableListOf()
                    }
                    map[code]?.add(Candidate(text, code, score))
                }
            }
        } catch (e: Exception) {
            // Error during parsing
        }

        // If map is empty or we had an error, use fallback
        if (map.isEmpty()) {
            map["64426"] = mutableListOf(Candidate("你好", "64426", 100000))
            map["748732"] = mutableListOf(Candidate("输入法", "748732", 90000))
        }

        val finalMap = mutableMapOf<String, List<Candidate>>()
        for ((code, candidates) in map) {
            finalMap[code] = candidates.sortedByDescending { it.score }
        }
        return finalMap
    }

    override fun getCandidates(code: String): List<Candidate> {
        return dictionary[code] ?: emptyList()
    }

    companion object {
        fun fromAssets(context: Context): BuiltinDictionary {
            return try {
                val inputStream = context.assets.open("t9_builtin_dict.tsv")
                BuiltinDictionary(inputStream)
            } catch (e: Exception) {
                BuiltinDictionary(emptyList()) // fallback
            }
        }
    }
}
