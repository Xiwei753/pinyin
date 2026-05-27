
已暂停，目标过于远大，心比天高，命比纸薄
# Personal IME (个人跨端拼音输入法基层)

这个项目是一个**个人拼音输入法基层方案**，当前的发展重心已完全转向 **Android 原生 T9 拼音输入法开发**。

## 当前阶段目标
- 是**先跑通 shared 层**：字库、设置、私人同步规则。
- 构建原生的Android T9拼音输入法

## 路线：Android 原生 T9 输入法

**Android 原生 T9 是当前唯一主线**
- 本项目当前的发展重心**唯一且完全**是 **Android 原生 T9 拼音输入法**。
- **Android 原生前端与 T9 核心**：九宫格键盘基于 `InputMethodService` 原生开发，位于 `frontends/android-ime/native-app/`。`T9Engine` 是自研输入内核，支持拼音音节解码、前缀候选匹配及多音节解析。
- **不再使用 Fcitx5 for Android / Rime / Trime**：旧版基于 Fcitx5 + RIME 的全拼/伪九宫测试方案已被彻底移除，不再参与主线构建和测试。本项目不是 Rime 方案，不要试图恢复旧路线。

## License / 许可证

- Current license: GNU General Public License v3.0
- Earlier versions were MIT-licensed
- Third-party dictionaries/resources keep their own licenses

- 当前代码和默认构建按 GPL v3 开源。
- 第三方词库来源说明和原始许可证保留在 `THIRD_PARTY_LICENSES/` 目录下。
- **当前内置词库状态**：目前项目内置的基础词库来源为 **rime-ice (雾凇拼音)**，以 GPLv3 协议引入，包含 5 万条常用词汇和短语。rime-ice 仅作为词库数据来源，不代表本项目使用 Rime 引擎。

## 项目架构

项目将核心数据、平台前端、工具脚本进行了明确的分层：

- **`shared/`**: 真正的共享层。两端共用的东西都在这里：
  - `dictionary/`: 共享词库 (基础词库、个人增量词库)
  - `settings/`: 共享设置 (功能开关、多端特定设置)
  - `sync-spec/`: 同步规范 (GitHub 个人私有仓库同步说明)
  - `tests/`: 共享测试样例
- **`frontends/`**: 平台前端层。
  - `android-ime/`: Android 前端，采用原生Kotlin/Java实现。
- **`tools/`**: 自动化工具层：
  - `sync/`: 负责私有仓库同步的脚本，支持增量拉取和推送。
  - `validate/`: 配置文件、词库合法性校验工具。
- **`personal-ime-core/`**: Python 实验室，用于研究词频、候选排序、九键逻辑等核心算法。它不是主线应用，而是研究和测试验证工具。

## Android 原生测试步骤

目前主线专注于原生 T9 引擎。您可以通过 Gradle 进行单元测试与构建调试 APK。

```bash
cd frontends/android-ime/native-app/android
./gradlew test assembleDebug
```

编译出的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 注意事项

- **隐私保护**：**私人词库绝不能提交到 public 仓库中**。请务必确认私人文件已被加入 `.gitignore`。
- **Token 安全**：切记不要将 GitHub token 写入代码，请使用环境变量或 SSH keys 验证权限。
