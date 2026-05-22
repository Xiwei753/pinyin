import os
from pathlib import Path
import pytest

from tools.dictionary.convert_rime_dict import default_corrections_path

def test_default_corrections_path():
    path = default_corrections_path()

    assert isinstance(path, Path)
    assert path.name == "pinyin_corrections.tsv"
    assert path.parent.name == "dictionary"
    assert path.exists(), f"Expected {path} to exist"
