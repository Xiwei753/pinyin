# Personal IME (个人跨端拼音输入法基层)

这个项目不是一个单一的输入法 App，而是一个**跨端个人拼音输入法基层方案**。

## 当前阶段目标
- 不是写完整 Android App。
- 不是写完整 fcitx5 插件。
- 是**先跑通 shared 层**：字库、设置、Rime 配置、私人同步规则。

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

## 下一步测试方式

1. **Linux 端部署**
   运行脚本：`frontends/linux-fcitx5/fcitx5/deploy-rime.sh`
   此脚本会自动将配置部署至 `~/.local/share/fcitx5/rime/`。

2. **Android 端打包**
   运行脚本：`frontends/android-ime/rime-package/package-rime-config.sh`
   此脚本会在 `build/` 目录下生成打包文件，方便在 Android 平台中导入。

3. **私人同步**
   先配置 `shared/settings/user.yaml`。
   然后运行：`tools/sync/init-private-repo.sh` 初始化私人仓库。
   之后可通过 `tools/sync/pull-private.sh` 和 `tools/sync/push-private.sh` 进行双向同步。

## 注意事项

- **九键状态**：目前的 `shared/rime/xiwei_t9.schema.yaml` 是初步的**实验性实现**方案，提供了基础的字母到数字映射，未来会继续优化。
- **隐私保护**：**私人词库绝不能提交到 public 仓库中**。请务必确认私人文件已被加入 `.gitignore`。
- **Token 安全**：切记不要将 GitHub token 写入代码，请使用环境变量或 SSH keys 验证权限。
