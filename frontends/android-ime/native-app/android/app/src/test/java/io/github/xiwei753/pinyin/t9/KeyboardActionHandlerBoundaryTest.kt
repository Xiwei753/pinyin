package io.github.xiwei753.pinyin.t9

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KeyboardActionHandlerBoundaryTest {
    @Test
    fun legacyWrappersAreDeprecatedAndLogicFree() {
        val source = sourceFile("io/github/xiwei753/pinyin/t9/KeyboardActionHandler.kt").readText()
        val wrappers = listOf(
            "toggleSymbolKey",
            "toggleNumberKey",
            "onDigitPressed",
            "onSeparator",
            "onZero",
            "onDelete",
            "onSpace",
            "onCandidateClick",
            "onPunctCommit",
            "switchKeyboardMode",
            "refreshCandidates",
        )

        for (wrapper in wrappers) {
            val declarationIndex = source.indexOf("fun $wrapper")
            assertTrue("missing wrapper $wrapper", declarationIndex >= 0)
            val prefix = source.substring(maxOf(0, declarationIndex - 240), declarationIndex)
            assertTrue("$wrapper must be deprecated", prefix.contains("@Deprecated"))
        }

        assertTrue(source.contains("fun onCandidateClick(index: Int) = handle(ImeInputAction.CandidateSelected(index))"))
        assertTrue(source.contains("fun switchKeyboardMode(targetMode: KeyboardMode) = handle(ImeInputAction.KeyboardModeSelected(targetMode.toInputMode()))"))
        assertTrue(source.contains("fun refreshCandidates(limit: Int): List<CandidateSnapshotItem>"))
        assertTrue(source.contains("updateCandidateLimit(limit)"))
    }

    private fun sourceFile(relativePath: String): File {
        val fromRoot = File("app/src/main/java/$relativePath")
        if (fromRoot.exists()) return fromRoot
        return File("src/main/java/$relativePath")
    }
}
