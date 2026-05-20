with open("frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/KeyboardActionHandler.kt", "r") as f:
    content = f.read()

content = content.replace(
"""    fun setActiveReading(reading: String): Boolean {
        val success = engine?.setActiveReading(reading) ?: false
        if (success) {
            val committedText = engine?.commitReadingAndKeepBuffer(reading)
            if (committedText != null) {
                actionSink.commitText(committedText)
            }
        }
        return success
    }""",
"""    fun setActiveReading(reading: String): Boolean {
        val success = engine?.setActiveReading(reading) ?: false
        if (success) {
            actionSink.refreshUi()
        }
        return success
    }""")

with open("frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/KeyboardActionHandler.kt", "w") as f:
    f.write(content)
