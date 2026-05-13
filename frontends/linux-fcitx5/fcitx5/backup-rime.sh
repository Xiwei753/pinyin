#!/bin/bash
# 备份旧配置
echo "开始备份旧的 fcitx5 rime 配置..."
if [ -d ~/.local/share/fcitx5/rime/ ]; then
  mkdir -p ~/.local/share/fcitx5/rime_backup_$(date +%Y%m%d%H%M%S)/
  cp -r ~/.local/share/fcitx5/rime/* ~/.local/share/fcitx5/rime_backup_$(date +%Y%m%d%H%M%S)/
  echo "备份完成！"
else
  echo "未找到现有配置，跳过备份。"
fi
