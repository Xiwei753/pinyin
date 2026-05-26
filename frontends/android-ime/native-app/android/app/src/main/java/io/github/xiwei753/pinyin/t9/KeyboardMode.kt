package io.github.xiwei753.pinyin.t9

enum class KeyboardMode {
    ChineseT9,
    EnglishT9,
    ChinesePinyin,
    EnglishQWERTY,
    Symbol,
    Number,
    
    // TODO: Next step, migrate clipboard to this independent panel, no longer expand it inside CandidateViewController
    ClipboardPanel,
    
    // TODO: Next step, migrate text selection to this independent panel, no longer expand it inside CandidateViewController
    SelectionPanel
}
