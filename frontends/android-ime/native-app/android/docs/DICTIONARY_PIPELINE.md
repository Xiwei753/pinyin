# 大词库管线 (Dictionary Pipeline)

本文档说明了 Android 九键输入法中文大词库的构建和查询机制。

## 词库来源与许可证

当前使用的主要词库来源为 `rime-ice` (雾凇拼音) 词库。
- **来源路径**: `third_party/rime-ice/cn_dicts/`
- **主要文件**:
  - `base.dict.yaml`: 基础常用词、短语、短句。
  - `8105.dict.yaml`: 《通用规范汉字表》8105 字单字表。
  - `ext.dict.yaml`: 补充扩展词汇。
- **补充短语**: `tools/dictionary/android_common_phrases.tsv`，包含一些针对 Android 系统和日常口语的高频补充词。
- **许可证**: `rime-ice` 采用 GPL 3.0 许可证。补充短语属于本仓库代码的衍生数据，与本仓库主许可证一致。

## 生成脚本与索引 (`build_t9_assets.py`)

大词库不采用运行时解析纯文本的形式，而是通过构建脚本 `tools/dictionary/build_t9_assets.py` 预先生成结构化的 SQLite 数据库 (`t9_dict.db`)，以保证 Android 端的启动速度和内存稳定性。

### 质量过滤规则
脚本在解析字典源时，执行严格的过滤，以防止垃圾词条污染候选栏：
- **非法字符**: 包含纯英文、Emoji、或非中文字符的词条将被直接过滤。
- **长度限制**: 文本长度大于 8 个字符的长句会被抛弃。
- **拼音合法性**: 只保留只含有 `a-z`、`ü` 以及空格的规范拼音。
- **长度校验**: 拼音包含的音节数必须严格等于中文文本的字符长度。

### 分层规则与配额
脚本支持最高 500,000 条数据输出，并执行严格的分层配额策略，合并同源或不同源的重复词条：
1. **基础多字词 (Phrase)**: 最多 300,000 条，来自 `base.dict.yaml`。
2. **基础单字 (Single)**: 约 12,000 条（结合 `base` 与 `8105`），`8105.dict.yaml` 严格仅作为单字表查询。
3. **补充短语与扩展**: 填补剩余额度（常用口语优先）。

### SQLite Schema 与索引
生成的 `t9_dict.db` 包含以下 Schema：
- `id` INTEGER PRIMARY KEY
- `text` TEXT NOT NULL
- `pinyin` TEXT NOT NULL
- `code` TEXT NOT NULL (例如 `"bu tai xing"` 对应 `"288249464"`)
- `syllable_count` INTEGER NOT NULL (拼音音节数)
- `freq` INTEGER NOT NULL (频率/权重)
- `source` TEXT NOT NULL (来源标识，如 `base`, `8105`, `common`)
- `origin` TEXT NOT NULL (`EXACT_SINGLE` 或 `EXACT_PHRASE`)
- `is_single` INTEGER NOT NULL (1 为单字，0 为词语)
- `is_phrase` INTEGER NOT NULL (1 为词语，0 为单字)

同时建立索引优化查询速度：
- `idx_pinyin` (pinyin)
- `idx_code` (code)
- `idx_text` (text)
- `idx_code_syllable` (code, syllable_count)
- `idx_pinyin_syllable` (pinyin, syllable_count)

## 如何重新生成词库与运行测试

**生成词库**:
```bash
cd frontends/android-ime/native-app/android
python tools/dictionary/build_t9_assets.py
# 生成文件位于: app/src/main/assets/t9_dict.db
```

**运行测试**:
```bash
./gradlew test assembleDebug
```

新增 Golden Case 时，请在 `T9EngineGoldenRealDictTest.kt` 中添加对应的 `assertCase` 记录。

## 架构限制与边界

1. **为什么不能数字 prefix 直接查中文词库？**
   九键输入必须经过 `数字 -> 拼音 -> 中文` 的解码流程。直接使用数字前缀查库会导致严重的候选混淆（例如输入 `28` 可能直接查出以 `28` 对应的任何中文长词），破坏“全拼音精准匹配”与 `preedit` 界面的一致性。
2. **为什么 8105 只能做单字表？**
   `8105` 字表包含大量低频生僻字，如果混合进全量词库进行打分排序，会导致常用的多字词组被生僻单字挤掉，极大降低输入效率。因此它在引擎查询中仅限于 `getSingleSyllableCandidates` 中使用。
