import sys
import re
import argparse

def convert_dict(input_file, output_file, max_lines=None, default_weight=0):
    lines_written = 0
    header_ended = False

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
                pinyin = parts[1]

                # Check for extremely long words
                if len(text) > 20:
                    continue

                # Fix specific pinyin error if found
                if text == "安卓":
                    pinyin = "an zhuo"

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

    args = parser.parse_args()

    convert_dict(args.input_file, args.output_file, args.max_lines, args.default_weight)
    print(f"Conversion complete. Output written to {args.output_file}")
