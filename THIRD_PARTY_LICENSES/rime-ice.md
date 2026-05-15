# rime-ice (雾凇拼音)

* **Dictionary Name**: rime-ice (雾凇拼音)
* **Upstream URL**: https://github.com/iDvel/rime-ice
* **Commit/Release**: `main` branch (as of download date)
* **License**: GPLv3
* **Original Files Used**: `cn_dicts/8105.dict.yaml` (converted and integrated into `t9_source_dict.tsv`)
* **Modified**: Yes, the data was parsed and reformatted, removing Rime-specific headers.
* **Modifications/Conversion**: Converted using `tools/dictionary/convert_rime_dict.py` to strip YAML metadata, default missing frequencies, handle specific pinyin typos (e.g. "安卓" mapped to "an zhuo"), and exported into a minimal `text<TAB>pinyin<TAB>weight` TSV format compatible with the T9 input core.

## Upstream License
This project uses the GPLv3 license. You can find the full license text in the [upstream repository](https://github.com/iDvel/rime-ice/blob/main/LICENSE).
