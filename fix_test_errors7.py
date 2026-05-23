with open('frontends/android-ime/native-app/android/app/src/test/java/io/github/xiwei753/pinyin/imecore/ImeCoreImportBoundaryTest.kt', 'r') as f:
    content = f.read()

replacer = '''
    @Test
    fun t9EngineDoesNotImportSQLiteDictionary() {
        val root = File("app/src/main/java/io/github/xiwei753/pinyin/t9/core")
        val dir = if (root.exists()) root else File("src/main/java/io/github/xiwei753/pinyin/t9/core")

        val offenders = dir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("import io.github.xiwei753.pinyin.t9.data.SQLiteDictionary") }
            .map { it.path }
            .toList()

        assertTrue("T9Engine must not expose t9.data.SQLiteDictionary: $offenders", offenders.isEmpty())
    }
}'''

# Find the exact end of the class
index = content.rfind('}')
if index != -1:
    content = content[:index] + replacer

with open('frontends/android-ime/native-app/android/app/src/test/java/io/github/xiwei753/pinyin/imecore/ImeCoreImportBoundaryTest.kt', 'w') as f:
    f.write(content)
