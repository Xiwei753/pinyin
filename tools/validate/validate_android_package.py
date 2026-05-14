#!/usr/bin/env python3
import os
import sys
import zipfile

def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
    zip_path = os.path.join(repo_root, "build", "android-rime-config.zip")

    if not os.path.exists(zip_path):
        print("未检测到 Android 打包产物，如需检查请先运行 package-rime-config.sh。")
        sys.exit(0)

    required_files = [
        "default.custom.yaml",
        "xiwei_pinyin.schema.yaml",
        "xiwei_t9.schema.yaml",
        "xiwei_pinyin.dict.yaml",
        "custom_phrase.txt",
        "symbols.yaml",
        "README.md"
    ]

    try:
        with zipfile.ZipFile(zip_path, 'r') as z:
            contained_files = z.namelist()
            missing_files = []
            for rf in required_files:
                if rf not in contained_files:
                    missing_files.append(rf)

            if missing_files:
                print(f"❌ 错误：Android Rime 打包文件中缺少必要文件: {', '.join(missing_files)}")
                sys.exit(1)

    except zipfile.BadZipFile:
        print("❌ 错误：Android Rime 打包文件不是有效的 zip 格式。")
        sys.exit(1)

    print("✅ Android Rime 打包检查通过")

if __name__ == "__main__":
    main()
