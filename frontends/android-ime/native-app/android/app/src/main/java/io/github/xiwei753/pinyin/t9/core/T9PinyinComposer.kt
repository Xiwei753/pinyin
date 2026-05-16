package io.github.xiwei753.pinyin.t9.core

data class PinyinComposition(
    val pinyinList: List<String>,
    val pinyinString: String,
    val isComplete: Boolean,
    val rawDigits: String,
    var score: Int = 0,
    val segmentDigits: List<String> = emptyList()
)

data class SyllableEdge(
    val start: Int,
    val end: Int,
    val syllable: String,
    val digitSpan: String,
    val isExact: Boolean,
    val typedLength: Int,
    val fullCodeLength: Int,
    val prefixOvershoot: Int,
    val isSeparator: Boolean = false,
    val isRaw: Boolean = false
)

class T9SyllableGraph(val buffer: String) {
    val edges = Array(buffer.length) { mutableListOf<SyllableEdge>() }

    init {
        buildGraph()
    }

    private fun buildGraph() {
        for (i in buffer.indices) {
            if (buffer[i] == '1') {
                edges[i].add(SyllableEdge(i, i + 1, "", "1", isExact = true, typedLength = 1, fullCodeLength = 0, prefixOvershoot = 0, isSeparator = true))
                continue
            }

            var nextOne = buffer.indexOf('1', i)
            if (nextOne == -1) nextOne = buffer.length

            for (j in (i + 1)..nextOne) {
                val part = buffer.substring(i, j)
                val isSegmentEnd = (j == nextOne)

                val exactSyllables = PinyinSyllableDecoder.getExactSyllables(part)
                for (syl in exactSyllables) {
                    val codeLen = T9CodeMapper.toCode(syl).length
                    edges[i].add(SyllableEdge(i, j, syl, part, isExact = true, typedLength = part.length, fullCodeLength = codeLen, prefixOvershoot = 0))
                }

                if (isSegmentEnd) {
                    val prefixSyllables = PinyinSyllableDecoder.getPrefixSyllables(part)
                    val exactSet = exactSyllables.toSet()
                    for (syl in prefixSyllables) {
                        if (syl !in exactSet) {
                            val codeLen = T9CodeMapper.toCode(syl).length
                            val overshoot = codeLen - part.length
                            edges[i].add(SyllableEdge(i, j, syl, part, isExact = false, typedLength = part.length, fullCodeLength = codeLen, prefixOvershoot = overshoot))
                        }
                    }
                }
            }

            // Fallback: if no edges from this node and it's not a separator, add a raw edge to next char
            // This ensures graph connectivity for garbage inputs
            if (edges[i].isEmpty() && buffer[i] != '1') {
                edges[i].add(SyllableEdge(i, i + 1, buffer.substring(i, i + 1), buffer.substring(i, i + 1), isExact = false, typedLength = 1, fullCodeLength = 1, prefixOvershoot = 0, isRaw = true))
            }
        }
    }
}

data class DecoderPath(
    val edges: List<SyllableEdge>,
    val score: Int
)

class T9BeamDecoder(private val graph: T9SyllableGraph, private val beamSize: Int = 32) {

    fun decode(): List<DecoderPath> {
        val bufferLen = graph.buffer.length
        if (bufferLen == 0) return emptyList()

        val dp = Array(bufferLen + 1) { mutableListOf<DecoderPath>() }
        dp[0].add(DecoderPath(emptyList(), 0))

        for (i in 0 until bufferLen) {
            if (dp[i].isEmpty()) continue

            for (path in dp[i]) {
                for (edge in graph.edges[i]) {
                    val nextEdges = path.edges + edge
                    val pathScore = scorePath(nextEdges, edge.end == bufferLen)
                    dp[edge.end].add(DecoderPath(nextEdges, pathScore))
                }
            }

            // Beam prune
            if (dp[i + 1].size > beamSize) {
                dp[i + 1].sortByDescending { it.score }
                dp[i + 1] = dp[i + 1].take(beamSize).toMutableList()
            }
        }

        return dp[bufferLen].sortedByDescending { it.score }.take(beamSize)
    }

