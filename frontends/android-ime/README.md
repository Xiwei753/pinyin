# Android 前端层

本目录包含了 Android 平台输入法的相关配置及部署方案。

## 第一阶段目标

Android 第一阶段**不是**自研完整的输入法 App。
第一阶段的核心是将 `shared/rime/` 中的共享配置打包，以便导入到支持 Rime 的安卓输入法前端（如：同文输入法 Trime 或 Fcitx5 for Android）中使用。

在这个阶段，我们主要验证以下功能：
- 全拼方案是否可用
- 自定义词库是否能够成功导入
- 自定义短语是否生效
- 九键方案是否能被识别加载

**注意：** 九键目前仅作为**实验方案**，请勿将其视为已完成的日用功能。

如果现成安卓 Rime 前端在未来无法满足极致流畅的动画和手感需求，我们再考虑在 `native-app/` 目录下开发原生 Android 输入法前端。

## 手机测试步骤

请按照以下步骤进行 Android 端的导入与测试：

你可以通过 GitHub Actions 自动打包，也可以在电脑上本地打包。

**方式一：通过 GitHub Actions 获取打包产物（推荐）**
1. 每次推送到 `main` 分支或创建 Pull Request 时，GitHub Actions 会自动进行校验和打包。也可以在项目的 Actions 页面手动触发 (workflow_dispatch)。
2. 打包完成后，在对应的 Actions 运行记录页面下载名为 `android-rime-config` 的 artifact。
3. 下载后解压得到 `android-rime-config.zip` 文件。
4. 注意：`build/` 目录和 zip 文件不会提交到代码仓库，仅作为 CI 产物提供下载。

**方式二：在电脑上本地打包：**
1. 在项目根目录运行以下命令打包 Rime 配置：
   ```bash
   bash frontends/android-ime/rime-package/package-rime-config.sh
   ```
2. 在本地获取生成的 `build/android-rime-config.zip` 文件。

**后续步骤：**
1. **传输文件：**
   将获取到的 `android-rime-config.zip` 文件传送到手机上。

3. **导入配置：**
   在手机上打开支持 Rime 的安卓输入法前端，在设置中选择从 zip 文件导入或恢复 Rime 数据。

4. **重新部署：**
   在输入法设置中点击“重新部署”或“重新加载 Rime 配置”。

5. **进行测试：**
   在任意文本框调出键盘，尝试输入以下拼音进行测试：
   - `nihao`
   - `shurufa`
   - `pinyin`
   - `zhongguo`
   - `tongbu`

6. **检查全拼方案：**
   如果输入法列表中能看到“希为拼音”并且输入正常，说明全拼方案加载成功。第一轮主要测试目标为希为拼音。

7. **检查九键方案：**
   如果能看到“希为九宫格”，说明九键方案至少被识别。**注意：希为九宫格目前只是实验方案，第一轮 Android 测试只确认九键方案能被识别，不要求日用效果。**
