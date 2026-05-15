package io.github.xiwei753.pinyin.t9.assets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class AssetsTest {
    @Test
    fun testDictionaryContainsCorrectAnZhuo() {
        val file = File("src/main/assets/t9_source_dict.tsv")
        assertNotNull(file)

        var found = false
        file.forEachLine { line ->
            val parts = line.split("\t")
            if (parts.size >= 2 && parts[0] == "安卓") {
                assertEquals("an zhuo", parts[1])
                found = true
            }
        }
        assertEquals("The word '安卓' was not found in the dictionary.", true, found)
    }
}
