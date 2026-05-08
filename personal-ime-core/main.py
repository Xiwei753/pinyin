"""Command-line prototype for the personal IME core."""

from __future__ import annotations

from pathlib import Path

from ime_core import ImeEngine


PROJECT_ROOT = Path(__file__).resolve().parent
BASE_DICT_PATH = PROJECT_ROOT / "data" / "base_dict.txt"
USER_FREQ_PATH = PROJECT_ROOT / "data" / "user_freq.json"


def main() -> None:
    engine = ImeEngine(BASE_DICT_PATH, USER_FREQ_PATH)

    print("Personal IME Core prototype")
    print("输入拼音查看候选词，输入 q 退出。")

    try:
        while True:
            pinyin = input("\n拼音> ").strip()
            if pinyin.lower() == "q":
                print("再见。")
                break
            if not pinyin:
                continue

            candidates = engine.candidates(pinyin)
            if not candidates:
                print("没有候选词。")
                continue

            for index, candidate in enumerate(candidates, start=1):
                print(f"{index}. {candidate.word} (score={candidate.score})")

            choice = input("选择编号（回车跳过，q 退出）> ").strip()
            if choice.lower() == "q":
                print("再见。")
                break
            if not choice:
                continue

            try:
                choice_index = int(choice)
            except ValueError:
                print("请输入有效编号。")
                continue

            if not 1 <= choice_index <= len(candidates):
                print("编号超出范围。")
                continue

            selected = candidates[choice_index - 1]
            engine.select(pinyin, selected.word)
            print(f"已选择：{selected.word}")
    except KeyboardInterrupt:
        print("\n再见。")


if __name__ == "__main__":
    main()
