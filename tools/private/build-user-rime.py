import os
import sys
import shutil

def main():
    print("=== 开始构建用户层 Rime 数据 ===")

    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
    private_sync_dir = os.path.join(repo_root, ".private_sync")
    template_dir = os.path.join(repo_root, "shared", "private-template")
    rime_dir = os.path.join(repo_root, "shared", "rime")

    # 决定读取的来源目录
    source_dir = private_sync_dir
    if not os.path.exists(private_sync_dir):
        print(f"提示: 未找到私人数据目录 {private_sync_dir}")
        print(f"提示: 将使用模板目录 {template_dir} 进行 Dry-run 生成测试。")
        source_dir = template_dir

    # 处理 custom-phrases.txt
    custom_phrases_src = os.path.join(source_dir, "custom-phrases.txt")
    custom_phrases_dest = os.path.join(rime_dir, "custom_phrase.txt")

    if os.path.exists(custom_phrases_src):
        try:
            print(f"正在读取 {custom_phrases_src}...")
            # 可以加入格式检查等逻辑
            with open(custom_phrases_src, 'r', encoding='utf-8') as f:
                content = f.read()

            with open(custom_phrases_dest, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"成功生成自定义短语文件: {custom_phrases_dest}")
        except Exception as e:
            print(f"错误: 处理 custom-phrases.txt 时出错: {e}")
            if isinstance(e, PermissionError):
                print("可能是文件权限问题。")
    else:
        print(f"警告: 未找到 {custom_phrases_src}，跳过自定义短语生成。")

    # TODO: 后续可以考虑解析 user-dictionary.yaml 生成 user_dict
    user_dict_src = os.path.join(source_dir, "user-dictionary.yaml")
    if os.path.exists(user_dict_src):
        print(f"提示: 发现 {user_dict_src}，目前采用 custom_phrase 机制处理高频短语，用户词库 YAML 暂不转换为 Rime dict 格式以保稳定。")

    print("=== 用户层 Rime 数据构建完成 ===")

if __name__ == "__main__":
    main()
