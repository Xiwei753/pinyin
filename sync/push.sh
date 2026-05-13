#!/bin/bash
# 提交并推送本地配置和词库
echo "开始推送本地更新到远端..."
git add ../rime/ ../config/ ../dictionary/
git commit -m "sync: 自动同步用户配置与词库"
git push
if [ $? -eq 0 ]; then
  echo "推送成功！"
else
  echo "推送失败，请检查网络或是否有尚未拉取的更新。"
  exit 1
fi
