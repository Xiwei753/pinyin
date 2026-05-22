package io.github.xiwei753.pinyin.imecore

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImeCoreImportBoundaryTest {
    @Test
    fun imecoreDoesNotImportAndroidSdk() {
        val root = File("app/src/main/java/io/github/xiwei753/pinyin/imecore")
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                file.readLines().any { line ->
                    line.trim().startsWith("import android.") ||
                        line.trim().startsWith("import android.view.") ||
                        line.trim().startsWith("import android.content.") ||
                        line.trim().startsWith("import android.inputmethodservice.") ||
                        line.trim().startsWith("import android.graphics.")
                }
            }
            .map { it.path }
            .toList()

        assertTrue("imecore must not import Android SDK: $offenders", offenders.isEmpty())
    }
}
