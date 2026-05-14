import zipfile
import json
import os
import sys

def validate():
    project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    zip_path = os.path.join(project_root, 'build', 'android-rime-fcitx5-userdata.zip')

    if not os.path.exists(zip_path):
        # We don't fail if it hasn't been built yet, we just print a message
        print(f"Skipping fcitx5 userdata package validation: {zip_path} not found.")
        return 0

    with zipfile.ZipFile(zip_path, 'r') as z:
        files = z.namelist()

        if 'metadata.json' not in files:
            print("Error: metadata.json not found in the fcitx5 userdata zip.")
            return 1

        with z.open('metadata.json') as f:
            metadata = json.load(f)

            if 'exportTime' not in metadata:
                if 'timestamp' in metadata:
                    print("Error: metadata.json uses 'timestamp' but 'exportTime' is missing.")
                else:
                    print("Error: metadata.json must contain 'exportTime'.")
                return 1

            if metadata.get('packageName') != 'org.fcitx.fcitx5.android':
                print(f"Error: Invalid packageName in metadata.json: {metadata.get('packageName')}")
                return 1

        if not any(f.startswith('external/') for f in files):
            print("Error: 'external/' directory not found in the zip.")
            return 1

        rime_files = [f for f in files if f.startswith('external/data/rime/')]
        if not rime_files:
            print("Error: Rime configuration directory 'external/data/rime/' not found or is empty in the zip.")
            return 1

        # Check for essential Rime files
        essential_files = [
            'external/data/rime/default.custom.yaml',
            'external/data/rime/xiwei_pinyin.schema.yaml',
            'external/data/rime/xiwei_t9.schema.yaml',
            'external/data/rime/xiwei_pinyin.dict.yaml',
            'external/data/rime/custom_phrase.txt',
            'external/data/rime/symbols.yaml'
        ]

        for required_file in essential_files:
            if required_file not in files:
                print(f"Error: Essential Rime file {required_file} is missing from the package.")
                return 1

    print("Fcitx5 Android UserData package is valid.")
    return 0

if __name__ == '__main__':
    sys.exit(validate())
