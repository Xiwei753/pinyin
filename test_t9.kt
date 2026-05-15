package io.github.xiwei753.pinyin.t9.core

fun main() {
    val comp = T9PinyinComposer()
    println(comp.getCompositions("288249464").map { it.pinyinString })
}
