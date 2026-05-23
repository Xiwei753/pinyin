package io.github.xiwei753.pinyin.t9.core

import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Test
import org.junit.Assert.assertTrue

class T9EngineBatchQueryCountTest {
    class MockDictionaryProvider : DictionaryProvider {
        var exactCallCount = 0
        var multipleCallCount = 0
        var prefixCallCount = 0
        var singleCallCount = 0

        override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> {
            exactCallCount++
            return emptyList()
        }

        override fun getPinyinExactCandidatesMultiple(pinyinSequences: List<String>): Map<String, List<Candidate>> {
            multipleCallCount++
            return emptyMap()
        }

        override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
            prefixCallCount++
            return emptyList()
        }

        override fun getSingleSyllableCandidates(syllable: String): List<Candidate> {
            singleCallCount++
            return emptyList()
        }

        override fun getCandidates(code: String): List<Candidate> = emptyList()
        override fun getExactCandidates(code: String): List<Candidate> = emptyList()
        override fun getPrefixCandidates(code: String): List<Candidate> = emptyList()
    }

    @Test
    fun testSentenceCandidatesUsesBatchQuery() {
        val mockDict = MockDictionaryProvider()
        val engine = T9Engine(mockDict)

        // Simulating a long input "bu tai xing" (28 824 9464)
        engine.inputDigit("2")
        engine.inputDigit("8")
        engine.inputDigit("8")
        engine.inputDigit("2")
        engine.inputDigit("4")
        engine.inputDigit("9")
        engine.inputDigit("4")
        engine.inputDigit("6")
        engine.inputDigit("4")

        engine.getCandidates()

        assertTrue("Should have used batch query for sentence candidates", mockDict.multipleCallCount > 0)
    }
}
