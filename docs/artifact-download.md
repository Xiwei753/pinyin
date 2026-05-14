# Android 产物下载与测试指南

通过 GitHub Actions，我们实现了 Android 端 Rime 配置包的自动化打包。每次推送到 `main` 分支或提交 Pull Request，云端都会自动进行校验并生成可下载的 zip 产物。

## 下载步骤

1. **进入 Actions 页面**
   打开本 GitHub 仓库的 [Actions](https://github.com/USERNAME/REPO/actions) 页面（请根据实际项目地址替换）。

2. **选择最新运行记录**
   在左侧的 Workflows 列表中选择 `Validate and Package`，然后点击最新一次成功的运行记录（带有绿色打勾图标）。

3. **下载 Artifact**
   在运行记录页面的底部，找到 **Artifacts** 区域，根据你的前端下载对应的压缩包：
   - **Fcitx5 for Android 用户**：下载 `android-rime-fcitx5-userdata`
   - **Trime 用户**：下载 `android-rime-trime`
   - **其他前端备用**：下载 `android-rime-generic`

4. **解压外层 ZIP**
   GitHub Actions 会自动把你生成的 zip 再包装一层 zip。所以下载下来后，你需要解压一次，暴露出里面真正的 `.zip` 产物（例如 `android-rime-fcitx5-userdata.zip` 或 `android-rime-trime.zip`）。

## 导入与测试

详细在手机上的操作步骤，请参阅专门的无电脑测试文档：[`docs/android-phone-only-test.md`](android-phone-only-test.md)。

以下是简要指南：

- **Fcitx5 用户**：将 `android-rime-fcitx5-userdata.zip` 传到手机，通过 Fcitx5 设置中的“导入用户数据”进行导入。
- **Trime 用户**：Trime 界面无导入入口。解压 `android-rime-trime.zip`，使用文件管理器将 `rime/` 文件夹复制覆盖至 Trime 数据目录。
- **部署**：无论使用何种方式，最后都必须在输入法界面触发 **重新部署**。

## 注意事项

- **产物过期**：GitHub Actions 的 Artifact 通常有保留期限（默认为 90 天）。如果发现最新的 Artifact 已经过期或因故没有生成，你可以点击 `Validate and Package` 工作流页面的 **Run workflow** 按钮，手动触发一次（`workflow_dispatch`）打包过程。
- **实验性功能**：目前的“希为九宫格”仅为实验性方案。测试时只需确认该方案存在并在切换时不会引发崩溃即可，暂不要求实际输入体验。第一阶段的重点测试对象为全拼方案“希为拼音”。
