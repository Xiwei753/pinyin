from tools.dictionary.convert_rime_dict import convert_dict, load_pinyin_corrections


def test_convert_dict_format(tmp_path):
    input_file = tmp_path / "test.dict.yaml"
    output_file = tmp_path / "test.tsv"

    input_file.write_text("""# Rime dict
name: test
version: "1"
...
你好	ni hao	1000
测试	ce shi
长词语会被过滤掉超过二十个字符的话就是这样	chang
安卓	an zhao	500
""", encoding="utf-8")

    convert_dict(str(input_file), str(output_file), max_lines=10, default_weight=50)

    lines = output_file.read_text(encoding="utf-8").strip().split('\n')
    assert len(lines) == 3
    assert lines[0] == "你好\tni hao\t1000"
    assert lines[1] == "测试\tce shi\t50"  # default weight applied
    assert lines[2] == "安卓\tan zhuo\t500"  # pinyin correction loaded from data file


def test_convert_dict_uses_data_driven_pinyin_corrections(tmp_path):
    correction_file = tmp_path / "corrections.tsv"
    correction_file.write_text("# text\tinput_pinyin\tcorrected_pinyin\n甲\tjia\tyi\n", encoding="utf-8")

    input_file = tmp_path / "test.dict.yaml"
    output_file = tmp_path / "test.tsv"
    input_file.write_text("""---
...
甲	jia	200
乙	yi	100
""", encoding="utf-8")

    convert_dict(str(input_file), str(output_file), corrections_path=str(correction_file))

    assert output_file.read_text(encoding="utf-8").strip().split('\n') == [
        "甲\tyi\t200",
        "乙\tyi\t100",
    ]
    assert load_pinyin_corrections(str(correction_file)) == {("甲", "jia"): "yi"}


def test_convert_dict_limits(tmp_path):
    input_file = tmp_path / "test.dict.yaml"
    output_file = tmp_path / "test.tsv"

    content = "---\n...\n" + "\n".join([f"词{i}\tci\t100" for i in range(10)])
    input_file.write_text(content, encoding="utf-8")

    convert_dict(str(input_file), str(output_file), max_lines=5)

    lines = output_file.read_text(encoding="utf-8").strip().split('\n')
    assert len(lines) == 5
