# Fcitx5 Android Rime 部署状态调试指南

如果你发现：
- 键盘底部显示了“中州韵（希）”或其他方案的名称
- 输入 `nihao` 等拼音时有预编辑（英文字母显示正常）
- **但是没有出现任何中文候选词**

这说明方案已被识别，但是 Rime 引擎可能没有成功编译/部署词典，或者配置存在问题。此时请按照以下步骤检查：

## 1. 检查 `data/rime/` 目录结构
使用文件管理器查看 Fcitx5 的 Rime 用户数据目录（通常可以通过 Fcitx5 设置中的用户数据目录入口找到，或者在 `Android/data/org.fcitx.fcitx5.android/files/data/rime/` 下）。

- 确认 `xiwei_pinyin.schema.yaml`、`xiwei_pinyin.dict.yaml`、`luna_pinyin.schema.yaml` 等基础文件是否存在。
- 确认 `DEPLOY_CHECK.txt` 是否存在。如果存在，说明导入或复制的路径是正确的。

## 2. 检查 `build/` 目录是否生成
部署成功的核心标志是 Rime 引擎在 `data/rime/` 目录下生成了一个名为 `build/` 的子目录。

- 如果 **`build/` 目录不存在**：说明 Rime 引擎根本没有被触发部署。请在 Fcitx5 设置中点击“重新部署”或者彻底重启 Fcitx5 实例。
- 如果 **`build/` 目录存在，但里面没有 `xiwei_pinyin.schema.yaml` 或 `xiwei_pinyin.prism.bin` 等相关产物**：说明“希为拼音”方案编译失败。请检查该方案是否被正确放入 `default.custom.yaml` 的 `schema_list`，或者尝试排查方案配置文件内部的错误。

## 3. 检查内置兜底方案
如果以上检查均正常，但仍然没有候选词：
- 请切换到内置方案 `luna_pinyin_simp` 或 `luna_pinyin` 进行测试。
- 如果内置方案有候选词，而自定义方案没有，说明这是自定义方案（例如依赖或词典文件格式）的问题。
- 如果所有方案都没有候选词，可能是之前的编译缓存损坏。请尝试删除 `build/` 目录，然后重新部署。

## 4. 导入用户数据包时出现 `open failed: ENOENT`
如果你在导入 `android-rime-fcitx5-userdata.zip` 时收到该错误，这是由于旧版打包脚本未提供严格的 zip 目录顺序导致的解压失败。新版仓库已经修复了此问题，请重新下载最新构建的 `android-rime-fcitx5-userdata.zip` 导入。

如果遇到其他问题，请参阅 `docs/android-rime-no-candidates-debug.md` 进行深入排查。
