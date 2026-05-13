import os
import sys

def read_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    os.chdir(root)

    success = True

    # Check default.custom.yaml non-empty
    p = "shared/rime/default.custom.yaml"
    if not os.path.isfile(p) or os.path.getsize(p) == 0:
        print(f"Error: {p} is missing or empty.")
        success = False

    # Check xiwei_pinyin.schema.yaml non-empty
    p = "shared/rime/xiwei_pinyin.schema.yaml"
    if not os.path.isfile(p) or os.path.getsize(p) == 0:
        print(f"Error: {p} is missing or empty.")
        success = False

    # Check custom_phrase.txt exists
    p = "shared/rime/custom_phrase.txt"
    if not os.path.isfile(p):
        print(f"Error: {p} is missing.")
        success = False

    # Check xiwei_pinyin.dict.yaml has at least 10 entries
    p = "shared/rime/xiwei_pinyin.dict.yaml"
    try:
        content = read_file(p)
        lines = [line.strip() for line in content.split('\n') if line.strip() and not line.startswith('#')]
        # Ensure at least 10 lines appear after the "..." marker
        idx = -1
        for i, line in enumerate(lines):
            if line == "...":
                idx = i
                break
        if idx != -1 and len(lines) - idx - 1 >= 10:
            pass
        else:
            print(f"Error: {p} does not have at least 10 word entries.")
            success = False
    except Exception as e:
        print(f"Error reading {p}: {e}")
        success = False

    # Check xiwei_t9.schema.yaml has algebra rules
    p = "shared/rime/xiwei_t9.schema.yaml"
    try:
        content = read_file(p)
        if "algebra:" not in content or "xform" not in content:
            print(f"Error: {p} seems to be missing T9 algebra implementations.")
            success = False
    except Exception as e:
        print(f"Error reading {p}: {e}")
        success = False

    if success:
        print("Rime files validation passed.")
        sys.exit(0)
    else:
        print("Rime files validation failed.")
        sys.exit(1)

if __name__ == "__main__":
    main()
