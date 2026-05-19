import argparse
import os
import re
import sys
import sqlite3
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

def parse_tsv_phrases(path: str, source: str) -> Iterable[Entry]:
    with open(path, "r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#"):
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
    source_bonus = 300000 if entry.source in ("base", "common") else 0
    phrase_bonus = 0
    if text_len >= 2:
        phrase_bonus += 180000
        phrase_bonus += min(text_len, 6) * 45000
        phrase_bonus += min(syllable_count, 6) * 30000
    else:
        phrase_bonus += 40000

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

        if entry.source == "common":
            merged[key] = entry
            continue
        if previous.source == "common":
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
    common_multi = [e for e in merged.values() if e.source == "common" and len(e.text) >= 2]
    ext_multi = [e for e in merged.values() if e.source == "ext" and len(e.text) >= 2]

    def sort_bucket(bucket: List[Entry]) -> List[Entry]:
        return sorted(bucket, key=selection_score, reverse=True)

    selected: Dict[Tuple[str, str], Entry] = {}

    for entry in sort_bucket(base_multi)[:BASE_MULTI_QUOTA]:
        selected[(entry.text, entry.pinyin)] = entry

    for entry in sort_bucket(base_single)[:BASE_SINGLE_QUOTA]:
        selected[(entry.text, entry.pinyin)] = entry

    for entry in sort_bucket(table_single):
        key = (entry.text, entry.pinyin)
        previous = selected.get(key)
        if previous is None or entry.weight > previous.weight:
            selected[key] = entry

    for entry in sort_bucket(common_multi):
        key = (entry.text, entry.pinyin)
        previous = selected.get(key)
        if previous is None or entry.weight > previous.weight:
            selected[key] = entry

    for entry in sort_bucket(ext_multi)[:MAX_OUTPUT_ENTRIES - len(selected)]:
        key = (entry.text, entry.pinyin)
        previous = selected.get(key)
        if previous is None or entry.weight > previous.weight:
            selected[key] = entry

    if len(selected) > MAX_OUTPUT_ENTRIES:
        selected = dict(
            ((entry.text, entry.pinyin), entry)
            for entry in sorted(selected.values(), key=selection_score, reverse=True)[:MAX_OUTPUT_ENTRIES]
        )

    return sorted(selected.values(), key=lambda e: (e.pinyin, e.text, -e.weight, e.source))

def get_t9_code(pinyin: str) -> str:
    mapping = {
        'a': '2', 'b': '2', 'c': '2',
        'd': '3', 'e': '3', 'f': '3',
        'g': '4', 'h': '4', 'i': '4',
        'j': '5', 'k': '5', 'l': '5',
        'm': '6', 'n': '6', 'o': '6',
        'p': '7', 'q': '7', 'r': '7', 's': '7',
        't': '8', 'u': '8', 'v': '8',
        'w': '9', 'x': '9', 'y': '9', 'z': '9'
    }
    return "".join(mapping.get(c, "") for c in pinyin)

def get_candidate_type(text: str, score: int) -> str:
    # Based on BuiltinDictionary.kt logic
    length = len(text)
    if length >= 4:
        c_type = "LONG_OR_LOW_FREQ"
    elif length == 3 and score < 30000:
        c_type = "LONG_OR_LOW_FREQ"
    elif length == 3:
        c_type = "NORMAL"
    elif length == 2 and score > 40000:
        c_type = "COMMON_SHORT"
    elif length == 2:
        c_type = "NORMAL"
    elif length == 1 and score > 60000:
        c_type = "COMMON_SHORT"
    elif length == 1:
        c_type = "SINGLE_CHAR"
    else:
        c_type = "NORMAL"

    if c_type != "LONG_OR_LOW_FREQ" and score < 5000:
        return "LONG_OR_LOW_FREQ"
    return c_type

def get_candidate_origin(text: str) -> str:
    if len(text) == 1:
        return "EXACT_SINGLE"
    return "EXACT_PHRASE"

