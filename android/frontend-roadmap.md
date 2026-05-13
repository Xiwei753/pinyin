# Frontend Roadmap (Android 前端路线图)

目前 Android 端仅作为 Rime 配置包的接收端。但未来的路线图规划如下：

- **第一阶段（当前）**：完全使用 Rime 引擎，并通过脚本生成导入包供现有 Android 输入法（如 Trime）使用。不进行专门的 UI 开发。
- **未来方向**：独立开发 Android 键盘 App。
  - 重点实现高度定制的九键体验。
  - 提供顺滑的触摸手感反馈（包括声音与振动，具体参考 `config/features.yaml` 中的前端配置）。
  - 候选栏动画与精细定制的键盘皮肤。
  - 核心逻辑依旧遵循 `core/` 下的契约，与 Linux 端共享引擎逻辑。
