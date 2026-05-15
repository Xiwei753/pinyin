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
        assertEquals("bu tai xing", comp3[0].pinyinString)

        val comp4 = composer.getCompositions("28182419464")
        assertTrue(comp4.any { it.pinyinString == "bu tai xing" })

        val comp5 = composer.getCompositions("546842692674264")
        assertEquals("jin tian wan shang", comp5[0].pinyinString)
    }
}
