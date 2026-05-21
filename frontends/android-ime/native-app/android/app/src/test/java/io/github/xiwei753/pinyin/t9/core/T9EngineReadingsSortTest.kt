package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class T9EngineReadingsSortTest {

    class MockDict : DictionaryProvider {
        private val list = mutableListOf<Candidate>()

        fun add(c: Candidate, pinyin: String) {
            val origin = if (c.text.length == 1) CandidateOrigin.EXACT_SINGLE else CandidateOrigin.EXACT_PHRASE
            list.add(Candidate(c.text, pinyin, c.score, c.type, "", origin))
        }

        override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> {
            return list.filter { it.code == pinyinSequence }
        }

        override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
            return list.filter { it.code.startsWith(pinyinPrefix) }
        }

        override fun getSingleSyllableCandidates(syllable: String): List<Candidate> {
            return list.filter { it.code == syllable }
        }

        override fun getCandidates(code: String): List<Candidate> = emptyList()
        override fun getExactCandidates(code: String): List<Candidate> = emptyList()
        override fun getPrefixCandidates(code: String): List<Candidate> = emptyList()
    }

    @Test
    fun testReadingsSortFor948264() {
        val dict = MockDict()
        // Add candidates for the syllables we expect: zhuang, zhuan, zhua, zhu
        // Using realistic scores where longer syllables might have higher scores
        dict.add(Candidate("庄", "zhuang", 80000, CandidateType.SINGLE_CHAR), "zhuang")
        dict.add(Candidate("转", "zhuan", 70000, CandidateType.SINGLE_CHAR), "zhuan")
        dict.add(Candidate("爪", "zhua", 60000, CandidateType.SINGLE_CHAR), "zhua")
        dict.add(Candidate("朱", "zhu", 50000, CandidateType.SINGLE_CHAR), "zhu")

        val engine = T9Engine(dict)
        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("8")
        engine.inputDigit("2")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val readings = engine.readings
        assertTrue("readings should not be empty", readings.isNotEmpty())
        assertTrue("readings should contain zhuang", readings.any { it == "zhuang" })
        assertTrue("readings should contain zhuan", readings.any { it == "zhuan" })
        assertTrue("readings should contain zhua", readings.any { it == "zhua" })
        assertTrue("readings should contain zhu", readings.any { it == "zhu" })

        // Check ordering: longer syllables should come first
        val zhuangIndex = readings.indexOfFirst { it == "zhuang" }
        val zhuanIndex = readings.indexOfFirst { it == "zhuan" }
        val zhuaIndex = readings.indexOfFirst { it == "zhua" }
        val zhuIndex = readings.indexOfFirst { it == "zhu" }

        assertTrue("zhuang should come before zhuan", zhuangIndex < zhuanIndex)
        assertTrue("zhuan should come before zhua", zhuanIndex < zhuaIndex)
        assertTrue("zhua should come before zhu", zhuaIndex < zhuIndex)
    }

    @Test
    fun testReadingsSortByLengthThenScore() {
        val dict = MockDict()
        // Add candidates for testing
        dict.add(Candidate("长音节", "chang", 10000, CandidateType.SINGLE_CHAR), "chang") // length 4, low score
        dict.add(Candidate("短", "duan", 50000, CandidateType.SINGLE_CHAR), "duan")     // length 4, high score
        dict.add(Candidate("中", "zhong", 30000, CandidateType.SINGLE_CHAR), "zhong")   // length 4, medium score
        dict.add(Candidate("单", "dan", 40000, CandidateType.SINGLE_CHAR), "dan")       // length 3, high score

        val engine = T9Engine(dict)
        engine.inputDigit("2")
        engine.inputDigit("4")
        engine.inputDigit("2")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val readings = engine.readings

        // Verify the sorting: first by length descending, then by score descending
        // Check that all 4-length syllables come before 3-length ones
        val fourLengthSyllables = readings.takeWhile { it.length >= 4 }
        val threePlusLengthSyllables = readings.dropWhile { it.length >= 4 }
        
        assertTrue("All 4+ length syllables should come before shorter ones", 
                threePlusLengthSyllables.all { it.length < 4 })
                
        // Check that within same length, higher score comes first
        // Extract the 4-length syllables and verify they're sorted by score descending
        val fourLengthList = fourLengthSyllables.toList()
        if (fourLengthList.size >= 2) {
            // Create a map of syllable to score from the engine's internal calculations
            val syllableScores = mutableMapOf<String, Int>()
            val validComps = engine.getValidCompositions()
            val nextIndex = engine.lockedSyllables.size
            for (comp in validComps) {
                if (comp.pinyinList.size > nextIndex) {
                    val syl = comp.pinyinList[nextIndex]
                    if (syl.isNotEmpty()) {
                        val currentScore = syllableScores.getOrDefault(syl, Int.MIN_VALUE)
                        if (comp.score > currentScore) {
                            syllableScores[syl] = comp.score
                        }
                    }
                }
            }
            // Allow selecting shorter prefixes if available in the graph
            if (syllableScores.isEmpty() && nextIndex == 0) {
                val buffer = engine.buffer
                for (end in buffer.length downTo 1) {
                    val prefix = buffer.substring(0, end)
                    for (s in PinyinSyllableDecoder.getExactSyllables(prefix)) {
                        if (s.isNotEmpty()) {
                            val currentScore = syllableScores.getOrDefault(s, Int.MIN_VALUE)
                            // For prefix, we don't have a composition score, use 0 as baseline
                            if (0 > currentScore) {
                                syllableScores[s] = 0
                            }
                        }
                    }
                }
            }
            
            // Verify that for adjacent pairs in the 4-length list, scores are descending
            for (i in 0 until fourLengthList.size - 1) {
                val score1 = syllableScores[fourLengthList[i]] ?: Int.MIN_VALUE
                val score2 = syllableScores[fourLengthList[i + 1]] ?: Int.MIN_VALUE
                // Since we're checking descending order, score1 should be >= score2
                assertTrue("Syllable '${fourLengthList[i]}' (score $score1) should come before '${fourLengthList[i + 1]}' (score $score2)",
                        score1 >= score2)
            }
        }
    }

    @Test
    fun testSelectingReadingUpdatesPreeditAndCandidates() {
        val dict = MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("能", "neng", 40000, CandidateType.SINGLE_CHAR), "neng")
        dict.add(Candidate("们", "men", 30000, CandidateType.SINGLE_CHAR), "men")

        val engine = T9Engine(dict)
        engine.inputDigit("6")
        engine.inputDigit("3")
        engine.inputDigit("6")
        engine.inputDigit("4")

        // Initial preedit should be meng (first reading)
        assertEquals("meng", engine.getPreedit())

        // Switch to neng
        val result = engine.setActiveReading("neng")
        assertTrue("setActiveReading(neng) should succeed", result)
        assertEquals("neng", engine.getPreedit())

        // Candidates should now be for neng
        val visible = engine.getVisibleCandidates()
        assertTrue("visible candidates should contain 能 when reading is neng", visible.any { it.text == "能" })

        // Switch back to meng
        val result2 = engine.setActiveReading("meng")
        assertTrue("setActiveReading(meng) should succeed", result2)
        assertEquals("meng", engine.getPreedit())

        // Candidates should now be for meng
        val visible2 = engine.getVisibleCandidates()
        assertTrue("meng candidates should contain 梦", visible2.any { it.text == "梦" })
        // Note: We didn't add 蒙 and 萌 to the dict, so we won't test for them
    }
}