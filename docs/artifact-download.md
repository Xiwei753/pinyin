# Android 产物下载与测试指南

通过 GitHub Actions，我们实现了 Android 端 Rime 配置包的自动化打包。每次推送到 `main` 分支或提交 Pull Request，云端都会自动进行校验并生成可下载的 zip 产物。

## 下载步骤

1. **进入 Actions 页面**
   打开本 GitHub 仓库的 [Actions](https://github.com/USERNAME/REPO/actions) 页面（请根据实际项目地址替换）。

2. **选择最新运行记录**
   在左侧的 Workflows 列表中选择 `Validate and Package`，然后点击最新一次成功的运行记录（带有绿色打勾图标）。

3. **下载 Artifact**
   在运行记录页面的底部，找到 **Artifacts** 区域，根据你的前端下载对应的压缩包：
   - Trime 用户下载 `android-rime-trime`
   - 其他尝试通用 zip 的用户下载 `android-rime-generic`

4. **解压文件**
   下载下来的文件解压后，你会得到一个带有 `.zip` 后缀的文件（例如 `android-rime-generic.zip`）。如果你的前端支持一键导入，**不需要**再解压这个里面的 zip。

## 导入与测试

1. **传输到手机**
   将对应的 zip 文件传到你的 Android 手机上。

2. **导入前端**
   打开你使用的安卓 Rime 前端（如“同文输入法”等），在设置中找到“从 zip 文件恢复”或“导入配置”选项，选中你刚才传到手机的 zip 包。

## 如果界面里没有导入按钮怎么办

1. **不要一直找**：部分前端（如 Fcitx5 for Android 配合 RIME）并没有提供 zip 导入功能。
2. **解压配置**：把手机上的 zip 包解压出来。
3. **手动复制**：使用高级文件管理器（如 Material Files、MT 管理器），将文件直接放入输入法的专属数据目录。详情请见 `docs/android-import-strategy.md`。

3. **重新部署**
   导入完成后，务必在输入法设置中点击**重新部署**（Deploy），让新配置生效。

## 注意事项

- **产物过期**：GitHub Actions 的 Artifact 通常有保留期限（默认为 90 天）。如果发现最新的 Artifact 已经过期或因故没有生成，你可以点击 `Validate and Package` 工作流页面的 **Run workflow** 按钮，手动触发一次（`workflow_dispatch`）打包过程。
- **实验性功能**：目前的“希为九宫格”仅为实验性方案。测试时只需确认该方案存在并在切换时不会引发崩溃即可，暂不要求实际输入体验。第一阶段的重点测试对象为全拼方案“希为拼音”。
