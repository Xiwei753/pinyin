package io.github.xiwei753.pinyin.t9.core

import org.junit.Test
import org.junit.Assert.assertTrue

class T9BeamDecoderTest6364 {
    @Test
    fun test6364RetainsMengAndNeng() {
        val graph = T9SyllableGraph("6364")
        val decoder = T9BeamDecoder(graph)
        val paths = decoder.decode()

        var hasMeng = false
        var hasNeng = false

        for (path in paths) {
            val text = path.edges.filter { !it.isSeparator }.joinToString(" ") { it.syllable }
            if (text == "meng") hasMeng = true
            if (text == "neng") hasNeng = true
        }

        assertTrue("Beam search must retain exact path 'meng'", hasMeng)
        assertTrue("Beam search must retain exact path 'neng'", hasNeng)
    }
}
