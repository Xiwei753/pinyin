import os
import sys

def read_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    os.chdir(root)

    success = True

    # 1. Check xiwei_pinyin.schema.yaml
    schema_path = "shared/rime/xiwei_pinyin.schema.yaml"
    try:
        content = read_file(schema_path)
        if "dictionary: xiwei_pinyin" not in content:
            print(f"Error: {schema_path} is missing 'dictionary: xiwei_pinyin'")
            success = False
        if "- script_translator" not in content:
            print(f"Error: {schema_path} is missing '- script_translator'")
            success = False
        if "alphabet: abcdefghijklmnopqrstuvwxyz" not in content:
            print(f"Error: {schema_path} is missing 'alphabet: abcdefghijklmnopqrstuvwxyz'")
            success = False
    except Exception as e:
        print(f"Error reading {schema_path}: {e}")
        success = False

    # 2. Check xiwei_pinyin.dict.yaml
    dict_path = "shared/rime/xiwei_pinyin.dict.yaml"
    required_words = [
        "你\tni",
        "好\thao",
        "你好\tni hao",
        "输入法\tshu ru fa",
        "拼音\tpin yin",
        "中国\tzhong guo",
        "同步\ttong bu"
    ]
    try:
        content = read_file(dict_path)
        for word in required_words:
            if word not in content:
                print(f"Error: {dict_path} is missing '{word}'")
                success = False
    except Exception as e:
        print(f"Error reading {dict_path}: {e}")
        success = False

    # 3. Check default.yaml or default.custom.yaml contains xiwei_pinyin
    default_path = "shared/rime/default.yaml"
    default_custom_path = "shared/rime/default.custom.yaml"
    has_xiwei_pinyin = False

    try:
        if os.path.exists(default_path) and "xiwei_pinyin" in read_file(default_path):
            has_xiwei_pinyin = True
        elif os.path.exists(default_custom_path) and "xiwei_pinyin" in read_file(default_custom_path):
            has_xiwei_pinyin = True

        if not has_xiwei_pinyin:
            print(f"Error: Neither {default_path} nor {default_custom_path} contains 'xiwei_pinyin'")
            success = False
    except Exception as e:
        print(f"Error reading default configuration: {e}")
        success = False

    if success:
        print("Rime static smoke test passed.")
        sys.exit(0)
    else:
        print("Rime static smoke test failed.")
        sys.exit(1)

if __name__ == "__main__":
    main()
