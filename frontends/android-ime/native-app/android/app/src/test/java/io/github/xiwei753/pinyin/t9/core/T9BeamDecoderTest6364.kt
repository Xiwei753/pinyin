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
        var mengScore = -Int.MAX_VALUE
        var nengScore = -Int.MAX_VALUE
        var mengeScore = -Int.MAX_VALUE

        for (path in paths) {
            val text = path.edges.filter { !it.isSeparator }.joinToString(" ") { it.syllable }
            if (text == "meng") {
                hasMeng = true
                mengScore = path.score
            }
            if (text == "neng") {
                hasNeng = true
                nengScore = path.score
            }
            if (text == "men ge") {
                mengeScore = path.score
            }
        }

        assertTrue("Beam search must retain exact path 'meng'", hasMeng)
        assertTrue("Beam search must retain exact path 'neng'", hasNeng)
        assertTrue("Exact path 'neng' must score higher than fragmented 'men ge'", nengScore > mengeScore)
        assertTrue("Exact path 'meng' must score higher than fragmented 'men ge'", mengScore > mengeScore)
    }
}
