# Personal IME (个人跨端拼音输入法基层)

这个项目不是一个单一的输入法 App，而是一个**跨端个人拼音输入法基层方案**。

## 当前阶段目标
- 不是写完整 Android App。
- 不是写完整 fcitx5 插件。
- 是**先跑通 shared 层**：字库、设置、Rime 配置、私人同步规则。

## 路线更新：自研输入核心与跨端前端

**当前项目方向调整：Android Native T9 成为重点**
- 本项目当前的发展重心已完全转向 **Android 原生 T9 拼音输入法开发**。
- **Android 原生前端与 T9 核心**：真正可用的九宫格键盘已经进入 `frontends/android-ime/native-app/` 进行原生开发。我们已经构建了 `T9Engine` 作为**自研输入内核**的最小地基，它与基于 `InputMethodService` 的原生前端协同工作。当前 T9 引擎已支持自动拼音转九键码、前缀候选匹配以及复杂的多音节解析。
- **Fcitx5 已归档 (Deprecated/Archived)**：原有的基于 Fcitx5 + RIME 的全拼/伪九宫测试方案由于键盘 UI 硬编码和私人词库导入机制复杂，已被标记为过时/归档，相关包装和同步脚本已被移除或停止维护，且不再参与主线构建和测试。

## License / 许可证

- Current license: GNU General Public License v3.0
- Earlier versions were MIT-licensed
- Third-party dictionaries/resources keep their own licenses

- 当前代码和默认构建按 GPL v3 开源。
- 第三方词库来源说明和原始许可证保留在 `THIRD_PARTY_LICENSES/` 目录下。
- **当前内置词库状态**：目前项目内置的基础测试词库来源于开源项目 **rime-ice (雾凇拼音)**，以 GPLv3 协议引入。为了控制应用体积和加载速度，当前在 Android 测试应用中集成的只是经过缩减和转换的测试规模子集，并非完整日用词库。包含 5 万条常用词汇和短语。未来如果需要升级到完整大词库，可使用 `tools/dictionary/convert_rime_dict.py` 转换完整的 `base.dict.yaml` 或 `8105.dict.yaml`，并可按需修改 `t9_source_dict.tsv` 或构建增量加载机制。

## 项目架构

项目将核心数据、平台前端、工具脚本进行了明确的分层：

- **`shared/`**: 真正的共享层。两端共用的东西都在这里：
  - `rime/`: Rime 第一阶段的共享配置 (schema, 字典, custom_phrase 等)
  - `dictionary/`: 共享词库 (基础词库、个人增量词库)
  - `settings/`: 共享设置 (功能开关、多端特定设置)
  - `sync-spec/`: 同步规范 (GitHub 个人私有仓库同步说明)
  - `tests/`: 共享测试样例
- **`frontends/`**: 平台前端层。为追求极致流畅，不强制共享 UI 实现。
  - `linux-fcitx5/`: Linux 前端，第一阶段基于 `fcitx5-rime`，未来考虑独立插件。
  - `android-ime/`: Android 前端，第一阶段打包 Rime 配置给同类 App，未来考虑用 Kotlin/Java 单独实现。
- **`tools/`**: 自动化工具层：
  - `sync/`: 负责私有仓库同步的脚本，支持增量拉取和推送。
  - `package/`: 针对各端打包的脚本。
  - `validate/`: 配置文件、词库合法性校验工具。
- **`personal-ime-core/`**: Python 实验室，用于研究词频、候选排序、九键逻辑等核心算法。它不是主线应用，而是研究和测试验证工具。

## Android 原生测试步骤

目前主线专注于原生 T9 引擎。您可以通过 Gradle 进行单元测试与构建调试 APK。

```bash
cd frontends/android-ime/native-app/android
./gradlew test assembleDebug
```

编译出的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 归档 / 旧版 Fcitx5 路线（不再维护）

早期曾尝试在 Linux 和 Android Fcitx5 上打包 Rime 配置提供输入：
- 相关包装脚本及 Fcitx5 userdata ZIP 生成代码已被移除。
- GitHub Actions CI 现在不再构建这些旧包。
- 详情可见早期版本历史提交。

## 注意事项

- **九键状态**：目前的 `shared/rime/xiwei_t9.schema.yaml` 仅保留为数字串候选实验。真正的九键 UI 和体验将在原生 Android 前端中实现。
- **隐私保护**：**私人词库绝不能提交到 public 仓库中**。请务必确认私人文件已被加入 `.gitignore`。
- **Token 安全**：切记不要将 GitHub token 写入代码，请使用环境变量或 SSH keys 验证权限。
