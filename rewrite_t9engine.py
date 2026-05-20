import re

with open("frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/core/T9Engine.kt", "r") as f:
    content = f.read()

# Replace activeReading and readings
content = re.sub(
r"    var activeReading: String\? = null\n        private set\n\n    val readings: List<String>\n        get\(\) \{[\s\S]*?    fun getReadingDigitSpan\(reading: String\): String \{",
"""    val lockedSyllables = mutableListOf<String>()

    val activeReading: String?
        get() = lockedSyllables.lastOrNull()

    fun getValidCompositions(): List<PinyinComposition> {
        val allCompositions = pinyinComposer.getCompositions(buffer)
        if (lockedSyllables.isEmpty()) return allCompositions

        return allCompositions.filter { comp ->
            if (comp.pinyinList.size < lockedSyllables.size) return@filter false
            var match = true
            for (i in lockedSyllables.indices) {
                if (comp.pinyinList[i] != lockedSyllables[i]) {
                    match = false
                    break
                }
            }
            match
        }
    }

    val readings: List<String>
        get() {
            if (buffer.isEmpty()) return emptyList()
            val validComps = getValidCompositions()
            val result = linkedSetOf<String>()

            val nextIndex = lockedSyllables.size
            for (comp in validComps) {
                if (comp.pinyinList.size > nextIndex) {
                    val syl = comp.pinyinList[nextIndex]
                    if (syl.isNotEmpty() && syl !in result) result.add(syl)
                }
            }

            // Allow selecting shorter prefixes if available in the graph
            if (result.isEmpty() && nextIndex == 0) {
                for (end in buffer.length downTo 1) {
                    val prefix = buffer.substring(0, end)
                    for (s in PinyinSyllableDecoder.getExactSyllables(prefix)) {
                        if (s.isNotEmpty() && s !in result) result.add(s)
                    }
                }
            }

            return result.toList()
        }

    fun getReadingDigitSpan(reading: String): String {""", content, count=1)


content = re.sub(
r"    fun setActiveReading\(reading: String\): Boolean \{[\s\S]*?    fun commitReadingAndKeepBuffer\(reading: String\): String\? \{[\s\S]*?        return best\.text\n    \}",
"""    fun setActiveReading(reading: String): Boolean {
        if (buffer.isEmpty()) return false
        var currentReadings = readings
        if (reading in currentReadings) {
            lockedSyllables.add(reading)
            lastVisibleBuffer = ""
            return true
        } else if (lockedSyllables.isNotEmpty()) {
            val previous = lockedSyllables.toList()
            lockedSyllables.removeLast()
            currentReadings = readings
            if (reading in currentReadings) {
                lockedSyllables.add(reading)
                lastVisibleBuffer = ""
                return true
            } else {
                lockedSyllables.clear()
                lockedSyllables.addAll(previous)
            }
        }

        if (lockedSyllables.isNotEmpty()) {
            val previous = lockedSyllables.toList()
            lockedSyllables.clear()
            currentReadings = readings
            if (reading in currentReadings) {
                lockedSyllables.add(reading)
                lastVisibleBuffer = ""
                return true
            } else {
                lockedSyllables.addAll(previous)
            }
        }

        return false
    }

    fun commitReadingAndKeepBuffer(reading: String): String? {
        return null // Removed as we no longer commit directly when selecting reading
    }""", content, count=1)


content = content.replace("activeReading = null", "lockedSyllables.clear()")

content = re.sub(r"    fun getPreedit\(\): String \{[\s\S]*?    fun getVisibleCandidates",
"""    fun getPreedit(): String {
        if (buffer.isEmpty()) return ""
        val compositions = getValidCompositions()
        val first = compositions.firstOrNull()
        if (first == null || first.pinyinString.isEmpty()) return ""
        return first.pinyinString
    }

    fun getVisibleCandidates""", content, count=1)


