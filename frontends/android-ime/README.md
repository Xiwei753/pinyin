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
2. 打包完成后，在对应的 Actions 运行记录页面下载相应的 artifact。
   - 同文输入法 (Trime) 用户建议下载 `android-rime-trime`，解压得到 `android-rime-trime.zip`。
   - 其他前端尝试下载 `android-rime-generic`，解压得到 `android-rime-generic.zip`。
3. 注意：`build/` 目录和 zip 文件不会提交到代码仓库，仅作为 CI 产物提供下载。

**方式二：在电脑上本地打包：**
1. 在项目根目录运行相应的命令打包：
   - 生成通用包：`bash frontends/android-ime/rime-package/package-generic-rime.sh`
   - 生成 Trime 专用包：`bash frontends/android-ime/rime-package/package-trime.sh`
2. 在本地获取生成的 `build/android-rime-*.zip` 文件。

**后续步骤：**
1. **传输文件：**
   将获取到的对应 zip 文件传送到手机上。

2. **导入配置：**
   在手机上打开支持 Rime 的安卓输入法前端，在设置中尝试寻找从 zip 文件导入或恢复 Rime 数据的功能。
   - ⚠️ 注意：目前不能保证 zip 一键导入能在所有前端生效（例如 Fcitx5 for Android 就不支持）。如果界面中找不到导入入口，请参阅本文档末尾的“如果界面里没有导入按钮怎么办”章节，以及 `docs/android-import-strategy.md` 了解替代方案。

3. **重新部署：**
   导入完成后，必须在输入法设置中点击“重新部署”或“重新加载 Rime 配置”。

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

## 如果界面里没有导入按钮怎么办

不同的 Android 前端导入方式不统一，不要在设置里毫无目的地乱点。由于当前版本的部分前端（如 Fcitx5 for Android + RIME Plugin）可能根本没有提供 zip 导入入口，你需要改走手动复制路线：

1. **不要再试图通过 UI 导入**：如果你翻遍了设置也找不到恢复/导入功能，那多半就是没有。
2. **解压文件**：把下载到的 zip 包在手机上解压，暴露出里面的配置文件。
3. **手动覆盖**：借助 Material Files、MT 管理器或通过连接电脑，找到输入法的工作目录（通常位于 `Android/data/<包名>/files/` 下，具体参考 [Android 导入方式决策策略](../../docs/android-import-strategy.md)），将解压出来的文件复制并覆盖进去。
4. **重新部署**：回到输入法 App 中点击“重新部署”。
5. **联系开发者调整**：如果你找到了某种隐蔽的导入方式或遇到了特定前端，请截图并反馈，我们会根据反馈更新专用包脚本。
