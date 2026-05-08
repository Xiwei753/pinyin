"""Base dictionary loading for the personal IME core."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class DictEntry:
    """One base dictionary entry."""

    pinyin: str
    word: str
    freq: int


class BaseDictionary:
    """Load and query a tab-separated base dictionary file."""

    def __init__(self, path: Path | str) -> None:
        self.path = Path(path)
        self._entries_by_pinyin: dict[str, list[DictEntry]] = {}
        self.load()

    def load(self) -> None:
        """Load dictionary entries, skipping malformed lines."""
        self._entries_by_pinyin.clear()

        if not self.path.exists():
            return

        with self.path.open("r", encoding="utf-8") as file:
            for line in file:
                parts = line.rstrip("\n").split("\t")
                if len(parts) != 3:
                    continue

                pinyin, word, freq_text = parts
                if not pinyin or not word:
                    continue

                try:
                    freq = int(freq_text)
                except ValueError:
                    continue

                entry = DictEntry(pinyin=pinyin, word=word, freq=freq)
                self._entries_by_pinyin.setdefault(pinyin, []).append(entry)

    def get(self, pinyin: str) -> list[DictEntry]:
        """Return entries for the exact pinyin input."""
        return list(self._entries_by_pinyin.get(pinyin, []))
