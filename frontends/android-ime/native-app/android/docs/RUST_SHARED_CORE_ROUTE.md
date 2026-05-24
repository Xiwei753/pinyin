# Rust Shared Core 技术路线设计说明书

## 1. 核心设计原则

为了实现“一个高效核心，多端（Android / Linux 等）前端复用”的目标，本项目决定彻底纠偏原有的技术路线，停止在 Android Kotlin 层过度编写大词库的核心查询、匹配与解码排序逻辑，也不引入复杂的 C++ native 核心（如 AOSP 里的 BinaryDictionary）。我们确立了基于 **Rust Shared Core** 的技术路线。

### 1.1 核心层与前端层分层说明
- **前端层 (Frontend)**：
  - **Android (Kotlin)**：仅负责 UI 渲染（包括 Keyboards、候选栏绘制、面板展示）、处理 `InputMethodService` 的生命周期和系统回调、触摸事件、振动 (Haptic) 触发、系统剪贴板历史监听等原生系统能力调用。
  - **Linux (Fcitx5 / Rime-alike shell)**：未来将只负责 fcitx5 插件或 D-Bus 接口对接，获取用户输入并传递给 Rust shared core，获取候选列表进行绘制。
  - Android 前端与 Rust shared core 之间通过 **JNI** 或 **UniFFI** 进行轻量级、无状态的绑定。**绝对不引入任何 C++ 依赖**。
- **共享核心层 (Rust Shared Core)**：
  - 核心逻辑全部在 Rust 编写（建议放置于 `core/pinyin-core` 或 `shared/rust/pinyin-core`）。
  - 负责 T9 数字数字串（如 `288249464`）到拼音音节的解码（包括 locked syllables 分割、拼音纠错匹配）。
  - 负责拼音预编辑区 (Preedit) 字符串的维护。
  - 负责基于预编译词库索引的快速中文词语/单字查询。
  - 负责单字 Fallback 匹配、前缀补全 (Prefix Completion) 逻辑。
  - 负责多词候选集的综合权重排序。

---

## 2. Rust Core 对外接口设计

Rust Shared Core 应该是无状态或轻量状态的，暴露出简洁的 C/JNI/UniFFI 兼容接口。其最基础的 API 接口集应当至少包含：

```rust
// 核心输入状态机接口示例
pub struct T9Engine {
    // 内部持有的预编译索引、历史输入状态、当前 raw_digits 等
}

impl T9Engine {
    /// 输入一个新的 T9 数字（'2'-'9'，'1' 表示音节分割符）
    pub fn input_digit(&mut self, digit: char) -> bool;

    /// 删除最后一个数字（回退解码状态，处理 locked/unlocked 音节）
    pub fn backspace(&mut self) -> bool;

    /// 清空当前输入状态，重置缓冲区
    pub fn clear(&mut self);

    /// 设置当前活跃的拼音切分/读音（例如用户在多音节中手动点选某一部分读音）
    pub fn set_active_reading(&mut self, index: usize);

    /// 获取当前应该在输入框/预编辑区展示的拼音串 (preedit string)
    pub fn get_preedit(&self) -> String;

    /// 获取当前数字串的所有可能拼音切分序列 (readings)
    pub fn get_readings(&self) -> Vec<String>;

    /// 获取符合当前输入的中文候选字词列表
    pub fn get_candidates(&self) -> Vec<Candidate>;

    /// 用户确认选择了某个候选词，更新引擎内部状态或进行词频调整
    pub fn commit_candidate(&mut self, candidate_index: usize) -> Option<String>;
}

pub struct Candidate {
    pub text: String,
    pub code: String,      // 对应的拼音
    pub score: u32,        // 排序权重
}
```

---

## 3. 词库预编译索引设计 (Dictionary Indexing)

为解决运行时大词库扫描性能问题，我们明确以下大词库路线：

1. **SQLite 的定位**：`SQLiteDictionary` 目前**只能作为过渡、调试和构建验证工具**，绝对不能作为长期热路径（Per-keypress）的主查询手段。在主查询热路径上使用 SQLite 会导致严重的卡顿。
2. **构建期预编译**：
   - 词库源（来自 `third_party/rime-ice/cn_dicts/base.dict.yaml` 及 `8105.dict.yaml` 等）在构建期（Build time）由构建工具直接处理并生成专用的 Rust 可读二进制文件。
   - **绝对不在运行时扫描复杂的 YAML/TSV 文本**。
3. **词库存储与查询阶段演进**：
   - **V1 阶段（内存 Map 快速 Lookup）**：在构建期将过滤并排序好的词条，编译打包成简单的紧凑二进制格式（例如 JSON / MessagePack / 自定义轻量平铺二进制表）。Rust core 启动时一次性加载到内存中，转为高效的内存 Map / Trie 树结构。
   - **V2 阶段（Mmap 零拷贝二进制索引）**：当词库规模达到数兆甚至数十兆时，升级为 `mmap` (Memory-mapped file) 格式的只读二进制索引。利用 `mmap` 特性，使词库检索直接基于文件映射进行，按需加载，内存占用低且极其高效。

---

## 4. UI 路线纠偏与功能面板解耦说明

### 4.1 候选顶栏的职责规范
- **允许的顶栏内容**：
  - **Pinyin Preedit Chip**：当前输入的预编辑拼音切分。
  - **中文候选词 (Chinese Candidates)**：拼音解码后的汉字候选列表。
  - **空态图标入口 (Empty State Icons)**：当 rawBuffer 为空时，仅保留 `📋` (剪贴板)、`⚙` (设置)、`↔` (光标与文本选择) 三个极简图标入口。
- **严禁的顶栏内容**：
  - **大汉字 Chip**：严禁在顶栏空态直接显示大汉字 “剪贴板”、“设置”、“选择” 等 Chip，必须用上述小图标替代，以保持视觉清爽。
  - **标点符号列表**：严禁在顶栏空态塞入 `， 。 ？ ！ ： …` 等标点符号。顶栏不是标点符号输入区，标点输入应交由专属的 `SymbolPanel` 或侧栏。

### 4.2 ClipboardPanel & SelectionPanel 架构边界
- 剪贴板历史（ClipboardPanel）与选择编辑（SelectionPanel）在概念上是**独立的 Keyboard Panel**，应该与 `ChineseT9`、`Symbol`、`Number` 处于同等平级地位。
- **界面与交互规范**：
  - 它们在完全独立展现时，不应显示左侧标点 rail，也不应显示底部的“符/123/中英/重输”控制栏。
  - 它们应在右上角或特定位置提供自己的返回/关闭按键，点击后直接退回到中文九键状态。
- **临时过渡方案声明**：
  - **重要说明**：当前 `CandidateViewController` 中直接将 `CLIPBOARD` 和 `SELECTION` 当作临时状态绘制在候选顶栏的区域中，这只是**临时实现的过渡方案**。
  - 严禁继续在 `CandidateViewController` 中堆积剪贴板历史排序、搜索、或复杂的文本编辑逻辑。
  - 下一步将把这两个面板彻底重构成与 `ChineseT9` 布局平级的独立布局面板，并通过原生的 `KeyboardMode` 进行状态切换。
