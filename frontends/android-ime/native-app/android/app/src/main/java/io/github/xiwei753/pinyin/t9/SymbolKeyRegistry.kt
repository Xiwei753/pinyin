package io.github.xiwei753.pinyin.t9

import android.util.SparseArray

class SymbolKeyRegistry {

    private val symbolMap: Map<Int, String> = mapOf(
        R.id.sym_1 to "\uFF0C", R.id.sym_2 to "\u3002", R.id.sym_3 to "\uFF1F", R.id.sym_4 to "\uFF01",
        R.id.sym_5 to "\uFF1A", R.id.sym_6 to "\uFF1B", R.id.sym_7 to "\u3001", R.id.sym_8 to "\u201C",
        R.id.sym_9 to "\u201D", R.id.sym_10 to "\u2018", R.id.sym_11 to "\u2019", R.id.sym_12 to "\u2014",
        R.id.sym_13 to "\u2026", R.id.sym_14 to "\u00B7", R.id.sym_15 to "\uFF5E", R.id.sym_16 to "+",
        R.id.sym_17 to "-", R.id.sym_18 to "\u00D7", R.id.sym_19 to "\u00F7", R.id.sym_20 to "=",
        R.id.sym_21 to "%", R.id.sym_22 to "&", R.id.sym_23 to "|", R.id.sym_24 to "\u221A",
        R.id.sym_25 to "\u2248", R.id.sym_26 to "\u2260", R.id.sym_27 to "\u2264", R.id.sym_28 to "\u2265",
        R.id.sym_29 to "\u00B1", R.id.sym_30 to "\u221E", R.id.sym_31 to "\uFF08", R.id.sym_32 to "\uFF09",
        R.id.sym_33 to "\u3010", R.id.sym_34 to "\u3011", R.id.sym_35 to "{", R.id.sym_36 to "}",
        R.id.sym_37 to "\u300A", R.id.sym_38 to "\u300B", R.id.sym_39 to "[", R.id.sym_40 to "]",
        R.id.sym_41 to "<", R.id.sym_42 to ">", R.id.sym_43 to "/", R.id.sym_44 to "\\",
        R.id.sym_45 to "\u00A7", R.id.sym_46 to "@", R.id.sym_47 to "#", R.id.sym_48 to "\uFFE5",
        R.id.sym_49 to "$", R.id.sym_50 to "*", R.id.sym_51 to "^", R.id.sym_52 to "_",
        R.id.sym_53 to "~", R.id.sym_54 to "`", R.id.sym_55 to "\u20AC", R.id.sym_56 to "\u00A3",
        R.id.sym_57 to "\u00A5", R.id.sym_58 to "\u00A9", R.id.sym_59 to "\u00AE", R.id.sym_60 to "\u2122"
    )

    fun getSymbolText(id: Int): String? = symbolMap[id]

    fun getAllSymbolIds(): List<Int> = symbolMap.keys.toList()

    fun getAllSymbolEntries(): List<Pair<Int, String>> = symbolMap.toList()
}
