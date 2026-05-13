#!/usr/bin/env bash
set -euo pipefail

# 自动定位仓库根目录
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

echo "=== 初始化私人用户数据仓库 ==="

# 确保有 user.yaml
if [ ! -f "shared/settings/user.yaml" ]; then
    echo "未找到 shared/settings/user.yaml，正在从模板复制..."
    cp "shared/settings/user.example.yaml" "shared/settings/user.yaml"
    echo "请配置 shared/settings/user.yaml 后再次运行此脚本。"
    exit 1
fi

PRIVATE_REPO_URL=$(grep "private_repo_url" "shared/settings/user.yaml" | cut -d'"' -f2 || true)

if [ -z "$PRIVATE_REPO_URL" ]; then
    echo "错误：无法在 shared/settings/user.yaml 中找到 private_repo_url"
    exit 1
fi

PRIVATE_DIR="$REPO_ROOT/.private_sync"

if [ -d "$PRIVATE_DIR" ]; then
    echo "私人数据目录已存在：$PRIVATE_DIR"
else
    echo "克隆私人仓库：$PRIVATE_REPO_URL 到 $PRIVATE_DIR"
    git clone "$PRIVATE_REPO_URL" "$PRIVATE_DIR" || true
fi

echo "私人数据仓库初始化完成。"
