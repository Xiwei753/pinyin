package io.github.xiwei753.pinyin.t9.testutil

import java.io.File

object TestPaths {
    fun repoRoot(): File {
        val rootStr = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { candidate ->
                File(candidate, "tools/dictionary/build_t9_assets.py").isFile &&
                    File(candidate, "AGENTS.md").isFile
            }?.absolutePath
            ?: throw IllegalStateException("Cannot find repository root from ${System.getProperty("user.dir")}")
        return File(rootStr)
    }

    fun androidProjectRoot(): File {
        return File(repoRoot(), "frontends/android-ime/native-app/android")
    }

    private fun generatedAssetsDir(): File {
        return File(androidProjectRoot(), "app/build/generated/t9Assets")
    }

    fun assetDictionary(): File {
        val file = File(generatedAssetsDir(), "t9_source_dict.tsv")
        require(file.isFile) { "Cannot find generated asset dictionary at ${file.absolutePath}. Run generateT9DictionaryAssets Gradle task first." }
        return file
    }

    fun assetDatabase(): File {
        val file = File(generatedAssetsDir(), "t9_dict.db")
        require(file.isFile) { "Cannot find generated asset database at ${file.absolutePath}. Run generateT9DictionaryAssets Gradle task first." }
        return file
    }

    fun productionFile(relativePath: String): File {
        val file = File(repoRoot(), relativePath)
        require(file.isFile) { "Cannot find production file at ${file.absolutePath}" }
        return file
    }
}
