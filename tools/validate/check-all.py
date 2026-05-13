import subprocess
import sys
import os

def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))

    scripts_to_run = [
        "tools/validate/validate_structure.py",
        "tools/validate/validate_rime_files.py"
    ]

    all_passed = True

    for script in scripts_to_run:
        script_path = os.path.join(repo_root, script)
        print(f"运行检查: {script}")
        try:
            result = subprocess.run([sys.executable, script_path], cwd=repo_root, check=True)
        except subprocess.CalledProcessError:
            print(f"❌ 检查失败: {script}")
            all_passed = False
            break

    if all_passed:
        print("\n✅ 基层检查通过")
        sys.exit(0)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
