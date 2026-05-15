import re

with open("frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/data/BuiltinDictionary.kt", "r") as f:
    text = f.read()

text = text.replace('pinyinExact["nihao"] = mutableListOf(candidate1)', '''
            exactMap["64426"] = mutableListOf(candidate1)
            prefixMap["6"] = mutableListOf(candidate1)
            prefixMap["64"] = mutableListOf(candidate1)
            prefixMap["644"] = mutableListOf(candidate1)
            prefixMap["6442"] = mutableListOf(candidate1)
            prefixMap["64426"] = mutableListOf(candidate1)

            exactMap["7487832"] = mutableListOf(candidate2)
            prefixMap["7"] = mutableListOf(candidate2)
            prefixMap["74"] = mutableListOf(candidate2)
            prefixMap["748"] = mutableListOf(candidate2)
            prefixMap["7487"] = mutableListOf(candidate2)
            prefixMap["74878"] = mutableListOf(candidate2)
            prefixMap["748783"] = mutableListOf(candidate2)
            prefixMap["7487832"] = mutableListOf(candidate2)

            pinyinExact["nihao"] = mutableListOf(candidate1)''')

with open("frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/data/BuiltinDictionary.kt", "w") as f:
    f.write(text)
