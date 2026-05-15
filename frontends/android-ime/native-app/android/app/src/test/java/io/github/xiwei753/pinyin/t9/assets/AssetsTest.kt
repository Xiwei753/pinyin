package io.github.xiwei753.pinyin.t9.assets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AssetsTest {
    @Test
    fun testDictionaryContainsCorrectAnZhuo() {
        val file = File("src/main/assets/t9_source_dict.tsv")
        assertTrue("Dictionary file should exist", file.exists())
        assertTrue("Dictionary file should have content", file.length() > 0)

        var lineCount = 0
        var foundAnZhuo = false
        val foundCommonWords = mutableSetOf<String>()
        val commonWordsToFind = setOf("你好", "输入法", "中国", "今天", "手机", "电脑", "安卓")

        file.forEachLine { line ->
            lineCount++
            val parts = line.split("\t")
            assertTrue("Line should have at least 3 columns", parts.size >= 3)

            val word = parts[0]
            val pinyin = parts[1]

            if (commonWordsToFind.contains(word)) {
                foundCommonWords.add(word)
            }

            if (word == "安卓") {
                assertEquals("an zhuo", pinyin)
                foundAnZhuo = true
            }
        }

        assertTrue("Dictionary should have more than 20000 lines, but has $lineCount", lineCount > 20000)
        assertTrue("The word '安卓' was not found in the dictionary.", foundAnZhuo)

        for (expectedWord in commonWordsToFind) {
             assertTrue("The word '$expectedWord' was not found in the dictionary.", foundCommonWords.contains(expectedWord))
        }
    }
}
