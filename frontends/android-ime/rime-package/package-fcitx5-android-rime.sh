#!/usr/bin/env bash
set -euo pipefail

echo "============================================================"
echo "注意：Fcitx5 for Android + RIME Plugin"
echo "目前不支持标准的 zip 一键导入 Rime 配置。"
echo "该前端需要手动复制配置目录，目前不生成假包。"
echo ""
echo "请参考 docs/android-import-strategy.md 中的说明，"
echo "使用 MT 管理器或 Material Files 等工具，将共享配置"
echo "手动放入 Fcitx5 的 Android/data 对应目录中。"
echo "============================================================"
exit 0
