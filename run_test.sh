#!/bin/bash
cd /app/frontends/android-ime/native-app/android
./gradlew testDebugUnitTest --tests "io.github.xiwei753.pinyin.t9.core.T9EngineNihaoTest" > test_output.txt
cat test_output.txt
