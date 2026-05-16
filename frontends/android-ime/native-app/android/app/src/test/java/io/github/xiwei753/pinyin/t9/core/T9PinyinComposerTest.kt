package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class T9PinyinComposerTest {
    @Test
    fun testComposer() {
        val composer = T9PinyinComposer()

        val comp1 = composer.getCompositions("28")
        assertTrue(comp1.any { it.pinyinString == "bu" })

        val comp2 = composer.getCompositions("28824")
        assertTrue(comp2.any { it.pinyinString == "bu tai" })

        val comp3 = composer.getCompositions("288249464")
        assertTrue(comp3.any { it.pinyinString == "bu tai xing" })
        assertTrue("bu tai xing must be in top 16", comp3.indexOfFirst { it.pinyinString == "bu tai xing" } < 16)

        val comp4 = composer.getCompositions("28182419464")
        assertTrue(comp4.any { it.pinyinString == "bu tai xing" })

        val comp5 = composer.getCompositions("546842692674264")
        assertTrue(comp5.any { it.pinyinString == "jin tian wan shang" })
        assertTrue("jin tian wan shang must be in top 16", comp5.indexOfFirst { it.pinyinString == "jin tian wan shang" } < 16)

        // 742744468 的高分路径必须包含并优先 sha shi hou
        val compShaShiHou = composer.getCompositions("742744468")
        val shaShiHouIndex = compShaShiHou.indexOfFirst { it.pinyinString == "sha shi hou" }
        assertTrue("Must contain sha shi hou", shaShiHouIndex != -1)
        assertTrue("sha shi hou must be in top 16", shaShiHouIndex < 16)

        // 7427 的 top composition 不能是 qia qian
        val compQiaQian = composer.getCompositions("7427")
        assertTrue(compQiaQian.isNotEmpty() && compQiaQian[0].pinyinString != "qia qian")

        // 74274468 的 top composition 不能是 qia ri gou
        val compQiaRiGou = composer.getCompositions("74274468")
        assertTrue(compQiaRiGou.isNotEmpty() && compQiaRiGou[0].pinyinString != "qia ri gou")

        val comp6364 = composer.getCompositions("6364")
        assertTrue(comp6364.any { it.pinyinString == "neng" })
        assertTrue(comp6364.any { it.pinyinString == "meng" })
        assertTrue(comp6364.isNotEmpty() && comp6364[0].pinyinString != "men ge")

        val mengGeIndex = comp6364.indexOfFirst { it.pinyinString == "men ge" }
        val nengIndex = comp6364.indexOfFirst { it.pinyinString == "neng" }

        // Either men ge is completely pruned, or it ranks lower than neng
        assertTrue("men ge must be severely penalized", mengGeIndex == -1 || mengGeIndex > nengIndex)
    }
}
