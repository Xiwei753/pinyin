import argparse
from pathlib import Path
from typing import Dict, Optional, Tuple

CorrectionMap = Dict[Tuple[str, str], str]


def default_corrections_path() -> Path:
    return Path(__file__).with_name("pinyin_corrections.tsv")


def load_pinyin_corrections(path: Optional[str] = None) -> CorrectionMap:
    correction_path = Path(path) if path else default_corrections_path()
    corrections: CorrectionMap = {}
    if not correction_path.exists():
        return corrections

    with correction_path.open("r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            if len(parts) != 3:
                raise ValueError(f"Invalid pinyin correction line in {correction_path}: {raw.rstrip()}")
            text, input_pinyin, corrected_pinyin = (part.strip() for part in parts)
            if not text or not input_pinyin or not corrected_pinyin:
                raise ValueError(f"Invalid empty pinyin correction field in {correction_path}: {raw.rstrip()}")
            corrections[(text, normalize_pinyin(input_pinyin))] = normalize_pinyin(corrected_pinyin)
    return corrections


def normalize_pinyin(pinyin: str) -> str:
    return " ".join(pinyin.lower().replace("u:", "ü").split())


def apply_pinyin_correction(text: str, pinyin: str, corrections: CorrectionMap) -> str:
    normalized = normalize_pinyin(pinyin)
    return corrections.get((text, normalized), normalized)


def convert_dict(input_file, output_file, max_lines=None, default_weight=0, corrections_path: Optional[str] = None):
    lines_written = 0
    header_ended = False
    corrections = load_pinyin_corrections(corrections_path)

    with open(input_file, 'r', encoding='utf-8') as f_in, open(output_file, 'w', encoding='utf-8') as f_out:
        for line in f_in:
            line = line.strip()

            # Skip empty lines and comments
            if not line or line.startswith('#'):
                continue

            # Skip YAML header until `...`
            if not header_ended:
                if line == '...':
                    header_ended = True
                continue

            # Parse `text<TAB>pinyin<TAB>weight`
            parts = line.split('\t')

            if len(parts) >= 2:
                text = parts[0]
                pinyin = apply_pinyin_correction(text, parts[1], corrections)

                # Check for extremely long words
                if len(text) > 20:
                    continue

                weight = default_weight
                if len(parts) >= 3:
                    try:
                        weight = int(parts[2])
                    except ValueError:
                        pass # use default_weight

                f_out.write(f"{text}\t{pinyin}\t{weight}\n")
                lines_written += 1

                if max_lines and lines_written >= max_lines:
                    break

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert Rime dictionary to TSV format")
    parser.add_argument("input_file", help="Input Rime .dict.yaml file")
    parser.add_argument("output_file", help="Output TSV file")
    parser.add_argument("--max-lines", type=int, help="Maximum number of lines to output")
    parser.add_argument("--default-weight", type=int, default=100, help="Default weight for words missing it")
    parser.add_argument("--corrections", help="Optional TSV file with text, input pinyin and corrected pinyin columns")

    args = parser.parse_args()

    convert_dict(args.input_file, args.output_file, args.max_lines, args.default_weight, args.corrections)
    print(f"Conversion complete. Output written to {args.output_file}")
