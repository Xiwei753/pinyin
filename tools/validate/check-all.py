import subprocess
import sys
import os

def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))

    scripts_to_run = [
        "tools/validate/validate_structure.py",
        "tools/validate/validate_rime_files.py",
        "tools/validate/validate_rime_smoke.py"
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

    # 检查 Android 打包产物 (如果存在)
    if all_passed:
        generic_zip_path = os.path.join(repo_root, "build", "android-rime-generic.zip")
        trime_zip_path = os.path.join(repo_root, "build", "android-rime-trime.zip")
        fcitx5_zip_path = os.path.join(repo_root, "build", "android-rime-fcitx5-userdata.zip")
        android_script = "tools/validate/validate_android_package.py"
        android_script_path = os.path.join(repo_root, android_script)
        fcitx5_script = "tools/validate/validate_fcitx5_userdata_package.py"
        fcitx5_script_path = os.path.join(repo_root, fcitx5_script)

        if os.path.exists(generic_zip_path) or os.path.exists(trime_zip_path):
            print(f"运行检查: {android_script}")
            try:
                subprocess.run([sys.executable, android_script_path], cwd=repo_root, check=True)
            except subprocess.CalledProcessError:
                print(f"❌ 检查失败: {android_script}")
                all_passed = False
        else:
            print("未检测到 Android 打包产物，如需检查请先运行 package-generic-rime.sh 或 package-trime.sh。")

        if os.path.exists(fcitx5_zip_path):
            print(f"运行检查: {fcitx5_script}")
            try:
                subprocess.run([sys.executable, fcitx5_script_path], cwd=repo_root, check=True)
            except subprocess.CalledProcessError:
                print(f"❌ 检查失败: {fcitx5_script}")
                all_passed = False
        else:
            print("未检测到 Fcitx5 UserData 打包产物，如需检查请先运行 package-fcitx5-userdata.sh。")

    if all_passed:
        print("\n✅ 基层检查通过")
        sys.exit(0)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
