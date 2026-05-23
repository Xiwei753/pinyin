# Android IME Technical Roadmap

本文档是给后续 Jules / Codex / ChatGPT 等代码代理看的技术路线约束，不是普通用户文档。任何 Android IME 任务都必须先遵守本文档，再看具体 issue 或临时需求。

## 当前路线结论

当前路线是正确的。

- Android 端继续走 Kotlin platform-independent `imecore` + Android adapter + Android render layer。
- 不要回退到以前 `Service` / controller 直接混杂输入状态、候选状态、键盘布局和 View 修改的模式。
- 不要现在 Rust 重构。
- 不要直接换 Rime。
- 不要直接换 Fcitx5。
- 不要为了 UI 漂亮破坏输入核心。

最近三阶段 Android 底层重构已经确认了当前方向：

- `645ca3e refactor(android): introduce platform independent IME core`
- `1e5ac17 refactor(android): keep candidate snapshots in IME core`
- `e6e53f8 refactor(android): isolate IME core candidate snapshots`

社区调研结论也支持这个方向：Android 官方模型是 `InputMethodService` + `InputConnection` + input view + candidates view；成熟输入法不会让一个 `Service` 同时管理输入状态、候选状态、键盘布局和渲染。Fcitx5 Android、Trime、HeliBoard、FlorisBoard 的经验都说明输入核心、平台 adapter、键盘前端和视觉渲染必须分层。本项目不直接换 Rime / Fcitx5，而是自研九键候选核心，但架构必须按成熟输入法分层。

## 分层架构

### A. imecore 平台无关核心

路径：

`frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/imecore`

职责：

- `InputMode`
- `ImeInputAction`
- `ImeSideEffect`
- `ImeStateMachine`
- `ImeUiState`
- `CompositionState`
- `CandidateStripState`
- `CandidateSnapshotItem`
- `PreeditState`
- `KeyboardSurfaceState`
- `RailState`
- `SymbolPanelState`
- `ThemeTokens` / `LayoutTokens`

规则：

- 不允许 import `android.*`。
- 不允许暴露 `t9.core.Candidate` 给 UI state。
- candidate snapshot 唯一 owner 是 `ImeStateMachine`。
- render 阶段不允许刷新候选。
- `CandidateSelected(index)` 必须提交当前 snapshot 对应项，不重新查候选。

### B. Android Adapter

路径：

`frontends/android-ime/native-app/android/app/src/main/java/io/github/xiwei753/pinyin/t9`

主要类：

- `XiweiT9ImeService`
- `KeyboardActionHandler`
- `T9EngineAdapter`
- `ImeActionSink`

职责：

- Android 生命周期。
- `InputConnection` 的 `commitText` / delete / enter action。
- Android View event -> `ImeInputAction`。
- `ImeSideEffect` -> Android side effect。
- `T9EngineAdapter` 把 `T9Engine` 候选映射成 `CandidateSnapshotItem`。

规则：

- `XiweiT9ImeService` 不维护第二套符号分类状态。
- `KeyboardActionHandler` 是兼容 adapter，不允许新增业务逻辑。
- 旧 wrapper 只能转发 `handle(ImeInputAction.xxx)`。
- `EnterActionPolicy` 是 Android adapter 特例，不能进入 `imecore`。

### C. Android Render Layer

主要类：

- `CandidateViewController`
- `XiweiKeyboardView`
- `KeyboardLayoutBuilder`
- `KeyboardRenderer`
- `KeyboardUiState`

职责：

- render `ImeUiState`。
- hitTest。
- 画键盘。
- 画候选栏。
- 画 preedit floating bar。

规则：

- `CandidateViewController` 是纯 render。
- `CandidateViewController` 不允许访问 `T9Engine`。
- `CandidateViewController` 不允许调用 `refreshCandidates`。
- `KeyboardLayoutBuilder` 只做 state -> layout model。
- `KeyboardRenderer` 只画，不决定业务模式。
- `XiweiKeyboardView` 只发 `ImeInputAction`，不决定候选逻辑。

## Android 九键核心规则

以下规则是永久约束，任何重构都不能破坏：

- 正确架构是：九键数字 -> 拼音音节解码 -> 拼音 preedit -> 拼音查字词。
- 不能回退到“数字 prefix 直接查中文词库”。
- `1` 键是拼音音节分隔符/分词键。
- `0` 键在有候选时提交第一候选，buffer 为空时输入空格。
- 完整拼音 preedit 悬浮在候选栏上方，不进入左侧栏。
- 左侧栏 composing 时显示 reading 分支。
- 没有 buffer 时左侧栏显示快捷标点：， 。 ？ ！
- 候选栏只显示中文候选，不显示 reading，不显示裸数字。
- 候选点击必须提交当前 UI snapshot 候选，不重新全量计算。
- `base.dict.yaml` 做常用词语词库。
- `8105.dict.yaml` 只做单字候选表。
- 不能把 `8105.dict.yaml` 当主词库刷冷僻字。
- 候选栏宁可少，不要乱喷垃圾候选。

