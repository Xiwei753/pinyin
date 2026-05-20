with open("frontends/android-ime/native-app/android/app/src/test/java/io/github/xiwei753/pinyin/t9/KeyboardActionHandlerTest.kt", "r") as f:
    content = f.read()

import re

# We will remove `testSetActiveReadingViaHandler` and `testSetActiveReadingCommitsText` and replace them with a test that verifies `setActiveReading` does NOT commit text
content = re.sub(
r"    @Test\n    fun testSetActiveReadingViaHandler\(\) \{[\s\S]*?    \}\n",
"""    @Test
    fun testSetActiveReadingDoesNotCommitText() {
        setupCandidates(listOf(
            Candidate("能", "636", 50000, CandidateType.SINGLE_CHAR, "neng", CandidateOrigin.EXACT_SINGLE),
            Candidate("梦", "6364", 40000, CandidateType.SINGLE_CHAR, "meng", CandidateOrigin.EXACT_SINGLE)
        ))
        handler.onDigitPressed("6")
        handler.onDigitPressed("3")
        handler.onDigitPressed("6")
        handler.onDigitPressed("4")

        val success = handler.setActiveReading("neng")
        assertTrue(success)
        verify(sink, never()).commitText(anyString())
        assertEquals("neng", handler.preedit)
    }
""", content)

content = re.sub(r"    @Test\n    fun testSetActiveReadingCommitsText\(\) \{[\s\S]*?    \}\n", "", content)


with open("frontends/android-ime/native-app/android/app/src/test/java/io/github/xiwei753/pinyin/t9/KeyboardActionHandlerTest.kt", "w") as f:
    f.write(content)
