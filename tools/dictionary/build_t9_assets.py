import os
import re
import sys
from dataclasses import dataclass
from typing import Dict, Iterable, List, Tuple

from convert_rime_dict import apply_pinyin_correction, load_pinyin_corrections, normalize_pinyin

MAX_OUTPUT_ENTRIES = 90000
BASE_MULTI_QUOTA = 62000
BASE_SINGLE_QUOTA = 12000
MIN_OUTPUT_ENTRIES = 30000
PINYIN_RE = re.compile(r"^[a-züv: ]+$")


@dataclass(frozen=True)
class Entry:
    text: str
    pinyin: str
    weight: int
    source: str


def repo_root() -> str:
    return os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def parse_rime_dict(path: str, source: str) -> Iterable[Entry]:
    header_ended = False
    with open(path, "r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            if not header_ended:
                if line == "...":
                    header_ended = True
                continue

            parts = line.split("\t")
            if len(parts) < 2:
                continue

            text = parts[0].strip()
            pinyin = normalize_pinyin(parts[1].strip())
            if not text or not pinyin or len(text) > 20 or not PINYIN_RE.match(pinyin):
                continue

            weight = 0
            if len(parts) >= 3:
                try:
                    weight = int(parts[2])
                except ValueError:
                    weight = 0

            yield Entry(text=text, pinyin=pinyin, weight=weight, source=source)


def selection_score(entry: Entry) -> Tuple[int, int, int, str, str]:
    text_len = len(entry.text)
    syllable_count = len(entry.pinyin.split())
    source_bonus = 300000 if entry.source == "base" else 0
    phrase_bonus = 0
    if text_len >= 2:
        phrase_bonus += 180000
        phrase_bonus += min(text_len, 6) * 45000
        phrase_bonus += min(syllable_count, 6) * 30000
    else:
        phrase_bonus += 40000

    # 以频率为主，同时用通用的词长/音节数/来源加权避免常用短语被纯行数截断挤掉。
    score = entry.weight + source_bonus + phrase_bonus
    return (score, entry.weight, text_len, entry.pinyin, entry.text)


def merge_entries(entries: Iterable[Entry], corrections: Dict[Tuple[str, str], str]) -> Dict[Tuple[str, str], Entry]:
    merged: Dict[Tuple[str, str], Entry] = {}
    for entry in entries:
        corrected_pinyin = apply_pinyin_correction(entry.text, entry.pinyin, corrections)
        if corrected_pinyin != entry.pinyin:
            entry = Entry(text=entry.text, pinyin=corrected_pinyin, weight=entry.weight, source=entry.source)

        key = (entry.text, entry.pinyin)
        previous = merged.get(key)
        if previous is None:
            merged[key] = entry
            continue

        if entry.weight > previous.weight:
            merged[key] = Entry(entry.text, entry.pinyin, entry.weight, previous.source if previous.source == "base" else entry.source)
        elif entry.source == "base" and previous.source != "base":
            merged[key] = Entry(previous.text, previous.pinyin, previous.weight, "base")
    return merged


def pick_entries(merged: Dict[Tuple[str, str], Entry]) -> List[Entry]:
    base_multi = [e for e in merged.values() if e.source == "base" and len(e.text) >= 2]
    base_single = [e for e in merged.values() if e.source == "base" and len(e.text) == 1]
    table_single = [e for e in merged.values() if e.source == "8105" and len(e.text) == 1]

    def sort_bucket(bucket: List[Entry]) -> List[Entry]:
        return sorted(bucket, key=selection_score, reverse=True)

    selected: Dict[Tuple[str, str], Entry] = {}

    for entry in sort_bucket(base_multi)[:BASE_MULTI_QUOTA]:
        selected[(entry.text, entry.pinyin)] = entry

    for entry in sort_bucket(base_single)[:BASE_SINGLE_QUOTA]:
        selected[(entry.text, entry.pinyin)] = entry

    # 8105 只承担单字候选表；全部保留，冷僻字依靠原始频率和引擎排序自然靠后。
    for entry in sort_bucket(table_single):
        key = (entry.text, entry.pinyin)
        previous = selected.get(key)
        if previous is None or entry.weight > previous.weight:
            selected[key] = entry

    if len(selected) > MAX_OUTPUT_ENTRIES:
        selected = dict(
            ((entry.text, entry.pinyin), entry)
            for entry in sorted(selected.values(), key=selection_score, reverse=True)[:MAX_OUTPUT_ENTRIES]
        )

    return sorted(selected.values(), key=lambda e: (e.pinyin, e.text, -e.weight))


def main() -> int:
    root_dir = repo_root()
    input_file_base = os.path.join(root_dir, "third_party", "rime-ice", "cn_dicts", "base.dict.yaml")
    input_file_8105 = os.path.join(root_dir, "third_party", "rime-ice", "cn_dicts", "8105.dict.yaml")
    output_file = os.path.join(root_dir, "frontends", "android-ime", "native-app", "android", "app", "src", "main", "assets", "t9_source_dict.tsv")

    for path in (input_file_base, input_file_8105):
        if not os.path.exists(path):
            print(f"Error: Input file not found at {path}")
            return 1

    os.makedirs(os.path.dirname(output_file), exist_ok=True)

    corrections = load_pinyin_corrections()

    entries: List[Entry] = []
    entries.extend(parse_rime_dict(input_file_base, "base"))
    entries.extend(e for e in parse_rime_dict(input_file_8105, "8105") if len(e.text) == 1)

    merged = merge_entries(entries, corrections)
    final_entries = pick_entries(merged)

    with open(output_file, "w", encoding="utf-8", newline="\n") as f:
        for entry in final_entries:
            f.write(f"{entry.text}\t{entry.pinyin}\t{entry.weight}\n")

    if len(final_entries) < MIN_OUTPUT_ENTRIES:
        print(
            f"Error: Generated dictionary has only {len(final_entries)} lines, "
            f"which is smaller than the required {MIN_OUTPUT_ENTRIES}."
        )
        return 1

    print(f"Successfully generated {output_file} with {len(final_entries)} lines.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
