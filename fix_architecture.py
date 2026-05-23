import re

# Update DictionaryProvider
with open('frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/data/DictionaryProvider.kt', 'r') as f:
    dict_content = f.read()

if 'val dictionaryVersion: Int' not in dict_content:
    dict_content = dict_content.replace('interface DictionaryProvider {', 'interface DictionaryProvider {\n    val dictionaryVersion: Int\n        get() = 0\n')
    with open('frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/data/DictionaryProvider.kt', 'w') as f:
        f.write(dict_content)

# Update SQLiteDictionary
with open('frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/data/SQLiteDictionary.kt', 'r') as f:
    sql_content = f.read()

if 'override val dictionaryVersion: Int' not in sql_content:
    sql_content = sql_content.replace('val loadedWordCount: Int', 'val loadedWordCount: Int\n')
    sql_content = sql_content.replace(') : DictionaryProvider {', ') : DictionaryProvider {\n\n    override val dictionaryVersion: Int\n        get() = loadedWordCount\n')
    with open('frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/data/SQLiteDictionary.kt', 'w') as f:
        f.write(sql_content)

# Update T9Engine
with open('frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/core/T9Engine.kt', 'r') as f:
    engine_content = f.read()

# Remove imports
engine_content = re.sub(r'import android\.util\.Log\n', '', engine_content)
engine_content = re.sub(r'import io\.github\.xiwei753\.pinyin\.t9\.data\.SQLiteDictionary\n', '', engine_content)

# Replace dictionary casts
engine_content = engine_content.replace('(dictionary as? SQLiteDictionary)?.loadedWordCount ?: 0', 'dictionary.dictionaryVersion')

# Replace Log.d
def log_replace(match):
    return """            val elapsed = System.currentTimeMillis() - startTime
            val preeditCount = topComps.size
            logger?.log("T9Engine", "generateCandidates: buffer=${currentBuffer.length} preedit=$preeditCount queries=$dbQueryCount time=${elapsed}ms")"""

engine_content = re.sub(r'            val elapsed = System\.currentTimeMillis\(\) - startTime\n            val preeditCount = topComps\.size\n            Log\.d\("T9Engine", "generateCandidates: buffer=\$\{currentBuffer\.length\} preedit=\$preeditCount queries=\$dbQueryCount time=\$\{elapsed\}ms"\)', log_replace, engine_content)

# Add logger constructor param
engine_content = engine_content.replace('private var userDictionary: io.github.xiwei753.pinyin.t9.data.UserDictionaryProvider? = null\n) {', 'private var userDictionary: io.github.xiwei753.pinyin.t9.data.UserDictionaryProvider? = null,\n    private val logger: io.github.xiwei753.pinyin.t9.T9DebugLogger? = null\n) {')

with open('frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/core/T9Engine.kt', 'w') as f:
    f.write(engine_content)
