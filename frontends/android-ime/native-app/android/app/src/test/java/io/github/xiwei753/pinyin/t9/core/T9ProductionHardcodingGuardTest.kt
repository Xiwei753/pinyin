package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.xiwei753.pinyin.t9.testutil.TestPaths
import java.io.File

class T9ProductionHardcodingGuardTest {
    @Test
    fun productionCodeDoesNotSpecialCaseGoldenInputsOrWords() {
        val productionFiles = listOf(
            "frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/core/T9Engine.kt",
            "frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/data/BuiltinDictionary.kt",
            "tools/dictionary/build_t9_assets.py"
        ).map { TestPaths.productionFile(it) }
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
