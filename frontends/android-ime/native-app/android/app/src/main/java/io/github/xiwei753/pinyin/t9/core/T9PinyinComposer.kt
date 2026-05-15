package io.github.xiwei753.pinyin.t9.core

data class PinyinComposition(
    val pinyinList: List<String>,
    val pinyinString: String,
    val isComplete: Boolean,
    val rawDigits: String
)

class T9PinyinComposer {

    fun getCompositions(buffer: String): List<PinyinComposition> {
        if (buffer.isEmpty()) return emptyList()

        val results = mutableListOf<PinyinComposition>()

        // Split by explicit separators '1'
        val segments = buffer.split("1")

        // Generate possible decodings for each segment
        // If a segment is empty (e.g. starting with 1, ending with 1, or consecutive 1s), we ignore it or treat it as a break

        val segmentPathsList = segments.map { segment ->
            if (segment.isEmpty()) {
                listOf(listOf<String>())
            } else {
                getPathsForSegment(segment)
            }
        }

        // Combine paths across segments
        val combinedPaths = combinePaths(segmentPathsList)

        for (path in combinedPaths) {
            val pinyinString = path.joinToString(" ")

            // Determine if complete: all non-empty segments' last syllable must be complete
            // This is slightly complex, so let's simplify:
            // A path is complete if its last syllable is an exact match for its digits.
            // But wait, we only want to return the best ones.
            results.add(PinyinComposition(path, pinyinString, true, buffer))
        }

        // Sort by some heuristic (e.g., fewer syllables = better, or longer syllables = better)
        // For now, let's just return distinct
        return results.distinctBy { it.pinyinString }
    }

    private fun getPathsForSegment(segment: String): List<List<String>> {
        val dp = Array<MutableList<List<String>>>(segment.length + 1) { mutableListOf() }
        dp[0].add(emptyList())

        for (i in 1..segment.length) {
            for (j in 0 until i) {
                if (dp[j].isEmpty()) continue

                val part = segment.substring(j, i)
                val isPrefix = (i == segment.length)

                val syllables = if (isPrefix) {
                    PinyinSyllableDecoder.getPrefixSyllables(part)
                } else {
                    PinyinSyllableDecoder.getExactSyllables(part)
                }

                for (prevPath in dp[j]) {
                    for (syllable in syllables) {
                        val newPath = prevPath + syllable
                        dp[i].add(newPath)
                    }
                }
            }

            // If we can't find any valid prefix, maybe it's just invalid, we can keep the raw digits or allow fallback?
            // To be robust, if dp[i] is empty, maybe we fallback to adding the raw digits.
        }

        val result = dp[segment.length]
        if (result.isEmpty()) {
            // Fallback for invalid segment
            return listOf(listOf(segment))
        }
        return result
    }

    private fun combinePaths(segmentPathsList: List<List<List<String>>>): List<List<String>> {
        var currentCombined = listOf(emptyList<String>())

        for (segmentPaths in segmentPathsList) {
            val newCombined = mutableListOf<List<String>>()
            for (comb in currentCombined) {
                for (path in segmentPaths) {
                    val newComb = comb.toMutableList()
                    newComb.addAll(path)
                    newCombined.add(newComb)
                }
            }
            currentCombined = newCombined
        }

        return currentCombined
    }
}
