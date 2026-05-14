# 共享核心路线文档

当前并非完整的自研内核，只是开始开发最小的 T9Engine 原型。
未来，输入核心将被 Android 和 Linux 共用。

分层如下：

1. **Android 前端**
   - 基于 Kotlin InputMethodService。
   - 负责九键 UI、候选栏、触摸、动画、上屏。

2. **Linux 前端**
   - 现阶段继续基于 fcitx5。
   - 未来计划开发 fcitx5 插件。
   - 负责接入 Linux 系统、按键事件处理、候选窗口渲染及上屏。

3. **Shared Core（共享核心）**
   - 负责拼音与九键解析。
   - 负责候选生成。
   - 负责词库管理。
   - 负责词频排序。
   - 负责用户学习逻辑。
   - 负责多端同步数据格式。

说明：
- 目前 Linux 端暂不急于自研前端。
- Linux 环境下继续使用 fcitx5-rime 进行过渡。
- 待自研核心稳定后，再通过 fcitx5 插件的形式接入 shared core。
