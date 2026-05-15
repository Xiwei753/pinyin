import os

def test_third_party_license_exists():
    license_path = os.path.join("THIRD_PARTY_LICENSES", "rime-ice.md")
    assert os.path.exists(license_path), "Third-party license for rime-ice missing"

    with open(license_path, 'r', encoding='utf-8') as f:
        content = f.read()
        assert "rime-ice" in content
        assert "GPLv3" in content
        assert "https://github.com/iDvel/rime-ice" in content
