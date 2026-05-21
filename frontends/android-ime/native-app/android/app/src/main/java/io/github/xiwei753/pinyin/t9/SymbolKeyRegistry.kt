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
        SEQUENCE("序号"),
        ARROW("箭头"),
        GREEK("希腊"),
        OTHER("其他")
    }

    private var nextId = 0
    private fun id() = ++nextId

    private data class SymbolEntry(val id: Int, val text: String, val category: Category)

    private val entries: List<SymbolEntry> = listOf(
        // ── 全角标点 (Full-width punctuation) ──
        SymbolEntry(id(), "\uFF0C", Category.FULLWIDTH_PUNCT),   // ，
        SymbolEntry(id(), "\u3002", Category.FULLWIDTH_PUNCT),   // 。
        SymbolEntry(id(), "\uFF1F", Category.FULLWIDTH_PUNCT),   // ？
        SymbolEntry(id(), "\uFF01", Category.FULLWIDTH_PUNCT),   // ！
        SymbolEntry(id(), "\uFF1A", Category.FULLWIDTH_PUNCT),   // ：
        SymbolEntry(id(), "\uFF1B", Category.FULLWIDTH_PUNCT),   // ；
        SymbolEntry(id(), "\u3001", Category.FULLWIDTH_PUNCT),   // 、
        SymbolEntry(id(), "\u201C", Category.FULLWIDTH_PUNCT),   // “
        SymbolEntry(id(), "\u201D", Category.FULLWIDTH_PUNCT),   // ”
        SymbolEntry(id(), "\u2018", Category.FULLWIDTH_PUNCT),   // ‘
        SymbolEntry(id(), "\u2019", Category.FULLWIDTH_PUNCT),   // ’
        SymbolEntry(id(), "\u2014", Category.FULLWIDTH_PUNCT),   // —
        SymbolEntry(id(), "\u2026", Category.FULLWIDTH_PUNCT),   // …
        SymbolEntry(id(), "\u00B7", Category.FULLWIDTH_PUNCT),   // ·
        SymbolEntry(id(), "\uFF5E", Category.FULLWIDTH_PUNCT),   // ～
        SymbolEntry(id(), "\u300A", Category.FULLWIDTH_PUNCT),   // 《
        SymbolEntry(id(), "\u300B", Category.FULLWIDTH_PUNCT),   // 》
        SymbolEntry(id(), "\u3008", Category.FULLWIDTH_PUNCT),   // 〈
        SymbolEntry(id(), "\u3009", Category.FULLWIDTH_PUNCT),   // 〉
        SymbolEntry(id(), "\u300C", Category.FULLWIDTH_PUNCT),   // 「
        SymbolEntry(id(), "\u300D", Category.FULLWIDTH_PUNCT),   // 」
        SymbolEntry(id(), "\u300E", Category.FULLWIDTH_PUNCT),   // 『
        SymbolEntry(id(), "\u300F", Category.FULLWIDTH_PUNCT),   // 』
        SymbolEntry(id(), "\u2013", Category.FULLWIDTH_PUNCT),   // –

        // ── 半角标点 (Half-width punctuation) ──
        SymbolEntry(id(), ",", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), ".", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "?", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "!", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), ":", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), ";", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "\"", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "'", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "/", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "\\", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "_", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "~", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "`", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "|", Category.HALFWIDTH_PUNCT),
        SymbolEntry(id(), "&", Category.HALFWIDTH_PUNCT),

        // ── 数学 (Math) ──
        SymbolEntry(id(), "+", Category.MATH),
        SymbolEntry(id(), "-", Category.MATH),
        SymbolEntry(id(), "\u00D7", Category.MATH),   // ×
        SymbolEntry(id(), "\u00F7", Category.MATH),   // ÷
        SymbolEntry(id(), "=", Category.MATH),
        SymbolEntry(id(), "%", Category.MATH),
        SymbolEntry(id(), "\u2030", Category.MATH),   // ‰
        SymbolEntry(id(), "\u221A", Category.MATH),   // √
        SymbolEntry(id(), "\u2248", Category.MATH),   // ≈
        SymbolEntry(id(), "\u2260", Category.MATH),   // ≠
        SymbolEntry(id(), "\u2264", Category.MATH),   // ≤
        SymbolEntry(id(), "\u2265", Category.MATH),   // ≥
        SymbolEntry(id(), "\u00B1", Category.MATH),   // ±
        SymbolEntry(id(), "\u221E", Category.MATH),   // ∞
        SymbolEntry(id(), "*", Category.MATH),
        SymbolEntry(id(), "^", Category.MATH),
        SymbolEntry(id(), "\u2220", Category.MATH),   // ∠
        SymbolEntry(id(), "\u22A5", Category.MATH),   // ⊥
        SymbolEntry(id(), "\u25B3", Category.MATH),   // △
        SymbolEntry(id(), "\u2234", Category.MATH),   // ∴
        SymbolEntry(id(), "\u2235", Category.MATH),   // ∵
        SymbolEntry(id(), "\u2202", Category.MATH),   // ∂
        SymbolEntry(id(), "\u2207", Category.MATH),   // ∇
        SymbolEntry(id(), "\u220F", Category.MATH),   // ∏
        SymbolEntry(id(), "\u2211", Category.MATH),   // ∑
        SymbolEntry(id(), "\u00B0", Category.MATH),   // °
        SymbolEntry(id(), "\u2032", Category.MATH),   // ′
        SymbolEntry(id(), "\u2033", Category.MATH),   // ″

        // ── 括号 (Brackets) ──
        SymbolEntry(id(), "\uFF08", Category.BRACKET),   // （
        SymbolEntry(id(), "\uFF09", Category.BRACKET),   // ）
        SymbolEntry(id(), "\u3010", Category.BRACKET),   // 【
        SymbolEntry(id(), "\u3011", Category.BRACKET),   // 】
        SymbolEntry(id(), "{", Category.BRACKET),
        SymbolEntry(id(), "}", Category.BRACKET),
        SymbolEntry(id(), "[", Category.BRACKET),
        SymbolEntry(id(), "]", Category.BRACKET),
        SymbolEntry(id(), "<", Category.BRACKET),
        SymbolEntry(id(), ">", Category.BRACKET),
        SymbolEntry(id(), "\u300A", Category.BRACKET),   // 《
        SymbolEntry(id(), "\u300B", Category.BRACKET),   // 》
        SymbolEntry(id(), "\u300C", Category.BRACKET),   // 「
        SymbolEntry(id(), "\u300D", Category.BRACKET),   // 」
        SymbolEntry(id(), "\u300E", Category.BRACKET),   // 『
        SymbolEntry(id(), "\u300F", Category.BRACKET),   // 』
        SymbolEntry(id(), "\u3014", Category.BRACKET),   // 〔
        SymbolEntry(id(), "\u3015", Category.BRACKET),   // 〕

        // ── 货币 (Currency) ──
        SymbolEntry(id(), "\uFFE5", Category.CURRENCY),   // ￥
        SymbolEntry(id(), "$", Category.CURRENCY),
        SymbolEntry(id(), "\u20AC", Category.CURRENCY),   // €
        SymbolEntry(id(), "\u00A3", Category.CURRENCY),   // £
        SymbolEntry(id(), "\u00A5", Category.CURRENCY),   // ¥
        SymbolEntry(id(), "\u20B1", Category.CURRENCY),   // ₱
        SymbolEntry(id(), "\u20A9", Category.CURRENCY),   // ₩
        SymbolEntry(id(), "\u20AA", Category.CURRENCY),   // ₪
        SymbolEntry(id(), "\u20BD", Category.CURRENCY),   // ₽
        SymbolEntry(id(), "\u00A2", Category.CURRENCY),   // ¢

        // ── 单位 (Units) ──
        SymbolEntry(id(), "\u00A7", Category.UNIT),   // §
        SymbolEntry(id(), "\u00B0", Category.UNIT),   // °
        SymbolEntry(id(), "\u2032", Category.UNIT),   // ′
        SymbolEntry(id(), "\u2033", Category.UNIT),   // ″
        SymbolEntry(id(), "%", Category.UNIT),
        SymbolEntry(id(), "\u2030", Category.UNIT),   // ‰
        SymbolEntry(id(), "\u2116", Category.UNIT),   // №
        SymbolEntry(id(), "\u00D7", Category.UNIT),   // × (as dimension)
        SymbolEntry(id(), "\u339B", Category.UNIT),   // ㎡
        SymbolEntry(id(), "\u33A5", Category.UNIT),   // ㎥
        SymbolEntry(id(), "\u339D", Category.UNIT),   // ㎝
        SymbolEntry(id(), "\u339C", Category.UNIT),   // ㎜
        SymbolEntry(id(), "\u33A1", Category.UNIT),   // ㎡
        SymbolEntry(id(), "\u33A4", Category.UNIT),   // ㎤
        SymbolEntry(id(), "\u33A6", Category.UNIT),   // ㎦
        SymbolEntry(id(), "\u33B1", Category.UNIT),   // ㎱
        SymbolEntry(id(), "\u33B2", Category.UNIT),   // ㎲
        SymbolEntry(id(), "\u33B3", Category.UNIT),   // ㎳
        SymbolEntry(id(), "\u33BA", Category.UNIT),   // ㎺
        SymbolEntry(id(), "\u33BB", Category.UNIT),   // ㎻
        SymbolEntry(id(), "\u33BC", Category.UNIT),   // ㎼
        SymbolEntry(id(), "\u33BD", Category.UNIT),   // ㎽

        // ── 网络/邮箱 (Network/Email) ──
        SymbolEntry(id(), "@", Category.NETWORK),
        SymbolEntry(id(), "#", Category.NETWORK),
        SymbolEntry(id(), "/", Category.NETWORK),
        SymbolEntry(id(), "\\", Category.NETWORK),
        SymbolEntry(id(), "_", Category.NETWORK),
        SymbolEntry(id(), "-", Category.NETWORK),
        SymbolEntry(id(), ".", Category.NETWORK),
        SymbolEntry(id(), "~", Category.NETWORK),
        SymbolEntry(id(), "|", Category.NETWORK),

        // ── 序号 (Sequence numbers) ──
        SymbolEntry(id(), "①", Category.SEQUENCE),
        SymbolEntry(id(), "②", Category.SEQUENCE),
        SymbolEntry(id(), "③", Category.SEQUENCE),
        SymbolEntry(id(), "④", Category.SEQUENCE),
        SymbolEntry(id(), "⑤", Category.SEQUENCE),
        SymbolEntry(id(), "⑥", Category.SEQUENCE),
        SymbolEntry(id(), "⑦", Category.SEQUENCE),
        SymbolEntry(id(), "⑧", Category.SEQUENCE),
        SymbolEntry(id(), "⑨", Category.SEQUENCE),
        SymbolEntry(id(), "⑩", Category.SEQUENCE),
        SymbolEntry(id(), "\u2160", Category.SEQUENCE),   // Ⅰ
        SymbolEntry(id(), "\u2161", Category.SEQUENCE),   // Ⅱ
        SymbolEntry(id(), "\u2162", Category.SEQUENCE),   // Ⅲ
        SymbolEntry(id(), "\u2163", Category.SEQUENCE),   // Ⅳ
        SymbolEntry(id(), "\u2164", Category.SEQUENCE),   // Ⅴ
        SymbolEntry(id(), "\u2165", Category.SEQUENCE),   // Ⅵ
        SymbolEntry(id(), "\u2166", Category.SEQUENCE),   // Ⅶ
        SymbolEntry(id(), "\u2167", Category.SEQUENCE),   // Ⅷ
        SymbolEntry(id(), "\u2168", Category.SEQUENCE),   // Ⅸ
        SymbolEntry(id(), "\u2169", Category.SEQUENCE),   // Ⅹ
        SymbolEntry(id(), "\u2460", Category.SEQUENCE),   // ⑴
        SymbolEntry(id(), "\u2461", Category.SEQUENCE),   // ⑵
        SymbolEntry(id(), "\u2462", Category.SEQUENCE),   // ⑶

        // ── 箭头 (Arrows) ──
        SymbolEntry(id(), "\u2190", Category.ARROW),   // ←
        SymbolEntry(id(), "\u2191", Category.ARROW),   // ↑
        SymbolEntry(id(), "\u2192", Category.ARROW),   // →
        SymbolEntry(id(), "\u2193", Category.ARROW),   // ↓
        SymbolEntry(id(), "\u2194", Category.ARROW),   // ↔
        SymbolEntry(id(), "\u2195", Category.ARROW),   // ↕
        SymbolEntry(id(), "\u21D0", Category.ARROW),   // ⇐
        SymbolEntry(id(), "\u21D1", Category.ARROW),   // ⇑
        SymbolEntry(id(), "\u21D2", Category.ARROW),   // ⇒
        SymbolEntry(id(), "\u21D3", Category.ARROW),   // ⇓
        SymbolEntry(id(), "\u21D4", Category.ARROW),   // ⇔
        SymbolEntry(id(), "\u21E7", Category.ARROW),   // ⇧
        SymbolEntry(id(), "\u27A1", Category.ARROW),   // ➡
        SymbolEntry(id(), "\u2B05", Category.ARROW),   // ⬅
        SymbolEntry(id(), "\u2B06", Category.ARROW),   // ⬆
        SymbolEntry(id(), "\u2B07", Category.ARROW),   // ⬇

        // ── 希腊字母 (Greek letters) ──
        SymbolEntry(id(), "\u0391", Category.GREEK),   // Α
        SymbolEntry(id(), "\u0392", Category.GREEK),   // Β
        SymbolEntry(id(), "\u0393", Category.GREEK),   // Γ
        SymbolEntry(id(), "\u0394", Category.GREEK),   // Δ
        SymbolEntry(id(), "\u0395", Category.GREEK),   // Ε
        SymbolEntry(id(), "\u0396", Category.GREEK),   // Ζ
        SymbolEntry(id(), "\u0397", Category.GREEK),   // Η
        SymbolEntry(id(), "\u0398", Category.GREEK),   // Θ
        SymbolEntry(id(), "\u0399", Category.GREEK),   // Ι
        SymbolEntry(id(), "\u039A", Category.GREEK),   // Κ
        SymbolEntry(id(), "\u039B", Category.GREEK),   // Λ
        SymbolEntry(id(), "\u039C", Category.GREEK),   // Μ
        SymbolEntry(id(), "\u039D", Category.GREEK),   // Ν
        SymbolEntry(id(), "\u039E", Category.GREEK),   // Ξ
        SymbolEntry(id(), "\u039F", Category.GREEK),   // Ο
        SymbolEntry(id(), "\u03A0", Category.GREEK),   // Π
        SymbolEntry(id(), "\u03A1", Category.GREEK),   // Ρ
        SymbolEntry(id(), "\u03A3", Category.GREEK),   // Σ
        SymbolEntry(id(), "\u03A4", Category.GREEK),   // Τ
        SymbolEntry(id(), "\u03A5", Category.GREEK),   // Υ
        SymbolEntry(id(), "\u03A6", Category.GREEK),   // Φ
        SymbolEntry(id(), "\u03A7", Category.GREEK),   // Χ
        SymbolEntry(id(), "\u03A8", Category.GREEK),   // Ψ
        SymbolEntry(id(), "\u03A9", Category.GREEK),   // Ω
        SymbolEntry(id(), "\u03B1", Category.GREEK),   // α
        SymbolEntry(id(), "\u03B2", Category.GREEK),   // β
        SymbolEntry(id(), "\u03B3", Category.GREEK),   // γ
        SymbolEntry(id(), "\u03B4", Category.GREEK),   // δ
        SymbolEntry(id(), "\u03B5", Category.GREEK),   // ε
        SymbolEntry(id(), "\u03B6", Category.GREEK),   // ζ
        SymbolEntry(id(), "\u03B7", Category.GREEK),   // η
        SymbolEntry(id(), "\u03B8", Category.GREEK),   // θ
        SymbolEntry(id(), "\u03B9", Category.GREEK),   // ι
        SymbolEntry(id(), "\u03BA", Category.GREEK),   // κ
        SymbolEntry(id(), "\u03BB", Category.GREEK),   // λ
        SymbolEntry(id(), "\u03BC", Category.GREEK),   // μ
        SymbolEntry(id(), "\u03BD", Category.GREEK),   // ν
        SymbolEntry(id(), "\u03BE", Category.GREEK),   // ξ
        SymbolEntry(id(), "\u03BF", Category.GREEK),   // ο
        SymbolEntry(id(), "\u03C0", Category.GREEK),   // π
        SymbolEntry(id(), "\u03C1", Category.GREEK),   // ρ
        SymbolEntry(id(), "\u03C3", Category.GREEK),   // σ
        SymbolEntry(id(), "\u03C4", Category.GREEK),   // τ
        SymbolEntry(id(), "\u03C5", Category.GREEK),   // υ
        SymbolEntry(id(), "\u03C6", Category.GREEK),   // φ
        SymbolEntry(id(), "\u03C7", Category.GREEK),   // χ
        SymbolEntry(id(), "\u03C8", Category.GREEK),   // ψ
        SymbolEntry(id(), "\u03C9", Category.GREEK),   // ω

        // ── 其他 (Other) ──
        SymbolEntry(id(), "\u00A9", Category.OTHER),   // ©
        SymbolEntry(id(), "\u00AE", Category.OTHER),   // ®
        SymbolEntry(id(), "\u2122", Category.OTHER),   // ™
        SymbolEntry(id(), "\u2117", Category.OTHER),   // ℗
        SymbolEntry(id(), "\u2605", Category.OTHER),   // ★
        SymbolEntry(id(), "\u2606", Category.OTHER),   // ☆
        SymbolEntry(id(), "\u2600", Category.OTHER),   // ☀
        SymbolEntry(id(), "\u2601", Category.OTHER),   // ☁
        SymbolEntry(id(), "\u2614", Category.OTHER),   // ☔
        SymbolEntry(id(), "\u2665", Category.OTHER),   // ♥
        SymbolEntry(id(), "\u2660", Category.OTHER),   // ♠
        SymbolEntry(id(), "\u2666", Category.OTHER),   // ♦
        SymbolEntry(id(), "\u2663", Category.OTHER),   // ♣
        SymbolEntry(id(), "\u2714", Category.OTHER),   // ✔
        SymbolEntry(id(), "\u2716", Category.OTHER),   // ✖
        SymbolEntry(id(), "\u271A", Category.OTHER),   // ✚
        SymbolEntry(id(), "\u2611", Category.OTHER),   // ☑
        SymbolEntry(id(), "\u2622", Category.OTHER),   // ☢
        SymbolEntry(id(), "\u2623", Category.OTHER),   // ☣
        SymbolEntry(id(), "\u262F", Category.OTHER),   // ☯
        SymbolEntry(id(), "\u263A", Category.OTHER),   // ☺
        SymbolEntry(id(), "\u2639", Category.OTHER),   // ☹
        SymbolEntry(id(), "\u266A", Category.OTHER),   // ♪
        SymbolEntry(id(), "\u266B", Category.OTHER),   // ♫
        SymbolEntry(id(), "\u26A0", Category.OTHER),   // ⚠
        SymbolEntry(id(), "\u26A1", Category.OTHER),   // ⚡
        SymbolEntry(id(), "\u26BD", Category.OTHER),   // ⚽
        SymbolEntry(id(), "\u26BE", Category.OTHER),   // ⚾
        SymbolEntry(id(), "\u2708", Category.OTHER),   // ✈
        SymbolEntry(id(), "\u2709", Category.OTHER),   // ✉
        SymbolEntry(id(), "\u2744", Category.OTHER),   // ❄
        SymbolEntry(id(), "\u2B50", Category.OTHER),   // ⭐
        SymbolEntry(id(), "\u00B6", Category.OTHER),   // ¶
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

    fun getSymbolCount(): Int = entries.size
}