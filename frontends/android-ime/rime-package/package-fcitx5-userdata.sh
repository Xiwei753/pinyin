#!/bin/bash
# Generates Fcitx5 Android User Data import package

set -e

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
TIMESTAMP=$(date +%s%3N)
cat <<JSON > "$STAGING_DIR/metadata.json"
{
  "packageName": "org.fcitx.fcitx5.android",
  "versionCode": 0,
  "versionName": "1.0.0",
  "timestamp": $TIMESTAMP
}
JSON

# 2. Create the target external directory structure
# Fcitx5 maps external/ to getExternalFilesDir(null)
# rimeengine.cpp uses PkgData/rime, and native-lib.cpp maps PkgData to extData/data
# Thus the path inside the zip is external/data/rime/
TARGET_DIR="$STAGING_DIR/external/data/rime"
mkdir -p "$TARGET_DIR"

# 3. Copy Rime files
cp -r "$RIME_SOURCE_DIR"/* "$TARGET_DIR/"

# 4. Create the zip
cd "$STAGING_DIR"
ZIP_OUTPUT="$BUILD_DIR/android-rime-fcitx5-userdata.zip"
rm -f "$ZIP_OUTPUT"
zip -r "$ZIP_OUTPUT" ./*

echo "Successfully generated $ZIP_OUTPUT"
