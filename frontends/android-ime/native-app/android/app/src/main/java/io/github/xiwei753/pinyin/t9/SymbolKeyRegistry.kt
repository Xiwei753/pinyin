package io.github.xiwei753.pinyin.t9

class SymbolKeyRegistry {

    enum class Category(val displayName: String) {
        FULLWIDTH_PUNCT("全角"),
        HALFWIDTH_PUNCT("半角"),
        MATH("数学"),
        BRACKET("括号"),
        CURRENCY("货币"),
        UNIT("单位"),
        NETWORK("网络"),
        OTHER("其他")
    }

    private data class SymbolEntry(val id: Int, val text: String, val category: Category)

    private val entries: List<SymbolEntry> = listOf(
        // Full-width punctuation (全角标点)
        SymbolEntry(R.id.sym_1, "\uFF0C", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_2, "\u3002", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_3, "\uFF1F", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_4, "\uFF01", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_5, "\uFF1A", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_6, "\uFF1B", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_7, "\u3001", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_8, "\u201C", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_9, "\u201D", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_10, "\u2018", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_11, "\u2019", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_12, "\u2014", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_13, "\u2026", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_14, "\u00B7", Category.FULLWIDTH_PUNCT),
        SymbolEntry(R.id.sym_15, "\uFF5E", Category.FULLWIDTH_PUNCT),
        // Half-width punctuation (半角标点)
        SymbolEntry(R.id.sym_43, "/", Category.HALFWIDTH_PUNCT),
        SymbolEntry(R.id.sym_44, "\\", Category.HALFWIDTH_PUNCT),
        SymbolEntry(R.id.sym_52, "_", Category.HALFWIDTH_PUNCT),
        SymbolEntry(R.id.sym_53, "~", Category.HALFWIDTH_PUNCT),
        SymbolEntry(R.id.sym_54, "`", Category.HALFWIDTH_PUNCT),
        // Math (数学)
        SymbolEntry(R.id.sym_16, "+", Category.MATH),
        SymbolEntry(R.id.sym_17, "-", Category.MATH),
        SymbolEntry(R.id.sym_18, "\u00D7", Category.MATH),
        SymbolEntry(R.id.sym_19, "\u00F7", Category.MATH),
        SymbolEntry(R.id.sym_20, "=", Category.MATH),
        SymbolEntry(R.id.sym_21, "%", Category.MATH),
        SymbolEntry(R.id.sym_22, "&", Category.MATH),
        SymbolEntry(R.id.sym_23, "|", Category.MATH),
        SymbolEntry(R.id.sym_24, "\u221A", Category.MATH),
        SymbolEntry(R.id.sym_25, "\u2248", Category.MATH),
        SymbolEntry(R.id.sym_26, "\u2260", Category.MATH),
        SymbolEntry(R.id.sym_27, "\u2264", Category.MATH),
        SymbolEntry(R.id.sym_28, "\u2265", Category.MATH),
        SymbolEntry(R.id.sym_29, "\u00B1", Category.MATH),
        SymbolEntry(R.id.sym_30, "\u221E", Category.MATH),
        SymbolEntry(R.id.sym_50, "*", Category.MATH),
        SymbolEntry(R.id.sym_51, "^", Category.MATH),
        // Brackets (括号)
        SymbolEntry(R.id.sym_31, "\uFF08", Category.BRACKET),
        SymbolEntry(R.id.sym_32, "\uFF09", Category.BRACKET),
        SymbolEntry(R.id.sym_33, "\u3010", Category.BRACKET),
        SymbolEntry(R.id.sym_34, "\u3011", Category.BRACKET),
        SymbolEntry(R.id.sym_35, "{", Category.BRACKET),
        SymbolEntry(R.id.sym_36, "}", Category.BRACKET),
        SymbolEntry(R.id.sym_37, "\u300A", Category.BRACKET),
        SymbolEntry(R.id.sym_38, "\u300B", Category.BRACKET),
        SymbolEntry(R.id.sym_39, "[", Category.BRACKET),
        SymbolEntry(R.id.sym_40, "]", Category.BRACKET),
        SymbolEntry(R.id.sym_41, "<", Category.BRACKET),
        SymbolEntry(R.id.sym_42, ">", Category.BRACKET),
        // Currency (货币)
        SymbolEntry(R.id.sym_48, "\uFFE5", Category.CURRENCY),
        SymbolEntry(R.id.sym_49, "$", Category.CURRENCY),
        SymbolEntry(R.id.sym_55, "\u20AC", Category.CURRENCY),
        SymbolEntry(R.id.sym_56, "\u00A3", Category.CURRENCY),
        SymbolEntry(R.id.sym_57, "\u00A5", Category.CURRENCY),
        // Unit (单位)
        SymbolEntry(R.id.sym_45, "\u00A7", Category.UNIT),
        // Network/Email (网络/邮箱)
        SymbolEntry(R.id.sym_46, "@", Category.NETWORK),
        SymbolEntry(R.id.sym_47, "#", Category.NETWORK),
        // Other (其他)
        SymbolEntry(R.id.sym_58, "\u00A9", Category.OTHER),
        SymbolEntry(R.id.sym_59, "\u00AE", Category.OTHER),
        SymbolEntry(R.id.sym_60, "\u2122", Category.OTHER),
    )

    private val symbolMap: Map<Int, String> by lazy {
        entries.associate { it.id to it.text }
    }

    private val categoryMap: Map<Category, List<SymbolEntry>> by lazy {
        entries.groupBy { it.category }
    }

    fun getSymbolText(id: Int): String? = symbolMap[id]

    fun getAllSymbolIds(): List<Int> = symbolMap.keys.toList()

    fun getAllSymbolEntries(): List<Pair<Int, String>> = entries.map { it.id to it.text }

    fun getSymbolsByCategory(category: Category): List<Pair<Int, String>> =
        categoryMap[category]?.map { it.id to it.text } ?: emptyList()

    fun getAllCategories(): List<Category> = entries.map { it.category }.distinct()
}
