# Sync Specification (同步规范)

本项目将输入法配置分为两部分：
1. **Public 项目仓库**：存放代码、模板、默认配置、全平台共享基础字库等不敏感数据。
2. **Private 用户数据仓库**：存放用户的个人字库、个人设置、用户词频、自定义短语等隐私数据。

## 哪些文件同步 (Private 仓库)
以下内容会被推送到 Private 仓库：
- `shared/dictionary/user/` (个人增量字库)
- `shared/rime/custom_phrase.txt` (自定义短语)
- `shared/settings/user.yaml` (个人设定)
- Rime 引擎生成的用户词频库（位于具体端侧部署目录的 `*.userdb`）

## 哪些文件不同步
以下内容仅保留在本地或属于 Public 仓库：
- 缓存文件（如 `.git/`, `.cache/`）
- `shared/dictionary/base/` (基础字库，走 Public 仓库更新)
- 工具脚本、前端源码 (走 Public 仓库更新)
- **注意：GitHub token 绝对不能写进仓库！**必须通过 SSH key 或环境变量进行验证。

## 仓库分工
- **Public 仓库**：维护输入法基础功能、前端实现、Rime 最小基础配置。
- **Private 仓库**：备份和在多端（Linux/Android）同步个人打字习惯和隐私数据。