## UI/视觉路线

- 现在 UI 丑的根因不是颜色，而是之前状态层混乱。
- 在当前 `imecore` / state / render 边界稳定前，不要做大规模视觉重写。
- 后续视觉美化只允许基于 `ThemeTokens` / `LayoutTokens` / `KeyboardRenderer` / `CandidateViewController` render state 来做。
- 不允许通过在 `Service` 里直接改 View 来修 UI。
- 不允许 XML 重新塞业务键位。
- `keyboard_view.xml` 只保留外壳：preedit floating bar、candidate bar、`XiweiKeyboardView`。
- 具体键位必须来自 `KeyboardLayoutModel`。

## 多平台路线

- Linux / 其他平台以后不要复用 Android `View` / `InputMethodService` / XML。
- Linux 端应该复用或对齐 `imecore` 的 `InputAction` / `ImeUiState` / `ImeSideEffect` 合约。
- Android adapter 执行 `InputConnection`，Linux adapter 将来执行 fcitx5 或其他平台的提交接口。
- Rust 如果以后引入，只替换 `imecore` 或候选引擎边界，不推翻 Android adapter/render。
- 目前不要急着 Rust，因为 Android 行为还在快速变化，先让 Kotlin core 稳定。

## 禁止路线

- 不要直接换 Rime。
- 不要直接换 Fcitx5。
- 不要现在 Rust 重构。
- 不要上 Compose 重写。
- 不要加同步。
- 不要加 AI。
- 不要加云词库。
- 不要做 x86 Android 特殊适配。
- 不要回退到数字 prefix 直接查中文词库。
- 不要只调 UI 表皮。
- 不要只写文档替代代码。
- 不要让 render 阶段刷新候选。
- 不要让 `CandidateViewController` 访问 engine。
- 不要让 `XiweiT9ImeService` 维护第二套业务状态。
- 不要在 `KeyboardActionHandler` wrapper 里新增业务逻辑。

## 阶段计划

### 阶段 1：底层边界稳定

- `imecore` 无 Android import。
- UI state 不暴露 `t9.core.Candidate`。
- candidate snapshot 唯一来源是 `ImeStateMachine`。
- render 只读 state。
- wrapper 只转发 action。

### 阶段 2：Android 行为补齐

- 各输入模式状态完全稳定。
- enter / delete / space / symbol / number / chinese/english 切换行为固定。
- 密码框 / URL / 数字输入框等 `inputType` 适配。
- 真实手机测试流程固定。
- `EditorInfo` / `inputType` 判断只允许放在 Android adapter 层，例如 `EditorInputTypePolicy`，不进入 `imecore`。
- 密码框、URL / Email、数字框、电话框默认不走中文候选，也不显示中文 preedit。

### 阶段 3：视觉重做

- 只在 state/render/token 基础上美化。
- 统一圆角、间距、字号、阴影、深色模式。
- 做接近现代输入法的候选栏、preedit、键盘区。
- 不改输入业务逻辑。

### 阶段 4：多平台抽取

- 评估是否把 `imecore` 拆成独立 Gradle/JVM module。
- 评估 Linux frontend 如何调用同一 action/state/effect 合约。
- 评估是否 Rust 化核心候选引擎。
- 不推翻已有 Android adapter/render。

## 必测 Golden Case

任何重构都不能破坏以下用例：

- `96` -> preedit `wo`，候选包含“我”。
- `64` -> preedit `ni`，候选包含“你”。
- `82` -> preedit `ta`，候选包含“他/她”。
- `33` -> preedit `de`，候选包含“的/得/地”。
- `744` -> preedit `shi`，候选包含“是”。
- `28` -> preedit `bu`，候选包含“不”。
- `28824` -> preedit `bu tai`，候选包含“不太”。
- `288249464` -> preedit `bu tai xing`，第一候选必须是“不太行”。
- `288249464` 不应该出现“不太新股”“不太英语”。
- `546842692674264` -> preedit `jin tian wan shang`，候选包含“今天晚上”。

### 阶段 5：大词库系统 V1 (已完成)
- 基于 rime-ice 引入 50 万级别高质量常用词汇。
- 修改了 SQLite schema，引入 `freq`, `syllable_count`, `origin` 等结构化字段。
- T9Engine 已经完成重构，实现了对 `EXACT_PHRASE` 的最高优先级展示。
