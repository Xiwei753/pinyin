import zipfile
import os
import sys

def simulate_extract():
    project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    zip_path = os.path.join(project_root, 'build', 'android-rime-fcitx5-userdata.zip')

    if not os.path.exists(zip_path):
        print(f"Skipping extract simulation: {zip_path} not found.")
        return 0

    print("Simulating Fcitx5 Android zip extraction...")

    simulated_dirs = set()

    with zipfile.ZipFile(zip_path, 'r') as z:
        for entry in z.infolist():
            name = entry.filename

            if name.endswith('/'):
                # Simulate dir.mkdir() (not mkdirs!)
                parent_dir = os.path.dirname(name.rstrip('/'))
                if parent_dir and parent_dir + '/' not in simulated_dirs:
                    print(f"Error (ENOENT): Cannot mkdir '{name}' because parent '{parent_dir}/' does not exist yet.")
                    return 1
                simulated_dirs.add(name)
                #print(f"mkdir: {name}")
            else:
                # Simulate file.outputStream()
                parent_dir = os.path.dirname(name)
                if parent_dir and parent_dir + '/' not in simulated_dirs:
                    print(f"Error (ENOENT): Cannot write file '{name}' because parent directory '{parent_dir}/' does not exist yet.")
                    return 1
                #print(f"write: {name}")

    print("Extraction simulation passed. Zip structure is Fcitx5-safe.")
    return 0

if __name__ == '__main__':
    sys.exit(simulate_extract())
