package io.github.xiwei753.pinyin.imecore

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class ImeCoreImportBoundaryTest {
    @Test
    fun imecoreDoesNotImportAndroidSdk() {
        val root = sourceDir()
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

    @Test
    fun imecoreUiStateDoesNotExposeT9Candidate() {
        val root = sourceDir()
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("t9.core.Candidate") }
            .map { it.path }
            .toList()

        assertTrue("imecore must not expose t9.core.Candidate: $offenders", offenders.isEmpty())
        assertFalse(ImeUiState().candidateStrip.candidates.any { it::class.java.name == "io.github.xiwei753.pinyin.t9.core.Candidate" })
    }

    private fun sourceDir(): File {
        val fromRoot = File("app/src/main/java/io/github/xiwei753/pinyin/imecore")
        if (fromRoot.exists()) return fromRoot
        return File("src/main/java/io/github/xiwei753/pinyin/imecore")
    }
}