def main() -> int:
    parser = argparse.ArgumentParser(description="Build T9 dictionary assets from Rime dictionary sources")
    parser.add_argument("--out-dir", default=None, help="Output directory for generated assets")
    args = parser.parse_args()

    root_dir = repo_root()
    input_file_base = os.path.join(root_dir, "third_party", "rime-ice", "cn_dicts", "base.dict.yaml")
    input_file_8105 = os.path.join(root_dir, "third_party", "rime-ice", "cn_dicts", "8105.dict.yaml")
    input_file_ext = os.path.join(root_dir, "third_party", "rime-ice", "cn_dicts", "ext.dict.yaml")
    input_file_common = os.path.join(root_dir, "tools", "dictionary", "android_common_phrases.tsv")

    if args.out_dir:
        output_dir = args.out_dir
    else:
        output_dir = os.path.join(root_dir, "frontends", "android-ime", "native-app", "android", "app", "src", "main", "assets")
    output_file = os.path.join(output_dir, "t9_source_dict.tsv")
    output_db_file = os.path.join(output_dir, "t9_dict.db")

    for path in (input_file_base, input_file_8105):
        if not os.path.exists(path):
            print(f"Error: Input file not found at {path}")
            return 1

    os.makedirs(output_dir, exist_ok=True)

    corrections = load_pinyin_corrections()

    entries: List[Entry] = []
    entries.extend(parse_rime_dict(input_file_base, "base"))
    entries.extend(e for e in parse_rime_dict(input_file_8105, "8105") if len(e.text) == 1)
    if os.path.exists(input_file_ext):
        entries.extend(parse_rime_dict(input_file_ext, "ext"))
    if os.path.exists(input_file_common):
        entries.extend(parse_tsv_phrases(input_file_common, "common"))

    merged = merge_entries(entries, corrections)
    final_entries = pick_entries(merged)

    with open(output_file, "w", encoding="utf-8", newline="\n") as f:
        for entry in final_entries:
            f.write(f"{entry.text}\t{entry.pinyin}\t{entry.weight}\n")

    if os.path.exists(output_db_file):
        os.remove(output_db_file)

    conn = sqlite3.connect(output_db_file)
    cursor = conn.cursor()

    cursor.execute("""
        CREATE TABLE entries (
            id INTEGER PRIMARY KEY,
            text TEXT NOT NULL,
            pinyin TEXT NOT NULL,
            syllable TEXT,
            code TEXT NOT NULL,
            score INTEGER NOT NULL,
            type TEXT NOT NULL,
            origin TEXT NOT NULL,
            source TEXT
        )
    """)

    db_entries = []
    for entry in final_entries:
        code = get_t9_code(entry.pinyin)
        c_type = get_candidate_type(entry.text, entry.weight)
        c_origin = get_candidate_origin(entry.text)
        syllables = entry.pinyin.split()
        single_syllable = syllables[0] if len(syllables) == 1 else None

        db_entries.append((
            entry.text,
            entry.pinyin,
            single_syllable,
            code,
            entry.weight,
            c_type,
            c_origin,
            entry.source
        ))

    cursor.executemany("""
        INSERT INTO entries (text, pinyin, syllable, code, score, type, origin, source)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, db_entries)

    cursor.execute("CREATE INDEX idx_pinyin ON entries (pinyin)")
    cursor.execute("CREATE INDEX idx_syllable ON entries (syllable)")
    cursor.execute("CREATE INDEX idx_code ON entries (code)")
    cursor.execute("CREATE INDEX idx_text ON entries (text)")
    cursor.execute("CREATE INDEX idx_code_score ON entries (code, score)")

    conn.commit()
    conn.close()

    if len(final_entries) < MIN_OUTPUT_ENTRIES:
        print(
            f"Error: Generated dictionary has only {len(final_entries)} lines, "
            f"which is smaller than the required {MIN_OUTPUT_ENTRIES}."
        )
        return 1

    print(f"Successfully generated {output_file} and {output_db_file} with {len(final_entries)} entries.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
