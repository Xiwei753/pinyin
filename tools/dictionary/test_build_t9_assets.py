import os
import sys

# Add the directory containing build_t9_assets.py to the path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from build_t9_assets import repo_root

def test_repo_root():
    """Test that repo_root returns the absolute path to the repository root."""
    root_path = repo_root()

    # Verify it's an absolute path
    assert os.path.isabs(root_path), "repo_root must return an absolute path"

    # Verify it points to the actual repo root by checking for known files/directories
    # e.g., tools/dictionary/build_t9_assets.py should exist relative to it
    expected_script_path = os.path.join(root_path, "tools", "dictionary", "build_t9_assets.py")
    assert os.path.exists(expected_script_path), f"Expected script not found at {expected_script_path}"

    # Verify it points to the repo root by checking for the 'tests' or '.github' directory
    assert os.path.isdir(os.path.join(root_path, "tools")), f"Expected 'tools' directory not found in {root_path}"
    assert os.path.isdir(os.path.join(root_path, "frontends")), f"Expected 'frontends' directory not found in {root_path}"
