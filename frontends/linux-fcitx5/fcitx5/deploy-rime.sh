#!/usr/bin/env bash
set -euo pipefail

# 自动定位仓库根目录
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$REPO_ROOT"

echo "开始部署 rime 配置到 ~/.local/share/fcitx5/rime/"
mkdir -p ~/.local/share/fcitx5/rime/
cp -r shared/rime/* ~/.local/share/fcitx5/rime/
echo "部署完成！"
