with open("frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/core/T9Engine.kt", "r") as f:
    content = f.read()

content = content.replace(
"""    fun inputDigit(digit: String) {
        if (digit.matches(Regex("^[1-9]$"))) {
            buffer += digit
            if (getValidCompositions().isEmpty()) {
                lockedSyllables.clear()
            }
        }
    }""",
"""    fun inputDigit(digit: String) {
        if (digit.matches(Regex("^[1-9]$"))) {
            buffer += digit
            lockedSyllables.clear()
        }
    }""")

with open("frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/core/T9Engine.kt", "w") as f:
    f.write(content)
