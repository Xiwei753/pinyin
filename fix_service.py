import re

with open('frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/XiweiT9ImeService.kt', 'r') as f:
    content = f.read()

content = content.replace('T9Engine(state.dictionary, userDictionary)', 'T9Engine(state.dictionary, userDictionary, debugLogger)')
content = content.replace('T9Engine(dict, userDictionary)', 'T9Engine(dict, userDictionary, debugLogger)')
content = content.replace('T9Engine(readyDict, userDictionary)', 'T9Engine(readyDict, userDictionary, debugLogger)')

with open('frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/XiweiT9ImeService.kt', 'w') as f:
    f.write(content)
