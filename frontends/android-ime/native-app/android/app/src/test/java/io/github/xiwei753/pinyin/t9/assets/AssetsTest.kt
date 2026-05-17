package io.github.xiwei753.pinyin.t9.assets

import io.github.xiwei753.pinyin.t9.data.BuiltinDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class AssetsTest {
    @Test
    fun testDictionaryContainsCorrectAnZhuo() {
        val file = File("src/main/assets/t9_source_dict.tsv")
        assertTrue("Dictionary file should exist", file.exists())
        assertTrue("Dictionary file should have content", file.length() > 0)

        var lineCount = 0
        var foundAnZhuo = false
        var foundShaShiHou = false
        val foundCommonWords = mutableSetOf<String>()
        val commonWordsToFind = setOf(
            "你好", "输入法", "中国", "今天", "手机", "电脑", "安卓", "我", "你", "他", "她", "的", "得", "地", "不", "是",
            "么", "美", "没", "每", "妹", "梦", "蒙", "萌", "猛", "孟", "能", "不太行", "今天晚上"
        )

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
            if (word == "啥时候") {
                assertEquals("sha shi hou", pinyin)
                foundShaShiHou = true
            }
        }

        assertTrue("Dictionary should have more than 30000 lines, but has $lineCount", lineCount > 30000)
        assertTrue("The word '安卓' was not found in the dictionary.", foundAnZhuo)
        assertTrue("The phrase '啥时候' from android_common_phrases.tsv was not found.", foundShaShiHou)

        for (expectedWord in commonWordsToFind) {
             assertTrue("The word '$expectedWord' was not found in the dictionary.", foundCommonWords.contains(expectedWord))
        }

        // Load the actual dictionary to test exact rules
        val inputStream = FileInputStream(file)
        val dictionary = BuiltinDictionary(inputStream)

        val meCandidates = dictionary.getSingleSyllableCandidates("me")
        assertTrue("getSingleSyllableCandidates(\"me\") MUST NOT return 'mei' candidates",
            meCandidates.none { it.sourcePinyin == "mei" || it.text in listOf("美", "没", "每", "妹") })

        val meiCandidates = dictionary.getSingleSyllableCandidates("mei")
        assertTrue("getSingleSyllableCandidates(\"mei\") MUST contain at least one of 美/没/每/妹",
            meiCandidates.any { it.text in listOf("美", "没", "每", "妹") })

        val mengCandidates = dictionary.getSingleSyllableCandidates("meng")
        assertTrue("getSingleSyllableCandidates(\"meng\") MUST contain at least one of 梦/蒙/萌/猛/孟",
            mengCandidates.any { it.text in listOf("梦", "蒙", "萌", "猛", "孟") })

        val nengCandidates = dictionary.getSingleSyllableCandidates("neng")
        assertTrue("getSingleSyllableCandidates(\"neng\") MUST contain 能",
            nengCandidates.any { it.text == "能" })

        val meNgCandidates = dictionary.getPinyinExactCandidates("me ng")
        assertTrue("getPinyinExactCandidates(\"me ng\") MUST NOT return 'meng' candidates",
            meNgCandidates.none { it.text in listOf("梦", "蒙", "萌", "猛", "孟") })
    }
}
