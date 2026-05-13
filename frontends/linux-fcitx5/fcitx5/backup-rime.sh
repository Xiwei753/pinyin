#!/usr/bin/env bash
set -euo pipefail

# 自动定位仓库根目录
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$REPO_ROOT"

# 备份旧配置
echo "开始备份旧的 fcitx5 rime 配置..."

RIME_DIR="$HOME/.local/share/fcitx5/rime"

if [ -d "$RIME_DIR" ]; then
  # 检查目录是否为空
  if [ -z "$(ls -A "$RIME_DIR")" ]; then
    echo "现有 rime 配置目录为空，跳过备份。"
  else
    timestamp="$(date +%Y%m%d%H%M%S)"
    backup_dir="$HOME/.local/share/fcitx5/rime_backup_$timestamp"

    mkdir -p "$backup_dir"
    cp -r "$RIME_DIR"/* "$backup_dir"/
    echo "备份完成！备份路径: $backup_dir"
  fi
else
  echo "未找到现有配置，跳过备份。"
fi
