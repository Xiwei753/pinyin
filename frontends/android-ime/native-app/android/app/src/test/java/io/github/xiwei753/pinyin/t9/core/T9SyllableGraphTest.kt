package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertTrue
import org.junit.Test

class T9SyllableGraphTest {

    @Test
    fun testGraphContainsShaShiHou() {
        val graph = T9SyllableGraph("742744468")

        // Find if there is a path through edges corresponding to "sha" "shi" "hou"
        val startEdge = graph.edges[0].find { it.syllable == "sha" && it.end == 3 }
        assertTrue("Graph must have edge for 'sha' starting at 0", startEdge != null)

        val midEdge = graph.edges[3].find { it.syllable == "shi" && it.end == 6 }
        assertTrue("Graph must have edge for 'shi' starting at 3", midEdge != null)

        val endEdge = graph.edges[6].find { it.syllable == "hou" && it.end == 9 }
        assertTrue("Graph must have edge for 'hou' starting at 6", endEdge != null)
    }

    @Test
    fun testGraphContainsQiaQian() {
        val graph = T9SyllableGraph("7427")
        val startEdge = graph.edges[0].find { it.syllable == "qia" && it.end == 3 }
        assertTrue("Graph must have edge for 'qia' starting at 0", startEdge != null)

        // This edge is an incomplete prefix edge
        val endEdge = graph.edges[3].find { it.syllable == "qian" && !it.isExact && it.end == 4 }
        assertTrue("Graph must have prefix edge for 'qian' starting at 3", endEdge != null)
    }
}
