# Android 端测试清单

本测试清单用于在将 Rime 配置导入到 Android 宿主输入法时，进行基础功能的验证。

## 1. 打包产物获取
你可以选择本地打包或使用 GitHub Actions 自动构建产物。

**方式一：GitHub Actions 获取（推荐）**
- [ ] 在仓库的 Actions 页面找到最新的 validate-and-package 运行记录，或者手动触发 workflow_dispatch。
- [ ] 下载对应的 artifact：
  - Fcitx5 用户下载 `android-rime-fcitx5-userdata`
  - Trime 用户下载 `android-rime-trime`
  - 其他用户备用 `android-rime-generic`
- [ ] 解压下载好的 artifact，确认得到对应的 zip 文件。

**方式二：本地打包**
- [ ] 运行打包脚本：`package-fcitx5-userdata.sh`，`package-trime.sh`，或 `package-generic-rime.sh`。
- [ ] 脚本执行成功，不应该报错。
- [ ] 成功生成对应的 `build/android-rime-*.zip` 文件。

## 2. Zip 内容检查
- [ ] 通用包 (generic)：确认压缩包内无多余顶级目录。
- [ ] Trime 手动包 (trime)：确认压缩包内部文件均位于 `rime/` 目录下。
- [ ] Fcitx5 数据包 (fcitx5-userdata)：确认包含 `metadata.json` 及 `external/data/rime/` 目录结构。
- [ ] 确认包含必要文件（不管是在根目录还是在对应子目录）：
  - `default.yaml`
  - `default.custom.yaml`
  - `xiwei_pinyin.schema.yaml`
  - `xiwei_t9.schema.yaml`
  - `xiwei_pinyin.dict.yaml`
  - `custom_phrase.txt`
  - `symbols.yaml`

## 3. 手机导入测试
- [ ] 成功将对应的 zip 包传至手机。
- [ ] **Fcitx5 Android**：在输入法设置中使用“导入用户数据”功能，选择 `android-rime-fcitx5-userdata.zip` 导入。
- [ ] **Trime**：将 `android-rime-trime.zip` 解压，将其中的 `rime/` 文件夹复制覆盖到 Trime 用户配置目录。
- [ ] 导入或覆盖完成后，点击“重新部署”（Deploy）或重新加载配置。
- [ ] 部署过程顺利完成，没有报错退出。

## 4. 全拼测试
- [ ] 方案列表中存在“希为拼音”方案并被选中。
- [ ] 尝试输入 `nihao`，可以得到“你好”。如果没看到候选词，尝试点击候选栏右侧小箭头展开。
- [ ] 尝试输入 `shurufa`，可以得到“输入法”。
- [ ] 尝试输入 `pinyin`，可以得到“拼音”。
- [ ] 尝试输入 `zhongguo`，可以得到“中国”。
- [ ] *故障排查：如果没有候选，请切换到内置的 `luna_pinyin_simp` (明月拼音·简化字) 测试。如果内置方案能出候选，说明是自定义词库问题。具体请参见 `docs/android-rime-no-candidates-debug.md`。*

## 5. 自定义短语测试
- [ ] 尝试输入自定义短语（具体视 `custom_phrase.txt` 内容而定），期望的短语能成功出现在候选栏。

## 6. 九键识别测试
- [ ] 方案列表中存在“希为九宫格”方案。
- [ ] 切换至“希为九宫格”方案，不会引发崩溃。
- [ ] *（注：希为拼音是第一轮主要测试目标，希为九宫格目前只是实验方案。第一轮 Android 测试只确认九键方案能被识别，不要求日用效果。）*

---

## 导入原则重申

1. **不要寻找通用的 zip 导入**：Trime 和 Fcitx5 Android 目前均不提供“一键导入普通 Rime zip”的功能。
2. **使用特定方式**：Fcitx5 走特定的用户数据导入格式；Trime 走文件管理器手动解压覆盖（详见 `docs/android-phone-only-test.md`）。
3. **重新部署**：只要发生了 Rime 配置变化，不管是覆盖文件还是导入数据包，都必须在输入法中重新部署 Rime。

## 常见失败原因

如果在测试中遇到问题，请检查以下常见原因：

**注意：如果能看到“中州韵（希）”，只能说明 schema 被识别，不代表词典已经部署成功。真正成功的标志是 `data/rime/build/` 生成部署产物，且输入 `nihao` 出现“你好”，或者至少有中文候选。**


1. **zip 结构不对**：某些前端（如 Trime）需要包内有 `rime/` 目录，而某些又不能有。请尝试不同的包（generic 或 trime）。
2. **安卓前端没有重新部署 Rime**：导入文件后，必须执行“重新部署”才能生效。
3. **方案没有加入 schema_list**：如果列表里没有看到方案，检查 `default.custom.yaml` 是否正确配置。
4. **九键只是实验方案**：如果九键无法正常打出词，这是因为目前九键只是初步的实验方案，还没有完善。
5. **安卓前端不支持某些 Rime 特性**：极少数前端可能存在对 Rime 高级特性（如 lua 脚本）的不兼容。
6. **文件编码问题**：确保所有的 yaml 或 txt 文件均为 UTF-8 无 BOM 编码，否则会导致解析失败。
