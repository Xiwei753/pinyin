class T9Core:
    def __init__(self):
        self.buffer = ""
        # 最小测试字典 (数字串 -> 候选词)
        self.dictionary = {
            "64426": ["你好"],
            "748732": ["输入法"],
            "746946": ["拼音"],
            "9466446": ["中国"],
            "866428": ["同步"]
        }

    def input_digit(self, digit: str):
        """输入数字（2-9）"""
        if digit in "23456789":
            self.buffer += digit

    def backspace(self):
        """删除最后一个数字"""
        if self.buffer:
            self.buffer = self.buffer[:-1]

    def clear(self):
        """清空输入缓冲区"""
        self.buffer = ""

    def get_candidates(self) -> list:
        """获取当前缓冲区的候选词"""
        if not self.buffer:
            return []

        # 精确匹配（最小原型暂不处理前缀匹配）
        return self.dictionary.get(self.buffer, [])

    def select_candidate(self, index: int) -> str:
        """选择候选词并返回要上屏的文本，随后清空缓冲区"""
        candidates = self.get_candidates()
        if 0 <= index < len(candidates):
            selected = candidates[index]
            self.clear()
            return selected
        return ""
