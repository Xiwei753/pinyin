package io.github.xiwei753.pinyin.t9.core

import org.junit.Test
import org.junit.Assert.assertTrue

class T9EngineRealDictTest {

    @Test
    fun testMengNengWithRealDict() {
        val dict = T9EngineTest.MockDict()
        dict.add(Candidate("梦", "meng", 50000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("蒙", "meng", 40000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("萌", "meng", 30000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("猛", "meng", 20000, CandidateType.SINGLE_CHAR), "meng")
        dict.add(Candidate("孟", "meng", 10000, CandidateType.SINGLE_CHAR), "meng")

        dict.add(Candidate("能", "neng", 100000, CandidateType.SINGLE_CHAR), "neng")

        val candsMeng = dict.getSingleSyllableCandidates("meng")
        assertTrue("meng should return some valid candidates", candsMeng.isNotEmpty())
        assertTrue("meng candidates should contain '梦', '蒙', '萌', '猛', or '孟'",
            candsMeng.any { c -> c.text in listOf("梦", "蒙", "萌", "猛", "孟") })

        val candsNeng = dict.getSingleSyllableCandidates("neng")
        assertTrue("neng should return some valid candidates", candsNeng.isNotEmpty())
        assertTrue("neng candidates should contain '能'", candsNeng.any { it.text == "能" })

        val candsMeNg = dict.getPinyinExactCandidates("me ng")
        assertTrue("me ng should not return meng's candidate '梦'", candsMeNg.none { it.text == "梦" })
    }
}
