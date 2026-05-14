import unittest
from t9_core import T9Core

class TestT9Core(unittest.TestCase):
    def setUp(self):
        self.core = T9Core()

    def test_input_and_candidates(self):
        # 模拟输入 64426 -> 你好
        self.core.input_digit("6")
        self.assertEqual(self.core.get_candidates(), [])

        self.core.input_digit("4")
        self.core.input_digit("4")
        self.core.input_digit("2")
        self.core.input_digit("6")
        self.assertEqual(self.core.get_candidates(), ["你好"])

        # 模拟输入 748732 -> 输入法
        self.core.clear()
        for d in "748732":
            self.core.input_digit(d)
        self.assertEqual(self.core.get_candidates(), ["输入法"])

    def test_backspace(self):
        # 输入 64426
        for d in "64426":
            self.core.input_digit(d)
        self.assertEqual(self.core.get_candidates(), ["你好"])

        # 删除一位
        self.core.backspace()
        self.assertEqual(self.core.buffer, "6442")
        self.assertEqual(self.core.get_candidates(), [])

        # 再补上 6
        self.core.input_digit("6")
        self.assertEqual(self.core.get_candidates(), ["你好"])

    def test_clear(self):
        for d in "748732":
            self.core.input_digit(d)
        self.assertEqual(self.core.buffer, "748732")

        self.core.clear()
        self.assertEqual(self.core.buffer, "")
        self.assertEqual(self.core.get_candidates(), [])

    def test_select_candidate(self):
        for d in "9466446":
            self.core.input_digit(d)
        self.assertEqual(self.core.get_candidates(), ["中国"])

        # 选择第一个候选
        result = self.core.select_candidate(0)
        self.assertEqual(result, "中国")

        # 选择后应该清空缓冲区
        self.assertEqual(self.core.buffer, "")
        self.assertEqual(self.core.get_candidates(), [])

    def test_invalid_input(self):
        # 测试非法输入被忽略
        self.core.input_digit("1")
        self.core.input_digit("0")
        self.core.input_digit("a")
        self.assertEqual(self.core.buffer, "")

if __name__ == '__main__':
    unittest.main()
