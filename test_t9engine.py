import subprocess

def run_test():
    result = subprocess.run(
        ["./gradlew", "test", "--tests", "io.github.xiwei753.pinyin.t9.core.T9EngineTest"],
        cwd="frontends/android-ime/native-app/android",
        capture_output=True,
        text=True
    )
    print(result.stdout)
    print(result.stderr)

run_test()
