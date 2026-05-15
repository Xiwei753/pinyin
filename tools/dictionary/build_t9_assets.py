import os
import subprocess
import sys

def main():
    root_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    input_file = os.path.join(root_dir, 'third_party', 'rime-ice', 'base.dict.yaml')
    output_file = os.path.join(root_dir, 'frontends', 'android-ime', 'native-app', 'android', 'app', 'src', 'main', 'assets', 't9_source_dict.tsv')
    convert_script = os.path.join(root_dir, 'tools', 'dictionary', 'convert_rime_dict.py')

    if not os.path.exists(input_file):
        print(f"Error: Input file not found at {input_file}")
        return 1

    os.makedirs(os.path.dirname(output_file), exist_ok=True)

    # Convert everything
    temp_output_file = output_file + ".tmp"
    cmd = [sys.executable, convert_script, input_file, temp_output_file]
    print(f"Running: {' '.join(cmd)}")
    result = subprocess.run(cmd)

    if result.returncode != 0:
        print(f"Error: Conversion script failed with code {result.returncode}")
        return result.returncode

    entries = []

    with open(temp_output_file, 'r', encoding='utf-8') as f:
        all_lines = f.readlines()

    for line in all_lines:
        parts = line.strip().split('\t')
        if len(parts) >= 3:
            word = parts[0]
            pinyin = parts[1]
            try:
                weight = int(parts[2])
            except:
                weight = 0

            # Filter bad typos explicitly
            if word == "安卓" and pinyin in ["tan zhuo", "tan zhao"]:
                continue

            entries.append((word, pinyin, weight, line))

    # We must ensure required words exist
    required_words = {"你好", "输入法", "中国", "今天", "手机", "电脑", "安卓"}
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
    os.remove(temp_output_file)

    return 0

if __name__ == '__main__':
    exit(main())
