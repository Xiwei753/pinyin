#!/usr/bin/env bash
set -euo pipefail

# 自动定位仓库根目录
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

PRIVATE_DIR="$REPO_ROOT/.private_sync"

echo "=== 拉取私人数据 ==="

if [ ! -d "$PRIVATE_DIR" ]; then
    echo "错误：私人数据仓库尚未初始化，请先运行 tools/sync/init-private-repo.sh"
    exit 1
fi

cd "$PRIVATE_DIR"
echo "正在从远程拉取最新更改..."
if ! git pull origin main; then
    echo "错误：拉取私人仓库数据失败，可能是网络问题或合并冲突。"
    exit 1
fi

echo "同步文件到工作区..."

# 运行用户数据构建工具
BUILD_SCRIPT="$REPO_ROOT/tools/private/build-user-rime.py"
if [ -f "$BUILD_SCRIPT" ]; then
    echo "运行用户 Rime 数据构建工具..."
    python3 "$BUILD_SCRIPT"
else
    echo "警告：未找到 $BUILD_SCRIPT"
fi

echo "拉取私人数据完成。"
