package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class T9ProductionHardcodingGuardTest {
    @Test
    fun productionCodeDoesNotSpecialCaseGoldenInputsOrWords() {
        val productionFiles = listOf(
            File("src/main/java/io/github/xiwei753/pinyin/t9/core/T9Engine.kt"),
            File("src/main/java/io/github/xiwei753/pinyin/t9/data/BuiltinDictionary.kt"),
            File("../../../../../tools/dictionary/build_t9_assets.py")
        )

        val forbiddenTokens = listOf(
            "288249464",
            "546842692674264",
            "不太行",
            "今天晚上",
            "不太新股",
            "不太英语",
            "required_words"
        )

        for (file in productionFiles) {
            assertTrue("${file.path} should exist", file.exists())
            val content = file.readText()
            for (token in forbiddenTokens) {
                assertFalse("${file.path} must not hardcode golden token $token", content.contains(token))
            }
        }
    }
}
