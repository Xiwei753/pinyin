# Android 前端路线图

- **当前阶段**：Android 原生 T9 输入法开发（Kotlin/Java InputMethodService + 自研 T9Engine）。
  - 不依赖 Rime 引擎、Trime 或 Fcitx5 for Android。
  - 不通过脚本生成 Rime 配置包。
  - 直接在 `native-app/` 下进行原生开发。

- **当前开发重点**：
  - 拼音音节解码的准确性和速度。
  - 候选正确性和常用字词覆盖率。
  - 键盘 UI 交互体验（触感反馈、主题、高度）。
  - 设置页和调试日志能力。

- **暂不开发**：
  - Linux 前端。
  - 云词库、在线 AI、账号同步。
  - 复杂动画和键盘皮肤。
  - 全键（QWERTY）布局。
