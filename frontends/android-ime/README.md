# Android 前端层

本目录包含 Android 平台输入法的原生开发和部署方案。

## 当前路线

Android 端已完全转向**原生 T9 输入法开发**，位于 `native-app/` 子目录。

- 基于 Kotlin/Java `InputMethodService` + 自研 `T9Engine`。
- 不使用 Rime 引擎、Trime 或 Fcitx5 for Android。
- 不通过 Rime 配置包方案部署。

旧版 Rime/Trime/Fcitx5 Android 配置包方案已彻底移除。

## 构建和测试

```
cd native-app/android
./gradlew test assembleDebug
```

编译出的 APK 位于 `native-app/android/app/build/outputs/apk/debug/app-debug.apk`。

## 开发阶段

当前阶段专注于 Android 原生 T9 输入法：
- 九键拼音输入完整管线
- 候选正确性与词库覆盖率
- 设置页（触感、主题、键盘高度、候选数量、调试日志）
- 调试日志开关与导出

未来路线（当前不开发）：
- Linux fcitx5 插件外壳（接入 shared core）
- 全键布局
- 云同步功能
