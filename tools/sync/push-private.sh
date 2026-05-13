#!/usr/bin/env bash
set -euo pipefail

# 自动定位仓库根目录
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

PRIVATE_DIR="$REPO_ROOT/.private_sync"

echo "=== 推送私人数据 ==="

if [ ! -d "$PRIVATE_DIR" ]; then
    echo "错误：私人数据仓库尚未初始化，请先运行 tools/sync/init-private-repo.sh"
    exit 1
fi

echo "正在将本地更新复制到私人仓库目录..."
if [ -f "$REPO_ROOT/shared/rime/custom_phrase.txt" ]; then
    cp "$REPO_ROOT/shared/rime/custom_phrase.txt" "$PRIVATE_DIR/custom_phrase.txt"
fi

if [ -d "$REPO_ROOT/shared/dictionary/user" ]; then
    mkdir -p "$PRIVATE_DIR/user_dict"
    cp -r "$REPO_ROOT/shared/dictionary/user/"* "$PRIVATE_DIR/user_dict/" 2>/dev/null || true
fi

cd "$PRIVATE_DIR"

if [ -n "$(git status --porcelain 2>/dev/null || true)" ]; then
    echo "检测到更改，正在提交并推送..."
    git add .
    git commit -m "Auto-sync private data: $(date +'%Y-%m-%d %H:%M:%S')" || true
    if ! git push origin main; then
        echo "错误：推送私人仓库失败，请检查网络或是否需要先 pull 解决冲突。"
        exit 1
    fi
    echo "推送成功。"
else
    echo "没有检测到需要同步的更改。"
fi
