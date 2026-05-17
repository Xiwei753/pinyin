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
    fun testGraphExactEdgesOnly() {
        val graph = T9SyllableGraph("7427")
        val startEdge = graph.edges[0].find { it.syllable == "qia" && it.end == 3 }
        assertTrue("Graph must have edge for 'qia' starting at 0", startEdge != null)

        // Graph should NOT contain incomplete prefix edges like 'qian' with length 1
        val endEdge = graph.edges[3].find { it.syllable == "qian" }
        assertTrue("Graph must NOT have prefix edge for 'qian'", endEdge == null)

        // All non-raw edges should be exact
        val allEdges = graph.edges.flatMap { it.toList() }
        assertTrue(allEdges.filter { !it.isRaw }.all { it.isExact })
    }

    @Test
    fun testGraph63MeNotMei() {
        val graph = T9SyllableGraph("63")
        val meEdge = graph.edges[0].find { it.syllable == "me" && it.end == 2 }
        assertTrue("Graph must have edge for 'me'", meEdge != null)

        val meiEdge = graph.edges[0].find { it.syllable == "mei" }
        assertTrue("Graph must NOT have prefix edge for 'mei' from 63", meiEdge == null)
    }

    @Test
    fun testGraph634Mei() {
        val graph = T9SyllableGraph("634")
        val meiEdge = graph.edges[0].find { it.syllable == "mei" && it.end == 3 }
        assertTrue("Graph must have edge for 'mei'", meiEdge != null)
    }

    @Test
    fun testGraph6364MengNeng() {
        val graph = T9SyllableGraph("6364")
        val mengEdge = graph.edges[0].find { it.syllable == "meng" && it.end == 4 }
        assertTrue("Graph must have edge for 'meng'", mengEdge != null)

        val nengEdge = graph.edges[0].find { it.syllable == "neng" && it.end == 4 }
        assertTrue("Graph must have edge for 'neng'", nengEdge != null)
    }
}
