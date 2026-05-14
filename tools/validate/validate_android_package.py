#!/usr/bin/env python3
import os
import sys
import zipfile

def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
    generic_zip_path = os.path.join(repo_root, "build", "android-rime-generic.zip")
    trime_zip_path = os.path.join(repo_root, "build", "android-rime-trime.zip")

    if not os.path.exists(generic_zip_path) and not os.path.exists(trime_zip_path):
        print("未检测到 Android 打包产物，如需检查请先运行 package-generic-rime.sh 或 package-trime.sh。")
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

    if os.path.exists(generic_zip_path):
        print(f"检查 {generic_zip_path} ...")
        try:
            with zipfile.ZipFile(generic_zip_path, 'r') as z:
                contained_files = z.namelist()
                print("📦 Generic Zip 内文件列表:")
                for f in contained_files:
                    print(f"  - {f}")
                    if f.startswith("shared/") or f.startswith("rime/"):
                        print(f"❌ 错误：Generic zip 顶层结构错误，发现多余目录结构: {f}")
                        sys.exit(1)

                missing_files = []
                for rf in required_files:
                    if rf not in contained_files:
                        missing_files.append(rf)

                if missing_files:
                    print(f"❌ 错误：Generic Rime 打包文件中缺少必要文件: {', '.join(missing_files)}")
                    sys.exit(1)
            print("✅ Generic Rime 打包检查通过")
        except zipfile.BadZipFile:
            print("❌ 错误：Generic Rime 打包文件不是有效的 zip 格式。")
            sys.exit(1)

    if os.path.exists(trime_zip_path):
        print(f"检查 {trime_zip_path} ...")
        try:
            with zipfile.ZipFile(trime_zip_path, 'r') as z:
                contained_files = z.namelist()
                print("📦 Trime Zip 内文件列表:")
                for f in contained_files:
                    print(f"  - {f}")
                    if not f.startswith("rime/"):
                        print(f"❌ 错误：Trime zip 必须包含 rime/ 顶级目录，发现异常文件: {f}")
                        sys.exit(1)

                missing_files = []
                for rf in required_files:
                    trime_rf = f"rime/{rf}"
                    if trime_rf not in contained_files:
                        missing_files.append(trime_rf)

                if missing_files:
                    print(f"❌ 错误：Trime 打包文件中缺少必要文件: {', '.join(missing_files)}")
                    sys.exit(1)
            print("✅ Trime Rime 打包检查通过")
        except zipfile.BadZipFile:
            print("❌ 错误：Trime Rime 打包文件不是有效的 zip 格式。")
            sys.exit(1)

if __name__ == "__main__":
    main()
