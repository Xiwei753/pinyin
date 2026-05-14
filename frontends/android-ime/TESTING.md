# Android 端测试清单

本测试清单用于在将 Rime 配置导入到 Android 宿主输入法时，进行基础功能的验证。

## 1. 打包产物获取
你可以选择本地打包或使用 GitHub Actions 自动构建产物。

**方式一：GitHub Actions 获取（推荐）**
- [ ] 在仓库的 Actions 页面找到最新的 validate-and-package 运行记录，或者手动触发 workflow_dispatch。
- [ ] 下载对应的 artifact：Trime 用户下载 `android-rime-trime`，其他用户尝试 `android-rime-generic`。
- [ ] 解压下载好的 artifact，确认得到对应的 zip 文件。

**方式二：本地打包**
- [ ] 运行打包脚本：`bash frontends/android-ime/rime-package/package-generic-rime.sh` 或 `package-trime.sh`
- [ ] 脚本执行成功，不应该报错。
- [ ] 成功生成打包文件：`build/android-rime-generic.zip` 或 `build/android-rime-trime.zip`

## 2. Zip 内容检查
- [ ] 通用包 (generic)：确认压缩包内部文件结构正确，不应有多余的顶级目录。
- [ ] Trime 专用包 (trime)：确认压缩包内部文件均位于 `rime/` 目录下。
- [ ] 确认包含必要文件（不管是否在子目录）：
  - `default.custom.yaml`
  - `xiwei_pinyin.schema.yaml`
  - `xiwei_t9.schema.yaml`
  - `xiwei_pinyin.dict.yaml`
  - `custom_phrase.txt`
  - `symbols.yaml`
  - `README.md`

## 3. 手机导入测试
- [ ] 成功将对应的 zip 包传至手机。
- [ ] 在 Android Rime 前端中尝试找到并导入该压缩包。
- [ ] **（备选）手动复制**：如果在 UI 中找不到导入入口，请将压缩包解压后，手动通过文件管理器将文件复制到输入法的配置目录。
- [ ] 点击“重新部署”（Deploy）或重新加载配置。
- [ ] 部署过程顺利完成，没有报错退出。

## 4. 全拼测试
- [ ] 方案列表中存在“希为拼音”方案并被选中。
- [ ] 尝试输入 `nihao`，可以得到“你好”。
- [ ] 尝试输入 `shurufa`，可以得到“输入法”。
- [ ] 尝试输入 `pinyin`，可以得到“拼音”。
- [ ] 尝试输入 `zhongguo`，可以得到“中国”。

## 5. 自定义短语测试
- [ ] 尝试输入自定义短语（具体视 `custom_phrase.txt` 内容而定），期望的短语能成功出现在候选栏。

## 6. 九键识别测试
- [ ] 方案列表中存在“希为九宫格”方案。
- [ ] 切换至“希为九宫格”方案，不会引发崩溃。
- [ ] *（注：希为拼音是第一轮主要测试目标，希为九宫格目前只是实验方案。第一轮 Android 测试只确认九键方案能被识别，不要求日用效果。）*

---

## 如果界面里没有导入按钮怎么办

1. **不要盲目寻找**：如果你在设置中无法找到导入功能（例如 Fcitx5 for Android 配合 RIME 插件目前就没有提供），请停止在界面中寻找。
2. **解压文件**：把下载的 zip 包解压出来。
3. **手动复制**：使用 Material Files 或 MT 管理器，将解压后的文件直接复制到前端应用的工作目录（通常位于 `Android/data/<包名>/files/` 中）。具体不同前端的路径请参考 `docs/android-import-strategy.md`。
4. **重新部署**：手动覆盖文件后，切记回到应用中点击“重新部署”。

## 常见失败原因

如果在测试中遇到问题，请检查以下常见原因：

1. **zip 结构不对**：某些前端（如 Trime）需要包内有 `rime/` 目录，而某些又不能有。请尝试不同的包（generic 或 trime）。
2. **安卓前端没有重新部署 Rime**：导入文件后，必须执行“重新部署”才能生效。
3. **方案没有加入 schema_list**：如果列表里没有看到方案，检查 `default.custom.yaml` 是否正确配置。
4. **九键只是实验方案**：如果九键无法正常打出词，这是因为目前九键只是初步的实验方案，还没有完善。
5. **安卓前端不支持某些 Rime 特性**：极少数前端可能存在对 Rime 高级特性（如 lua 脚本）的不兼容。
6. **文件编码问题**：确保所有的 yaml 或 txt 文件均为 UTF-8 无 BOM 编码，否则会导致解析失败。
