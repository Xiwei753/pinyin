package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertTrue
import org.junit.Test

class T9BeamDecoderTest {

    @Test
    fun testDecoderKeepsShaShiHou() {
        val graph = T9SyllableGraph("742744468")
        val decoder = T9BeamDecoder(graph)
        val paths = decoder.decode()

        val containsShaShiHou = paths.any { path ->
            val text = path.edges.filter { !it.isSeparator }.joinToString(" ") { it.syllable }
            text == "sha shi hou"
        }

        assertTrue("Beam search must retain 'sha shi hou' in top 32 paths", containsShaShiHou)
    }

    @Test
    fun testDecoderPenalizesOvershoot() {
        val graph = T9SyllableGraph("7427")
        val decoder = T9BeamDecoder(graph)
        val paths = decoder.decode()

        // Ensure the top path is NOT qia qian
        val topText = paths[0].edges.filter { !it.isSeparator }.joinToString(" ") { it.syllable }
        assertTrue("Top path should not be 'qia qian'", topText != "qia qian")
    }
}
