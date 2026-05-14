# 私人数据同步指南

## 为什么要区分 public 和 private 仓库？

本输入法项目分为两部分：
1. **Public 仓库**（本仓库）：存放核心代码、打包脚本、默认配置和格式模板。这部分是公开的，目的是分享给社区或方便在不同设备上克隆基础环境。
2. **Private 仓库**（你的私人仓库）：存放你个人的词库、常用短语、个人地址、账号等敏感信息。这部分必须是**私有的**，防止隐私泄露。

## 文件放置规则

*   **Public 仓库放什么：**
    *   `shared/rime/*.yaml` (基础配置)
    *   `shared/private-template/*` (私人数据格式的示例模板)
    *   核心 Python 代码和各类打包、验证脚本。
*   **Private 仓库放什么：**
    *   你真实的 `custom-phrases.txt` (包含你配置好的快捷输入短语)。
    *   你真实的 `user-dictionary.yaml` (包含你调整过的高频词汇)。
    *   你真实的 `user-settings.yaml` (包含你自己的输入习惯配置)。

## 同步机制

1.  **初始化**：使用 `tools/sync/init-private-repo.sh` 将你的私人仓库克隆到 `.private_sync/` 目录下。该目录在 `.gitignore` 中，不会被提交到 public 仓库。
2.  **拉取 (Pull)**：使用 `tools/sync/pull-private.sh` 从私人仓库拉取最新数据。脚本会自动调用 `tools/private/build-user-rime.py`，将你的数据（如 `custom-phrases.txt`）生成到 `shared/rime/custom_phrase.txt` 供 Rime 使用。
3.  **推送 (Push)**：如果你在本地修改了 `.private_sync/` 中的文件，可以使用 `tools/sync/push-private.sh` 推送到你的私人仓库。脚本会在推送前提示确认。

## Android 端使用说明

目前手机端处于基础测试阶段。
*   打包脚本（如 `package-fcitx5-userdata.sh`）在运行时，如果检测到本地存在 `.private_sync/` 目录，会自动将你的私人词库/短语合并打包到 Fcitx5 的导入包中。
*   后续当 Android 原生前端开发完善后，将会在 App 设置中提供直接登录 GitHub 进行同步的入口，不再需要通过电脑打包。

## ⚠️ 重要安全警告

1.  **绝对不要**将包含个人信息的真实词库、短语、地址等提交到本 public 仓库。
2.  **绝对不要**在任何公开的配置文件或脚本中硬编码你的 GitHub Token。
3.  确保你的私人数据仓库在 GitHub 上被设置为 **Private (私有)**。
