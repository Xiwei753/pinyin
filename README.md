# Personal IME (个人跨端拼音输入法基层)

这个项目不是一个单一的输入法 App，而是一个**跨端个人拼音输入法基层方案**。

## 当前阶段目标
- 不是写完整 Android App。
- 不是写完整 fcitx5 插件。
- 是**先跑通 shared 层**：字库、设置、Rime 配置、私人同步规则。

## 下一阶段：个人词库和同步

目前 Android 基础输入（Fcitx5 Android + RIME 插件）已经跑通链路，并且能正常生成和展示候选词。由于不需要写完整的 Android App，也不急着主攻九键和 UI 动画，当前的重点转向**私人词库同步链路**的打通。

我们的目标是：
- **Public 仓库**（本仓库）：继续存放代码、模板、默认配置。
- **Private 仓库**：单独存放用户的私人词库、短语和设置，确保隐私安全。
- 通过新增的 `tools/private/build-user-rime.py` 工具，读取私人数据并生成用户层 Rime 数据。
- Android 打包脚本在检测到私人数据存在时，会自动将其合并到 Rime 目录中，一起导入到手机测试。

更多详情请参考 [私人数据同步指南](docs/private-sync.md)。

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

## 第一轮 Linux 测试步骤

如果您使用的是 openSUSE / Tumbleweed 等 Linux 发行版，可以按照以下步骤进行第一轮测试：

```bash
git pull
chmod +x frontends/linux-fcitx5/fcitx5/*.sh
bash frontends/linux-fcitx5/fcitx5/install-fcitx5-rime.sh
bash frontends/linux-fcitx5/fcitx5/backup-rime.sh
bash frontends/linux-fcitx5/fcitx5/deploy-rime.sh
python tools/validate/check-all.py
fcitx5 -r
```

部署并检查通过后，请按如下步骤使用：
1. 重启 fcitx5 或重新部署 Rime (也可以直接使用上面的 `fcitx5 -r` 命令)。
2. 在输入法方案里选择“希为拼音”。
3. 先测试基础拼音输入（例如：`nihao`、`shurufa`、`pinyin`）。
4. **注意**：九键当前是实验方案，先不要当成已完成日用功能。

## 第一轮 Android 测试步骤

Android 端第一阶段通过打包 Rime 配置，导入现有的 Android 宿主输入法中运行。希为拼音是第一轮主要测试目标。

由于不同 Android 前端行为差异极大，我们针对不同前端提供不同打包产物和导入方式：

1. **获取打包产物**：
   在 GitHub 仓库 Actions 页面最新运行记录底部下载对应 Artifact：
   - Fcitx5 for Android 用户：下载 `android-rime-fcitx5-userdata`
   - Trime 用户：下载 `android-rime-trime`
   解压下载的 artifact，得到真正的 `.zip` 产物。
2. **导入配置（按前端）**：
   - **Fcitx5**：通过输入法设置内的“导入用户数据”功能，导入 `android-rime-fcitx5-userdata.zip`。
   - **Trime**：界面无通用导入入口。需解压 `android-rime-trime.zip`，将其中的 `rime/` 文件夹用文件管理器手动复制至应用数据目录。
   - 无论哪种方式，完成后**必须重新部署**。
   - （如果在 Fcitx5 导入 userdata 时遇到 `open failed: ENOENT` 报错，说明旧包的压缩结构不符合 Fcitx5 要求，最新源码已修复此问题，请重新下载最新构建的 artifact）。
   - 详细的手机测试步骤及文件覆盖路径，请参考 `docs/android-phone-only-test.md`。
3. **测试功能**：
   先测试全拼（如：`nihao`, `shurufa`）。
4. **导入成功但遇到候选词问题怎么办**：
   如果键盘显示了中州韵（希），但输入时只有预编辑字母，说明是方案配置问题。请重新下载最新的打包产物，并按照上述步骤重新导入和重新部署。你可以尝试切换到内置兜底的“明月拼音·简化字” (`luna_pinyin_simp`) 或“明月拼音” (`luna_pinyin`) 以排查是环境问题还是方案问题。
   **如果出现了候选，但第一候选是“妳好”、“逆號”等繁体/异体字**，这说明部署已经成功，当前问题是缺少简体过滤。本项目现已为 `xiwei_pinyin` 增加了 `simplifier` 和 `uniquifier` 并默认开启简体，请下载最新的 artifact，导入并测试 `nihao` 的第一候选是否为“你好”。
   详情参阅 `docs/android-phone-only-test.md`，`docs/android-rime-no-candidates-debug.md` 以及 `docs/fcitx5-android-rime-deploy-debug.md`。

   **注意：如果能看到“中州韵（希）”，只能说明 schema 被识别，不代表词典已经部署成功。真正成功的标志是 `data/rime/build/` 生成部署产物，且输入 `nihao` 出现中文候选。**
5. **注意**：当前 `xiwei_pinyin.dict.yaml` 主要是作为包含简体词汇的测试词库。为了稳定性，`xiwei_pinyin` 目前底层借用 `luna_pinyin`，后续会进入“主词库+个人词库叠加”的新结构。
6. **注意**：希为九宫格目前只是实验方案，第一轮 Android 测试只确认九键方案能被识别，不要求日用效果。
6. **注意**：`build/` 目录和 zip 文件作为 CI 产物提供，请勿将它们提交到代码仓库。

## 后续测试说明（供参考）

**私人同步**
先配置 `shared/settings/user.yaml`。
请注意：`custom-phrases.txt` 的标准格式为 `候选词<TAB>编码<TAB>权重`，请勿使用旧版 `编码<TAB>候选词` 的格式。
然后运行：`tools/sync/init-private-repo.sh` 初始化私人仓库。
之后可通过 `tools/sync/pull-private.sh` 和 `tools/sync/push-private.sh` 进行双向同步。

## 注意事项

- **九键状态**：目前的 `shared/rime/xiwei_t9.schema.yaml` 是初步的**实验性实现**方案，提供了基础的字母到数字映射，未来会继续优化。
- **隐私保护**：**私人词库绝不能提交到 public 仓库中**。请务必确认私人文件已被加入 `.gitignore`。
- **Token 安全**：切记不要将 GitHub token 写入代码，请使用环境变量或 SSH keys 验证权限。
