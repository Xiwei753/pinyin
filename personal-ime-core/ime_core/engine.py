"""Minimal pinyin IME engine."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from .dictionary import BaseDictionary
from .user_freq import UserFrequency


USER_FREQ_WEIGHT = 1000


@dataclass(frozen=True)
class Candidate:
    """A candidate word returned by the IME engine."""

    pinyin: str
    word: str
    base_freq: int
    user_freq: int
    score: int


class ImeEngine:
    """Combine base dictionary frequency and user frequency for candidates."""

    def __init__(self, base_dict_path: Path | str, user_freq_path: Path | str) -> None:
        self.dictionary = BaseDictionary(base_dict_path)
        self.user_frequency = UserFrequency(user_freq_path)

    def candidates(self, pinyin: str) -> list[Candidate]:
        """Return candidates sorted by base frequency plus weighted user frequency."""
        result: list[Candidate] = []

        for entry in self.dictionary.get(pinyin):
            user_freq = self.user_frequency.get(entry.pinyin, entry.word)
            score = entry.freq + user_freq * USER_FREQ_WEIGHT
            result.append(
                Candidate(
                    pinyin=entry.pinyin,
                    word=entry.word,
                    base_freq=entry.freq,
                    user_freq=user_freq,
                    score=score,
                )
            )

        return sorted(result, key=lambda item: (-item.score, -item.base_freq, item.word))

    def select(self, pinyin: str, word: str) -> None:
        """Record that the user selected a word for the given pinyin."""
        self.user_frequency.increment(pinyin, word)
