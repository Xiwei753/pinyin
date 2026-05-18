# 共享核心路线文档

当前自研 T9Engine 作为共享输入核心，已被 Android 原生前端使用。
未来，共享核心（T9Engine、词库、候选逻辑）将被 Android 和 Linux 共用。

分层如下：

1. **Android 前端**
   - 基于 Kotlin InputMethodService。
   - 负责九键 UI、候选栏、触摸、动画、上屏。
   - 使用 T9ImeController 将按键事件委托给共享核心。

2. **Linux 前端**
   - 当前不开发。
   - 未来方向：开发 fcitx5 插件外壳，接入本项目自研 shared core。
   - fcitx5 只作为 Linux 系统输入法框架/前端壳。
   - 不使用 fcitx5-rime 作为核心引擎。

3. **Shared Core（共享核心）**
   - 负责拼音与九键解析。
   - 负责候选生成。
   - 负责词库管理。
   - 负责词频排序。
   - 负责用户学习逻辑。
   - 负责多端同步数据格式。

## 当前进展
- 仓库主许可证为 GPL v3。
- 词库来源为 rime-ice（雾凇拼音），仅作词库数据，不是输入引擎。
- T9Engine 已从 Service 拆出，负责输入状态。
- T9ImeController 已将按键逻辑从 Service 抽离为纯 Kotlin 控制器。
- Candidate 是核心候选数据结构。
- BuiltinDictionary 负责数据来源，拼音源词库为 `assets/t9_source_dict.tsv`。
- `T9CodeMapper` 自动生成九键数字码。
- DictionaryProvider 接口为后续接入用户词库、同步词库的入口。
- Android Service 只负责 UI 渲染和系统上屏。
