package io.github.xiwei753.pinyin.t9.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import io.github.xiwei753.pinyin.t9.testutil.TestSQLiteDictionary
import io.github.xiwei753.pinyin.t9.testutil.TestPaths

class UserDictionaryClearTest {

    private lateinit var userDict: TestUserDictionary

    @Before
    fun setUp() {
        userDict = TestUserDictionary()
    }

    @Test
    fun clearRemovesAllUserCandidates() {
        userDict.recordSelection("我", "wo")
        userDict.recordSelection("你好", "ni hao")
        assertFalse("清空前应有用户候选", userDict.getUserCandidates("wo").isEmpty())
        assertFalse("清空前应有用户候选", userDict.getUserCandidates("ni hao").isEmpty())

        userDict.clearUserDictionary()

        assertTrue("清空后 getUserCandidates 应返回空", userDict.getUserCandidates("wo").isEmpty())
        assertTrue("清空后 getUserCandidates 应返回空", userDict.getUserCandidates("ni hao").isEmpty())
    }

    @Test
    fun clearRemovesUserBoost() {
        userDict.recordSelection("我", "wo")
        assertTrue("清空前应有 boost", userDict.getUserBoost("wo", "我") > 0)

        userDict.clearUserDictionary()

        assertEquals("清空后 boost 应为 0", 0, userDict.getUserBoost("wo", "我"))
    }

    @Test
    fun clearDoesNotAffectBuiltinDictionary() {
        val dict = TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath)
        val beforeCount = dict.getLoadedWordCount()
        assertTrue("内置词库应有词", beforeCount > 30000)

        userDict.recordSelection("测试", "ce shi")
        userDict.clearUserDictionary()

        val afterCount = dict.getLoadedWordCount()
        assertEquals("清空用户词库不应影响内置词库词数", beforeCount, afterCount)

        val woCands = dict.getPinyinExactCandidates("wo")
        assertTrue("清空用户词库后内置词库仍应有 '我'", woCands.any { it.text == "我" })
    }

    @Test
    fun clearOnEmptyDictDoesNotCrash() {
        userDict.clearUserDictionary()
        assertTrue("清空空用户词库不应崩溃", userDict.getUserCandidates("wo").isEmpty())
    }

    @Test
    fun multipleClearCallsAreIdempotent() {
        userDict.recordSelection("我", "wo")
        userDict.clearUserDictionary()
        userDict.clearUserDictionary()
        userDict.clearUserDictionary()
        assertTrue("多次清空幂等", userDict.getUserCandidates("wo").isEmpty())
    }

    @Test
    fun historyBoostDisappearsAfterClear() {
        userDict.recordSelection("我", "wo")
        userDict.recordSelection("我", "wo")
        val beforeBoost = userDict.getUserBoost("wo", "我")
        assertTrue("清空前 boost 应大于 0", beforeBoost > 0)

        userDict.clearUserDictionary()
        val afterBoost = userDict.getUserBoost("wo", "我")
        assertEquals("清空后 boost 应为 0", 0, afterBoost)
    }

    @Test
    fun clearOnlyRemovesUserEntriesNotBuiltin() {
        val dict = TestSQLiteDictionary(TestPaths.assetDatabase().absolutePath)
        val buCands = dict.getPinyinExactCandidates("bu")
        assertTrue("内置词库应有 '不'", buCands.any { it.text == "不" })

        userDict.recordSelection("不", "bu")
        val userCands = userDict.getUserCandidates("bu")
        assertFalse("用户词库应有记录", userCands.isEmpty())

        userDict.clearUserDictionary()
        assertTrue("用户词库清空后为空", userDict.getUserCandidates("bu").isEmpty())

        val buCandsAfter = dict.getPinyinExactCandidates("bu")
        assertTrue("内置词库仍应有 '不'", buCandsAfter.any { it.text == "不" })
    }

    private class TestUserDictionary : UserDictionaryProvider {
        private val entries = mutableMapOf<Pair<String, String>, Int>()

        override fun recordSelection(text: String, pinyin: String) {
            if (text.isEmpty() || pinyin.isEmpty()) return
            if (text.matches(Regex("^[0-9]+$"))) return
            if (text.matches(Regex("^[a-zA-Z\\s]+$"))) return
            if (text.contains(" ")) return
            val key = Pair(text, pinyin)
            entries[key] = (entries[key] ?: 0) + 1
        }

        override fun getUserCandidates(pinyin: String): List<io.github.xiwei753.pinyin.t9.core.Candidate> {
            return entries.filter { it.key.second == pinyin }
                .map { (key, count) ->
                    val score = 200000 + count * 10000
                    val type = if (key.first.length == 1)
                        io.github.xiwei753.pinyin.t9.core.CandidateType.SINGLE_CHAR
                    else
                        io.github.xiwei753.pinyin.t9.core.CandidateType.NORMAL
                    io.github.xiwei753.pinyin.t9.core.Candidate(
                        key.first, pinyin, score, type, pinyin,
                        io.github.xiwei753.pinyin.t9.core.CandidateOrigin.USER_HISTORY
                    )
                }
                .sortedByDescending { it.score }
        }

        override fun getUserBoost(pinyin: String, text: String): Int {
            return (entries[Pair(text, pinyin)] ?: 0) * 15000
        }

        override fun clearUserDictionary() {
            entries.clear()
        }

        override fun close() {}
    }
}
