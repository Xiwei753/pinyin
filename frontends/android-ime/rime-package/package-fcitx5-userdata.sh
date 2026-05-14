#!/bin/bash
# Generates Fcitx5 Android User Data import package

set -euo pipefail

# Setup directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
BUILD_DIR="$PROJECT_ROOT/build"
RIME_SOURCE_DIR="$PROJECT_ROOT/shared/rime"

# Create build dir
mkdir -p "$BUILD_DIR"

# Create a temporary staging area
STAGING_DIR="$(mktemp -d)"
echo "Using staging dir: $STAGING_DIR"

# Function to clean up staging area
cleanup() {
    rm -rf "$STAGING_DIR"
}
trap cleanup EXIT

# 1. Create metadata.json
# Fcitx5 Android UserDataManager.kt expects this to identify the package
EXPORT_TIME=$(date +%s%3N)
cat <<JSON > "$STAGING_DIR/metadata.json"
{
  "packageName": "org.fcitx.fcitx5.android",
  "versionCode": 0,
  "versionName": "generated",
  "exportTime": $EXPORT_TIME
}
JSON

# 2. Create the target external directory structure
# Fcitx5 maps external/ to getExternalFilesDir(null)
# rimeengine.cpp uses PkgData/rime, and native-lib.cpp maps PkgData to extData/data
# Thus the path inside the zip is external/data/rime/
TARGET_DIR="$STAGING_DIR/external/data/rime"
mkdir -p "$TARGET_DIR"

# 3. Copy Rime files
# Make sure we do not copy the build/ cache to avoid breaking the import
rsync -av --exclude="build/" "$RIME_SOURCE_DIR"/ "$TARGET_DIR/"

# Add a README.txt to instruct users to reload configuration or restart Fcitx after importing
cat <<EOF > "$TARGET_DIR/README.txt"
重要提示：
导入此用户数据包后，由于 Rime 配置发生变更，必须让 Fcitx5 重新加载。
如果你发现没有候选词，请：
1. 删除已有 data/rime/build 目录（如果有）
2. 在 Fcitx5 设置中点击 RIME 的“重新部署”或彻底重启 Fcitx5 实例。
更多排查信息详见 docs/android-rime-no-candidates-debug.md
EOF

# 4. Create the zip
cd "$STAGING_DIR"
ZIP_OUTPUT="$BUILD_DIR/android-rime-fcitx5-userdata.zip"
rm -f "$ZIP_OUTPUT"
zip -r "$ZIP_OUTPUT" ./*

echo "Successfully generated $ZIP_OUTPUT"
echo "Zip contents:"
unzip -l "$ZIP_OUTPUT"
