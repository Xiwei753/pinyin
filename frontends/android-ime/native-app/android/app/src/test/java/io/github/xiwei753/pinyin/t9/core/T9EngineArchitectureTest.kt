package io.github.xiwei753.pinyin.t9.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class T9EngineArchitectureTest {
    @Test
    fun t9EngineDoesNotImportSQLiteDictionary() {
        var root = File("app/src/main/java/io/github/xiwei753/pinyin/t9/core")
        if (!root.exists()) {
            root = File("src/main/java/io/github/xiwei753/pinyin/t9/core")
        }
        if (!root.exists()) return

        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("import io.github.xiwei753.pinyin.t9.data.SQLiteDictionary") }
            .map { it.path }
            .toList()

        assertTrue("T9Engine must not expose t9.data.SQLiteDictionary: $offenders", offenders.isEmpty())
    }
}
