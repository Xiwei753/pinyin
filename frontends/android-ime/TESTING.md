# Android 端测试清单

## 构建验证

在开发环境运行完整构建和单元测试：

```bash
cd native-app/android
./gradlew test assembleDebug
```

- [ ] `test` 全部通过，无失败用例。
- [ ] `assembleDebug` 成功生成 `app-debug.apk`。

## 单元测试覆盖

核心层测试（纯 JVM，不依赖 Android 框架）：
- [ ] `T9ImeControllerTest` — 按键行为、候选提交、状态清空
- [ ] `T9EngineTest*` — 拼音解码、候选生成、回退逻辑
- [ ] `SettingsRepositoryTest` — 设置读写
- [ ] `T9DebugLoggerTest` — 日志存储、持久化、清空

Service 浅层测试：
- [ ] `XiweiT9ImeServiceLifecycleTest` — 生命周期不崩溃
- [ ] `XiweiT9ImeServiceLoggingTest` — 日志开关控制
- [ ] `XiweiT9ImeServiceUiBehaviorTest` — 候选数量传递、键盘高度安全性

## 真机测试清单

在 Android 设备上安装 APK 后：
- [ ] 启用输入法并切换成功。
- [ ] 输入 `96`，preedit 显示 `wo`，候选包含"我"。
- [ ] 输入 `64`，preedit 显示 `ni`，候选包含"你"。
- [ ] 输入 `288249464`，preedit 显示 `bu tai xing`，第一候选为"不太行"。
- [ ] 按 `0` 键：buffer 为空时输入空格，有候选时提交第一候选。
- [ ] 按 `1` 键：非空 buffer 时插入音节分隔符。
- [ ] 设置页全部控件可用：触感开关、候选数量、主题、键盘高度、调试日志。
- [ ] 指定真机测试员使用其手机进行上述组合测试。
