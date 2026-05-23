with open('frontends/android-ime/native-app/android/app/src/test/java/io/github/xiwei753/pinyin/imecore/ImeCoreImportBoundaryTest.kt', 'r') as f:
    content = f.read()

if 'import io.github.xiwei753.pinyin.t9.data.SQLiteDictionary' in content:
    content = content.replace('import io.github.xiwei753.pinyin.t9.data.SQLiteDictionary', '')

with open('frontends/android-ime/native-app/android/app/src/test/java/io/github/xiwei753/pinyin/imecore/ImeCoreImportBoundaryTest.kt', 'w') as f:
    f.write(content)
