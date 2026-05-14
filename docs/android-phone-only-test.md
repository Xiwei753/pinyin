# Android 纯手机端测试指南

如果你手边只有 Android 手机，没有电脑参与，请按照此指南完成 Rime 配置的获取、导入和测试。

## 1. 下载打包产物

由于不同的前端导入机制不互通，你需要根据你使用的输入法下载不同的包。

1. 使用手机浏览器打开本项目 GitHub 仓库的 **Actions** 页面。
2. 点击最新的 `Validate and Package` 工作流运行记录（绿色的 ✅）。
3. 滑到底部找到 **Artifacts**。
4. 根据你的前端下载：
   - **Fcitx5 for Android 用户**：下载 `android-rime-fcitx5-userdata`
   - **Trime / 同文输入法用户**：下载 `android-rime-trime`
   - （备用）如果你的前端明确支持直接导入纯配置 zip，下载 `android-rime-generic`
5. 下载后你通常会得到一个外层带有 GitHub 包装的 zip 文件，请使用手机自带的文件管理器解压它，直到暴露出里面的 `android-rime-*.zip` 文件。

## 2. 导入与部署（按前端区分）

### A. Fcitx5 for Android + RIME 插件

Fcitx5 **不**支持直接把普通 zip 扔进去。你必须使用我们专门为它生成的“用户数据包”。

1. 确认你已经解压得到了 `android-rime-fcitx5-userdata.zip`。
2. 打开 Fcitx5 设置 App。
3. 找到并点击 **“导入用户数据”** （Import user data） 或类似的数据恢复选项。
4. 在文件选择器中，选中 `android-rime-fcitx5-userdata.zip`。
5. 等待应用提示导入成功。
6. 进入 Fcitx5 设置中，找到 RIME 插件的设置，或者键盘设置，点击 **重新部署** (Deploy)。

*注：当前的 `external/data/rime/` 目录结构是根据 Fcitx5 Android 的 UserDataManager 把 `external/` 映射到 `getExternalFilesDir(null)` 的机制推导出来的。如果导入时出现 `open failed: ENOENT`，说明 zip 目录结构不符合 Fcitx5 的解压器要求（缺少显式父目录或目录顺序错误）。新版打包脚本已按官方要求生成结构，请重新下载最新的 `android-rime-fcitx5-userdata.zip` 导入即可。如果导入 `android-rime-fcitx5-userdata` 后没有看到“希为拼音”，先不要继续乱点，说明 `external/data/rime/` 路径可能还需要按 Fcitx5 RIME 插件源码继续校正。*

### B. Trime (同文输入法)

Trime 目前**没有**好用的通用 zip 导入入口，你必须走手动复制。

1. 确认你已经解压得到了 `android-rime-trime.zip`。
2. **再次解压** `android-rime-trime.zip`。你应该能看到解压出来一个叫做 `rime` 的文件夹。
3. 打开一个高级文件管理器，例如 [Material Files (质感文件)](https://github.com/zhanghai/MaterialFiles) 或 MT 管理器。
4. 复制刚才解压得到的那个 `rime/` 文件夹。
5. 导航到 Trime 的应用数据目录。由于 Android 版本的不同，路径可能是以下之一：
   - `内部存储/rime/` (较老版本 Android)
   - `Android/data/com.osfans.trime/files/rime/` (Android 11+)
6. 将你复制的 `rime/` 文件夹覆盖过去。如果你找不到路径，可以使用文件管理器搜索。
7. 回到 Trime 应用内，在主界面或设置中点击 **重新部署** (Deploy)。如果报错，请确保复制时你直接替换了整个文件夹，或者文件权限正确。

## 3. 进行基础测试

成功部署后，在任意可输入的文本框（如便签、微信搜索框）调出你的输入法键盘。

1. 确认输入法当前的键盘/方案名为 **希为拼音**。如果不是，长按空格或使用方案切换键进行切换。
2. 输入以下拼音进行验证：
   - `nihao` -> 期望候选：你好
   - `shurufa` -> 期望候选：输入法
   - `pinyin` -> 期望候选：拼音
   - `zhongguo` -> 期望候选：中国
   - `tongbu` -> 期望候选：同步

如果以上能够正常打出，说明部署和配置均已成功生效！

## 4. 导入成功但遇到候选词问题怎么办

如果你在测试时遇到问题：

- 如果键盘显示了“中州韵（希）”或类似字样，说明方案已成功加载。
- **如果输入 `nihao` 只有拼音字母没有中文候选**：这说明是 Rime 词库未成功编译或配置问题。请重新回到 Actions 下载最新包，导入并**重新部署**。你可以尝试切换到内置兜底的“明月拼音·简化字” (`luna_pinyin_simp`) 来判断是引擎问题还是特定方案配置问题。
- **如果输入 `nihao` 出现了候选，但第一候选是繁体/异体字（如“妳好”、“逆號”）**：恭喜你，这说明部署已经完全成功！当前进入了简繁过滤阶段。由于早期为了保证稳定性临时使用了 luna_pinyin 词库，未加简体过滤。本项目现已修复此问题（增加了 simplifier + uniquifier）。解决办法是：更新到新版的 artifact 后重新导入。重新测试 `nihao`，第一候选应为“你好”；测试 `shurufa`，第一候选应为“输入法”。

详细排查步骤请参考 `docs/android-rime-no-candidates-debug.md` 和 `docs/fcitx5-android-rime-deploy-debug.md`。

*注意：如果能看到“中州韵（希）”，只能说明 schema 被识别，不代表词典已经部署成功。真正成功的标志是 `data/rime/build/` 生成部署产物，且输入 `nihao` 出现中文候选。*

*注：你可以尝试切换到“希为九宫格”方案，但目前它仅作为实验识别之用，尚未完善日用打字逻辑。*
