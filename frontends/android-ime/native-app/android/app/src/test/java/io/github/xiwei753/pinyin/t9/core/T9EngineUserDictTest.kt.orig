package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class T9EngineUserDictTest {
    class MockDictWithPhrases : DictionaryProvider {
    override fun getPinyinExactCandidatesMultiple(pinyinSequences: List<String>): Map<String, List<Candidate>> {
        val result = mutableMapOf<String, List<Candidate>>()
        for (seq in pinyinSequences) {
            result[seq] = list.filter { it.code == seq }
        }
        return result
    }
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

    class MockUserDict : io.github.xiwei753.pinyin.t9.data.UserDictionaryProvider {
        private val userEntries = mutableMapOf<Pair<String, String>, Int>()

        override fun recordSelection(text: String, pinyin: String) {
            if (text.isEmpty() || pinyin.isEmpty()) return
            if (text.matches(Regex("^[0-9]+$"))) return
            if (text.matches(Regex("^[a-zA-Z\\s]+$"))) return
            if (text.contains(" ")) return
            val key = Pair(text, pinyin)
            userEntries[key] = (userEntries[key] ?: 0) + 1
        }

        override fun getUserCandidates(pinyin: String): List<Candidate> {
            return userEntries.filter { it.key.second == pinyin }
                .map { (key, count) ->
                    val score = 200000 + count * 10000
                    val type = if (key.first.length == 1) CandidateType.SINGLE_CHAR else CandidateType.NORMAL
                    Candidate(key.first, pinyin, score, type, pinyin, CandidateOrigin.USER_HISTORY)
                }
                .sortedByDescending { it.score }
        }

        override fun getUserBoost(pinyin: String, text: String): Int {
            val key = Pair(text, pinyin)
            return (userEntries[key] ?: 0) * 15000
        }

        override fun clearUserDictionary() {
            userEntries.clear()
        }

        override fun close() {}
    }

    @Test
    fun userSelectionIncreasesCount() {
        val dict = MockDictWithPhrases()
        dict.add(Candidate("我", "wo", 100000, CandidateType.SINGLE_CHAR), "wo")
        dict.add(Candidate("你", "ni", 90000, CandidateType.SINGLE_CHAR), "ni")
        dict.add(Candidate("好", "hao", 80000, CandidateType.SINGLE_CHAR), "hao")

        val userDict = MockUserDict()
        val engine = T9Engine(dict, userDict)

        engine.inputDigit("9")
        engine.inputDigit("6")
        val cands = engine.getVisibleCandidates()

        val woCand = cands.find { it.text == "我" }
        assertTrue(woCand != null)

        if (woCand != null) {
            engine.commitCandidate(woCand)
        }

        // After commit the buffer is cleared; re-type digits to see boosted score
        engine.inputDigit("9")
        engine.inputDigit("6")
        val boosted = engine.getVisibleCandidates()
        val boostedWo = boosted.find { it.text == "我" }

        assertTrue("用户选择后应该有提升", boostedWo!!.score > 100000)
    }

    @Test
    fun userHistoryBoostsSamePinyin() {
        val dict = MockDictWithPhrases()
        dict.add(Candidate("我", "wo", 100000, CandidateType.SINGLE_CHAR), "wo")
        dict.add(Candidate("卧", "wo", 90000, CandidateType.SINGLE_CHAR), "wo")
        dict.add(Candidate("握", "wo", 80000, CandidateType.SINGLE_CHAR), "wo")

        val userDict = MockUserDict()
        userDict.recordSelection("我", "wo")
        userDict.recordSelection("我", "wo")

        val engine = T9Engine(dict, userDict)
        engine.inputDigit("9")
        engine.inputDigit("6")

        val cands = engine.getVisibleCandidates()
        val firstText = cands.firstOrNull()?.text
        assertEquals("用户多次选择的词应该排序提升", "我", firstText)
    }

    @Test
    fun notLearnPureNumbers() {
        val dict = MockDictWithPhrases()
        val userDict = MockUserDict()

        userDict.recordSelection("123", "123")
        userDict.recordSelection("456", "yi er san")

        assertTrue("不应学习纯数字", userDict.getUserCandidates("123").isEmpty())
    }

    @Test
    fun notLearnSpaces() {
        val dict = MockDictWithPhrases()
        val userDict = MockUserDict()

        userDict.recordSelection("我 你", "wo ni")

        assertTrue("不应学习带空格的文本", userDict.getUserCandidates("wo ni").isEmpty())
    }

    @Test
    fun notLearnPurePinyin() {
        val dict = MockDictWithPhrases()
        val userDict = MockUserDict()

        userDict.recordSelection("wo", "wo")
        userDict.recordSelection("ni", "ni")

        assertTrue("不应学习纯拼音", userDict.getUserCandidates("wo").isEmpty())
    }

    @Test
    fun userDictClearWorks() {
        val dict = MockDictWithPhrases()
        dict.add(Candidate("我", "wo", 100000, CandidateType.SINGLE_CHAR), "wo")

        val userDict = MockUserDict()
        userDict.recordSelection("我", "wo")

        val engine = T9Engine(dict, userDict)
        engine.inputDigit("9")
        engine.inputDigit("6")
        assertTrue("清空前有用户候选", engine.getVisibleCandidates().any { it.origin == CandidateOrigin.USER_HISTORY })

        userDict.clearUserDictionary()

        val engine2 = T9Engine(dict, userDict)
        engine2.inputDigit("9")
        engine2.inputDigit("6")
        assertFalse("清空后无用户候选", engine2.getVisibleCandidates().any { it.origin == CandidateOrigin.USER_HISTORY })
    }

    @Test
    fun safeDynamicCombinationsLimited() {
        val dict = MockDictWithPhrases()
        dict.add(Candidate("不", "bu", 100000, CandidateType.SINGLE_CHAR), "bu")
        dict.add(Candidate("太", "tai", 90000, CandidateType.SINGLE_CHAR), "tai")
        dict.add(Candidate("行", "xing", 80000, CandidateType.SINGLE_CHAR), "xing")
        dict.add(Candidate("不太行", "bu tai xing", 100000, CandidateType.NORMAL), "bu tai xing")
        dict.add(Candidate("很", "hen", 70000, CandidateType.SINGLE_CHAR), "hen")
        dict.add(Candidate("好", "hao", 60000, CandidateType.SINGLE_CHAR), "hao")

        val engine = T9Engine(dict, null)
        engine.inputDigit("2")
        engine.inputDigit("8")
        engine.inputDigit("8")
        engine.inputDigit("2")
        engine.inputDigit("4")
        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("4")

        val cands = engine.getVisibleCandidates()
        val firstText = cands.firstOrNull()?.text
        assertEquals("精确短语优先", "不太行", firstText)
    }
}