    private fun scorePath(edges: List<SyllableEdge>, isCompleteSequence: Boolean): Int {
        var score = 0
        var isAllExact = true

        for (i in edges.indices) {
            val edge = edges[i]
            if (edge.isSeparator) continue

            if (!edge.isExact) isAllExact = false

            if (edge.isRaw) {
                score -= 10000
                continue
            }

            // Base score based on typed length
            score += edge.typedLength * 100

            if (edge.isExact) {
                score += 500 // Bonus for exact match
            } else {
                // Prefix penalty
                val overshoot = edge.prefixOvershoot
                score -= overshoot * 100

                // Heavily penalize brain completion from a very short prefix (1-2 digits)
                if (edge.typedLength <= 2) {
                    score -= 5000
                }
            }

            if (edge.typedLength == 1 && edge.fullCodeLength == 1) {
                score -= 50 // Slight penalty for 1-letter syllables to favor combining
            }

            if (T9PinyinComposer.COMMON_SYLLABLES.contains(edge.syllable)) {
                score += 50
            }

            // Greedy bonus: favor longer syllables earlier
            score += edge.fullCodeLength * Math.max(1, 10 - i)
        }

        // Penalty for having too many segments (favors fewer, longer syllables)
        val nonSeparatorCount = edges.count { !it.isSeparator }
        score -= nonSeparatorCount * 100

        // Bonus if the entire sequence is complete without prefix brain completion
        if (isCompleteSequence && isAllExact) {
            score += 10000
        }

        // Massive penalty for path having too many individual 1-character syllables
        // to combat garbage combinations like "a tu ai yi mi" instead of "bu tai xing"
        val singleLetterCount = edges.count { it.fullCodeLength == 1 && !it.isSeparator }
        score -= singleLetterCount * 2000

        // Bonus for multi-letter syllables
        val multiLetterCount = edges.count { it.fullCodeLength > 1 && !it.isSeparator }
        score += multiLetterCount * 500

        // Penalize paths with many syllables to prevent fragmentation,
        // but scale it less aggressively so longer inputs like 15 digits don't get destroyed
        val validEdges = edges.filter { !it.isSeparator }
        score -= validEdges.size * 1500

        return score
    }
}

class T9PinyinComposer {

    companion object {
        val COMMON_SYLLABLES = setOf(
            "de", "shi", "yi", "bu", "le", "wo", "ta", "you", "zhe", "shang", "zhong", "guo",
            "ren", "zai", "dao", "ge", "hui", "ji", "ke", "chu", "ye", "zi", "wan", "tian", "jin",
            "xing", "tai", "ming", "dong", "xiao", "da", "di", "xue", "sheng", "nian", "yue", "ri",
            "hao", "kan", "ting", "shuo", "hua", "men", "duo", "shao", "qian", "lai", "qu"
        )
    }

    fun getCompositions(buffer: String): List<PinyinComposition> {
        if (buffer.isEmpty()) return emptyList()

        val graph = T9SyllableGraph(buffer)
        val decoder = T9BeamDecoder(graph)
        val paths = decoder.decode()

        val results = paths.map { path ->
            val validEdges = path.edges.filter { !it.isSeparator }
            val pinyinList = validEdges.map { it.syllable }
            val segmentDigits = validEdges.map { it.digitSpan }
            val pinyinString = pinyinList.joinToString(" ")
            val isComplete = validEdges.all { it.isExact }

            PinyinComposition(
                pinyinList = pinyinList,
                pinyinString = pinyinString,
                isComplete = isComplete,
                rawDigits = buffer,
                score = path.score,
                segmentDigits = segmentDigits
            )
        }

        return results.distinctBy { it.pinyinString }.sortedWith(Comparator { c1, c2 ->
            if (c1.score != c2.score) {
                c2.score.compareTo(c1.score)
            } else {
                c1.pinyinString.compareTo(c2.pinyinString)
            }
        })
    }
}
