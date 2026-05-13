#!/usr/bin/env bash
set -euo pipefail

# 自动定位仓库根目录
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$REPO_ROOT"

# 检查并安装 fcitx5-rime
echo "开始安装 fcitx5-rime..."
# TODO: 根据不同的 Linux 发行版(apt/pacman/dnf)补充具体安装命令
echo "安装 fcitx5-rime 完成。(这里仅做提示)"
