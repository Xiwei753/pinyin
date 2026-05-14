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
        if "dictionary: luna_pinyin" not in content:
            print(f"Error: {schema_path} is missing 'dictionary: luna_pinyin'")
            success = False
        if "- script_translator" not in content:
            print(f"Error: {schema_path} is missing '- script_translator'")
            success = False
        if "alphabet: abcdefghijklmnopqrstuvwxyz" not in content:
            print(f"Error: {schema_path} is missing 'alphabet: abcdefghijklmnopqrstuvwxyz'")
            success = False
        if "- simplifier" not in content:
            print(f"Error: {schema_path} is missing '- simplifier'")
            success = False
        if "- uniquifier" not in content:
            print(f"Error: {schema_path} is missing '- uniquifier'")
            success = False
        if "name: simplification" not in content:
            print(f"Error: {schema_path} is missing 'name: simplification'")
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
    traditional_words = [
        "妳好", "逆號", "擬好"
    ]
    try:
        content = read_file(dict_path)
        for word in required_words:
            if word not in content:
                print(f"Error: {dict_path} is missing '{word}'")
                success = False

        for word in traditional_words:
            if word in content:
                print(f"Error: {dict_path} incorrectly contains traditional word '{word}'")
                success = False

        if "import_tables:" not in content or "luna_pinyin" not in content:
            print(f"Error: {dict_path} is missing 'import_tables: [luna_pinyin]'")
            success = False
    except Exception as e:
        print(f"Error reading {dict_path}: {e}")
        success = False

    # 3. Check default.yaml or default.custom.yaml contains xiwei_pinyin and a debugging fallback schema
    default_path = "shared/rime/default.yaml"
    default_custom_path = "shared/rime/default.custom.yaml"
    has_xiwei_pinyin = False

    try:
        default_content = read_file(default_path) if os.path.exists(default_path) else ""
        default_custom_content = read_file(default_custom_path) if os.path.exists(default_custom_path) else ""

        if "xiwei_pinyin" in default_content or "xiwei_pinyin" in default_custom_content:
            has_xiwei_pinyin = True

        if not has_xiwei_pinyin:
            print(f"Error: Neither {default_path} nor {default_custom_path} contains 'xiwei_pinyin'")
            success = False

        has_luna_pinyin = "luna_pinyin" in default_content or "luna_pinyin" in default_custom_content
        has_luna_pinyin_simp = "luna_pinyin_simp" in default_content or "luna_pinyin_simp" in default_custom_content
        if not has_luna_pinyin and not has_luna_pinyin_simp:
            print(f"Error: Neither {default_path} nor {default_custom_path} contains 'luna_pinyin' or 'luna_pinyin_simp' as a debug fallback.")
            success = False

    except Exception as e:
        print(f"Error reading default configuration: {e}")
        success = False

    # 4. Check DEPLOY_CHECK.txt
    deploy_check_path = "shared/rime/DEPLOY_CHECK.txt"
    if not os.path.exists(deploy_check_path):
        print(f"Error: {deploy_check_path} is missing.")
        success = False

    # 5. Check private sync script and templates
    build_user_rime_path = "tools/private/build-user-rime.py"
    if not os.path.exists(build_user_rime_path):
        print(f"Error: {build_user_rime_path} is missing.")
        success = False

    private_template_dir = "shared/private-template"
    if not os.path.isdir(private_template_dir):
        print(f"Error: {private_template_dir} is missing.")
        success = False

    # 6. Check custom_phrase.txt format
    custom_phrase_path = "shared/rime/custom_phrase.txt"
    if os.path.exists(custom_phrase_path):
        try:
            with open(custom_phrase_path, 'r', encoding='utf-8') as f:
                content = f.read()
                if "狗屁通\tgpt" not in content:
                    print(f"Error: {custom_phrase_path} is missing '狗屁通\\tgpt'")
                    success = False
                if "希为拼音\tpinyin" not in content:
                    print(f"Error: {custom_phrase_path} is missing '希为拼音\\tpinyin'")
                    success = False
                if "gpt\t狗屁通" in content:
                    print(f"Error: {custom_phrase_path} contains old format 'gpt\\t狗屁通'")
                    success = False
                if "pinyin\t希为拼音" in content:
                    print(f"Error: {custom_phrase_path} contains old format 'pinyin\\t希为拼音'")
                    success = False

                # Check lines have 3 columns
                f.seek(0)
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#'):
                        parts = line.split('\t')
                        if len(parts) < 3:
                            print(f"Error: {custom_phrase_path} line does not have 3 columns: '{line}'")
                            success = False
        except Exception as e:
            print(f"Error checking {custom_phrase_path}: {e}")
            success = False
    else:
        print(f"Warning: {custom_phrase_path} is missing, skipping format check.")


    # Check that .private_sync is not committed (must be in .gitignore)
    try:
        gitignore_path = ".gitignore"
        if os.path.exists(gitignore_path):
            with open(gitignore_path, "r") as f:
                content = f.read()
                if ".private_sync" not in content and ".private_sync/" not in content:
                    print("Error: .private_sync must be in .gitignore")
                    success = False
    except Exception as e:
        print(f"Error checking .gitignore: {e}")
        success = False

    if success:
        print("Rime static smoke test passed.")
        sys.exit(0)
    else:
        print("Rime static smoke test failed.")
        sys.exit(1)

if __name__ == "__main__":
    main()
