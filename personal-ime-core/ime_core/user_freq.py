"""Persistent user frequency storage."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


class UserFrequency:
    """Store how often a user selected a word for a pinyin input."""

    def __init__(self, path: Path | str) -> None:
        self.path = Path(path)
        self._data: dict[str, dict[str, int]] = {}
        self.load()

    def load(self) -> None:
        """Load user frequency data. Missing, empty, or invalid files become empty."""
        self._data = {}

        if not self.path.exists() or self.path.stat().st_size == 0:
            return

        try:
            raw_data: Any = json.loads(self.path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            return

        if not isinstance(raw_data, dict):
            return

        for pinyin, words in raw_data.items():
            if not isinstance(pinyin, str) or not isinstance(words, dict):
                continue

            clean_words: dict[str, int] = {}
            for word, count in words.items():
                if isinstance(word, str) and isinstance(count, int) and count > 0:
                    clean_words[word] = count

            if clean_words:
                self._data[pinyin] = clean_words

    def get(self, pinyin: str, word: str) -> int:
        """Return the saved selection count for a pinyin/word pair."""
        return self._data.get(pinyin, {}).get(word, 0)

    def increment(self, pinyin: str, word: str) -> None:
        """Increment and save the selection count for a pinyin/word pair."""
        self._data.setdefault(pinyin, {})[word] = self.get(pinyin, word) + 1
        self.save()

    def save(self) -> None:
        """Write user frequency data to disk."""
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.path.write_text(
            json.dumps(self._data, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
