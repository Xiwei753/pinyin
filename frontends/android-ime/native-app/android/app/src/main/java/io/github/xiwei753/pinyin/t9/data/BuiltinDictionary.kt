package io.github.xiwei753.pinyin.t9.data

import android.content.Context
import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.core.T9CodeMapper
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader

class BuiltinDictionary : DictionaryProvider {
    private val prefixDictionary: Map<String, List<Candidate>>
    private val exactDictionary: Map<String, List<Candidate>>

    private val pinyinExactIndex: Map<String, List<Candidate>>
    private val pinyinPrefixIndex: Map<String, List<Candidate>>
    private val singleSyllableIndex: Map<String, List<Candidate>>

    var isFallback: Boolean = false
        private set
    var loadedWordCount: Int = 0
        private set

    constructor(lines: List<String>) {
        val result = parseLines(lines)
        prefixDictionary = result.prefixMap
        exactDictionary = result.exactMap
        pinyinExactIndex = result.pinyinExact
        pinyinPrefixIndex = result.pinyinPrefix
        singleSyllableIndex = result.singleSyllable
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
        prefixDictionary = result.prefixMap
        exactDictionary = result.exactMap
        pinyinExactIndex = result.pinyinExact
        pinyinPrefixIndex = result.pinyinPrefix
        singleSyllableIndex = result.singleSyllable
    }

    private data class ParseResult(
        val prefixMap: Map<String, List<Candidate>>,
        val exactMap: Map<String, List<Candidate>>,
        val pinyinExact: Map<String, List<Candidate>>,
        val pinyinPrefix: Map<String, List<Candidate>>,
        val singleSyllable: Map<String, List<Candidate>>
    )

    private fun parseLines(lines: List<String>): ParseResult {
        val prefixMap = mutableMapOf<String, MutableList<Candidate>>()
        val exactMap = mutableMapOf<String, MutableList<Candidate>>()

        val pinyinExact = mutableMapOf<String, MutableList<Candidate>>()
        val pinyinPrefix = mutableMapOf<String, MutableList<Candidate>>()
        val singleSyllable = mutableMapOf<String, MutableList<Candidate>>()

        var hasValidEntries = false
        var count = 0

        try {
            for (line in lines) {
                val parts = line.split("\t")
                if (parts.size >= 3) {
                    val text = parts[0]
                    val pinyin = parts[1] // like "bu tai xing"
                    val score = parts[2].toIntOrNull() ?: 0
                    val code = T9CodeMapper.toCode(pinyin)

                    if (code.isNotEmpty()) {
                        hasValidEntries = true
                        count++

                        val type = when {
                            text.length >= 4 -> CandidateType.LONG_OR_LOW_FREQ
                            text.length == 3 && score < 30000 -> CandidateType.LONG_OR_LOW_FREQ
                            text.length == 3 -> CandidateType.NORMAL
                            text.length == 2 && score > 40000 -> CandidateType.COMMON_SHORT
                            text.length == 2 -> CandidateType.NORMAL
                            text.length == 1 && score > 60000 -> CandidateType.COMMON_SHORT
                            text.length == 1 -> CandidateType.SINGLE_CHAR
                            else -> CandidateType.NORMAL
                        }

                        val finalType = if (type != CandidateType.LONG_OR_LOW_FREQ && score < 5000) CandidateType.LONG_OR_LOW_FREQ else type

                        val candidate = Candidate(text, code, score, finalType)

                        // Legacy maps
                        exactMap.getOrPut(code) { mutableListOf() }.add(candidate)
                        for (i in 1..code.length) {
                            val prefix = code.substring(0, i)
                            prefixMap.getOrPut(prefix) { mutableListOf() }.add(candidate)
                        }

                        // Pinyin maps
                        val pinyinClean = pinyin.replace(" ", "")
                        pinyinExact.getOrPut(pinyinClean) { mutableListOf() }.add(candidate)

                        // Prefix for pinyin
                        val syllables = pinyin.split(" ")
                        if (syllables.size == 1) {
                            singleSyllable.getOrPut(syllables[0]) { mutableListOf() }.add(candidate)
                        }

                        for (i in 1..syllables.size) {
                            val pre = syllables.take(i).joinToString("")
                            pinyinPrefix.getOrPut(pre) { mutableListOf() }.add(candidate)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Error
        }

        if (!hasValidEntries) {
            isFallback = true
            loadedWordCount = 2
            val candidate1 = Candidate("你好", "64426", 100000)
            val candidate2 = Candidate("输入法", "7487832", 90000)


            exactMap["64426"] = mutableListOf(candidate1)
            prefixMap["6"] = mutableListOf(candidate1)
            prefixMap["64"] = mutableListOf(candidate1)
            prefixMap["644"] = mutableListOf(candidate1)
            prefixMap["6442"] = mutableListOf(candidate1)
            prefixMap["64426"] = mutableListOf(candidate1)

            exactMap["7487832"] = mutableListOf(candidate2)
            prefixMap["7"] = mutableListOf(candidate2)
            prefixMap["74"] = mutableListOf(candidate2)
            prefixMap["748"] = mutableListOf(candidate2)
            prefixMap["7487"] = mutableListOf(candidate2)
            prefixMap["74878"] = mutableListOf(candidate2)
            prefixMap["748783"] = mutableListOf(candidate2)
            prefixMap["7487832"] = mutableListOf(candidate2)

            pinyinExact["nihao"] = mutableListOf(candidate1)
            pinyinPrefix["ni"] = mutableListOf(candidate1)
            pinyinPrefix["nihao"] = mutableListOf(candidate1)

            pinyinExact["shurufa"] = mutableListOf(candidate2)
            pinyinPrefix["shu"] = mutableListOf(candidate2)
            pinyinPrefix["shuru"] = mutableListOf(candidate2)
            pinyinPrefix["shurufa"] = mutableListOf(candidate2)
        } else {
            isFallback = false
            loadedWordCount = count
        }

        fun finalizeMap(map: Map<String, List<Candidate>>): Map<String, List<Candidate>> {
            val finalMap = mutableMapOf<String, List<Candidate>>()
            for ((k, v) in map) {
                finalMap[k] = v.distinctBy { it.text }.sortedByDescending { it.score }.take(100)
            }
            return finalMap
        }

        return ParseResult(
            finalizeMap(prefixMap),
            finalizeMap(exactMap),
            finalizeMap(pinyinExact),
            finalizeMap(pinyinPrefix),
            finalizeMap(singleSyllable)
        )
    }

    override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> {
        return pinyinExactIndex[pinyinSequence] ?: emptyList()
    }

    override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
        return pinyinPrefixIndex[pinyinPrefix] ?: emptyList()
    }

    override fun getSingleSyllableCandidates(syllable: String): List<Candidate> {
        return singleSyllableIndex[syllable] ?: emptyList()
    }

    override fun getCandidates(code: String): List<Candidate> {
        return getPrefixCandidates(code)
    }

    override fun getExactCandidates(code: String): List<Candidate> {
        return exactDictionary[code] ?: emptyList()
    }

    override fun getPrefixCandidates(code: String): List<Candidate> {
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
