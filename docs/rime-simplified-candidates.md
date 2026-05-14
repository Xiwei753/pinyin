# Rime Simplified Candidates Phase

## Current Status
Currently, Android Rime has successfully completed the deployment phase. The artifacts are generating correctly, the schema (`xiwei_pinyin`) can be successfully deployed via Fcitx5 Android, and typing pinyin generates valid candidates.

However, during initial testing, typing words like `nihao` produced mixed traditional and simplified candidates (e.g., "妳好", "逆號", "擬好") in the top results instead of solely prioritizing the simplified "你好".

## Root Cause
To ensure that Rime schemas correctly deployed and rendered candidates during our smoke test, `xiwei_pinyin` was explicitly configured to use the built-in dictionary `luna_pinyin` (which inherently contains both traditional and simplified words, leaning towards traditional variations for certain characters if unfiltered).

There was initially no simplification filter applied to the engine's translation process in `xiwei_pinyin`.

## Solution
To make "希为拼音" a simplified-priority input method:
1. We modified `xiwei_pinyin.schema.yaml` to include the `simplifier` and `uniquifier` filters in the `engine/filters` section.
2. We added a `simplification` switch and defaulted its state to simplified Chinese (`reset: 1`, `states: [ 漢字, 汉字 ]`).

## Testing Goals for this Phase
1. This phase is intended to ensure that **Simplified Chinese candidates are prioritized**.
2. Typing `nihao` should yield "你好" as the first candidate.
3. Typing `shurufa` should yield "输入法" as the first candidate.
4. (Note: We do not require completely wiping out all traditional characters from later candidates yet; the primary goal is ensuring the correct simplified forms are at the top).
