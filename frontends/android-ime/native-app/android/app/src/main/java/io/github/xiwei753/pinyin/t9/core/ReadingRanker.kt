package io.github.xiwei753.pinyin.t9.core

data class RankedReading(
    val syllable: String,
    val score: Int,
    val category: ReadingCategory,
)

enum class ReadingCategory {
    COMPLETE_FULL_SPAN,
    MULTI_SYLLABLE_DERIVED,
    PREFIX_FALLBACK,
}

object ReadingRanker {

    // Readings that produce no useful Chinese T9 candidates
    private val LOW_VALUE_READINGS = setOf("ng", "o")

    fun rank(
        buffer: String,
        compositions: List<PinyinComposition>,
        nextIndex: Int,
    ): List<RankedReading> {
        if (buffer.isEmpty()) return emptyList()

        val fullSpanSyllables = mutableMapOf<String, Int>()
        val multiSyllableSyllables = mutableMapOf<String, Int>()

        for (comp in compositions) {
            if (comp.pinyinList.size > nextIndex) {
                val syl = comp.pinyinList[nextIndex]
                if (syl.isEmpty()) continue

                val isDigit = syl.all { it.isDigit() }
                if (isDigit) {
                    val prefixes = PinyinSyllableDecoder.getPrefixSyllables(syl)
                    for (ps in prefixes) {
                        val current = multiSyllableSyllables.getOrDefault(ps, Int.MIN_VALUE)
                        val score = comp.score - 1000
                        if (score > current) {
                            multiSyllableSyllables[ps] = score
                        }
                    }
                    continue
                }

                val isFullSpan = comp.pinyinList.size == nextIndex + 1 && comp.isComplete

                if (isFullSpan) {
                    val current = fullSpanSyllables.getOrDefault(syl, Int.MIN_VALUE)
                    if (comp.score > current) {
                        fullSpanSyllables[syl] = comp.score
                    }
                } else {
                    val current = multiSyllableSyllables.getOrDefault(syl, Int.MIN_VALUE)
                    if (comp.score > current) {
                        multiSyllableSyllables[syl] = comp.score
                    }
                }
            }
        }

        val prefixFallback = mutableMapOf<String, Int>()
        if (fullSpanSyllables.isEmpty() && multiSyllableSyllables.isEmpty() && nextIndex == 0) {
            for (end in buffer.length downTo 1) {
                val prefix = buffer.substring(0, end)
                for (s in PinyinSyllableDecoder.getExactSyllables(prefix)) {
                    if (s.isNotEmpty()) {
                        val current = prefixFallback.getOrDefault(s, Int.MIN_VALUE)
                        if (0 > current) {
                            prefixFallback[s] = 0
                        }
                    }
                }
            }
        }

        val results = mutableListOf<RankedReading>()

        // Category 1: Complete full-span syllables
        for ((syl, score) in fullSpanSyllables) {
            results.add(RankedReading(syl, score, ReadingCategory.COMPLETE_FULL_SPAN))
        }

        // Category 2: Multi-syllable derived
        for ((syl, score) in multiSyllableSyllables) {
            if (fullSpanSyllables.containsKey(syl)) continue
            results.add(RankedReading(syl, score, ReadingCategory.MULTI_SYLLABLE_DERIVED))
        }

        // Category 3: Prefix fallback
        for ((syl, score) in prefixFallback) {
            if (fullSpanSyllables.containsKey(syl)) continue
            if (multiSyllableSyllables.containsKey(syl)) continue
            results.add(RankedReading(syl, score, ReadingCategory.PREFIX_FALLBACK))
        }

        return results
            .filter { it.syllable !in LOW_VALUE_READINGS }
            .sortedWith(
                compareBy<RankedReading> { it.category.ordinal }
                    .thenByDescending { it.syllable.length }
                    .thenByDescending { it.score }
                    .thenBy { it.syllable }
            )
    }
}
