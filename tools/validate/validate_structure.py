import os
import sys

def check_dir(path):
    if not os.path.isdir(path):
        print(f"Error: Directory '{path}' does not exist.")
        return False
    return True

def check_file(path):
    if not os.path.isfile(path):
        print(f"Error: File '{path}' does not exist.")
        return False
    return True

def check_not_exists(path):
    if os.path.exists(path):
        print(f"Error: Old directory '{path}' still exists.")
        return False
    return True

def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    os.chdir(root)

    success = True

    # Required directories
    req_dirs = [
        "shared/rime",
        "shared/settings",
        "frontends/linux-fcitx5",
        "frontends/android-ime",
        "tools/sync"
    ]
    for d in req_dirs:
        success &= check_dir(d)

    # Required files
    success &= check_file(".gitignore")

    # Old directories that shouldn't exist
    old_dirs = ["rime", "config", "core", "sync"]
    for d in old_dirs:
        success &= check_not_exists(d)

    if success:
        print("Structure validation passed.")
        sys.exit(0)
    else:
        print("Structure validation failed.")
        sys.exit(1)

if __name__ == "__main__":
    main()
