#!/bin/bash
# 从远端拉取最新配置和词库
echo "开始从远端拉取更新..."
git pull --rebase
if [ $? -eq 0 ]; then
  echo "拉取成功！"
else
  echo "拉取失败，请检查网络或处理冲突。"
  exit 1
fi
