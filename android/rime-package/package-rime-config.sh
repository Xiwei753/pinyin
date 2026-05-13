#!/bin/bash
# 将 rime 目录打包供 Android 输入法导入
echo "开始打包 Rime 配置文件..."
mkdir -p build/
zip -r build/android-rime-config.zip ../../rime/*
echo "打包完成。生成的文件：build/android-rime-config.zip"
echo "请将其导入至 Android 端支持 Rime 的输入法应用中。"
