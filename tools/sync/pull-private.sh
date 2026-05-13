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
# 这里是将 .private_sync/ 的文件覆盖到 shared/ 等对应位置
if [ -f "$PRIVATE_DIR/custom_phrase.txt" ]; then
    cp "$PRIVATE_DIR/custom_phrase.txt" "$REPO_ROOT/shared/rime/custom_phrase.txt"
fi
if [ -d "$PRIVATE_DIR/user_dict" ]; then
    mkdir -p "$REPO_ROOT/shared/dictionary/user"
    cp -r "$PRIVATE_DIR/user_dict/"* "$REPO_ROOT/shared/dictionary/user/" || true
fi

echo "拉取私人数据完成。"
