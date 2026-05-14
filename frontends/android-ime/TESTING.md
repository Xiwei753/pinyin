# Android 端测试清单

本测试清单用于在将 Rime 配置导入到 Android 宿主输入法时，进行基础功能的验证。

## 1. 打包产物获取
你可以选择本地打包或使用 GitHub Actions 自动构建产物。

**方式一：GitHub Actions 获取（推荐）**
- [ ] 在仓库的 Actions 页面找到最新的 validate-and-package 运行记录，或者手动触发 workflow_dispatch。
- [ ] 下载名为 `android-rime-config` 的 artifact。
- [ ] 解压下载好的 artifact，确认得到 `android-rime-config.zip` 文件。

**方式二：本地打包**
- [ ] 运行打包脚本：`bash frontends/android-ime/rime-package/package-rime-config.sh`
- [ ] 脚本执行成功，不应该报错。
- [ ] 成功生成打包文件：`build/android-rime-config.zip`

## 2. Zip 内容检查
- [ ] 确认压缩包内部文件结构正确，不应有多余的顶级目录。
- [ ] 确认包含必要文件：
  - `default.custom.yaml`
  - `xiwei_pinyin.schema.yaml`
  - `xiwei_t9.schema.yaml`
  - `xiwei_pinyin.dict.yaml`
  - `custom_phrase.txt`
  - `symbols.yaml`
  - `README.md`

## 3. 手机导入测试
- [ ] 成功将 `build/android-rime-config.zip` 传至手机。
- [ ] 在 Android Rime 前端（如同文输入法）中找到并导入该压缩包。
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

## 常见失败原因

如果在测试中遇到问题，请检查以下常见原因：

1. **zip 结构不对**：打包时是否多带了一层目录，导致安卓前端找不到 Rime 配置文件。
2. **安卓前端没有重新部署 Rime**：导入文件后，必须执行“重新部署”才能生效。
3. **方案没有加入 schema_list**：如果列表里没有看到方案，检查 `default.custom.yaml` 是否正确配置。
4. **九键只是实验方案**：如果九键无法正常打出词，这是因为目前九键只是初步的实验方案，还没有完善。
5. **安卓前端不支持某些 Rime 特性**：极少数前端可能存在对 Rime 高级特性（如 lua 脚本）的不兼容。
6. **文件编码问题**：确保所有的 yaml 或 txt 文件均为 UTF-8 无 BOM 编码，否则会导致解析失败。
