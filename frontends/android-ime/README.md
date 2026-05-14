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
   - **Fcitx5 for Android 用户**：下载 `android-rime-fcitx5-userdata`。这是 Fcitx5 专用用户数据导入包，不是普通 Rime zip，需通过 Fcitx5 的“用户数据导入”功能导入。
   - **同文输入法 (Trime) 用户**：下载 `android-rime-trime`。此包解压后得到 `rime/` 目录，需手动使用文件管理器将其复制到 Trime 的用户目录下（不再寻找界面的导入按钮）。
   - **其他支持普通文件导入的前端**：下载 `android-rime-generic` 备用。
3. 注意：`build/` 目录和 zip 文件不会提交到代码仓库，仅作为 CI 产物提供下载。

**方式二：在电脑上本地打包：**
1. 在项目根目录运行相应的命令打包：
   - 生成 Fcitx5 专用包：`bash frontends/android-ime/rime-package/package-fcitx5-userdata.sh`
   - 生成 Trime 手动复制包：`bash frontends/android-ime/rime-package/package-trime.sh`
   - 生成通用包：`bash frontends/android-ime/rime-package/package-generic-rime.sh`
2. 在本地获取生成的 `build/android-rime-*.zip` 文件。

**后续步骤：**
请根据你的前端选择导入方式：

- **Fcitx5 for Android**：将 `android-rime-fcitx5-userdata.zip` 传至手机，在 Fcitx5 设置中寻找“导入用户数据”功能并选择该压缩包导入。
- **Trime**：将 `android-rime-trime.zip` 传至手机解压，将解压出的 `rime/` 文件夹复制覆盖到 Trime 用户资料目录下。

导入或覆盖完成后，必须在输入法设置中点击“重新部署”或“重新加载 Rime 配置”。

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

## 关于导入方式的重要说明

Android Rime 前端并没有统一的导入标准，不要在界面里瞎点寻找通用的 Rime zip 导入入口。

1. **Trime 优先走手动复制**：当前 Trime 界面无明确 zip 导入按钮。获取 `android-rime-trime.zip` 后，请直接解压出 `rime/` 文件夹，使用文件管理器（如 Material Files / MT 管理器）复制到对应的用户数据目录（详见 `docs/android-phone-only-test.md`）。
2. **Fcitx5 Android 优先走用户数据导入**：Fcitx5 的 RIME 插件不提供直接导入 Rime zip 的功能。必须使用 `android-rime-fcitx5-userdata.zip`，它遵循 Fcitx5 Android 用户数据的规范（内含 `metadata.json` 且按 `external/data/rime/` 组织）。
3. **重新部署**：无论是覆盖目录还是通过数据恢复导入，最终都需要回到输入法应用中触发“重新部署”。
