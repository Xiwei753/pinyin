# personal-ime-core

A minimal personal Chinese pinyin IME core written in Python.

This project is primarily for personal use.

Provided as-is, without warranty.

Current stage: standalone IME core, no fcitx5 integration yet.

## What is implemented

- Load a base dictionary from `data/base_dict.txt`.
- Accept exact pinyin input such as `nihao`.
- Return candidate words sorted by base frequency plus weighted user frequency.
- Save selected candidate counts to `data/user_freq.json`.
- Reload user frequency on the next program start so previously selected words rank higher.
- Provide a simple command-line interaction in `main.py`.

## What is not implemented

- No fcitx5 integration.
- No GUI.
- No sync or cloud features.
- No fuzzy pinyin.
- No full-sentence input.

## Dictionary format

`data/base_dict.txt` uses one entry per line:

```text
pinyin<TAB>word<TAB>freq
```

Example:

```text
nihao	你好	1000
```

Malformed lines are skipped.

## Requirements

- Python 3.11+
- pytest for running tests

## Run the prototype

From this directory:

```bash
python main.py
```

Then enter pinyin, choose a candidate number, or enter `q` to quit.

## Run tests

From this directory:

```bash
python -m pytest
```

## Suggested next steps

1. Add more base dictionary entries.
2. Add tests for empty or invalid `user_freq.json`.
3. Add simple pinyin normalization if needed.
4. Later, design a thin adapter for fcitx5 integration after the standalone core is stable.
