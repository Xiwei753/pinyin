package io.github.xiwei753.pinyin.t9

import io.github.xiwei753.pinyin.t9.core.Candidate
import io.github.xiwei753.pinyin.t9.core.CandidateType
import io.github.xiwei753.pinyin.t9.core.T9Engine
import io.github.xiwei753.pinyin.t9.data.DictionaryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class KeyboardActionHandlerAsyncTest {

    private class SlowDictionary(
        private val delayMs: Long,
    ) : DictionaryProvider {
        private fun pause() {
            try {
                Thread.sleep(delayMs)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        override fun getPinyinExactCandidates(pinyinSequence: String): List<Candidate> {
            pause()
            return emptyList()
        }

        override fun getPinyinExactCandidatesMultiple(pinyinSequences: List<String>): Map<String, List<Candidate>> {
            pause()
            return pinyinSequences.associateWith { emptyList() }
        }

        override fun getPinyinPrefixCandidates(pinyinPrefix: String): List<Candidate> {
            pause()
            return emptyList()
        }

        override fun getSingleSyllableCandidates(syllable: String): List<Candidate> {
            pause()
            return emptyList()
        }

        override fun getCandidates(code: String): List<Candidate> = emptyList()
        override fun getExactCandidates(code: String): List<Candidate> = emptyList()
        override fun getPrefixCandidates(code: String): List<Candidate> = emptyList()
    }

    @Test
    fun fastDispatchDoesNotWaitForSlowCandidateWorker_onBuTaiXing() {
        val sink = mock(ImeActionSink::class.java)
        val handler = KeyboardActionHandler(sink, deferCandidateComputation = true)
        val engine = T9Engine(SlowDictionary(delayMs = 120))
        handler.attachEngine(engine)
        try {
            val start = System.nanoTime()
            for (digit in "288249464") {
                handler.onDigitPressed(digit.toString())
            }
            val elapsedMs = (System.nanoTime() - start) / 1_000_000

            assertTrue("handle path must return quickly, took ${elapsedMs}ms", elapsedMs < 200)
            assertEquals("288249464", handler.rawBuffer)
            assertEquals("bu tai xing", handler.preedit)
        } finally {
            handler.destroy()
        }
    }

    @Test
    fun fastDispatchDoesNotWaitForSlowCandidateWorker_onJinTianWanShang() {
        val sink = mock(ImeActionSink::class.java)
        val handler = KeyboardActionHandler(sink, deferCandidateComputation = true)
        val engine = T9Engine(SlowDictionary(delayMs = 120))
        handler.attachEngine(engine)
        try {
            val start = System.nanoTime()
            for (digit in "546842692674264") {
                handler.onDigitPressed(digit.toString())
            }
            val elapsedMs = (System.nanoTime() - start) / 1_000_000

            assertTrue("handle path must return quickly, took ${elapsedMs}ms", elapsedMs < 250)
            assertEquals("546842692674264", handler.rawBuffer)
            assertEquals("jin tian wan shang", handler.preedit)
        } finally {
            handler.destroy()
        }
    }
}
