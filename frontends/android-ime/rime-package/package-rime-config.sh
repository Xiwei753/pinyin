#!/usr/bin/env bash
set -euo pipefail

# 自动定位仓库根目录
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$REPO_ROOT"

# 将 rime 目录打包供 Android 输入法导入
echo "开始打包 Rime 配置文件..."
mkdir -p build/
cd shared/rime/
zip -r ../../build/android-rime-config.zip *
cd ../../
echo "打包完成。生成的文件：build/android-rime-config.zip"
echo "请将其导入至 Android 端支持 Rime 的输入法应用中。"
