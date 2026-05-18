# Project Rules

This repository is for a cross-platform Chinese input method. The current priority is the Android T9 Chinese IME.

The user is not a programmer and mainly tests APK behavior. Do not ask the user to make technical decisions when the codebase can answer the question. Read the code, make the change, run tests, and fix failures until the required commands pass.

## Stage: Android Native T9 Only

- The ONLY current development target is Android native T9 IME (Kotlin/Java InputMethodService + T9Engine).
- Do NOT propose, plan, or implement work on Linux, desktop, or any non-Android platform in the current stage.
- Do NOT read old Fcitx5 Android / Rime / Trime documentation to formulate tasks.
- Do NOT propose restoring Fcitx5 Android, Trime, or Rime configuration packages.
- Those routes are permanently removed from the repository and will not return.

## Linux Future Direction (Do Not Implement Now)

- Linux is NOT developed in the current stage.
- Future Linux direction: develop a fcitx5 plugin shell that wraps the project's shared core.
- fcitx5 acts only as the system input method framework/frontend shell; it will NOT use fcitx5-rime as the core engine.
- The shared core (T9Engine, dictionary, candidate logic) will power both Android and future Linux frontends.
- Do NOT add Linux plugin code, CI, or packaging in the current stage.

## rime-ice Role

- `third_party/rime-ice` is ONLY a dictionary data source, NOT an input engine.
- The project does NOT use the Rime engine.
- `base.dict.yaml` is the common word dictionary.
- `8105.dict.yaml` is only a single-character candidate table.
- `tools/dictionary/build_t9_assets.py` generates `assets/t9_source_dict.tsv` for the Android app.

## Core Android T9 Architecture Rules

- Correct pipeline: T9 digits -> pinyin syllable decoding -> pinyin preedit -> Chinese word/character lookup.
- Do not fall back to direct numeric-prefix lookup against Chinese dictionaries.
- Key `1` is the pinyin syllable separator / segmentation key.
- Key `0` commits the first candidate when candidates exist; when the buffer is empty, it inserts a space.
- The composing/preedit area should show pinyin, not raw digits as the main visible text.
- Candidate click must commit the current UI cached candidate, not recompute a fresh full candidate list.
- `base.dict.yaml` is the common word dictionary.
- `8105.dict.yaml` is only a single-character candidate table.
- Do not use `8105.dict.yaml` as the main word dictionary.
- Candidate list should prefer fewer good candidates over many garbage candidates.

## General Rules

- Do not add cloud sync.
- Do not add AI features.
- Do not add cloud dictionaries.
- Do not implement x86 Android special support.
- Do not only tune UI when the root cause is engine/dictionary logic.
- Do not only write documentation.
- Do not fake correctness by hardcoding the listed test cases.
- Do not hide failures. Fix compile/test failures until the required commands pass.

## Required Android Verification Command

```
cd frontends/android-ime/native-app/android
./gradlew test assembleDebug
```

If the command fails, debug and fix it without asking the user.
