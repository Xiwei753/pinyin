package io.github.xiwei753.pinyin.t9.core

data class QwertyPinyinComposition(
    val pinyinList: List<String>,
    val pinyinString: String,
    val isComplete: Boolean,
    val rawLetters: String,
    val score: Int = 0,
)

class PinyinSyllableParser {

    companion object {
        private val validSyllables: Set<String> = PinyinSyllableDecoder.SYLLABLES.toSet()

        private val commonSyllables = setOf(
            "de", "shi", "yi", "bu", "le", "wo", "ta", "ni", "nin", "you",
            "zhe", "shang", "zhong", "guo", "ren", "zai", "dao", "ge", "hui",
            "ji", "ke", "chu", "ye", "zi", "wan", "tian", "jin", "xing", "tai",
            "ming", "dong", "xiao", "da", "di", "xue", "sheng", "nian", "yue",
            "ri", "hao", "kan", "ting", "shuo", "hua", "men", "duo", "shao",
            "qian", "lai", "qu", "mei", "shen", "zhi", "hai", "xiang", "jian",
            "jiao", "xia", "hou", "jue", "qiao", "pian", "bian", "mian", "dian",
            "tiao", "liao", "liu", "xiu", "qiu", "ti", "li", "ji", "pi", "mi",
            "di", "si", "zi", "ci", "ri", "qi", "bi", "neng", "zou", "dou",
            "tou", "wan", "guan", "kuan", "huan", "chuan", "zhu", "chu", "shu",
            "ru", "lu", "nu", "du", "tu", "gu", "ku", "hu", "zu", "cu", "su",
            "ju", "qu", "xu", "yu",
        )
    }

    fun getCompositions(letters: String): List<QwertyPinyinComposition> {
        if (letters.isEmpty()) return emptyList()
        val lower = letters.lowercase()
        val len = lower.length

        val edgeMap = Array(len) { mutableListOf<Pair<Int, String>>() }
        for (i in 0 until len) {
            for (j in i + 1..len) {
                val sub = lower.substring(i, j)
                if (sub in validSyllables) {
                    edgeMap[i].add(j to sub)
                }
            }
            if (edgeMap[i].isEmpty()) {
                edgeMap[i].add((i + 1) to lower.substring(i, i + 1))
            }
        }

        data class Path(val syllables: List<String>, val positions: List<Int>, val score: Int)

        val dp = Array(len + 1) { mutableListOf<Path>() }
        dp[0].add(Path(emptyList(), listOf(0), 0))

        for (i in 0 until len) {
            if (dp[i].isEmpty()) continue
            for ((j, syl) in edgeMap[i]) {
                for (path in dp[i]) {
                    val newSyllables = path.syllables + syl
                    val newPositions = path.positions + j
                    val sylIsValid = syl in validSyllables
                    val sylIsCommon = syl in commonSyllables
                    var score = path.score + syl.length * 100
                    if (sylIsValid) score += 500
                    if (sylIsCommon) score += 100
                    dp[j].add(Path(newSyllables, newPositions, score))
                }
            }
            if (dp[i + 1].size > 16) {
                dp[i + 1].sortByDescending { it.score }
                dp[i + 1] = dp[i + 1].take(16).toMutableList()
            }
        }

        val validSylCount = lower.count { c -> c in validSyllables.joinToString("") }

        return dp[len]
            .sortedByDescending { it.score }
            .take(8)
            .map { path ->
                val pinyinList = path.syllables
                val pinyinString = pinyinList.joinToString(" ")
                val isComplete = pinyinList.all { it in validSyllables }
                QwertyPinyinComposition(
                    pinyinList = pinyinList,
                    pinyinString = pinyinString,
                    isComplete = isComplete,
                    rawLetters = letters,
                    score = path.score,
                )
            }
            .distinctBy { it.pinyinString }
    }
}
