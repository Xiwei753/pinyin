#!/bin/bash
# 部署 rime 配置到 fcitx5
echo "开始部署 rime 配置到 ~/.local/share/fcitx5/rime/"
mkdir -p ~/.local/share/fcitx5/rime/
cp -r ../../rime/* ~/.local/share/fcitx5/rime/
echo "部署完成！"
