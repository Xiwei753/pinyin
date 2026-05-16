import os
import subprocess
import sys

def main():
    root_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    input_file_base = os.path.join(root_dir, 'third_party', 'rime-ice', 'base.dict.yaml')
    input_file_8105 = os.path.join(root_dir, 'third_party', 'rime-ice', '8105.dict.yaml')
    output_file = os.path.join(root_dir, 'frontends', 'android-ime', 'native-app', 'android', 'app', 'src', 'main', 'assets', 't9_source_dict.tsv')
    convert_script = os.path.join(root_dir, 'tools', 'dictionary', 'convert_rime_dict.py')

    if not os.path.exists(input_file_base):
        print(f"Error: Input file not found at {input_file_base}")
        return 1

    if not os.path.exists(input_file_8105):
        print(f"Error: Input file not found at {input_file_8105}")
        return 1

    os.makedirs(os.path.dirname(output_file), exist_ok=True)

    temp_output_base = output_file + ".base.tmp"
    cmd_base = [sys.executable, convert_script, input_file_base, temp_output_base]
    print(f"Running: {' '.join(cmd_base)}")
    result_base = subprocess.run(cmd_base)
    if result_base.returncode != 0:
        return result_base.returncode

    temp_output_8105 = output_file + ".8105.tmp"
    cmd_8105 = [sys.executable, convert_script, input_file_8105, temp_output_8105]
    print(f"Running: {' '.join(cmd_8105)}")
    result_8105 = subprocess.run(cmd_8105)
    if result_8105.returncode != 0:
        return result_8105.returncode

    merged_dict = {}

    def process_lines(filename, is_8105):
        with open(filename, 'r', encoding='utf-8') as f:
            for line in f:
                parts = line.strip().split('\t')
                if len(parts) >= 3:
                    word = parts[0]
                    pinyin = parts[1]
                    try:
                        weight = int(parts[2])
                    except:
                        weight = 0

                    if is_8105 and len(word) != 1:
                        continue

                    if word == "安卓" and pinyin in ["tan zhuo", "tan zhao"]:
                        continue

                    key = (word, pinyin)
                    if key in merged_dict:
                        merged_dict[key] = max(merged_dict[key], weight)
                    else:
                        merged_dict[key] = weight

    process_lines(temp_output_base, False)
    process_lines(temp_output_8105, True)

    entries = []
    for (word, pinyin), weight in merged_dict.items():
        line = f"{word}\t{pinyin}\t{weight}\n"
        entries.append((word, pinyin, weight, line))

    # We must ensure required words exist
    required_words = {"你好", "输入法", "中国", "今天", "手机", "电脑", "安卓", "啥时候"}
    required_entries = [e for e in entries if e[0] in required_words]

    # Remove required entries from main pool so we can add them back
    for req in required_entries:
        if req in entries:
            entries.remove(req)

    # Sort by weight descending
    entries.sort(key=lambda x: x[2], reverse=True)

    # Take top up to 49000 minus required entries length
    top_entries = entries[:49000 - len(required_entries)]

    # Add required entries back
    top_entries.extend(required_entries)

    # Sort them alphabetically by pinyin to maintain dictionary sorted property if needed
    top_entries.sort(key=lambda x: x[1])

    # Write to final file
    final_lines = [entry[3] for entry in top_entries]

    with open(output_file, 'w', encoding='utf-8') as f:
         f.writelines(final_lines)

    print(f"Successfully generated {output_file} with {len(final_lines)} lines.")
    os.remove(temp_output_base)
    os.remove(temp_output_8105)

    return 0

if __name__ == '__main__':
    exit(main())
