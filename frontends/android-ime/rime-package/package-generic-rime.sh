#!/usr/bin/env bash
set -euo pipefail

# 自动定位仓库根目录
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$REPO_ROOT"

ZIP_FILE="build/android-rime-generic.zip"

echo "开始打包 Android 端 generic Rime 配置文件..."

# 删除旧的 zip，避免旧文件混入
if [ -f "$ZIP_FILE" ]; then
    echo "清理旧的打包文件: $ZIP_FILE"
    rm "$ZIP_FILE"
fi

mkdir -p build/

# 检查需要的文件是否存在
REQUIRED_FILES=(
    "default.custom.yaml"
    "xiwei_pinyin.schema.yaml"
    "xiwei_t9.schema.yaml"
    "xiwei_pinyin.dict.yaml"
    "custom_phrase.txt"
    "symbols.yaml"
    "README.md"
)

echo "检查必要文件..."
cd shared/rime/
for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo "错误：缺少必要文件 $file"
        exit 1
    fi
done

echo "打包文件到 $ZIP_FILE..."
zip -r "../../$ZIP_FILE" *

cd ../../

# 打包后自动检查 zip 是否存在
if [ ! -f "$ZIP_FILE" ]; then
    echo "错误：打包失败，$ZIP_FILE 未生成"
    exit 1
fi

echo "打包成功！"
echo "压缩包内包含的文件列表："
unzip -l "$ZIP_FILE"

echo "--------------------------------------------------"
echo "请将 build/android-rime-generic.zip 传到手机，"
echo "并在支持纯文件 zip 导入的安卓输入法应用中导入该配置。"
echo "注意：请不要把 build/ 目录或 zip 提交到代码仓库。"
echo "--------------------------------------------------"
