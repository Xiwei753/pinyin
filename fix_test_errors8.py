with open('frontends/android-ime/native-app/android/app/src/test/java/io/github/xiwei753/pinyin/imecore/ImeCoreImportBoundaryTest.kt', 'r') as f:
    lines = f.readlines()

out = []
found_test = False
for line in lines:
    if line.strip() == "fun t9EngineDoesNotImportSQLiteDictionary() {":
        found_test = True
    if found_test and line.strip() == "}":
        found_test = False
        continue
    if not found_test and "t9EngineDoesNotImportSQLiteDictionary" not in line and "@Test" not in line or line.strip() == "}":
        out.append(line)

new_content = ''.join(out)

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

index = new_content.rfind('}')
if index != -1:
    new_content = new_content[:index] + replacer

with open('frontends/android-ime/native-app/android/app/src/test/java/io/github/xiwei753/pinyin/imecore/ImeCoreImportBoundaryTest.kt', 'w') as f:
    f.write(new_content)
