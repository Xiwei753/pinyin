from __future__ import annotations

import json

from ime_core.dictionary import BaseDictionary
from ime_core.engine import ImeEngine


def write_base_dict(path):
    path.write_text(
        "nihao\t你好\t1000\n"
        "nihao\t你号\t100\n"
        "bad line\n"
        "broken\t坏行\tnot-a-number\n"
        "zhongguo\t中国\t1000\n",
        encoding="utf-8",
    )


def test_loads_base_dictionary(tmp_path):
    base_dict_path = tmp_path / "base_dict.txt"
    write_base_dict(base_dict_path)

    dictionary = BaseDictionary(base_dict_path)

    entries = dictionary.get("nihao")
    assert [entry.word for entry in entries] == ["你好", "你号"]


def test_nihao_returns_nihao_word(tmp_path):
    base_dict_path = tmp_path / "base_dict.txt"
    user_freq_path = tmp_path / "user_freq.json"
    write_base_dict(base_dict_path)

    engine = ImeEngine(base_dict_path, user_freq_path)

    words = [candidate.word for candidate in engine.candidates("nihao")]
    assert "你好" in words
    assert words[0] == "你好"


def test_select_saves_user_frequency(tmp_path):
    base_dict_path = tmp_path / "base_dict.txt"
    user_freq_path = tmp_path / "user_freq.json"
    write_base_dict(base_dict_path)
    engine = ImeEngine(base_dict_path, user_freq_path)

    engine.select("nihao", "你号")

    saved = json.loads(user_freq_path.read_text(encoding="utf-8"))
    assert saved == {"nihao": {"你号": 1}}


def test_user_frequency_persists_after_reload_and_affects_ranking(tmp_path):
    base_dict_path = tmp_path / "base_dict.txt"
    user_freq_path = tmp_path / "user_freq.json"
    write_base_dict(base_dict_path)
    engine = ImeEngine(base_dict_path, user_freq_path)
    engine.select("nihao", "你号")

    reloaded_engine = ImeEngine(base_dict_path, user_freq_path)

    candidates = reloaded_engine.candidates("nihao")
    assert candidates[0].word == "你号"
    assert candidates[0].user_freq == 1
