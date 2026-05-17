package io.github.xiwei753.pinyin.t9.testutil

import java.io.File

object TestPaths {
    fun repoRoot(): File {
        val rootStr = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { candidate ->
                File(candidate, "tools/dictionary/build_t9_assets.py").isFile &&
                    File(candidate, "frontends/android-ime/native-app/android/app/src/main/assets/t9_source_dict.tsv").isFile
            }?.absolutePath
            ?: throw IllegalStateException("Cannot find repository root from ${System.getProperty("user.dir")}")
        return File(rootStr)
    }

    fun androidProjectRoot(): File {
        return File(repoRoot(), "frontends/android-ime/native-app/android")
    }

    fun assetDictionary(): File {
        val file = File(androidProjectRoot(), "app/src/main/assets/t9_source_dict.tsv")
        require(file.isFile) { "Cannot find real asset dictionary at ${file.absolutePath}" }
        return file
    }

    fun productionFile(relativePath: String): File {
        val file = File(repoRoot(), relativePath)
        require(file.isFile) { "Cannot find production file at ${file.absolutePath}" }
        return file
    }
}
