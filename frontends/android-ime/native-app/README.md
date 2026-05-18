# Android 原生前端

Android 原生 T9 拼音输入法，基于 `InputMethodService` + 自研 `T9Engine`。

不依赖 Rime、Trime 或 Fcitx5 for Android。这是独立的 Android 原生输入法实现。

## 子目录说明
- `android/`：Android Gradle 工程源码目录。

## 本地构建

```sh
cd frontends/android-ime/native-app/android
./gradlew test assembleDebug
```

## 架构要点

- **T9Engine**：自研输入内核，负责拼音音节解码、候选生成、词库查询。
- **T9ImeController**：纯 Kotlin 控制器，负责按键行为逻辑（0/1/删除/候选点击）。
- **XiweiT9ImeService**：Android `InputMethodService`，只负责 UI 渲染、系统上屏、触感反馈。
- **SettingsRepository**：统一设置管理（触感、主题、键盘高度、候选数量、调试日志）。
- **T9DebugLogStore**：内存+文件双层调试日志存储，支持导出分享。
- **BuiltinDictionary**：词库数据来源，使用 rime-ice 作为词库数据（不是引擎）。
- **T9CodeMapper**：自动拼音转九键数字码。
- **DictionaryProvider**：词库接口，为后续用户词库和同步预留。
