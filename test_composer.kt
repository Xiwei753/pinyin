import io.github.xiwei753.pinyin.t9.core.T9PinyinComposer

fun main() {
    val composer = T9PinyinComposer()
    println(composer.getCompositions("28824"))
}
