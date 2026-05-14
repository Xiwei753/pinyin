# Android 原生前端

此目录用于存放未来将要开发的 Android 原生前端代码。

考虑到通过 Rime schema 无法解决由于宿主输入法（如 Fcitx5 Android）硬编码带来的 UI 问题，项目接下来将探索构建一个真正原生的输入法前端。

## 当前状态
目前处于最小原型探索阶段。详细的开发计划与目标请参见 `T9_FRONTEND_PLAN.md`。

这是一个独立的 Android 原生九键输入法原型，用于验证九键 UI、输入缓冲和候选上屏的流程，不包含 Rime 引擎、同步功能和复杂词库。

## 子目录说明
- `prototype/`: 存放早期的概念验证和原型测试脚本（例如用 Python 编写的逻辑原型或基础的设计草案），先确保逻辑和架构可行，再进入正式的 Kotlin 和 Android 工程开发。
- `android/`: 最小 Android Gradle 工程源码目录。

## 本地构建

这是最小的 Android 九键输入法原型。暂不接 Rime，暂不接同步，暂不处理复杂候选。

- T9Engine 已经从 Service 拆出
- Candidate 是核心候选数据结构
- BuiltinDictionary 是临时内置词库
- 后续大词库 / 用户词库 / 同步都应该替换或扩展 data 层
- Android Service 只负责 UI 和系统上屏

构建命令如下：

```sh
cd frontends/android-ime/native-app/android
./gradlew assembleDebug
```
