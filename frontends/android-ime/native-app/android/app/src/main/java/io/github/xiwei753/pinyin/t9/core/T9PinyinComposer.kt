package io.github.xiwei753.pinyin.t9.core

data class PinyinComposition(
    val pinyinList: List<String>,
    val pinyinString: String,
    val isComplete: Boolean,
    val rawDigits: String,
    var score: Int = 0,
    val segmentDigits: List<String> = emptyList()
)

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

        val results = mutableListOf<PinyinComposition>()
        val segments = buffer.split("1")

        val segmentPathsList = segments.map { segment ->
            if (segment.isEmpty()) {
                listOf(Pair(emptyList<Pair<String, String>>(), true))
            } else {
                getPathsForSegment(segment)
            }
        }

        val combinedPaths = combinePaths(segmentPathsList)

        for ((pathWithDigits, isComplete) in combinedPaths) {
            val path = pathWithDigits.map { it.first }
            val segDigits = pathWithDigits.map { it.second }
            val pinyinString = path.joinToString(" ")

            var score = 0
            if (isComplete) score += 10000
            score -= path.size * 100

            for (i in path.indices) {
                val syl = path[i]
                val digitLen = segDigits[i].length
                val codeLen = T9CodeMapper.toCode(syl).length

                if (codeLen == 1) {
                    score -= 50
                }
                if (COMMON_SYLLABLES.contains(syl)) {
                    score += 10
                }

                // Greedy bonus: prefer longer syllables earlier in the path
                score += codeLen * Math.max(1, (10 - i))

                // Penalize brain-completed prefixes so short tails don't win over conservative ones
                if (!isComplete && i == path.size - 1) {
                    val overshoot = codeLen - digitLen
                    score -= overshoot * 100
                }
            }

            results.add(PinyinComposition(path, pinyinString, isComplete, buffer, score, segDigits))
        }

        return results.distinctBy { it.pinyinString }.sortedWith(Comparator { c1, c2 ->
            if (c1.score != c2.score) {
                c2.score.compareTo(c1.score)
            } else {
                c1.pinyinString.compareTo(c2.pinyinString)
            }
        })
    }

    private fun getPathsForSegment(segment: String): List<Pair<List<Pair<String, String>>, Boolean>> {
        val dp = Array<MutableList<Pair<List<Pair<String, String>>, Boolean>>>(segment.length + 1) { mutableListOf() }
        dp[0].add(Pair(emptyList(), true))

        for (i in 1..segment.length) {
            for (j in 0 until i) {
                if (dp[j].isEmpty()) continue

                val part = segment.substring(j, i)
                val isPrefix = (i == segment.length)

                val exactSyllables = PinyinSyllableDecoder.getExactSyllables(part)
                val prefixSyllables = if (isPrefix) PinyinSyllableDecoder.getPrefixSyllables(part) else emptyList()

                for ((prevPath, _) in dp[j]) {
                    for (syllable in exactSyllables) {
                        dp[i].add(Pair(prevPath + Pair(syllable, part), true))
                    }

                    if (isPrefix) {
                        val exactSet = exactSyllables.toSet()
                        for (syllable in prefixSyllables) {
                            if (syllable !in exactSet) {
                                dp[i].add(Pair(prevPath + Pair(syllable, part), false))
                            }
                        }
                    }
                }
            }
        }

        val result = dp[segment.length]
        if (result.isEmpty()) {
            return listOf(Pair(listOf(Pair(segment, segment)), false))
        }
        return result
    }

    private fun combinePaths(segmentPathsList: List<List<Pair<List<Pair<String, String>>, Boolean>>>): List<Pair<List<Pair<String, String>>, Boolean>> {
        var currentCombined = listOf(Pair(emptyList<Pair<String, String>>(), true))

        for (segmentPaths in segmentPathsList) {
            val newCombined = mutableListOf<Pair<List<Pair<String, String>>, Boolean>>()
            for (comb in currentCombined) {
                for (path in segmentPaths) {
                    val newCombList = comb.first.toMutableList()
                    newCombList.addAll(path.first)
                    val newComplete = comb.second && path.second
                    newCombined.add(Pair(newCombList, newComplete))
                }
            }
            currentCombined = newCombined
        }

        return currentCombined
    }
}