content = re.sub(r"    fun getVisibleCandidates\(limit: Int = 30\): List<Candidate> \{[\s\S]*?        val combined = mutableListOf<Candidate>\(\)",
r"""    fun getVisibleCandidates(limit: Int = 30): List<Candidate> {
        if (buffer.isEmpty()) return emptyList()
        if (buffer == lastVisibleBuffer && limit == lastVisibleLimit && lastVisibleCandidates.isNotEmpty()) {
            return lastVisibleCandidates
        }

        val compositions = getValidCompositions()
        val primaryPinyin: String = compositions.firstOrNull { it.isComplete }?.pinyinString
            ?: compositions.firstOrNull()?.pinyinString ?: ""

        val userCandidates = if (primaryPinyin.isNotEmpty() && userDictionary != null) {
            userDictionary!!.getUserCandidates(primaryPinyin).map { c ->
                val boost = userDictionary!!.getUserBoost(c.sourcePinyin, c.text)
                c.copy(score = c.score + boost)
            }
        } else {
            emptyList()
        }

        val exactCandidatesRaw = generateCandidates(buffer, limit + 10, allowDynamic = false)

        val exactCandidates = exactCandidatesRaw.filter { c ->
            if (c.origin != CandidateOrigin.EXACT_SINGLE && c.origin != CandidateOrigin.EXACT_PHRASE) return@filter false
            if (c.text == buffer) return@filter false
            if (c.text.matches(Regex("^[a-zA-Z\\\\s]+$"))) return@filter false

            if (buffer.length == 1) {
                if (c.text.length > 1) return@filter false
            }

            if (buffer.length == 2) {
                if (c.text.length > 2) return@filter false
            }

            true
        }

        val hasExactPhrase = exactCandidates.any { it.origin == CandidateOrigin.EXACT_PHRASE && it.text.length >= 2 }

        val safeDynamicCandidates = if (!hasExactPhrase && buffer.length >= 5 && compositions.isNotEmpty()) {
            generateSafeDynamicCandidates(compositions, limit).map { c ->
                c.copy(origin = CandidateOrigin.SAFE_DYNAMIC_COMPOSITION)
            }
        } else {
            emptyList()
        }

        val combined = mutableListOf<Candidate>()""", content, count=1)


content = re.sub(r"    private fun generateCandidates\(currentBuffer: String, limit: Int, allowDynamic: Boolean = true,\n                                   preferredPinyin: String\? = null\): List<Candidate> \{[\s\S]*?        val compositions = if \(preferredPinyin != null\) \{[\s\S]*?        \} else \{[\s\S]*?            allCompositions\n        \}",
"""    private fun generateCandidates(currentBuffer: String, limit: Int, allowDynamic: Boolean = true): List<Candidate> {
        val compositions = getValidCompositions()""", content, count=1)


content = re.sub(r"    private fun generateSafeDynamicCandidates\(compositions: List<PinyinComposition>, limit: Int, preferredPinyin: String\? = null\): List<Candidate> \{",
"""    private fun generateSafeDynamicCandidates(compositions: List<PinyinComposition>, limit: Int): List<Candidate> {""", content, count=1)


# Also ensure inputDigit and backspace try to maintain lockedSyllables
content = content.replace(
"""    fun inputDigit(digit: String) {
        if (digit.matches(Regex("^[1-9]$"))) {
            buffer += digit
            lockedSyllables.clear()
        }
    }""",
"""    fun inputDigit(digit: String) {
        if (digit.matches(Regex("^[1-9]$"))) {
            buffer += digit
            if (getValidCompositions().isEmpty()) {
                lockedSyllables.clear()
            }
        }
    }""")

content = content.replace(
"""    fun backspace() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.substring(0, buffer.length - 1)
            lockedSyllables.clear()
        }
    }""",
"""    fun backspace() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.substring(0, buffer.length - 1)
            while (lockedSyllables.isNotEmpty() && getValidCompositions().isEmpty()) {
                lockedSyllables.removeLast()
            }
        }
    }""")


with open("frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9/core/T9Engine.kt", "w") as f:
    f.write(content)
